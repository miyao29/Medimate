package com.example.medimate.recommendation.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

// API 요청 인터페이스
public interface FoodSafetyApi {
    //HTTP GET 방식으로 요청할 API임을 명시
    @GET("getHtfsItem01")
    Call<FoodResponse> getSupplements(
            @Query("ServiceKey") String apiKey,
            @Query("pageNo") int pageNo,
            @Query("numOfRows") int numOfRows,
            @Query("type") String type
    );
}
