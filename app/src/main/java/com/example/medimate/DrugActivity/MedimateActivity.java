package com.example.medimate.DrugActivity;

import android.Manifest;
import android.app.Activity; // Activity import 필수
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.medimate.GPT.models.Drug;
import com.example.medimate.R;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MedimateActivity extends AppCompatActivity implements
        CropGuideStartDialog.OnCropConfirmedListener { // 크롭 가이드 리스너만 유지

    private MainViewModel viewModel;
    private LoadingDialog loadingDialog;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> cropLauncher;

    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.ocr_main);

        setupWindowInsets();

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        loadingDialog = new LoadingDialog(this);

        findViewById(R.id.card_camera).setOnClickListener(v -> checkCameraPermissionAndLaunch());
        findViewById(R.id.card_gallery).setOnClickListener(v -> launchGallery());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        setupCameraLauncher();
        setupGalleryLauncher();
        setupCropLauncher();
        setupPermissionLauncher();

        setupObservers();
    }

    // 1. 카메라 촬영 로직
    private void checkCameraPermissionAndLaunch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera(); // 바로 카메라 실행
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    // 2. 사진/갤러리 결과 처리 (processImageUri -> CropGuideStartDialog)
    private void processImageUri(Uri sourceUri) {
        if (sourceUri == null) {
            Toast.makeText(this, "이미지를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        // 바로 startCrop 하지 않고, 크롭 가이드 팝업을 먼저 띄움!
        showCropGuideDialog(sourceUri);
    }

    private void showCropGuideDialog(Uri uri) {
        CropGuideStartDialog dialog = CropGuideStartDialog.newInstance(uri);
        dialog.show(getSupportFragmentManager(), "CropGuideStartDialog");
    }

    // 3. 크롭 가이드 관련 (CropGuideStartDialog -> onCropConfirmed)
    @Override
    public void onCropConfirmed(Uri uri) {

        // 크롭 가이드 팝업에서 '네, 자르러 갈게요' 누르면 실행됨
        startCrop(uri);
    }

    //  4. 실제 크롭 실행 (UCrop)
    private void startCrop(Uri sourceUri) {
        String destinationFileName = "Cropped_" + System.currentTimeMillis() + ".jpg";
        Uri destinationUri = Uri.fromFile(new File(getCacheDir(), destinationFileName));

        UCrop.Options options = new UCrop.Options();
        options.setToolbarColor(ContextCompat.getColor(this, R.color.black));
        options.setStatusBarColor(ContextCompat.getColor(this, R.color.black));
        options.setToolbarWidgetColor(ContextCompat.getColor(this, R.color.white));
        options.setActiveControlsWidgetColor(Color.parseColor("#636F65"));
        options.setCompressionQuality(90);
        options.setFreeStyleCropEnabled(true);

        Intent intent = UCrop.of(sourceUri, destinationUri)
                .withOptions(options)
                .getIntent(this);

        cropLauncher.launch(intent);
    }

    //5. 크롭 완료 후 처리 (UCrop -> processFinalImage)
    private void setupCropLauncher() {
        cropLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        final Uri resultUri = UCrop.getOutput(result.getData());
                        if (resultUri != null) {
                            processFinalImage(resultUri); // 최종 분석 시작
                        }
                    } else if (result.getResultCode() == UCrop.RESULT_ERROR) {
                        final Throwable cropError = UCrop.getError(result.getData());
                        Toast.makeText(this, "이미지 자르기 실패", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void processFinalImage(Uri croppedUri) {
        new Thread(() -> {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), croppedUri);
                runOnUiThread(() -> {
                    viewModel.startImageProcessing(bitmap);
                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "사진 처리 실패.", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void setupWindowInsets() {
        View mainView = findViewById(R.id.ocr_main_root);
        View btnBack = findViewById(R.id.btnBack);
        View headerIcon = findViewById(R.id.header_icon);
        View cardGallery = findViewById(R.id.card_gallery);

        if (mainView == null || btnBack == null || headerIcon == null || cardGallery == null) return;

        final int backBtnMarginTop = ((ViewGroup.MarginLayoutParams) btnBack.getLayoutParams()).topMargin;
        final int iconMarginTop = ((ViewGroup.MarginLayoutParams) headerIcon.getLayoutParams()).topMargin;
        final int galleryMarginBottom = ((ViewGroup.MarginLayoutParams) cardGallery.getLayoutParams()).bottomMargin;

        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);

            ViewGroup.MarginLayoutParams backParams = (ViewGroup.MarginLayoutParams) btnBack.getLayoutParams();
            backParams.topMargin = backBtnMarginTop + systemBars.top;
            btnBack.setLayoutParams(backParams);

            ViewGroup.MarginLayoutParams iconParams = (ViewGroup.MarginLayoutParams) headerIcon.getLayoutParams();
            iconParams.topMargin = iconMarginTop + systemBars.top;
            headerIcon.setLayoutParams(iconParams);

            ViewGroup.MarginLayoutParams galleryParams = (ViewGroup.MarginLayoutParams) cardGallery.getLayoutParams();
            galleryParams.bottomMargin = galleryMarginBottom + systemBars.bottom;
            cardGallery.setLayoutParams(galleryParams);
            return insets;
        });
    }

    private void setupObservers() {
        viewModel.getDrugDataLiveData().observe(this, responseData -> {
            if (responseData == null) return;
            if (responseData.getDrugs() == null) {
                Toast.makeText(this, "약품 분석에 실패했습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            List<Drug> drugs = responseData.getDrugs();
            if (drugs.isEmpty()) {
                Toast.makeText(this, "약품이 인식되지 않았습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            goToDrugListScreen(drugs);
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                if (!loadingDialog.isShowing()) loadingDialog.show();
            } else {
                if (loadingDialog.isShowing()) loadingDialog.dismiss();
            }
        });
    }

    private void goToDrugListScreen(List<Drug> drugs) {
        Intent intent = new Intent(MedimateActivity.this, DrugListActivity.class);
        intent.putExtra("drugList", new ArrayList<>(drugs));
        startActivity(intent);
    }

    private void setupCameraLauncher() {
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                processImageUri(imageUri);
            }
        });
    }

    private void setupGalleryLauncher() {
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) processImageUri(uri);
        });
    }

    private void setupPermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                launchCamera(); // 권한 허용되면 바로 실행
            } else {
                Toast.makeText(this, "카메라 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void launchCamera() {
        imageUri = createImageUri();
        if (imageUri == null) {
            Toast.makeText(this, "저장 공간을 확보할 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraLauncher.launch(intent);
    }

    private void launchGallery() {
        galleryLauncher.launch("image/*");
    }

    private Uri createImageUri() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Medimate_Capture_" + System.currentTimeMillis());
        values.put(MediaStore.Images.Media.DESCRIPTION, "From Medimate App");
        return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }
}