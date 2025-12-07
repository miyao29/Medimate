package com.example.medimate.recommendation.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import java.util.concurrent.TimeUnit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    //API요청의 기본 서버 주소
    private static final String BASE_URL = "https://apis.data.go.kr/1471000/HtfsInfoService03/";

    private static FoodSafetyApi instance;

    public static synchronized FoodSafetyApi getInstance() {
        if (instance == null) {
            //네트워크 통신 로그를 Logcat에 출력하기 위한 인터셉터 설정
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY); //BODY: 요청/응답의 모든 내용 보여줌

            //네트워크 통신 세부 설정을 위한 OkHttpClient 객체 생성
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging) //위에서 만든 로깅 인터셉터 추가
                    .connectTimeout(5, TimeUnit.SECONDS) //서버 연결 시도 시간
                    .readTimeout(5, TimeUnit.SECONDS) //서버로부터 응답을 기다리는 시간
                    .writeTimeout(5, TimeUnit.SECONDS) //서버로 데이터를 보내는 시간
                    .build();

            //Retrofit(통신 기능에 사용하는 코드를 쉽게 만들어놓은) 라이브러리 사용
            instance = new Retrofit.Builder()
                    .baseUrl(BASE_URL) //기본 서버 주소 설정
                    .client(client) //위에서 만든 설정 사용
                    // 서버로 받은 XML 데이터를 우리가 만든 Kotlin 데이터 클래스로 변환해줄 변환기 설정
                    .addConverterFactory(SimpleXmlConverterFactory.create())
                    .build()
                    .create(FoodSafetyApi.class); //FoodSafetyApi 인터페이스의 구현체를 자동으로 만들어 반환
        }
        return instance;
    }

    private static NaverShoppingApi naverInstance;

    public static synchronized NaverShoppingApi getNaverInstance() {
        if (naverInstance == null) {
            naverInstance = new Retrofit.Builder()
                    .baseUrl("https://openapi.naver.com/") // 네이버 주소
                    .addConverterFactory(GsonConverterFactory.create()) // JSON 변환기 사용
                    .build()
                    .create(NaverShoppingApi.class);
        }
        return naverInstance;
    }
}
