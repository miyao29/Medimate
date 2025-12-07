package com.example.medimate.DrugActivity;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.medimate.R;

public class CropGuideStartDialog extends DialogFragment {

    private static final String ARG_IMAGE_URI = "image_uri";
    private Uri imageUri;
    private OnCropConfirmedListener listener;

    public interface OnCropConfirmedListener {
        void onCropConfirmed(Uri uri);
    }

    public CropGuideStartDialog() {
        // Required empty public constructor
    }

    public static CropGuideStartDialog newInstance(Uri uri) {
        CropGuideStartDialog fragment = new CropGuideStartDialog();
        Bundle args = new Bundle();
        args.putParcelable(ARG_IMAGE_URI, uri);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnCropConfirmedListener) {
            listener = (OnCropConfirmedListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnCropConfirmedListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            imageUri = getArguments().getParcelable(ARG_IMAGE_URI);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_crop_guide_start, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        view.findViewById(R.id.btn_start_crop).setOnClickListener(v -> {
            dismiss(); // 팝업 닫기
            if (listener != null && imageUri != null) {
                listener.onCropConfirmed(imageUri);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {

            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}