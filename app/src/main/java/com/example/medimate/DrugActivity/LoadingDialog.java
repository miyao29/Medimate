package com.example.medimate.DrugActivity;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.medimate.R;

public class LoadingDialog extends Dialog {

    private String customMessage = null; // 변경할 메시지 저장용

    public LoadingDialog(@NonNull Context context) {
        super(context);
    }

    public void setMessage(String message) {
        this.customMessage = message;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_loading);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            setCancelable(false);
        }

        if (customMessage != null) {
            TextView tvMessage = findViewById(R.id.tv_loading_message);
            if (tvMessage != null) {
                tvMessage.setText(customMessage);
            }
        }
    }
}