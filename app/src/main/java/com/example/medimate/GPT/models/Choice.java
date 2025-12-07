package com.example.medimate.GPT.models;

import com.google.gson.annotations.SerializedName;

public class Choice {
    @SerializedName("message")
    public Message message;
}