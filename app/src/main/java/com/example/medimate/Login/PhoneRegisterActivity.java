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

import com.example.medimate.R;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.TimeUnit;

public class PhoneRegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    // XML id 그대로 맞춤 (activity_phone_register.xml)
    private EditText etPhone;
    private EditText etVerifyCode;
    private TextView btnSendCode;
    private TextView btnResend;
    private TextView btnVerifyCode;
    private TextView tvCodeLabel;

    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_register);

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

        // View 매핑 (id 전부 XML이랑 동일)
        etPhone = findViewById(R.id.etPhone);
        etVerifyCode = findViewById(R.id.etVerifyCode);
        btnSendCode = findViewById(R.id.btnSendCode);
        btnResend = findViewById(R.id.btnResend);
        btnVerifyCode = findViewById(R.id.btnVerifyCode);
        tvCodeLabel = findViewById(R.id.tvCodeLabel);

        // 처음에는 인증번호 관련 UI 숨김 (XML에도 gone이지만 한번 더 확실히)
        etVerifyCode.setVisibility(View.GONE);
        tvCodeLabel.setVisibility(View.GONE);
        btnResend.setVisibility(View.GONE);
        btnVerifyCode.setVisibility(View.GONE);

        // 인증요청
        btnSendCode.setOnClickListener(v -> sendCode(false));

        // 재전송
        btnResend.setOnClickListener(v -> sendCode(true));

        // 인증하기 (회원가입)
        btnVerifyCode.setOnClickListener(v -> verifyCode());
    }

    /** 인증번호 요청 */
    private void sendCode(boolean isResend) {
        String phone = etPhone.getText().toString().trim();

        if (phone.isEmpty()) {
            Toast.makeText(this, "전화번호를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 한국 번호 +82 변환
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
                    // 회원가입에서는 자동완성 무시 (원하면 여기서 바로 로그인도 가능)
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    Toast.makeText(PhoneRegisterActivity.this,
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
                    btnVerifyCode.setVisibility(View.VISIBLE);

                    Toast.makeText(PhoneRegisterActivity.this,
                            "인증번호가 발송되었습니다.", Toast.LENGTH_SHORT).show();
                }
            };

    /** 인증번호 검증 */
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
                        Toast.makeText(this, "인증 실패", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    checkUserExists(user);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "코드가 일치하지 않습니다.", Toast.LENGTH_SHORT).show());
    }

    /** 이미 가입된 번호인지 확인 */
    private void checkUserExists(FirebaseUser user) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // 이미 회원 → 로그인 화면으로 유도
                        Toast.makeText(this,
                                "이미 가입된 번호입니다. 로그인 화면으로 이동합니다.",
                                Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(this, PhoneLoginActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // 신규 회원 → 닉네임 입력 화면으로
                        Intent intent = new Intent(this, RegisterUserInfoActivity.class);
                        intent.putExtra("uid", user.getUid());
                        intent.putExtra("provider", "phone");
                        intent.putExtra("phone", user.getPhoneNumber());
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "회원 정보 조회 실패", Toast.LENGTH_SHORT).show());
    }
}
