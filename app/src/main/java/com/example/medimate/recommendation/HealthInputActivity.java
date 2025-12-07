package com.example.medimate.recommendation;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.medimate.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HealthInputActivity extends AppCompatActivity {

    private Map<String, ArrayList<String>> symptomKeywords; // 칩 텍스트와 실제 검색 키워드 매핑
    private ArrayList<Chip> selectedChips = new ArrayList<>(); // 선택된 칩들을 추적
    private Button submitButton;
    private final int MAX_SELECTION_COUNT = 3; // 최대 선택 가능 개수

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_health_input);

        // XML의 루트 뷰(id: input)를 찾음
        View mainView = findViewById(R.id.input);

        int originalPaddingLeft = mainView.getPaddingLeft();
        int originalPaddingTop = mainView.getPaddingTop();
        int originalPaddingRight = mainView.getPaddingRight();
        int originalPaddingBottom = mainView.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            v.setPadding(
                    originalPaddingLeft + systemBars.left,
                    originalPaddingTop + systemBars.top,
                    originalPaddingRight + systemBars.right,
                    originalPaddingBottom + systemBars.bottom
            );

            return insets;
        });

        mainView.post(() -> {

            setupSymptomKeywords(); // 검색 키워드 매핑 초기화

            // ChipGroup과 Chip들을 찾아 리스너 설정
            setupChipGroup(findViewById(R.id.chip_group_health_checkup));
            setupChipGroup(findViewById(R.id.chip_group_organs));
            setupChipGroup(findViewById(R.id.chip_group_daily_life));

            submitButton = findViewById(R.id.submitButton);
            updateSubmitButton(); // 초기 버튼 상태 업데이트

            submitButton.setOnClickListener(v -> {
                ArrayList<String> selectedChipTexts = new ArrayList<>();
                HashMap<String, ArrayList<String>> keywordsForSelectedChips = new HashMap<>();

                for (Chip chip : selectedChips) {
                    String chipText = chip.getText().toString();
                    selectedChipTexts.add(chipText); // 1. 칩 텍스트 추가

                    if (symptomKeywords.containsKey(chipText)) {
                        // 2. 칩 텍스트와 매칭되는 키워드 리스트를 맵에 추가
                        keywordsForSelectedChips.put(chipText, symptomKeywords.get(chipText));
                    }
                }

                if (!selectedChipTexts.isEmpty()) {
                    Intent intent = new Intent(HealthInputActivity.this, RecommendActivity.class);

                    // Intent에 2가지 데이터를 담아 보냅니다.
                    intent.putStringArrayListExtra("selected_chip_texts", selectedChipTexts);
                    intent.putExtra("keyword_map", keywordsForSelectedChips); // HashMap은 Serializable이라 putExtra로 전송

                    startActivity(intent);
                } else {
                    Toast.makeText(HealthInputActivity.this, "하나 이상의 증상을 선택해주세요.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void setupSymptomKeywords() {
        symptomKeywords = new HashMap<>();
        // 칩 텍스트와 실제 API 검색 키워드 매핑
        symptomKeywords.put("간 건강", new ArrayList<String>() {{ add("간"); add("간 건강"); }});
        symptomKeywords.put("활력/피로", new ArrayList<String>() {{ add("피로"); add("활력"); }});
        symptomKeywords.put("체지방 관리", new ArrayList<String>() {{ add("체지방"); add("다이어트"); }});
        symptomKeywords.put("콜레스테롤", new ArrayList<String>() {{ add("콜레스테롤"); }});
        symptomKeywords.put("혈당 관리", new ArrayList<String>() {{ add("혈당"); add("당뇨"); }});
        symptomKeywords.put("혈압 관리", new ArrayList<String>() {{ add("혈압"); }});

        symptomKeywords.put("뇌 건강", new ArrayList<String>() {{ add("뇌"); add("기억력"); }});
        symptomKeywords.put("눈 건강", new ArrayList<String>() {{ add("눈"); add("시력"); add("황반"); }});
        symptomKeywords.put("뼈 건강", new ArrayList<String>() {{ add("뼈"); add("관절"); add("골다공증"); }}); // 관절은 이중 포함 가능성
        symptomKeywords.put("위 건강", new ArrayList<String>() {{ add("위"); add("소화"); add("헬리코박터"); }});
        symptomKeywords.put("장 건강", new ArrayList<String>() {{ add("장"); add("배변"); add("프로바이오틱스"); }});

        symptomKeywords.put("관절/근육", new ArrayList<String>() {{ add("관절"); add("근육"); add("연골"); }}); // 뼈와 이중 포함 가능성
        symptomKeywords.put("면역력", new ArrayList<String>() {{ add("면역"); add("면역력"); }});
        symptomKeywords.put("모발/손톱", new ArrayList<String>() {{ add("모발"); add("손톱"); add("탈모"); }});
        // 수면 관련 키워드 확대
        symptomKeywords.put("수면", new ArrayList<String>() {{ add("수면"); add("숙면"); add("수면의 질"); add("불면"); add("잠"); }});
        symptomKeywords.put("스트레스", new ArrayList<String>() {{ add("스트레스"); add("긴장"); }});
        symptomKeywords.put("항노화/항산화", new ArrayList<String>() {{ add("항산화"); add("노화"); }});
        symptomKeywords.put("피부", new ArrayList<String>() {{ add("피부"); add("자외선"); add("탄력"); }});
    }


    private void setupChipGroup(ChipGroup chipGroup) {
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            Chip chip = (Chip) chipGroup.getChildAt(i);
            chip.setCheckable(true); // 칩을 선택 가능하게 만듭니다.

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (selectedChips.size() < MAX_SELECTION_COUNT) {
                        selectedChips.add(chip);
                    } else {
                        // 최대 개수 초과 시 선택 취소 및 메시지
                        chip.setChecked(false);
                        Toast.makeText(HealthInputActivity.this, "최대 " + MAX_SELECTION_COUNT + "개까지 선택할 수 있습니다.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    selectedChips.remove(chip);
                }
                updateSubmitButton();
            });
        }
    }

    private void updateSubmitButton() {
        int count = selectedChips.size();
        submitButton.setText(count + "개 선택 완료");
        submitButton.setEnabled(count > 0); // 1개 이상 선택 시 버튼 활성화
    }
}