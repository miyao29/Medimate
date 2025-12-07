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
import android.widget.Toast;

import com.example.medimate.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.kakao.sdk.user.UserApiClient;

public class WithdrawActivity extends AppCompatActivity {

    private Button btnWithdrawConfirm;
    private MaterialButton btnWithdrawCancel;

    private String uid;   // MyInfoActivity → 전달받음
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_withdraw);
        EdgeToEdge.enable(this);

        // 패딩 처리
        View mainView = findViewById(R.id.main);
        int originalLeft = mainView.getPaddingLeft();
        int originalTop = mainView.getPaddingTop();
        int originalRight = mainView.getPaddingRight();
        int originalBottom = mainView.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    originalLeft + systemBars.left,
                    originalTop + systemBars.top,
                    originalRight + systemBars.right,
                    originalBottom + systemBars.bottom
            );
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        btnWithdrawConfirm = findViewById(R.id.btnWithdrawConfirm);
        btnWithdrawCancel = findViewById(R.id.btnWithdrawCancel);

        uid = getIntent().getStringExtra("uid");
        btnWithdrawConfirm.setOnClickListener(v -> deleteAccount());

        btnWithdrawCancel.setOnClickListener(v -> finish());
        btnWithdrawConfirm = findViewById(R.id.btnWithdrawConfirm);

        uid = getIntent().getStringExtra("uid");  // 전달받은 UID

        btnWithdrawConfirm.setOnClickListener(v -> deleteAccount());
    }

    /** ★ 회원 탈퇴 (subcollection 포함 완전 삭제) */
    private void deleteAccount() {

        if (uid == null) {
            Toast.makeText(this, "계정 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1) medications 삭제
        db.collection("users").document(uid).collection("medications")
                .get()
                .addOnSuccessListener(meds -> {

                    for (DocumentSnapshot doc : meds) {
                        doc.getReference().delete();
                    }

                    // 2) checks 삭제
                    db.collection("users").document(uid).collection("checks")
                            .get()
                            .addOnSuccessListener(checks -> {

                                for (DocumentSnapshot doc : checks) {
                                    doc.getReference().delete();
                                }

                                // 3) 메인 user 문서 삭제
                                db.collection("users").document(uid)
                                        .delete()
                                        .addOnSuccessListener(aVoid -> {

                                            FirebaseUser user = mAuth.getCurrentUser();

                                            // 4) FirebaseAuth 계정 삭제
                                            if (user != null) {
                                                user.delete().addOnCompleteListener(task -> {
                                                    unlinkProviderThenGo();
                                                });
                                            } else {
                                                unlinkProviderThenGo();
                                            }
                                        });
                            });
                });
    }

    /** provider unlink 후 Welcome 이동 */
    private void unlinkProviderThenGo() {

        // 카카오 계정
        UserApiClient.getInstance().me((kakaoUser, error) -> {
            if (kakaoUser != null) {
                UserApiClient.getInstance().unlink(unlinkError -> {
                    goToWelcome();
                    return null;
                });
                return null;
            }

            // 구글 계정
            GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
            if (acct != null) {
                GoogleSignInClient googleClient =
                        GoogleSignIn.getClient(this,
                                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestEmail()
                                        .build());

                googleClient.revokeAccess().addOnCompleteListener(task -> {
                    goToWelcome();
                });
                return null;
            }

            // 전화번호 로그인
            goToWelcome();
            return null;
        });
    }

    /** Welcome 화면으로 이동 */
    private void goToWelcome() {
        mAuth.signOut();

        Intent intent = new Intent(WithdrawActivity.this, WelcomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finishAffinity();
    }
}
