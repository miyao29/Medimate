package com.example.medimate.recommendation.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface NaverShoppingApi {
    @GET("v1/search/shop.json")
    Call<NaverResponse> searchItems(
            @Header("X-Naver-Client-Id") String clientId,
            @Header("X-Naver-Client-Secret") String clientSecret,
            @Query("query") String query,
            @Query("display") int display, // 보여줄 개수 (예: 5)
            @Query("sort") String sort     // 정렬 (sim:유사도순, date:날짜순)
    );
}
