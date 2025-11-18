package com.example.medimate.recommendation;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.medimate.R;
import com.example.medimate.recommendation.api.FoodResponse;
import com.example.medimate.recommendation.api.NaverItem;
import com.example.medimate.recommendation.api.NaverResponse;
import com.example.medimate.recommendation.api.Product;
import com.example.medimate.recommendation.api.RetrofitClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecommendActivity extends AppCompatActivity {
    private final String apiKey = "5vdpC0fiXEBDtS8A/bOV5Ql5cWmDmsIKEcpv4bryubBdLpyXAnET8rszjBUPgqHL3uCOgQhz2GDc/aI3x1CHQg==";
    private ProductAdapter productAdapter;

    // ▼ 데이터 임시 저장소 (증상별로 데이터를 따로 모아두기 위함)
    private final Map<String, List<Product>> groupedPublicData = new HashMap<>();
    private final Map<String, List<NaverItem>> groupedNaverData = new HashMap<>();

    // ▼ 네이버 API 요청이 몇 개 남았는지 세는 카운터
    private int pendingNaverRequests = 0;
    private ArrayList<String> globalChipTexts; // 순서 유지를 위해 저장

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommend);
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.recommend), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupRecyclerView();
        loadProductsAndGroupThem(1, 100);
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        productAdapter = new ProductAdapter();
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(productAdapter);
    }

    // 1. 공공데이터 로딩 및 그룹화 시작
    private void loadProductsAndGroupThem(int pageNo, int numOfRows) {
        Call<FoodResponse> call = RetrofitClient.getInstance().getSupplements(apiKey, pageNo, numOfRows, "xml");

        call.enqueue(new Callback<FoodResponse>() {
            @Override
            public void onResponse(@NonNull Call<FoodResponse> call, @NonNull Response<FoodResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    FoodResponse foodResponse = response.body();
                    List<Product> fetchedProducts = new ArrayList<>();
                    if (foodResponse.body != null && foodResponse.body.items != null && foodResponse.body.items.productList != null) {
                        fetchedProducts = foodResponse.body.items.productList;
                    }

                    // 데이터 초기화
                    groupedPublicData.clear();
                    groupedNaverData.clear();
                    pendingNaverRequests = 0;

                    // Intent 데이터 받기
                    globalChipTexts = getIntent().getStringArrayListExtra("selected_chip_texts");
                    HashMap<String, ArrayList<String>> keywordMap = (HashMap<String, ArrayList<String>>) getIntent().getSerializableExtra("keyword_map");

                    if (globalChipTexts == null || keywordMap == null) {
                        Toast.makeText(RecommendActivity.this, "데이터 로딩 실패", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // ▼ 각 칩(증상)별로 반복문 시작
                    for (String chipText : globalChipTexts) {
                        List<String> keywords = keywordMap.get(chipText);
                        if (keywords == null) continue;

                        // 1. 이 그룹에 해당하는 공공데이터 제품 찾기
                        List<Product> productsInGroup = new ArrayList<>();
                        for (Product product : fetchedProducts) {
                            if (product.mainFunction == null) continue;
                            for (String symptom : keywords) {
                                if (product.mainFunction.contains(symptom)) {
                                    productsInGroup.add(product);
                                    break;
                                }
                            }
                        }
                        // 맵에 저장 (나중에 화면에 뿌릴 때 사용)
                        groupedPublicData.put(chipText, productsInGroup);

                        // 2. 이 그룹을 위한 네이버 검색어 결정 (제품이 있으면 최빈 단어, 없으면 칩 이름)
                        String searchKeyword = getBestKeyword(productsInGroup);
                        if (searchKeyword.isEmpty()) {
                            searchKeyword = chipText; // 제품이 없으면 '간 건강' 등으로 바로 검색
                        }

                        // 3. 네이버 검색 요청 (비동기)
                        pendingNaverRequests++; // 요청 하나 추가
                        fetchNaverShoppingForGroup(chipText, searchKeyword + " 영양제");
                    }

                    // 만약 요청할 게 하나도 없다면 바로 화면 갱신
                    if (pendingNaverRequests == 0) {
                        updateFinalList();
                    }

                } else {
                    Log.e("RecommendActivity", "API 응답 없음");
                    Toast.makeText(RecommendActivity.this, "데이터 로딩 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<FoodResponse> call, @NonNull Throwable t) {
                Log.e("RecommendActivity", "API 호출 실패", t);
                Toast.makeText(RecommendActivity.this, "데이터 로딩 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 2. 특정 그룹(증상)을 위한 네이버 쇼핑 검색
    private void fetchNaverShoppingForGroup(String chipText, String keyword) {
        // ★ 네이버 API ID/SECRET 입력
        String clientId = "4eXlq_OLVRz9jCUBp8Jh";
        String clientSecret = "pXCrt0f9Yz";

        Call<NaverResponse> call = RetrofitClient.getNaverInstance().searchItems(clientId, clientSecret, keyword, 5, "sim");

        call.enqueue(new Callback<NaverResponse>() {
            @Override
            public void onResponse(Call<NaverResponse> call, Response<NaverResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 결과를 맵에 저장 (키: 증상 이름)
                    groupedNaverData.put(chipText, response.body().items);
                }
                checkAndLoadAllData(); // 요청 하나 끝났다고 보고
            }

            @Override
            public void onFailure(Call<NaverResponse> call, Throwable t) {
                Log.e("NaverAPI", "검색 실패: " + chipText);
                checkAndLoadAllData(); // 실패해도 일단 보고 (무한 대기 방지)
            }
        });
    }

    // 3. 모든 네이버 요청이 끝났는지 확인하는 함수
    private synchronized void checkAndLoadAllData() {
        pendingNaverRequests--; // 남은 요청 수 감소
        if (pendingNaverRequests <= 0) {
            // 모든 요청이 끝났으면 화면 그리기!
            updateFinalList();
        }
    }

    // 4. 최종적으로 리스트를 합쳐서 어댑터에 넣는 함수
    private void updateFinalList() {
        List<DisplayableItem> displayList = new ArrayList<>();

        // 사용자가 선택한 칩 순서대로 데이터를 쌓음
        for (String chipText : globalChipTexts) {

            // (1) 헤더 추가
            displayList.add(new HeaderItem("'" + chipText + "'을 위한 추천"));

            // (2) 네이버 쇼핑 결과 5개 추가
            List<NaverItem> naverItems = groupedNaverData.get(chipText);
            if (naverItems != null && !naverItems.isEmpty()) {
                for (NaverItem item : naverItems) {
                    displayList.add(new NaverProductItem(item));
                }
            }
        }

        if (displayList.isEmpty()) {
            Toast.makeText(RecommendActivity.this, "관련 제품을 찾지 못했습니다.", Toast.LENGTH_LONG).show();
        } else {
            productAdapter.setItems(displayList);
        }
    }

    // 최빈 단어 추출 함수
    // 최빈 단어 추출 함수 (업그레이드 버전)
    private String getBestKeyword(List<Product> products) {
        if (products == null || products.isEmpty()) return "";

        HashMap<String, Integer> wordCount = new HashMap<>();
        for (Product p : products) {
            if (p.productName == null) continue;
            // 특수문자 제거
            String cleanName = p.productName.replaceAll("[^가-힣a-zA-Z0-9]", " ");
            String[] words = cleanName.split("\\s+");

            for (String w : words) {
                if (w.length() < 2) continue;

                // ★ 제외할 단어 대폭 추가 (회사명, 제형, 무의미한 수식어 등)
                if (w.equals("비타민") || w.equals("캡슐") || w.equals("정") || w.equals("개월") ||
                        w.equals("분") || w.equals("코리아") || w.equals("제약") || w.equals("바이오") ||
                        w.equals("건강") || w.equals("기능성") || w.equals("식품") || w.equals("의약") ||
                        w.equals("플러스") || w.equals("골드") || w.equals("프리미엄") || w.equals("케어") ||
                        w.equals("박스") || w.equals("세트") || w.equals("리필") || w.equals("주식회사") ||
                        w.equals("포") || w.equals("스틱") || w.equals("액") || w.equals("환")) {
                    continue;
                }

                wordCount.put(w, wordCount.getOrDefault(w, 0) + 1);
            }
        }

        String bestKeyword = "";
        int maxCount = 0;
        for (String key : wordCount.keySet()) {
            if (wordCount.get(key) > maxCount) {
                maxCount = wordCount.get(key);
                bestKeyword = key;
            }
        }
        return bestKeyword;
    }
}