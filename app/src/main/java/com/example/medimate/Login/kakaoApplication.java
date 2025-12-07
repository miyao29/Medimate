package com.example.medimate.Login;

import android.app.Application;

import com.google.firebase.FirebaseApp;
import com.kakao.sdk.common.KakaoSdk;

public class kakaoApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Kakao SDK 초기화
        KakaoSdk.init(this, "8584d06d8efefa1258ffe3dfd1c1ddb6");
    }
}