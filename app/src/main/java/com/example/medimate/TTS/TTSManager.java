package com.example.medimate.TTS;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import java.util.Locale;

public class TTSManager {

    private TextToSpeech tts;
    private boolean isInitialized = false;

    // 1. 생성될 때 Context를 받아와서 초기화
    public TTSManager(Context context) {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.KOREAN);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS_MGR", "Korean language is not supported.");
                } else {
                    isInitialized = true; // 초기화 성공
                }
            } else {
                Log.e("TTS_MGR", "Initialization failed.");
            }
        });
    }

    // 2. '말하기' 기능 (외부에서 호출할 함수)
    public void speak(String text) {
        if (!isInitialized) {
            Log.e("TTS_MGR", "TTS not initialized.");
            return;
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    // 3. '종료' 기능 (메모리 해제)
    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}