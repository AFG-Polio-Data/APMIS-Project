package de.symeda.sormas.backend.campaign.data;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.imageio.ImageIO;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import de.symeda.sormas.api.campaign.data.CampaignFormImageValidationDto;

@Stateless
@LocalBean
public class YoloImageValidationService {

	private static final Logger logger = LoggerFactory.getLogger(YoloImageValidationService.class);

	private static final int INPUT_WIDTH = 640;
	private static final int INPUT_HEIGHT = 640;
	private static final float CONFIDENCE_THRESHOLD = 0.50f;
	private static final String DEFAULT_MODEL_RESOURCE = "/models/yolov8n.onnx";

	private static final String[] COCO_CLASSES = new String[] {
			"person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat", "traffic light",
			"fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog", "horse", "sheep", "cow",
			"elephant", "bear", "zebra", "giraffe", "backpack", "umbrella", "handbag", "tie", "suitcase", "frisbee",
			"skis", "snowboard", "sports ball", "kite", "baseball bat", "baseball glove", "skateboard", "surfboard",
			"tennis racket", "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple",
			"sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
			"potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote", "keyboard", "cell phone",
			"microwave", "oven", "toaster", "sink", "refrigerator", "book", "clock", "vase", "scissors", "teddy bear",
			"hair drier", "toothbrush"
	};

	private static final Set<Integer> PROHIBITED_CLASS_INDICES = Set.of(
			0,  // person / face / selfie
			14, // bird
			15, // cat
			16, // dog
			17, // horse
			18, // sheep
			19, // cow
			20, // elephant
			21, // bear
			22, // zebra
			23  // giraffe
	);

	private static volatile OrtEnvironment environment;
	private static volatile OrtSession session;
	private static volatile String inputTensorName;
	private static volatile boolean modelLoaded = false;
	private static final Object INIT_LOCK = new Object();

	@PostConstruct
	public void init() {
		ensureModelLoaded();
	}

	public boolean isModelReady() {
		ensureModelLoaded();
		return modelLoaded;
	}

	private void ensureModelLoaded() {
		if (modelLoaded) {
			return;
		}

		synchronized (INIT_LOCK) {
			if (modelLoaded) {
				return;
			}

			try {
				if (environment == null) {
					environment = OrtEnvironment.getEnvironment("YoloImageValidation");
				}

				Path modelPath = resolveModelPath();
				if (modelPath == null || !Files.exists(modelPath)) {
					logger.warn("YOLOv8 ONNX model not found. Face and animal detection will be bypassed until yolov8n.onnx is provided.");
					return;
				}

				OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();
				session = environment.createSession(modelPath.toString(), sessionOptions);
				inputTensorName = session.getInputNames().iterator().next();
				modelLoaded = true;
				logger.info("YOLOv8 ONNX model successfully loaded from: {}", modelPath);
			} catch (Exception ex) {
				logger.error("Failed to initialize YOLOv8 ONNX model session: {}", ex.getMessage(), ex);
			}
		}
	}

	private Path resolveModelPath() {
		String customPath = System.getProperty("yolo.model.path");
		if (StringUtils.isNotBlank(customPath)) {
			Path p = Path.of(customPath.trim());
			if (Files.exists(p)) {
				return p;
			}
		}

		try (InputStream in = getClass().getResourceAsStream(DEFAULT_MODEL_RESOURCE)) {
			if (in != null) {
				Path tempModel = Files.createTempFile("yolov8n-", ".onnx");
				tempModel.toFile().deleteOnExit();
				Files.copy(in, tempModel, StandardCopyOption.REPLACE_EXISTING);
				return tempModel;
			}
		} catch (Exception ex) {
			logger.warn("Could not read embedded model resource {}: {}", DEFAULT_MODEL_RESOURCE, ex.getMessage());
		}

		File fallbackFile = new File("models/yolov8n.onnx");
		if (fallbackFile.exists()) {
			return fallbackFile.toPath();
		}

		return null;
	}

