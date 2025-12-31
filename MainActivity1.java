package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.net.Uri;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.tencentcloudapi.ocr.v20181119.models.IDCardOCRResponse;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity1 extends AppCompatActivity {

    private static final String TAG = "CameraTest";

    private ImageView imagePreview;
    private Button captureButton, analyzeButton, signUtilButton;

    private Bitmap capturedImage;
    private String currentPhotoPath;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main1);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupActivityLaunchers();
        setupButtonListeners();
    }

    private void initViews() {
        imagePreview = findViewById(R.id.iv_photo);
        captureButton = findViewById(R.id.btn_take_photo);
        analyzeButton = findViewById(R.id.btn_identify);
        signUtilButton = findViewById(R.id.btn_sign_util);
    }

    private void setupButtonListeners() {
        captureButton.setOnClickListener(v -> checkCameraPermission());

        analyzeButton.setOnClickListener(v -> processImage());

        signUtilButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity1.this, SignUtil.class);
            startActivity(intent);
        });
    }

    private void setupActivityLaunchers() {

        requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        openCamera();
                    } else {
                        Toast.makeText(this, "需要相机权限才能拍照", Toast.LENGTH_LONG).show();
                    }
                });

        cameraLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        handleCameraResult();
                    } else {
                        Toast.makeText(this, "拍照取消", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        try {
            File photoFile = createImageFile();
            Uri photoUri = FileProvider.getUriForFile(
                    this,
                    "com.example.myapplication.provider",
                    photoFile
            );

            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            cameraLauncher.launch(intent);

        } catch (Exception e) {
            Log.e(TAG, "打开相机失败", e);
            Toast.makeText(this, "无法打开相机", Toast.LENGTH_LONG).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IDCARD_" + timeStamp;

        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir == null) {
            storageDir = getFilesDir();
        }

        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    /**
     * 关键方法：只从文件路径读取照片
     */
    private void handleCameraResult() {

        if (currentPhotoPath == null) {
            Toast.makeText(this, "照片路径为空", Toast.LENGTH_SHORT).show();
            return;
        }

        File file = new File(currentPhotoPath);
        if (!file.exists()) {
            Toast.makeText(this, "照片文件不存在", Toast.LENGTH_SHORT).show();
            return;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1; // 身份证识别不缩太小

        capturedImage = BitmapFactory.decodeFile(currentPhotoPath, options);

        if (capturedImage != null) {
            imagePreview.setImageBitmap(capturedImage);
            Toast.makeText(this, "照片拍摄成功", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "照片解析失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * OCR 识别（腾讯云官方 SDK）
     */
    private void processImage() {

        if (capturedImage == null) {
            Toast.makeText(this, "请先拍摄身份证照片", Toast.LENGTH_SHORT).show();
            return;
        }

        if (capturedImage.getWidth() < 1000) {
            Toast.makeText(this, "图片分辨率过低，请重新拍照", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                // 1. Bitmap → byte[]
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                capturedImage.compress(Bitmap.CompressFormat.JPEG, 95, stream);
                byte[] imgData = stream.toByteArray();

                // 2. Base64
                String base64Img = Base64Util.encode(imgData);

                // 3. 调用腾讯云官方 SDK
                IDCardOCRResponse sdkResp =
                        OcrSdkHelper.idCardOcr(base64Img);

                // 4. SDK → IdentifyResult
                IdentifyResult result = new IdentifyResult();
                result.setName(sdkResp.getName());
                result.setSex(sdkResp.getSex());
                result.setNation(sdkResp.getNation());
                result.setBirth(sdkResp.getBirth());
                result.setAddress(sdkResp.getAddress());
                result.setId(sdkResp.getIdNum());

                // 5. 跳转结果页
                runOnUiThread(() -> {
                    Intent intent = new Intent(MainActivity1.this, ResultActivity.class);
                    intent.putExtra("ocr_data", result);
                    startActivity(intent);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(MainActivity1.this,
                                "识别失败：" + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

}
