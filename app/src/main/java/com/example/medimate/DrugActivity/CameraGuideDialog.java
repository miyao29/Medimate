package com.example.medimate.DrugActivity;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;

import com.example.medimate.R;

public class CameraGuideDialog extends Dialog {

    private OnStartCameraListener listener;

    public interface OnStartCameraListener {
        void onStartCamera();
    }

    public CameraGuideDialog(@NonNull Context context, OnStartCameraListener listener) {
        super(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_camera_guide);

        if (getWindow() != null) {

            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            setCancelable(false);

            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        ImageButton btnBack = findViewById(R.id.btnBackDialog);
        btnBack.setOnClickListener(v -> dismiss());

        Button btnStart = findViewById(R.id.btn_start_camera);
        btnStart.setOnClickListener(v -> {
            dismiss();
            if (listener != null) {
                listener.onStartCamera();
            }
        });
    }
}