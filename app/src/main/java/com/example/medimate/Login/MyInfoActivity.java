package com.example.medimate.Login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.medimate.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.kakao.sdk.user.UserApiClient;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MyInfoActivity extends AppCompatActivity {

    private TextView tvProvider, tvEmail, tvPhone, tvNickname;
    private Button btnLogout, btnWithdraw;

    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_info);

        EdgeToEdge.enable(this);

// 2. 시스템 바 패딩 설정
        View mainView = findViewById(R.id.main);

        // XML에 설정된 기본 여백(24dp, 16dp 등)을 기억해둡니다.
        int originalLeft = mainView.getPaddingLeft();
        int originalTop = mainView.getPaddingTop();
        int originalRight = mainView.getPaddingRight();
        int originalBottom = mainView.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            // "기본 여백 + 시스템 바 높이"만큼 패딩을 줍니다.
            // 이렇게 하면 배경색은 시스템 바 뒤까지 꽉 차고, 내용은 그만큼 안쪽으로 들어옵니다.
            v.setPadding(
                    originalLeft + systemBars.left,
                    originalTop + systemBars.top,
                    originalRight + systemBars.right,
                    originalBottom + systemBars.bottom
            );
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        tvProvider = findViewById(R.id.tvProvider);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvNickname = findViewById(R.id.tvNickname);

        btnLogout = findViewById(R.id.btnLogout);
        btnWithdraw = findViewById(R.id.btnWithdraw);

        uid = getIntent().getStringExtra("uid");

        if (uid == null) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) uid = user.getUid();
        }

        loadUserInfo();

        btnLogout.setOnClickListener(v -> logoutUser());
        btnWithdraw.setOnClickListener(v -> {
            Intent intent = new Intent(MyInfoActivity.this, WithdrawActivity.class);
            intent.putExtra("uid", uid);
            startActivity(intent);
        });
    }

    private void loadUserInfo() {
        if (uid == null) {
            Toast.makeText(this, "로그인 정보 없음", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(this::applyUserData)
                .addOnFailureListener(e ->
                        Toast.makeText(this, "유저 정보 로드 실패", Toast.LENGTH_SHORT).show());
    }

    private void applyUserData(DocumentSnapshot doc) {

        if (!doc.exists()) {
            Toast.makeText(this, "유저 정보 없음", Toast.LENGTH_SHORT).show();
            return;
        }

        String provider = doc.getString("provider");
        String nickname = doc.getString("nickname");
        String email = doc.getString("email");
        String phone = doc.getString("phone");

        tvNickname.setText(nickname != null ? nickname : "닉네임 없음");

        if ("kakao".equals(provider)) {
            tvProvider.setText("카카오 로그인");
            tvEmail.setVisibility(View.GONE);
            tvPhone.setVisibility(View.GONE);
            return;
        }

        if ("google".equals(provider)) {
            tvProvider.setText("구글 로그인");
            tvEmail.setVisibility(View.VISIBLE);
            tvPhone.setVisibility(View.GONE);
            tvEmail.setText(email != null ? email : "-");
        }

        if ("phone".equals(provider)) {
            tvProvider.setText("전화번호 로그인");
            tvPhone.setVisibility(View.VISIBLE);
            tvEmail.setVisibility(View.GONE);
            tvPhone.setText(phone != null ? phone : "-");
        }
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();

        try { UserApiClient.getInstance().logout(error -> null); }
        catch (Exception ignored) {}

        Intent intent = new Intent(MyInfoActivity.this, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
