package com.example.medimate.GPT;

import android.util.Log;
import com.google.gson.Gson;
import com.example.medimate.GPT.models.GptFullResponse;
import com.example.medimate.GPT.models.GptRequest;
import com.example.medimate.GPT.models.GptResponse;
import com.example.medimate.GPT.models.Message;
import java.util.Collections;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GptProcessor {

    public interface GptCallback {
        void onSuccess(GptFullResponse responseData);
        void onError(String errorMessage);
    }

    private GptApiService apiService;
    private Gson gson;

    public GptProcessor() {
        this.apiService = ApiClient.getApiService();
        this.gson = new Gson();
    }

    public void processText(String rawOcrText, String apiKey, GptCallback callback) {
        Log.d("GptProcessor", "Sending text to GPT-4o (Anti-Mixing Mode)...");

        String prompt = getSystemPrompt(rawOcrText);
        Message userMessage = new Message("user", prompt);

        // GptRequest 설정: model="gpt-4o", temperature=0.0 (변수 제거, 완전 정직 모드)
        GptRequest request = new GptRequest(Collections.singletonList(userMessage));

        apiService.getChatCompletion(apiKey, request).enqueue(new Callback<GptResponse>() {
            @Override
            public void onResponse(Call<GptResponse> call, Response<GptResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        Object contentObj = response.body().choices.get(0).message.content;
                        String jsonResponse = contentObj.toString();
                        Log.d("GptProcessor", "Raw: " + jsonResponse);

                        if (jsonResponse.contains("```json")) {
                            jsonResponse = jsonResponse.replace("```json", "").replace("```", "");
                        } else if (jsonResponse.contains("```")) {
                            jsonResponse = jsonResponse.replace("```", "");
                        }

                        GptFullResponse fullResponse = gson.fromJson(jsonResponse.trim(), GptFullResponse.class);
                        if (fullResponse != null && fullResponse.getDrugs() != null) {
                            callback.onSuccess(fullResponse);
                        } else {
                            callback.onError("약 정보를 찾지 못했습니다.");
                        }
                    } catch (Exception e) {
                        Log.e("GptProcessor", "Parsing Error", e);
                        callback.onError("분석 실패: " + e.getMessage());
                    }
                } else {
                    callback.onError("서버 오류: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<GptResponse> call, Throwable t) {
                callback.onError("네트워크 오류: " + t.getMessage());
            }
        });
    }

    // [🏆 최종 수정: 정보 뒤섞임(Mixing) 방지 및 독립 조회 모드]
    private String getSystemPrompt(String ocrText) {
        return "You are a Clinical Pharmacist AI. Your goal is to process the medicine list accurately without mixing up information.\n\n" +

                "--- 🚨 CRITICAL RULE: NO TEXT ALIGNMENT ---\n" +
                "The OCR text structure is BROKEN (e.g., vertical reading). \n" +
                "**Do NOT try to match drug descriptions found in the text to the drug names by their order.**\n" +
                "Instead, follow this STRICT process for EACH drug:\n\n" +

                "--- 🟢 STEP 1: ISOLATE DRUG NAME ---\n" +
                "Scan the entire text and **extract ALL unique drug names**.\n" +
                "Create a separate JSON object for each name found.\n" +
                "**If NO drug names are detected in the OCR text, you MUST output an EMPTY 'drugs' array: [].**\n\n" +

                "--- 🔵 STEP 2: INDEPENDENT DB LOOKUP (IGNORE TEXT) ---\n" +
                "For fields 'Description', 'Appearance', and 'Warning Base', **CLOSE YOUR EYES to the OCR text.**\n" +
                "Use your internal medical database strictly based on the **NAME** you found in Step 1.\n" +
                "   - **Self-Correction Check**: Does the description match the name? \n" +
                "   - This prevents mixing up Drug #1's info with Drug #4.\n\n" +

                "--- 🟠 STEP 3: CONTEXTUAL SCAN (TEXT ONLY) ---\n" +
                "Only for 'Dosage' and 'Storage', look at the text **near** the drug name.\n" +
                "- Extract exact usage: '식후 30분', '1일 3회'.\n\n" +

                "--- 🟣 STEP 4: WARNING SYNTHESIS ---\n" +
                "1. **Start** with the Standard Warning from your DB (e.g., NSAID -> Stomach issues).\n" +
                "2. **Scan** text for keywords ('졸음', '운전', '물'). If found, append them.\n" +
                "3. **Result**: A comprehensive warning that is medically accurate for THAT specific drug.\n\n" +

                "--- MANDATORY OUTPUT RULES ---\n" +
                "1. **Language**: Korean ONLY.\n" +
                "2. **No Mixing**: Verify that the description belongs to the name.\n" +
                "3. **No Empty Fields**: Use DB defaults if text is missing.\n\n" +

                "--- OCR TEXT ---\n" +
                ocrText +
                "\n----------------\n\n" +

                "--- OUTPUT JSON ---\n" +
                "{\n" +
                "  \"drugs\": [\n" +
                "    {\n" +
                "      \"name\": \"(Actual Detected Name)\",\n" +
                "      \"appearance_display\": \"(FROM DB ONLY)\", \"appearance_tts\": \"(Sentence)\",\n" +
                "      \"description_display\": \"(FROM DB ONLY)\", \"description_tts\": \"(Polite sentence)\",\n" +
                "      \"dosage_display\": \"(FROM TEXT)\", \"dosage_tts\": \"(FROM TEXT, polite)\",\n" +
                "      \"storage_display\": \"(FROM TEXT)\", \"storage_tts\": \"(FROM TEXT, polite)\",\n" +
                "      \"warning_display\": \"(DB + TEXT KEYWORDS)\", \"warning_tts\": \"(DB + TEXT KEYWORDS, polite)\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"common_instructions\": \"(Summary)\"\n" +
                "}";
        }
    }