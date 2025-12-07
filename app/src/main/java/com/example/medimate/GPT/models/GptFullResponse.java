package com.example.medimate.GPT.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GptFullResponse {

    @SerializedName("drugs")
    public List<Drug> drugs; // 약 목록

    @SerializedName("common_instructions")
    public String commonInstructions;

    // 예: "모든 약은 식후 30분에..."

    public List<Drug> getDrugs() { return drugs; }
    public String getCommonInstructions() { return commonInstructions; }
}