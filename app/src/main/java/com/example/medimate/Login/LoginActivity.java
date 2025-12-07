package com.example.medimate.Login;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.medimate.MainActivity;
import com.example.medimate.R;
import com.kakao.sdk.user.UserApiClient;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleClient;
    private ActivityResultLauncher<Intent> googleLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EdgeToEdge.enable(this);

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

        mAuth = FirebaseAuth.getInstance();

        // ===== Google SignIn 객체 생성 =====
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleClient = GoogleSignIn.getClient(this, gso);

        // ===== Google 로그인 런처 설정 =====
        googleLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Task<GoogleSignInAccount> task =
                                GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        handleGoogleLogin(task);
                    }
                });

        // ===== 구글 로그인 버튼 =====
        findViewById(R.id.btnGoogleLogin).setOnClickListener(v -> {
            Intent signInIntent = mGoogleClient.getSignInIntent();
            googleLauncher.launch(signInIntent);
        });

        // 전화번호 로그인 이동
        findViewById(R.id.btnPhoneLogin).setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, PhoneLoginActivity.class))
        );

        // 회원가입 버튼
        findViewById(R.id.btnGoSignup).setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignupMethodActivity.class))
        );
    }

    // ---------------------------------------
    // 1) Google 로그인 결과 처리
    // ---------------------------------------
    private void handleGoogleLogin(Task<GoogleSignInAccount> task) {
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            Toast.makeText(this, "Google 로그인 실패", Toast.LENGTH_SHORT).show();
        }
    }

    // FirebaseAuth 로그인 + Firestore 저장 여부 체크
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        checkGoogleUserInFirestore(user);
                    } else {
                        Toast.makeText(this, "Firebase 인증 실패", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkGoogleUserInFirestore(FirebaseUser user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();
        DocumentReference docRef = db.collection("users").document(uid);

        docRef.get().addOnSuccessListener(snapshot -> {

            if (snapshot.exists()) {
                moveToMain(uid);
                return;
            }

            GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);

            String nickname = (acct != null && acct.getDisplayName() != null)
                    ? acct.getDisplayName()
                    : "사용자";

            String email = (acct != null)
                    ? acct.getEmail()
                    : user.getEmail();

            Map<String, Object> data = new HashMap<>();
            data.put("provider", "google");
            data.put("email", email);
            data.put("nickname", nickname);

            docRef.set(data).addOnSuccessListener(aVoid -> moveToMain(uid));
        });
    }


    // ---------------------------------------
    // 2) 메인 이동
    // ---------------------------------------
    private void moveToMain(String uid) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("uid", uid);    // ★ uid 전달 (카카오/구글/전화 동일)
        startActivity(intent);
        finish();
    }
}
