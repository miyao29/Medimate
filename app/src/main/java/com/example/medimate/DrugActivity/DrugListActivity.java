package com.example.medimate.DrugActivity;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medimate.GPT.models.Drug;
import com.example.medimate.OCR.DrugDetailDialog;
import com.example.medimate.R;

import java.util.ArrayList;

public class DrugListActivity extends AppCompatActivity {

    private ArrayList<Drug> drugList;
    private RecyclerView recyclerView;
    private ImageButton backButton;
    private DrugListAdapter adapter;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_drug_list);

        // 1. 뷰 ID 연결
        View mainView = findViewById(R.id.main);
        backButton = findViewById(R.id.back_button);
        recyclerView = findViewById(R.id.drug_list_recycler);

        // 2. 기존 마진/패딩값 저장
        final int backBtnMarginTop = ((ViewGroup.MarginLayoutParams) backButton.getLayoutParams()).topMargin;
        final int recyclerPaddingBottom = recyclerView.getPaddingBottom();

        // 3. Edge-to-Edge 설정 적용
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // 배경 꽉 채우기
            v.setPadding(systemBars.left, 0, systemBars.right, 0);

            // 뒤로가기 버튼 내리기
            ViewGroup.MarginLayoutParams backParams = (ViewGroup.MarginLayoutParams) backButton.getLayoutParams();
            backParams.topMargin = backBtnMarginTop + systemBars.top;
            backButton.setLayoutParams(backParams);

            // 리스트 하단 패딩 추가
            recyclerView.setPadding(
                    recyclerView.getPaddingLeft(),
                    recyclerView.getPaddingTop(),
                    recyclerView.getPaddingRight(),
                    recyclerPaddingBottom + systemBars.bottom
            );

            return insets;
        });

        // 4. 데이터 받기 및 어댑터 연결
        drugList = (ArrayList<Drug>) getIntent().getSerializableExtra("drugList");

        if (drugList == null) {
            drugList = new ArrayList<>();
        }

        adapter = new DrugListAdapter(drugList, this::openDetailDialog);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        backButton.setOnClickListener(v -> finish());
    }

    private void openDetailDialog(Drug drug) {
        DrugDetailDialog dialog = new DrugDetailDialog(drug);
        dialog.show(getSupportFragmentManager(), "DrugDetailDialog");
    }
}