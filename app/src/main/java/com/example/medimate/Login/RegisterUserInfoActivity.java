package com.example.medimate.Login;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.medimate.MainActivity;
import com.example.medimate.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterUserInfoActivity extends AppCompatActivity {

    private EditText etNickName;
    private Button btnFinish;

    private String uid;
    private String provider;
    private String email;   // google only
    private String phone;   // phone only

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 수정 1: EdgeToEdge를 먼저 활성화
        EdgeToEdge.enable(this);

        // 수정 2: setContentView를 올바른 레이아웃으로 한 번만 호출
        setContentView(R.layout.activity_register_user_info);


        View mainView = findViewById(R.id.main);

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

        etNickName = findViewById(R.id.etNickName);
        btnFinish = findViewById(R.id.btnFinish);

        uid = getIntent().getStringExtra("uid");
        provider = getIntent().getStringExtra("provider");
        email = getIntent().getStringExtra("email");
        phone = getIntent().getStringExtra("phone");

        btnFinish.setOnClickListener(v -> saveUserInfo());
    }

    private void saveUserInfo() {
        String nickname = etNickName.getText().toString().trim();

        if (nickname.isEmpty()) {
            Toast.makeText(this, "닉네임을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> data = new HashMap<>();
        data.put("provider", provider);
        data.put("nickname", nickname);

        if (email != null) {
            data.put("email", email);
        }

        if (phone != null) {
            data.put("phone", phone);
        }

        db.collection("users")
                .document(uid)
                .set(data)
                .addOnSuccessListener(a -> {
                    Intent i = new Intent(this, MainActivity.class);
                    i.putExtra("uid", uid);
                    startActivity(i);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "저장 실패", Toast.LENGTH_SHORT).show());
    }
}