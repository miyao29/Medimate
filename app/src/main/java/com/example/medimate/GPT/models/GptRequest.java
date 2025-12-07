package com.example.medimate.GPT.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GptRequest {

    @SerializedName("model")
    String model = "gpt-4o";

    @SerializedName("messages")
    List<Message> messages;

    @SerializedName("response_format")
    ResponseFormat responseFormat = new ResponseFormat("json_object");

    @SerializedName("temperature")
    double temperature = 0.3;

    public GptRequest(List<Message> messages) {
        this.messages = messages;
    }

    public static class ResponseFormat {
        @SerializedName("type")
        String type;
        public ResponseFormat(String type) { this.type = type; }
    }
}