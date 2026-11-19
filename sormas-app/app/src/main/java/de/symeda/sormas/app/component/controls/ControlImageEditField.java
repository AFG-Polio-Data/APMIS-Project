package de.symeda.sormas.app.component.controls;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.databinding.BindingAdapter;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import de.symeda.sormas.api.campaign.data.CampaignFormImageNamingContext;
import de.symeda.sormas.api.campaign.data.CampaignFormImageSource;
import de.symeda.sormas.api.campaign.data.CampaignFormImageValidationDto;
import de.symeda.sormas.api.campaign.data.CampaignFormImageValue;
import de.symeda.sormas.api.campaign.form.CampaignFormElement;
import de.symeda.sormas.api.document.DocumentRelatedEntityType;
import de.symeda.sormas.app.BuildConfig;
import de.symeda.sormas.app.R;
import de.symeda.sormas.app.backend.campaign.data.CampaignFormData;
import de.symeda.sormas.app.backend.common.DatabaseHelper;
import de.symeda.sormas.app.backend.config.ConfigProvider;
import de.symeda.sormas.app.backend.document.Document;
import de.symeda.sormas.app.component.VisualState;
import de.symeda.sormas.app.component.VisualStateControlType;
import de.symeda.sormas.app.util.MobileYoloImageValidator;

public class ControlImageEditField extends ControlPropertyEditField<Object> {

    private final Gson gson = new Gson();
    private List<CampaignFormImageValue> imageValues = new ArrayList<>();

    private CampaignFormElement element;
    private ActivityResultLauncher<Intent> cameraLauncher;
    ActivityResultLauncher<String> permissionLauncher;

    private OnCaptureRequestListener captureRequestListener;

    private LinearLayout thumbnailContainer;   // was: private ImageView imagePreview
    private TextView imageCountBadge;
    private TextView imageHintText;
    private int pendingReplaceIndex = -1;
    private boolean readOnly;
    private Uri pendingOutputUri;
    private File pendingOutputFile;
    private String pendingLocalId;
    private String campaignFormDataUuid;
    private CampaignFormData record;