	public CampaignFormImageValidationDto validate(byte[] imageBytes) {
		if (imageBytes == null || imageBytes.length == 0) {
			return CampaignFormImageValidationDto.valid();
		}

		if (!isModelReady()) {
			logger.debug("YOLOv8 model is not loaded. Skipping content validation.");
			return CampaignFormImageValidationDto.valid();
		}

		try {
			BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
			if (original == null) {
				return CampaignFormImageValidationDto.valid();
			}

			float[] inputBuffer = preprocessImage(original);

			OnnxTensor inputTensor = null;
			OrtSession.Result result = null;

			try {
				long[] shape = new long[] { 1, 3, INPUT_HEIGHT, INPUT_WIDTH };
				inputTensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(inputBuffer), shape);
				result = session.run(Collections.singletonMap(inputTensorName, inputTensor));

				float[][][] output = (float[][][]) result.get(0).getValue();
				Set<String> prohibitedLabels = analyzePredictions(output);

				if (!prohibitedLabels.isEmpty()) {
					String msg = "Image contains prohibited content: " + String.join(", ", prohibitedLabels) + ". Please upload document or campaign object only.";
					return CampaignFormImageValidationDto.invalid(new ArrayList<>(prohibitedLabels), msg);
				}

				return CampaignFormImageValidationDto.valid();
			} finally {
				if (result != null) {
					result.close();
				}
				if (inputTensor != null) {
					inputTensor.close();
				}
			}
		} catch (OrtException e) {
			logger.error("ONNX inference failed: {}", e.getMessage(), e);
			return CampaignFormImageValidationDto.valid();
		} catch (Exception e) {
			logger.error("Error during image validation: {}", e.getMessage(), e);
			return CampaignFormImageValidationDto.valid();
		}
	}

	private float[] preprocessImage(BufferedImage image) {
		BufferedImage resized = new BufferedImage(INPUT_WIDTH, INPUT_HEIGHT, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = resized.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(image, 0, 0, INPUT_WIDTH, INPUT_HEIGHT, null);
		g.dispose();

		float[] floatArray = new float[3 * INPUT_WIDTH * INPUT_HEIGHT];
		int channelSize = INPUT_WIDTH * INPUT_HEIGHT;

		for (int y = 0; y < INPUT_HEIGHT; y++) {
			for (int x = 0; x < INPUT_WIDTH; x++) {
				int rgb = resized.getRGB(x, y);
				int r = (rgb >> 16) & 0xFF;
				int gVal = (rgb >> 8) & 0xFF;
				int b = rgb & 0xFF;

				int index = y * INPUT_WIDTH + x;
				floatArray[index] = r / 255.0f;
				floatArray[channelSize + index] = gVal / 255.0f;
				floatArray[2 * channelSize + index] = b / 255.0f;
			}
		}

		return floatArray;
	}

	private Set<String> analyzePredictions(float[][][] output) {
		Set<String> detected = new HashSet<>();

		if (output == null || output.length == 0 || output[0].length < 84) {
			return detected;
		}

		float[][] predictions = output[0]; // [84][8400]
		int numAnchors = predictions[0].length; // 8400

		for (int col = 0; col < numAnchors; col++) {
			int bestClassIndex = -1;
			float maxScore = 0.0f;

			for (int row = 4; row < 84; row++) {
				float score = predictions[row][col];
				if (score > maxScore) {
					maxScore = score;
					bestClassIndex = row - 4;
				}
			}

			if (maxScore >= CONFIDENCE_THRESHOLD && PROHIBITED_CLASS_INDICES.contains(bestClassIndex)) {
				String labelName = bestClassIndex < COCO_CLASSES.length ? COCO_CLASSES[bestClassIndex] : "prohibited-entity";
				if ("person".equalsIgnoreCase(labelName)) {
					detected.add("human face / person");
				} else {
					detected.add("animal (" + labelName + ")");
				}
			}
		}

		return detected;
	}
}
