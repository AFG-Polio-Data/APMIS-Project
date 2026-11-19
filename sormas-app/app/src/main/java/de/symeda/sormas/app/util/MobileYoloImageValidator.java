package de.symeda.sormas.app.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import de.symeda.sormas.api.campaign.data.CampaignFormImageValidationDto;

/**
 * On-device neural network validation service using YOLOv8-nano ONNX.
 * Validates whether an image contains human faces/selfies (person) or animals.
 */
public class MobileYoloImageValidator {

    private static final String TAG = "MobileYoloValidator";

    private static final int INPUT_WIDTH = 640;
    private static final int INPUT_HEIGHT = 640;
    private static final float CONFIDENCE_THRESHOLD = 0.50f;
    private static final String MODEL_ASSET_PATH = "models/yolov8n.onnx";
    private static final String MODEL_CACHE_FILENAME = "yolov8n.onnx";

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

    // Class 0: person/face/selfie; Classes 14-23: animals
    private static final Set<Integer> PROHIBITED_CLASS_INDICES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(0, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23))
    );

    private static volatile MobileYoloImageValidator instance;
    private static final Object INIT_LOCK = new Object();

    private OrtEnvironment environment;
    private OrtSession session;
    private String inputTensorName;
    private boolean modelLoaded = false;

    private MobileYoloImageValidator() {
    }

    public static MobileYoloImageValidator getInstance() {
        if (instance == null) {
            synchronized (INIT_LOCK) {
                if (instance == null) {
                    instance = new MobileYoloImageValidator();
                }
            }
        }
        return instance;
    }

    /**
     * Initializes the ONNX environment and loads the model session from assets.
     */
    public synchronized boolean ensureInitialized(Context context) {
        if (modelLoaded) {
            return true;
        }

        try {
            if (environment == null) {
                environment = OrtEnvironment.getEnvironment("MobileYoloImageValidation");
            }

            File modelFile = getOrExtractModelFile(context.getApplicationContext());
            if (modelFile == null || !modelFile.exists()) {
                Log.w(TAG, "YOLOv8 ONNX model file could not be found or extracted. Validation will be bypassed.");
                return false;
            }

            OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();
            int cpuCores = Runtime.getRuntime().availableProcessors();
            sessionOptions.setIntraOpNumThreads(Math.max(1, Math.min(4, cpuCores / 2)));

            session = environment.createSession(modelFile.getAbsolutePath(), sessionOptions);
            inputTensorName = session.getInputNames().iterator().next();
            modelLoaded = true;
            Log.i(TAG, "YOLOv8 ONNX model successfully initialized on Android: " + modelFile.getAbsolutePath());
            return true;
        } catch (Throwable e) {
            Log.e(TAG, "Failed to initialize YOLOv8 ONNX model session: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Extracts the ONNX model from the APK assets directory to app files if not already present.
     */
    private File getOrExtractModelFile(Context context) {
        try {
            File targetFile = new File(context.getFilesDir(), MODEL_CACHE_FILENAME);
            long assetLength = -1;
            try (InputStream in = context.getAssets().open(MODEL_ASSET_PATH)) {
                assetLength = in.available();
            } catch (Exception ignored) {
            }

            if (targetFile.exists() && (assetLength <= 0 || targetFile.length() == assetLength)) {
                return targetFile;
            }

            try (InputStream in = context.getAssets().open(MODEL_ASSET_PATH);
                 FileOutputStream out = new FileOutputStream(targetFile)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            }

            return targetFile;
        } catch (Exception e) {
            Log.e(TAG, "Error extracting YOLO model asset: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Validates an image byte array.
     * Note: This method must be called from a background thread to prevent UI drops.
     */
    public CampaignFormImageValidationDto validate(Context context, byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return CampaignFormImageValidationDto.valid();
        }

        if (!ensureInitialized(context)) {
            Log.d(TAG, "YOLO model is not ready, allowing image pass-through.");
            return CampaignFormImageValidationDto.valid();
        }

        Bitmap original = null;
        Bitmap scaled = null;
        OnnxTensor inputTensor = null;
        OrtSession.Result result = null;

        try {
            original = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            if (original == null) {
                return CampaignFormImageValidationDto.valid();
            }

            scaled = Bitmap.createScaledBitmap(original, INPUT_WIDTH, INPUT_HEIGHT, true);
            if (scaled != original) {
                original.recycle();
                original = null;
            }

            float[] inputBuffer = preprocessBitmap(scaled);
            scaled.recycle();
            scaled = null;

            long[] shape = new long[] { 1, 3, INPUT_HEIGHT, INPUT_WIDTH };
            inputTensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(inputBuffer), shape);
            result = session.run(Collections.singletonMap(inputTensorName, inputTensor));

            float[][][] output = (float[][][]) result.get(0).getValue();
            Set<String> prohibitedLabels = analyzePredictions(output);

            if (!prohibitedLabels.isEmpty()) {
                String detectedStr = joinStrings(new ArrayList<>(prohibitedLabels), ", ");
                String msg = "Image contains prohibited content: " + detectedStr + ". Please capture the document or campaign object only.";
                Log.w(TAG, "Validation failed: " + msg);
                return CampaignFormImageValidationDto.invalid(new ArrayList<>(prohibitedLabels), msg);
            }

            return CampaignFormImageValidationDto.valid();
        } catch (OrtException e) {
            Log.e(TAG, "ONNX inference error: " + e.getMessage(), e);
            return CampaignFormImageValidationDto.valid();
        } catch (Throwable e) {
            Log.e(TAG, "Unexpected error during mobile YOLO validation: " + e.getMessage(), e);
            return CampaignFormImageValidationDto.valid();
        } finally {
            if (original != null && !original.isRecycled()) {
                original.recycle();
            }
            if (scaled != null && !scaled.isRecycled()) {
                scaled.recycle();
            }
            if (result != null) {
                result.close();
            }
            if (inputTensor != null) {
                inputTensor.close();
            }
        }
    }

    private float[] preprocessBitmap(Bitmap bitmap) {
        int[] pixels = new int[INPUT_WIDTH * INPUT_HEIGHT];
        bitmap.getPixels(pixels, 0, INPUT_WIDTH, 0, 0, INPUT_WIDTH, INPUT_HEIGHT);

        float[] floatArray = new float[3 * INPUT_WIDTH * INPUT_HEIGHT];
        int channelSize = INPUT_WIDTH * INPUT_HEIGHT;

        for (int y = 0; y < INPUT_HEIGHT; y++) {
            for (int x = 0; x < INPUT_WIDTH; x++) {
                int rgb = pixels[y * INPUT_WIDTH + x];
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                int index = y * INPUT_WIDTH + x;
                floatArray[index] = r / 255.0f;
                floatArray[channelSize + index] = g / 255.0f;
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
                String labelName = bestClassIndex < COCO_CLASSES.length ? COCO_CLASSES[bestClassIndex] : "prohibited";
                if ("person".equalsIgnoreCase(labelName)) {
                    detected.add("human face / person");
                } else {
                    detected.add("animal (" + labelName + ")");
                }
            }
        }

        return detected;
    }

    private static String joinStrings(List<String> list, String delimiter) {
        if (list == null || list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(delimiter);
            sb.append(list.get(i));
        }
        return sb.toString();
    }
}
