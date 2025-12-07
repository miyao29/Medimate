package com.example.medimate.Login;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.medimate.MainActivity;
import com.example.medimate.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignupMethodActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleClient;
    private ActivityResultLauncher<Intent> googleLauncher;

    // 개인정보 처리방침 관련 뷰
    private CheckBox cbPrivacyPolicyAgree;
    private TextView tvViewPolicy;

    // 회원가입 버튼 뷰
    private LinearLayout btnGoogleSignup;
    private LinearLayout btnPhoneSignup;

    // 경고 메시지 상수
    private static final String CONSENT_WARNING = "개인정보 처리방침에 동의해야 회원가입을 진행할 수 있습니다.";

    // PDF 파일의 내용을 텍스트 상수로 변환하여 포함시켰습니다.
    private static final String PRIVACY_POLICY_CONTENT =
            "제1조(본 방침의 공개)\n" +
                    "1. 회사는 이용자가 언제든지 쉽게 본 방침을 확인할 수 있도록 회사 홈페이지 첫 화면 또는 첫 화면과의 연결화면을 통해 본 방침을 공개하고 있습니다.\n" +
                    "2. 회사는 제1항에 따라 본 방침을 공개하는 경우 글자 크기, 색상 등을 활용하여 이용자가 본 방침을 쉽게 확인할 수 있도록 합니다.\n\n" +

                    "제2조(본 방침의 변경)\n" +
                    "1. 본 방침은 개인정보 관련 법령, 지침, 고시 또는 정부나 회사 서비스의 정책이나 내용의 변경에 따라 개정될 수 있습니다.\n" +
                    "2. 회사는 본 방침을 개정하는 경우 홈페이지 공지사항 또는 서면·이메일 등으로 공지합니다.\n" +
                    "3. 회사는 본 방침 개정의 시행일로부터 최소 7일 이전에 공지합니다. 다만, 이용자 권리의 중요한 변경이 있을 경우에는 최소 30일 전에 공지합니다.\n\n" +

                    "제3조(회원 가입을 위한 정보)\n" +
                    "회사는 이용자의 회사 서비스에 대한 회원가입을 위하여 다음과 같은 정보를 수집합니다.\n" +
                    "1. 필수 수집 정보: 이메일 주소, 비밀번호, 이름, 닉네임, 생년월일 및 휴대폰 번호\n\n" +

                    "제4조(본인 인증을 위한 정보)\n" +
                    "회사는 이용자의 본인인증을 위하여 다음과 같은 정보를 수집합니다.\n" +
                    "1. 필수 수집 정보: 휴대폰 번호, 이메일 주소, 이름 및 생년월일\n\n" +

                    "제5조(개인정보 수집 방법)\n" +
                    "회사는 홈페이지 입력, 어플리케이션 이용, 이메일 수신 등의 방법으로 이용자의 개인정보를 수집합니다.\n\n" +

                    "제6조(개인정보의 이용)\n" +
                    "회사는 개인정보를 공지사항 전달, 이용문의 회신, 서비스 제공, 부정 이용 방지 등의 목적으로 이용합니다.\n\n" +

                    "제7조(개인정보의 보유 및 이용기간)\n" +
                    "1. 회사는 이용 목적 달성을 위한 기간 동안 개인정보를 보유 및 이용합니다.\n" +
                    "2. 서비스 부정이용기록은 회원 탈퇴 시점으로부터 최대 1년간 보관합니다.\n\n" +

                    "제8조(법령에 따른 개인정보의 보유 및 이용기간)\n" +
                    "관계 법령에 따라 다음의 정보를 보관합니다.\n" +
                    "- 계약/청약철회 기록: 5년\n" +
                    "- 대금결제/재화 공급 기록: 5년\n" +
                    "- 소비자 불만/분쟁처리 기록: 3년\n" +
                    "- 표시/광고 기록: 6개월\n" +
                    "- 웹사이트 로그 기록: 3개월\n" +
                    "- 전자금융거래 기록: 5년\n" +
                    "- 개인위치정보 기록: 6개월\n\n" +

                    "제9조(개인정보의 파기원칙)\n" +
                    "회사는 개인정보 처리 목적 달성 등 개인정보가 불필요하게 되면 지체 없이 파기합니다.\n\n" +

                    "제10조 및 제11조(파기절차 및 방법)\n" +
                    "전자적 파일은 복구 불가능한 방법으로 삭제하며, 종이 문서는 분쇄하거나 소각합니다.\n\n" +

                    "제12조(광고성 정보의 전송 조치)\n" +
                    "영리목적의 광고성 정보 전송 시 이용자의 명시적인 사전 동의를 받으며, 수신 거부 의사를 표시한 경우 전송하지 않습니다.\n\n" +

                    "제13조(이용자의 의무)\n" +
                    "이용자는 자신의 개인정보를 최신 상태로 유지해야 하며, 타인의 정보를 도용하여 회원가입을 할 수 없습니다.\n\n" +

                    "제14조(개인정보 유출 등에 대한 조치)\n" +
                    "회사는 개인정보 유출 사실을 안 때에는 지체 없이 이용자에게 알리고 관련 기관에 신고합니다.\n\n" +

                    "제16조(개인정보 자동 수집 장치의 설치·운영)\n" +
                    "회사는 쿠키(Cookie)를 운용하며, 이용자는 웹브라우저 설정을 통해 쿠키 저장을 거부할 수 있습니다.\n\n" +

                    "제18조(권익침해에 대한 구제방법)\n" +
                    "개인정보침해로 인한 구제를 받기 위해 개인정보분쟁조정위원회(1833-6972), 한국인터넷진흥원(118) 등에 상담을 신청할 수 있습니다.\n\n" +

                    "부칙\n" +
                    "본 방침은 2025.12.05.부터 시행됩니다.";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup_method);
        EdgeToEdge.enable(this);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
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
        }

        mAuth = FirebaseAuth.getInstance();

        // ---------- Google 설정 ----------
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleClient = GoogleSignIn.getClient(this, gso);

        // =========================================================
        // 개인정보 처리 방침 동의 및 버튼 클릭 리스너 설정
        // =========================================================

        // 1. 뷰 초기화
        cbPrivacyPolicyAgree = findViewById(R.id.cb_privacy_policy_agree);
        tvViewPolicy = findViewById(R.id.tv_view_policy);

        btnGoogleSignup = findViewById(R.id.btnGoogleSignup);
        btnPhoneSignup = findViewById(R.id.btnPhoneSignup);

        // 2. 처리방침 보기 링크 설정 -> 팝업 다이얼로그 띄우기
        tvViewPolicy.setOnClickListener(v -> showPrivacyPolicyDialog());

        // 3. Google 회원가입 Launcher 설정
        googleLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Task<GoogleSignInAccount> task =
                                GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        handleGoogleSignup(task);
                    } else {
                        Toast.makeText(this, "Google 로그인 취소됨", Toast.LENGTH_SHORT).show();
                    }
                });

        // 4. 회원가입 버튼 클릭 리스너 설정 (체크 로직 포함)

        // ========== 1) 구글 회원가입 ==========
        btnGoogleSignup.setOnClickListener(v -> {
            if (!cbPrivacyPolicyAgree.isChecked()) {
                Toast.makeText(this, CONSENT_WARNING, Toast.LENGTH_SHORT).show();
                return;
            }
            Intent signInIntent = mGoogleClient.getSignInIntent();
            googleLauncher.launch(signInIntent);
        });

        // ========== 2) 전화번호 회원가입 ==========
        btnPhoneSignup.setOnClickListener(v -> {
            if (!cbPrivacyPolicyAgree.isChecked()) {
                Toast.makeText(this, CONSENT_WARNING, Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(this, PhoneRegisterActivity.class));
        });
        // =========================================================
    }

    // ---------------------------------------
    // 개인정보 처리방침 내용을 보여주는 다이얼로그
    // ---------------------------------------
    private void showPrivacyPolicyDialog() {
        new AlertDialog.Builder(this)
                .setTitle("개인정보 처리방침") // 팝업 제목
                .setMessage(PRIVACY_POLICY_CONTENT) // 위에서 정의한 PDF 내용 텍스트
                .setPositiveButton("확인", null) // 확인 버튼 (누르면 닫힘)
                .show();
    }

    // ---------------------------------------
    // Google 회원가입 인텐트 결과 처리
    // ---------------------------------------
    private void handleGoogleSignup(Task<GoogleSignInAccount> task) {
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            firebaseAuthWithGoogleSignup(account.getIdToken());
        } catch (ApiException e) {
            Toast.makeText(this, "Google 로그인 실패", Toast.LENGTH_SHORT).show();
        }
    }

    // ---------------------------------------
    // Google Firebase 인증 처리
    // --------------------------------------
    private void firebaseAuthWithGoogleSignup(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        String email = mAuth.getCurrentUser().getEmail();

                        FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(uid)
                                .get()
                                .addOnSuccessListener(doc -> {
                                    if (doc.exists()) {
                                        goMain(uid);
                                    } else {
                                        goRegister(uid, "google", email);
                                    }
                                });
                    } else {
                        Toast.makeText(this, "Firebase 구글 인증 실패", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ---------------------------------------
    // 화면 이동 메소드들
    // ---------------------------------------
    private void goRegister(String uid, String provider, String email) {
        Intent i = new Intent(this, RegisterUserInfoActivity.class);
        i.putExtra("uid", uid);
        i.putExtra("provider", provider);
        i.putExtra("email", email);
        startActivity(i);
        finish();
    }

    private void goMain(String uid) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("uid", uid);
        startActivity(i);
        finish();
    }
}