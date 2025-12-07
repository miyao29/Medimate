package com.example.medimate.OCR;

import android.app.Dialog;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window; // Window import 추가
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medimate.R;
import com.example.medimate.GPT.models.Drug;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DrugDetailDialog extends DialogFragment {

    private Drug drug;
    private TextToSpeech tts;
    private TextView infoDisplayTextView;

    public DrugDetailDialog(Drug drug) {
        this.drug = drug;
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_NoActionBar);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.drug_detail_popup, container, false);

        // 뒤로가기 버튼
        ImageButton back = v.findViewById(R.id.btnBack);
        back.setOnClickListener(view -> dismiss());

        // 제목을 약 이름으로 자동 변경
        TextView title = v.findViewById(R.id.detailTitle);
        title.setText(safe(drug.getName()));

        // 새로 추가한 TextView를 찾아서 연결
        infoDisplayTextView = v.findViewById(R.id.tv_info_display);
        infoDisplayTextView.setText("버튼을 눌러 정보를 확인하세요."); // 초기 메시지

        // Drug → DetailItem 변환 (RecyclerView에 표시될 내용)
        List<DetailItem> items = new ArrayList<>();
        // ... (이하 Drug -> DetailItem 변환 로직은 생략)

        items.add(new DetailItem("생김새",
                safe(drug.appearanceDisplay), safe(drug.appearanceTts)));
        items.add(new DetailItem("약 설명",
                safe(drug.descriptionDisplay), safe(drug.descriptionTts)));
        items.add(new DetailItem("복용방법",
                safe(drug.dosageDisplay), safe(drug.dosageTts)));
        items.add(new DetailItem("보관방법",
                safe(drug.storageDisplay), safe(drug.storageTts)));
        items.add(new DetailItem("주의사항",
                safe(drug.warningDisplay), safe(drug.warningTts)));


        // RecyclerView 설정
        RecyclerView rv = v.findViewById(R.id.detailRecyclerView);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(new DetailAdapter(items, this::speak, infoDisplayTextView));

        // TTS 초기화
        tts = new TextToSpeech(getContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.KOREAN);
                tts.setSpeechRate(1.0f);
            }
        });

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. EdgeToEdge 활성화 (Window 설정) - 기존 코드 유지
        Window window = getDialog() != null ? getDialog().getWindow() : null;
        if (window != null) {
            ViewCompat.getWindowInsetsController(window.getDecorView())
                    .setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }

        // 2. 시스템 바 패딩 설정 (Insets Listener) 수정 부분!
        // 여기서부터 기존 ViewCompat 코드를 지우고 아래 내용을 붙여넣으세요.

        View btnBack = view.findViewById(R.id.btnBack);
        View infoContainer = view.findViewById(R.id.info_container); // 하단 설명창

        // 기존 마진값 저장 (중복 적용 방지)
        final int backBtnMarginTop = ((ViewGroup.MarginLayoutParams) btnBack.getLayoutParams()).topMargin;
        final int infoMarginBottom = ((ViewGroup.MarginLayoutParams) infoContainer.getLayoutParams()).bottomMargin;

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // (1) 전체 배경은 위아래 꽉 채우기 (패딩 0)
            v.setPadding(systemBars.left, 0, systemBars.right, 0);

            // (2) [상단] 뒤로가기 버튼 내리기 (제목도 버튼 따라 자동으로 내려옴)
            ViewGroup.MarginLayoutParams backParams = (ViewGroup.MarginLayoutParams) btnBack.getLayoutParams();
            backParams.topMargin = backBtnMarginTop + systemBars.top;
            btnBack.setLayoutParams(backParams);

            // (3) [하단] 설명창(info_container)이 내비게이션 바 위로 올라오도록 처리
            ViewGroup.MarginLayoutParams infoParams = (ViewGroup.MarginLayoutParams) infoContainer.getLayoutParams();
            infoParams.bottomMargin = infoMarginBottom + systemBars.bottom;
            infoContainer.setLayoutParams(infoParams);

            return insets;
        });
    }

    // null-safe 핸들링
    private String safe(String s) {
        return s == null ? "정보 없음" : s;
    }

    // TTS 실행
    private void speak(String text) {
        if (tts != null && text != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    // 팝업 크기 조절 (화면 90% -> MATCH_PARENT로 수정)
    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            // 다이얼로그 크기를 화면 전체로 설정
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }


    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}