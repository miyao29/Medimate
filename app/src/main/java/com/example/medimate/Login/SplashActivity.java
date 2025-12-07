package com.example.medimate.Login;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.example.medimate.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.kakao.sdk.user.UserApiClient;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkAutoLogin();
    }

    private void checkAutoLogin() {

        // ------------------------------
        // 1) FirebaseAuth 자동 로그인 (Google + Phone)
        // ------------------------------
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser != null) {
            String uid = firebaseUser.getUid();
            goToMain(uid);
            return;
        }

        // ------------------------------
        // 2) Kakao 자동 로그인 체크
        // ------------------------------
        UserApiClient.getInstance().accessTokenInfo((tokenInfo, error) -> {

            // 카카오 토큰이 유효하지 않음 → 로그인 필요
            if (error != null) {
                goToWelcome();
                return null;
            }

            // 토큰 유효하므로 카카오 계정 정보 한번 더 가져오기 (uid 안정적으로 얻기)
            UserApiClient.getInstance().me((user, meError) -> {

                if (meError != null || user == null) {
                    goToWelcome();
                    return null;
                }

                String kakaoUid = "kakao_" + user.getId();
                goToMain(kakaoUid);
                return null;
            });

            return null;
        });
    }

    private void goToMain(String uid) {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        intent.putExtra("uid", uid);
        startActivity(intent);
        finish();
    }

    private void goToWelcome() {
        Intent intent = new Intent(SplashActivity.this, WelcomeActivity.class);
        startActivity(intent);
        finish();
    }
}
