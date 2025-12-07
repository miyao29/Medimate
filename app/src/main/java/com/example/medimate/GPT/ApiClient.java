package com.example.medimate.GPT;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String BASE_URL = "https://api.openai.com/";
    private static Retrofit retrofit = null;

    public static GptApiService getApiService() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

            httpClient.connectTimeout(60, TimeUnit.SECONDS); // 연결 타임아웃
            httpClient.readTimeout(60, TimeUnit.SECONDS);    // 읽기 타임아웃
            httpClient.writeTimeout(60, TimeUnit.SECONDS);   // 쓰기 타임아웃

            httpClient.addInterceptor(logging);

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(httpClient.build())
                    .build();
        }
        return retrofit.create(GptApiService.class);
    }
}
