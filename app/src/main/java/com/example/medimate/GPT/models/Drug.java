package com.example.medimate.GPT.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable; // 이게 꼭 필요합니다!
public class Drug implements Serializable {

    @SerializedName("name")
    public String name;

    // --- 1. 생김새 (화면용 / 음성용) ---
    @SerializedName("appearance_display")
    public String appearanceDisplay;
    @SerializedName("appearance_tts")
    public String appearanceTts;

    // --- 2. 약 설명 (화면용 / 음성용) ---
    @SerializedName("description_display")
    public String descriptionDisplay;
    @SerializedName("description_tts")
    public String descriptionTts;

    // --- 3. 복약안내 (화면용 / 음성용) ---
    @SerializedName("dosage_display")
    public String dosageDisplay;
    @SerializedName("dosage_tts")
    public String dosageTts;

    // --- 4. 보관방법 (화면용 / 음성용) ---
    @SerializedName("storage_display")
    public String storageDisplay;
    @SerializedName("storage_tts")
    public String storageTts;

    // --- 5. 주의사항 (화면용 / 음성용) ---
    @SerializedName("warning_display")
    public String warningDisplay;
    @SerializedName("warning_tts")
    public String warningTts;

    // --- Getter 함수들 (편의용) ---
    public String getName() { return name; }
}
