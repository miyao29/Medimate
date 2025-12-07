package com.example.medimate.GPT;

import com.example.medimate.GPT.models.GptRequest;
import com.example.medimate.GPT.models.GptResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface GptApiService {
    @POST("v1/chat/completions")
    Call<GptResponse> getChatCompletion(
            @Header("Authorization") String apiKey,
            @Body GptRequest requestBody
    );
}