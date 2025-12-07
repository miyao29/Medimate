package com.example.medimate.Login;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.medimate.MainActivity;
import com.example.medimate.R;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.TimeUnit;

public class PhoneLoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    // XML id 그대로 맞춤 (activity_phone_login.xml)
    private EditText etPhone;
    private EditText etVerifyCode;
    private TextView btnSendCode;
    private TextView btnResend;
    private TextView btnVerifyFinal;
    private TextView tvCodeLabel;

    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_login); // ★ 로그인 XML

        EdgeToEdge.enable(this);

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

        mAuth = FirebaseAuth.getInstance();

        // View 매핑
        etPhone = findViewById(R.id.etPhone);
        etVerifyCode = findViewById(R.id.etVerifyCode);
        btnSendCode = findViewById(R.id.btnSendCode);
        btnResend = findViewById(R.id.btnResend);
        btnVerifyFinal = findViewById(R.id.btnVerifyFinal);
        tvCodeLabel = findViewById(R.id.tvCodeLabel);

        // 초기 상태: 인증번호 관련 숨김
        etVerifyCode.setVisibility(View.GONE);
        tvCodeLabel.setVisibility(View.GONE);
        btnResend.setVisibility(View.GONE);
        btnVerifyFinal.setVisibility(View.GONE);

        // 인증요청
        btnSendCode.setOnClickListener(v -> sendCode(false));

        // 재전송
        btnResend.setOnClickListener(v -> sendCode(true));

        // 인증하기 (로그인)
        btnVerifyFinal.setOnClickListener(v -> verifyCode());
    }

    /** 인증번호 요청 */
    private void sendCode(boolean isResend) {
        String phone = etPhone.getText().toString().trim();

        if (phone.isEmpty()) {
            Toast.makeText(this, "전화번호를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!phone.startsWith("+82")) {
            phone = phone.replace("-", "").replace(" ", "");
            if (phone.startsWith("0")) {
                phone = phone.substring(1);
            }
            phone = "+82" + phone;
        }

        PhoneAuthOptions.Builder builder = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(callbacks);

        if (isResend && resendToken != null) {
            builder.setForceResendingToken(resendToken);
        }

        PhoneAuthProvider.verifyPhoneNumber(builder.build());
    }

    /** 인증 콜백 */
    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    // 자동 인증되면 여기서 바로 로그인 처리도 가능하지만,
                    // 지금은 수동 인증 흐름 유지
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    Toast.makeText(PhoneLoginActivity.this,
                            "인증 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onCodeSent(@NonNull String id,
                                       @NonNull PhoneAuthProvider.ForceResendingToken token) {

                    verificationId = id;
                    resendToken = token;

                    // UI 토글
                    btnSendCode.setVisibility(View.GONE);
                    tvCodeLabel.setVisibility(View.VISIBLE);
                    etVerifyCode.setVisibility(View.VISIBLE);
                    btnResend.setVisibility(View.VISIBLE);
                    btnVerifyFinal.setVisibility(View.VISIBLE);

                    Toast.makeText(PhoneLoginActivity.this,
                            "인증번호가 발송되었습니다.", Toast.LENGTH_SHORT).show();
                }
            };

    /** 인증번호 검증 (로그인) */
    private void verifyCode() {
        if (verificationId == null) {
            Toast.makeText(this, "먼저 인증요청을 해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        String code = etVerifyCode.getText().toString().trim();
        if (code.isEmpty()) {
            Toast.makeText(this, "인증번호를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        PhoneAuthCredential credential =
                PhoneAuthProvider.getCredential(verificationId, code);

        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user == null) {
                        Toast.makeText(this, "로그인 실패", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    checkPhoneUserAndLogin(user);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "코드가 일치하지 않습니다.", Toast.LENGTH_SHORT).show());
    }

    /** Firestore에 회원 정보가 있는지 확인 후 로그인 */
    private void checkPhoneUserAndLogin(FirebaseUser user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this,
                                "가입된 전화번호가 아닙니다. 먼저 회원가입을 진행하세요.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 로그인 성공 → 메인 화면
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.putExtra("uid", user.getUid());
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "회원 정보 확인 실패", Toast.LENGTH_SHORT).show());
    }
}
