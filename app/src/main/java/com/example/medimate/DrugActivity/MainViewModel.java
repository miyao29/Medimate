package com.example.medimate.DrugActivity;

import android.app.Application;
import android.graphics.Bitmap;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.medimate.BuildConfig;
import com.example.medimate.GPT.GptProcessor;
import com.example.medimate.GPT.models.GptFullResponse; // (새 '틀' import)
import com.example.medimate.OCR.OcrProcessor;
import com.example.medimate.TTS.TTSManager;

public class MainViewModel extends AndroidViewModel {

    private OcrProcessor ocrProcessor;
    private GptProcessor gptProcessor;
    private TTSManager ttsManager;
    private static final String OPENAI_API_KEY = "Bearer " + BuildConfig.OPENAI_API_KEY;

    private MutableLiveData<GptFullResponse> drugDataLiveData = new MutableLiveData<>();

    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);


    public LiveData<GptFullResponse> getDrugDataLiveData() {
        return drugDataLiveData;
    }
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public MainViewModel(@NonNull Application application) {
        super(application);
        ocrProcessor = new OcrProcessor();
        gptProcessor = new GptProcessor();
        ttsManager = new TTSManager(application.getApplicationContext());
    }

    public void startImageProcessing(Bitmap bitmap) {
        isLoading.postValue(true);
        drugDataLiveData.postValue(null);
        ocrProcessor.processBitmap(bitmap, new OcrProcessor.OcrCallback() {
            @Override
            public void onSuccess(String rawText) {

                gptProcessor.processText(rawText, OPENAI_API_KEY, new GptProcessor.GptCallback() {

                    @Override
                    public void onSuccess(GptFullResponse responseData) {
                        Log.d("MainViewModel", "GPT Success. Drugs found: " + responseData.getDrugs().size());
                        drugDataLiveData.postValue(responseData);
                        isLoading.postValue(false);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e("MainViewModel", "GPT Error: " + errorMessage);
                        isLoading.postValue(false);
                        ttsManager.speak("분석에 실패했습니다. " + errorMessage);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("MainViewModel", "OCR Error: " + errorMessage);
                isLoading.postValue(false);
                ttsManager.speak("글자 인식에 실패했습니다. " + errorMessage);
            }
        });
    }

    /**
     * MainActivity의 팝업 버튼이 이 함수를 호출합니다.
     * @param textToSpeak (예: drug.getDosage())
     */
    public void speakText(String textToSpeak) {
        if (textToSpeak != null && !textToSpeak.isEmpty()) {
            ttsManager.speak(textToSpeak);
        } else {
            ttsManager.speak("정보가 없습니다.");
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (ttsManager != null) {
            ttsManager.shutdown();
        }
    }
}