    public ControlImageEditField(Context context) { super(context); }
    public ControlImageEditField(Context context, AttributeSet attrs) { super(context, attrs); }
    public ControlImageEditField(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); }


    public interface OnCaptureRequestListener {
        void onCaptureRequested(ControlImageEditField field);
    }


    public void setOnCaptureRequestListener(OnCaptureRequestListener listener) {
        this.captureRequestListener = listener;
    }


    // ---------- ControlPropertyField contract ----------

    @Override
    protected Object getFieldValue() {
        if (imageValues == null || imageValues.isEmpty()) {
            return null;
        }
        if (isMultipleAllowed()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (CampaignFormImageValue v : imageValues) {
                if (v != null) {
                    list.add(v.toMap());
                }
            }
            return list;                                   // List<Map>
        }
        CampaignFormImageValue first = imageValues.get(0);
        return first != null ? first.toMap() : null;       // Map
    }

    @Override
    protected void setFieldValue(Object value) {
        if (imageValues == null) {
            imageValues = new ArrayList<>();
        }
        imageValues.clear();

        if (value instanceof Map<?,?>) {
            CampaignFormImageValue v = CampaignFormImageValue.fromMap((Map<?,?>) value);
            if (v != null) imageValues.add(v);
        } else if (value instanceof List<?>) {
            for (Object o : (List<?>) value) {
                if (o instanceof Map<?,?>) {
                    CampaignFormImageValue v = CampaignFormImageValue.fromMap((Map<?,?>) o);
                    if (v != null) imageValues.add(v);
                }
            }
        } else if (value instanceof String && !((String) value).trim().isEmpty()) {
            // legacy rows that stored a JSON string
            parseJsonAndFill((String) value);   // your existing JSON parse code
        }
        refreshPreview();
    }


    @BindingAdapter(value = {
            "value",
           }, requireAll = false)
    public static void setValue(ControlImageEditField imageField, Object value) {
        imageField.setFieldValue(value);
    }


    @BindingAdapter(value = { "value", "record"}, requireAll = false)
    public static void setValue(ControlImageEditField imageField, Object value, CampaignFormData record) {
        imageField.setCampaignFormDataUuid(record.getUuid());
        imageField.setRecord(record);
        imageField.setFieldValue(value);
    }

    @Override
    protected void initialize(Context context, AttributeSet attrs, int defStyle) {
        // nothing to read from XML for now
    }
    @Override
    protected void inflateView(Context context, AttributeSet attrs, int defStyle) {
        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            throw new RuntimeException("Unable to inflate layout in " + getClass().getName());
        }
        inflater.inflate(R.layout.control_image_edit_layout, this);
        initViews();                       // <-- was relying on onFinishInflate
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initViews();                       // keep for XML-inflated usage
    }

    private void initViews() {
        thumbnailContainer = findViewById(R.id.image_thumbnail_container);
        imageCountBadge = findViewById(R.id.image_count_badge);
        imageHintText = findViewById(R.id.image_hint_text);

        if (thumbnailContainer != null) {
            thumbnailContainer.setOnClickListener(v -> launchBackCameraOnly());
        }
        refreshPreview();
    }

    @Override
    protected void requestFocusForContentView(View nextView) {
        thumbnailContainer.requestFocus();
    }

    // ---------- ControlPropertyEditField contract ----------

    @Override
    public void setHint(String hint) {
        if (imageHintText != null) imageHintText.setText(hint);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (thumbnailContainer != null) thumbnailContainer.setEnabled(enabled);
    }

    @Override
    protected void changeVisualState(VisualState state) {
        // Minimal: keep label color + enabled state in sync like text fields do
        if (state == VisualState.ERROR || state == VisualState.DISABLED) {
            setEnabled(state != VisualState.DISABLED);
        }
    }

    // ---------- Camera capture (adapted from ControlCameraImageField) ----------

    public void configureForImageCapture(CampaignFormElement campaignFormElement,
                                         ActivityResultLauncher<Intent> launcher,
                                         ActivityResultLauncher<String> permissionLauncher) {
        this.element = campaignFormElement;
        this.cameraLauncher = launcher;
        this.permissionLauncher = permissionLauncher;
        refreshPreview();
    }

    public void setCampaignFormDataUuid(String campaignFormDataUuid) {
        this.campaignFormDataUuid = campaignFormDataUuid;
    }

    public void setRecord(CampaignFormData record) {
        this.record = record;
    }

    public void launchCamera() {
        launchBackCameraOnly();
    }

    private void launchBackCameraOnly() {
        if (readOnly) return;

        if (captureRequestListener != null) {
            captureRequestListener.onCaptureRequested(this);
        }


        if (cameraLauncher == null) {
            Toast.makeText(getContext(), "Camera is not available here", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ask for permission only now, on demand
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            if (permissionLauncher != null) {
                permissionLauncher.launch(Manifest.permission.CAMERA);
            } else {
                Toast.makeText(getContext(), "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if (imageValues == null) imageValues = new ArrayList<>();
        if (isMultipleAllowed() && imageValues.size() >= getMaxCount()) {
            Toast.makeText(getContext(), "Maximum image count reached", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            pendingLocalId = UUID.randomUUID().toString();
            File baseDir = new File(
                    getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    "campaign_images");
            if (!baseDir.exists() && !baseDir.mkdirs()) {
                Toast.makeText(getContext(), "Unable to prepare storage", Toast.LENGTH_SHORT).show();
                return;
            }
            pendingOutputFile = new File(baseDir, pendingLocalId + ".jpg");
            pendingOutputUri = FileProvider.getUriForFile(
                    getContext(), BuildConfig.APPLICATION_ID + ".fileprovider", pendingOutputFile);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, pendingOutputUri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (intent.resolveActivity(getContext().getPackageManager()) == null) {
                Toast.makeText(getContext(), "No camera app found", Toast.LENGTH_SHORT).show();
                return;
            }
            cameraLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to open camera", Toast.LENGTH_SHORT).show();
        }
    }

    public void handleCameraResult(int resultCode) {
        if (resultCode != Activity.RESULT_OK || pendingOutputFile == null || !pendingOutputFile.exists()) {
            clearPending();
            return;
        }

        final File capturedFile = pendingOutputFile;
        final String localId = pendingLocalId;
        final int replaceIndex = pendingReplaceIndex;
        final Context context = getContext();

        final ProgressDialog progressDialog = ProgressDialog.show(context, "", "Validating image quality...", true, false);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                long originalSize = capturedFile.length();
                byte[] rawBytes = readFileBytes(capturedFile);

                // Resize/compress to under 200KB before storing.
                final byte[] bytes = compressImage(rawBytes, 200L * 1024L);

                // Blur detection
                final boolean blurry = isBlurry(bytes);

                // YOLO AI validation for human face/person/selfie or animals
                final CampaignFormImageValidationDto validationDto;
                if (!blurry) {
                    validationDto = MobileYoloImageValidator.getInstance().validate(context, bytes);
                } else {
                    validationDto = null;
                }

                post(() -> {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        try {
                            progressDialog.dismiss();
                        } catch (Exception ignored) {
                        }
                    }

                    if (blurry) {
                        Toast.makeText(getContext(), "Image appears blurry. Please retake.", Toast.LENGTH_LONG).show();
                        if (capturedFile != null) {
                            capturedFile.delete();
                        }
                        clearPending();
                        return;
                    }

                    if (validationDto != null && !validationDto.isValid()) {
                        new AlertDialog.Builder(getContext())
                                .setTitle("Image Rejected")
                                .setMessage(validationDto.getMessage())
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                        if (capturedFile != null) {
                            capturedFile.delete();
                        }
                        clearPending();
                        return;
                    }

                    try {
                        CampaignFormImageValue value = new CampaignFormImageValue();
                        value.setLocalId(localId);
                        value.setOriginalFileName(capturedFile.getName());
                        value.setGeneratedFileName(capturedFile.getName());
                        value.setMimeType("image/jpeg");
                        value.setSource(CampaignFormImageSource.MOBILE_CAMERA);
                        value.setCapturedAt(System.currentTimeMillis());
                        value.setDeviceType(Build.MODEL);
                        value.setOsVersion(Build.VERSION.RELEASE);
                        value.setOriginalSizeBytes(originalSize);
                        value.setCompressedSizeBytes((long) bytes.length);

                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                        if (options.outWidth > 0) value.setWidth(options.outWidth);
                        if (options.outHeight > 0) value.setHeight(options.outHeight);

                        // Persist the captured bytes into the local documents table (mirrors the web flow:
                        // CampaignFormImageFacade.uploadImage -> document table). The temp file is then removed.
                        String generatedName = generateImageFileName(namingContextForRecord()) + ".jpg";

                        Document document = DatabaseHelper.getDocumentDao().build();
                        document.setRelatedEntityUuid(campaignFormDataUuid);
                        document.setRelatedEntityType(DocumentRelatedEntityType.CAMPAIGN_FORM_DATA.name());
                        document.setName(generatedName);
                        document.setMimeType("image/jpeg");
                        document.setSize(bytes.length);
                        document.setContent(bytes);
                        document.setUploadingUser(ConfigProvider.getUser());
                        DatabaseHelper.getDocumentDao().saveAndSnapshot(document);

                        value.setOriginalSizeBytes(originalSize);
                        value.setCompressedSizeBytes((long) bytes.length);
                        value.setOriginalFileName(capturedFile.getName());
                        value.setGeneratedFileName(generatedName);
                        value.setImageId(document.getUuid());
                        value.setLocalId(document.getUuid());

                        if (imageValues == null) imageValues = new ArrayList<>();

                        if (!isMultipleAllowed() || replaceIndex >= 0) {
                            if (replaceIndex >= 0 && replaceIndex < imageValues.size()) {
                                imageValues.set(replaceIndex, value);   // replace the retaken image
                            } else {
                                imageValues.clear();
                                imageValues.add(value);                // single-image: replace the only image
                            }
                        } else {
                            imageValues.add(value);                    // multi-image: append
                        }

                        refreshPreview();
                        onValueChanged();          // <-- fires the fragment's value listener
                    } catch (Exception e) {
                        Log.e("ControlImageEditField", "Failed to persist captured image", e);
                        Toast.makeText(getContext(), "Failed to save image", Toast.LENGTH_SHORT).show();
                    } finally {
                        if (capturedFile != null) {
                            capturedFile.delete();
                        }
                        pendingReplaceIndex = -1;
                        clearPending();
                    }
                });
            } catch (Exception e) {
                Log.e("ControlImageEditField", "Failed to process captured image", e);
                post(() -> {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        try {
                            progressDialog.dismiss();
                        } catch (Exception ignored) {
                        }
                    }
                    Toast.makeText(getContext(), "Failed to process image", Toast.LENGTH_SHORT).show();
                    if (capturedFile != null) {
                        capturedFile.delete();
                    }
                    clearPending();
                });
            }
        });
    }

    private void refreshPreview() {
        if (thumbnailContainer == null) return;
        thumbnailContainer.removeAllViews();

        if (imageValues == null || imageValues.isEmpty()) {
            if (!readOnly)  thumbnailContainer.addView(createAddTile());
            imageCountBadge.setVisibility(GONE);
            imageHintText.setText("Tap + to capture an image");
            return;
        }

        for (int i = 0; i < imageValues.size(); i++) {
            thumbnailContainer.addView(createThumbnailTile(i));
        }

        // Add another "+" tile while we still have capacity
        if (isMultipleAllowed() && imageValues.size() < getMaxCount()) {
            thumbnailContainer.addView(createAddTile());
        }

        imageCountBadge.setText(String.valueOf(imageValues.size()));
        imageCountBadge.setVisibility(VISIBLE);
    }

    private ImageView createAddTile() {
        ImageView tile = new ImageView(getContext());
        int size = dp(96);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.setMargins(dp(4), 0, dp(4), 0);
        tile.setLayoutParams(lp);
        tile.setImageResource(R.drawable.ic_add_24dp);
        tile.setBackgroundResource(R.drawable.control_camera_tile_bg); // optional rounded bg
        tile.setScaleType(ImageView.ScaleType.CENTER);
        tile.setOnClickListener(v -> {
            pendingReplaceIndex = -1;
            launchBackCameraOnly();
        });
        return tile;
    }

    private View createThumbnailTile(int index) {
        FrameLayout tile = new FrameLayout(getContext());
        int size = dp(96);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.setMargins(dp(4), 0, dp(4), 0);
        tile.setLayoutParams(lp);

        ImageView thumb = new ImageView(getContext());
        thumb.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);

        Bitmap bmp = loadImageBitmap(imageValues.get(index));
        if (bmp != null) {
            thumb.setImageBitmap(bmp);
        }
        tile.addView(thumb);

        // Small "x" badge to delete
        if (!readOnly) {
            TextView remove = new TextView(getContext());
            remove.setText("×");
            remove.setTextColor(Color.WHITE);
            remove.setBackgroundColor(0xCC000000);
            remove.setPadding(dp(8), dp(2), dp(8), dp(4));
            FrameLayout.LayoutParams rlp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            rlp.gravity = Gravity.TOP | Gravity.END;
            remove.setLayoutParams(rlp);
            remove.setOnClickListener(v -> removeImage(index));
            tile.addView(remove);
        }
        // Tap thumbnail -> full-screen preview
        thumb.setOnClickListener(v -> showImagePreview(index));
        tile.setOnClickListener(v -> showImagePreview(index));

        return tile;
    }

    private void showImagePreview(int index) {
        if (index < 0 || index >= imageValues.size()) return;
        final Bitmap bmp = loadImageBitmap(imageValues.get(index));
        if (bmp == null) return;

        Dialog dialog = new Dialog(getContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

        ImageView full = new ImageView(getContext());
        full.setImageBitmap(bmp);
        full.setScaleType(ImageView.ScaleType.FIT_CENTER);
        root.addView(full, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout buttons = new LinearLayout(getContext());
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);
        buttons.setPadding(0, dp(8), 0, dp(16));

        if (!readOnly) {

            buttons.addView(actionButton("Retake", v -> {
                pendingReplaceIndex = index;
                dialog.dismiss();
                launchBackCameraOnly();
            }));

            buttons.addView(actionButton("Delete", v -> {
                dialog.dismiss();
                removeImage(index);
            }));
        }
        buttons.addView(actionButton("Close", v -> dialog.dismiss()));

        root.addView(buttons, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        dialog.setContentView(root);
        dialog.show();
    }

    private Button actionButton(String label, View.OnClickListener listener) {
        Button b = new Button(getContext());
        b.setText(label);
        b.setOnClickListener(listener);
        return b;
    }

    private void removeImage(int index) {
        if (index < 0 || index >= imageValues.size()) return;
        imageValues.remove(index);
        refreshPreview();
        onValueChanged();          // marks the form dirty via the fragment's listener
    }

    private Bitmap loadImageBitmap(CampaignFormImageValue v) {
        if (v == null) return null;
        String id = v.getImageId() != null && !v.getImageId().trim().isEmpty() ? v.getImageId() : v.getLocalId();
        if (id != null && !id.trim().isEmpty()) {
            try {
                Document doc = DatabaseHelper.getDocumentDao().queryUuid(id);
                if (doc != null && doc.getContent() != null && doc.getContent().length > 0) {
                    return BitmapFactory.decodeByteArray(doc.getContent(), 0, doc.getContent().length);
                }
            } catch (Exception e) {
                // fall back to the legacy file below
            }
        }
        File file = imageFileFor(v);
        if (file != null && file.exists()) {
            return BitmapFactory.decodeFile(file.getAbsolutePath());
        }
        return null;
    }

    private static byte[] readFileBytes(File file) throws IOException {
        FileInputStream in = new FileInputStream(file);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    private byte[] compressImage(byte[] data, long maxBytes) {
        try {
            if (data.length <= maxBytes) {
                return data;
            }
            Bitmap original = BitmapFactory.decodeByteArray(data, 0, data.length);
            if (original == null) {
                return data;
            }
            int w = original.getWidth();
            int h = original.getHeight();
            int maxDim = 1200;
            if (w > maxDim || h > maxDim) {
                float scale = Math.min((float) maxDim / w, (float) maxDim / h);
                w = Math.max(1, Math.round(w * scale));
                h = Math.max(1, Math.round(h * scale));
            }
            Bitmap scaled = Bitmap.createScaledBitmap(original, w, h, true);
            if (scaled != original) {
                original.recycle();
            }
            int quality = 90;
            ByteArrayOutputStream out;
            do {
                out = new ByteArrayOutputStream();
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out);
                quality -= 10;
            } while (out.size() > maxBytes && quality > 20);
            scaled.recycle();
            return out.toByteArray();
        } catch (Exception e) {
            return data;
        }
    }

    private boolean isBlurry(byte[] data) {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 4;
            Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length, opts);
            if (bmp == null) {
                return false;
            }
            int w = bmp.getWidth();
            int h = bmp.getHeight();
            int[] pixels = new int[w * h];
            bmp.getPixels(pixels, 0, w, 0, 0, w, h);

            double sum = 0;
            double sumSq = 0;
            int n = 0;
            for (int y = 1; y < h - 1; y++) {
                for (int x = 1; x < w - 1; x++) {
                    int idx = y * w + x;
                    int r = (pixels[idx] >> 16) & 0xFF;
                    int g = (pixels[idx] >> 8) & 0xFF;
                    int b = pixels[idx] & 0xFF;
                    double lum = 0.299 * r + 0.587 * g + 0.114 * b;

                    int idxL = y * w + (x - 1);
                    int idxR = y * w + (x + 1);
                    int idxU = (y - 1) * w + x;
                    int idxD = (y + 1) * w + x;
                    double lumL = 0.299 * ((pixels[idxL] >> 16) & 0xFF) + 0.587 * ((pixels[idxL] >> 8) & 0xFF) + 0.114 * (pixels[idxL] & 0xFF);
                    double lumR = 0.299 * ((pixels[idxR] >> 16) & 0xFF) + 0.587 * ((pixels[idxR] >> 8) & 0xFF) + 0.114 * (pixels[idxR] & 0xFF);
                    double lumU = 0.299 * ((pixels[idxU] >> 16) & 0xFF) + 0.587 * ((pixels[idxU] >> 8) & 0xFF) + 0.114 * (pixels[idxU] & 0xFF);
                    double lumD = 0.299 * ((pixels[idxD] >> 16) & 0xFF) + 0.587 * ((pixels[idxD] >> 8) & 0xFF) + 0.114 * (pixels[idxD] & 0xFF);

                    double lap = 4 * lum - lumL - lumR - lumU - lumD;
                    sum += lap;
                    sumSq += lap * lap;
                    n++;
                }
            }
            bmp.recycle();
            if (n == 0) {
                return false;
            }
            double variance = (sumSq - (sum * sum) / n) / n;
            return variance < 100.0;
        } catch (Exception e) {
            return false;
        }
    }

    private CampaignFormImageNamingContext namingContextForRecord() {
        CampaignFormImageNamingContext ctx = new CampaignFormImageNamingContext();
        if (record != null) {
            ctx.setRegion(record.getArea() != null ? record.getArea().getName() : null);
            ctx.setProvince(record.getRegion() != null ? record.getRegion().getName() : null);
            ctx.setDistrict(record.getDistrict() != null ? record.getDistrict().getName() : null);
            ctx.setClusterNumber(record.getCommunity() != null && record.getCommunity().getClusterNumber() != null ? String.valueOf(record.getCommunity().getClusterNumber()) : null);
            ctx.setClusterName(record.getCommunity() != null ? record.getCommunity().getName() : null);
        }
        return ctx;
    }

    private String generateImageFileName(CampaignFormImageNamingContext ctx) {
        if (ctx == null) {
            return "NA_NA_NA_NA_NA";
        }
        return sanitizeSegment(ctx.getRegion()) + "_" + sanitizeSegment(ctx.getProvince()) + "_"
                + sanitizeSegment(ctx.getDistrict()) + "_" + sanitizeSegment(ctx.getClusterNumber()) + "_"
                + sanitizeSegment(ctx.getClusterName());
    }

    private String sanitizeSegment(String input) {
        if (input == null) {
            return "NA";
        }
        String normalized = input.trim();
        normalized = normalized.replaceAll("\\s+", "-");
        normalized = normalized.replaceAll("[^A-Za-z0-9\\-]", "");
        normalized = normalized.replaceAll("-+", "-");
        int start = 0, end = normalized.length();
        while (start < end && normalized.charAt(start) == '-') start++;
        while (end > start && normalized.charAt(end - 1) == '-') end--;
        normalized = normalized.substring(start, end);
        return normalized.isEmpty() ? "NA" : normalized;
    }

    private File imageFileFor(CampaignFormImageValue v) {
        if (v == null || v.getLocalId() == null) return null;
        return new File(getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "campaign_images/" + v.getLocalId() + ".jpg");
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private int getMaxCount() {
        if (element == null || element.getImageMaxCount() == null || element.getImageMaxCount() < 1) {
            return 1;
        }
        return Math.min(5, element.getImageMaxCount());
    }

    private boolean isMultipleAllowed() {
        return (element != null && Boolean.TRUE.equals(element.getImageMultiple())) || getMaxCount() > 1;
    }

    private void clearPending() {
        pendingOutputUri = null;
        pendingOutputFile = null;
        pendingLocalId = null;
    }

    public void setReadOnly(boolean isReadOnly) {
        this.readOnly = isReadOnly;
        refreshPreview();
    }


    private void parseJsonAndFill(String json) {
        try {
            Gson gson = new Gson();
            JsonElement el = gson.fromJson(json, JsonElement.class);
            if (el == null || el.isJsonNull()) {
                return;
            }
            if (el.isJsonArray()) {
                for (JsonElement item : el.getAsJsonArray()) {
                    if (item.isJsonObject()) {
                        Map<String, Object> map = gson.fromJson(item, new TypeToken<Map<String, Object>>() {}.getType());
                        CampaignFormImageValue v = CampaignFormImageValue.fromMap(map);
                        if (v != null) {
                            imageValues.add(v);
                        }
                    }
                }
            } else if (el.isJsonObject()) {
                Map<String, Object> map = gson.fromJson(el, new TypeToken<Map<String, Object>>() {}.getType());
                CampaignFormImageValue v = CampaignFormImageValue.fromMap(map);
                if (v != null) {
                    imageValues.add(v);
                }
            }
        } catch (Exception e) {
            Log.w("ControlImageEditField", "Failed to parse legacy image JSON", e);
        }
    }

}