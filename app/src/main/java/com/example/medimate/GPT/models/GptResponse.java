package com.example.medimate.GPT.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GptResponse {
    @SerializedName("choices")
    public List<Choice> choices;
}