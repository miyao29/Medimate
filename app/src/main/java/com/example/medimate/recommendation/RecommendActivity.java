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

import com.example.medimate.BuildConfig;
import com.example.medimate.R;
import com.example.medimate.DrugActivity.LoadingDialog;
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

    // --- 1. API 키 설정 ---
    // 공공데이터포털에서 발급받은 '일반 인증키(Encoding)'입니다.
    // Retrofit 인터페이스에서 @Query(encoded=true) 설정을 했기 때문에 인코딩된 키를 그대로 사용합니다.
    private final String apiKey = BuildConfig.PUBLIC_DATA_KEY;

    // --- 2. UI 및 어댑터 ---
    private ProductAdapter productAdapter; // 리사이클러뷰에 데이터를 연결해줄 어댑터
    private LoadingDialog loadingDialog;   // 데이터 로딩 중에 보여줄 팝업창

    // --- 3. 데이터 저장소 ---
    // 공공데이터(식품안전나라)에서 가져온 제품들을 증상별로 분류해 저장하는 맵
    private final Map<String, List<Product>> groupedPublicData = new HashMap<>();
    // 네이버 쇼핑에서 검색된 최저가 상품들을 증상별로 저장하는 맵
    private final Map<String, List<NaverItem>> groupedNaverData = new HashMap<>();

    // --- 4. 로직 제어 변수 ---
    private int pendingNaverRequests = 0; // 진행 중인 네이버 API 요청 개수 (모두 0이 되면 로딩 종료)
    private ArrayList<String> globalChipTexts; // 사용자가 선택한 증상 목록 (예: ["간 건강", "피로"])
    private HashMap<String, ArrayList<String>> keywordMap; // 증상별 상세 검색 키워드 맵

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1) 화면 레이아웃 설정 (Edge-to-Edge 적용)
        setContentView(R.layout.activity_recommend);
        EdgeToEdge.enable(this);

        // 시스템 바(상태바, 내비게이션 바) 영역만큼 패딩을 줘서 내용이 가려지지 않게 함
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.recommend), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 2) 로딩 다이얼로그 초기화 및 문구 설정
        loadingDialog = new LoadingDialog(this);
        loadingDialog.setMessage("추천 결과를 불러오는 중입니다");

        // 3) 리스트뷰(RecyclerView) 설정
        setupRecyclerView();

        // 4) 이전 화면(HealthInputActivity)에서 넘겨준 데이터 받기
        globalChipTexts = getIntent().getStringArrayListExtra("selected_chip_texts");
        keywordMap = (HashMap<String, ArrayList<String>>) getIntent().getSerializableExtra("keyword_map");

        // 데이터가 제대로 안 넘어왔으면 종료 (안전 장치)
        if (globalChipTexts == null || globalChipTexts.isEmpty()) {
            Toast.makeText(this, "선택된 증상이 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 5) 로딩창 띄우고 데이터 분석 시작!
        loadingDialog.show();
        loadProductsAndGroupThem(1, 100); // 1페이지, 100개 요청
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        productAdapter = new ProductAdapter();
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(productAdapter);
    }

    // =================================================================
    // [STEP 1] 공공데이터(식품안전나라) 로딩
    // =================================================================
    private void loadProductsAndGroupThem(int pageNo, int numOfRows) {
        // Retrofit을 이용해 공공데이터 API 호출
        Call<FoodResponse> call = RetrofitClient.getInstance().getSupplements(apiKey, pageNo, numOfRows, "xml");

        call.enqueue(new Callback<FoodResponse>() {
            @Override
            public void onResponse(@NonNull Call<FoodResponse> call, @NonNull Response<FoodResponse> response) {
                List<Product> fetchedProducts = new ArrayList<>();

                // 통신 성공 및 데이터 존재 확인
                if (response.isSuccessful() && response.body() != null) {
                    FoodResponse foodResponse = response.body();
                    if (foodResponse.body != null && foodResponse.body.items != null && foodResponse.body.items.productList != null) {
                        fetchedProducts = foodResponse.body.items.productList;
                    }
                } else {
                    Log.e("RecommendActivity", "공공데이터 응답 실패");
                }

                // ★ 1-1. 가져온 데이터 필터링 (해외직구 등 제외)
                List<Product> filteredProducts = filterPublicData(fetchedProducts);

                // ★ 1-2. 성공하든 실패하든 무조건 다음 단계(네이버 검색)로 진행 (플랜 B)
                processAndSearchNaver(filteredProducts);
            }

            @Override
            public void onFailure(@NonNull Call<FoodResponse> call, @NonNull Throwable t) {
                // 타임아웃 등으로 통신 자체가 실패했을 때
                Log.e("RecommendActivity", "공공데이터 통신 오류", t);

                // ★ 에러가 나도 멈추지 않고 빈 리스트를 가지고 다음 단계로 진행
                processAndSearchNaver(new ArrayList<>());
            }
        });
    }

    // [유틸] 해외/직구/수출용 제품 걸러내기
    private List<Product> filterPublicData(List<Product> originalList) {
        List<Product> cleanList = new ArrayList<>();
        if (originalList == null) return cleanList;

        for (Product p : originalList) {
            String name = p.productName;
            if (name == null) continue;
            // 이름에 특정 키워드가 포함되면 리스트에서 제외
            if (name.contains("해외") || name.contains("직구") || name.contains("수출용")) {
                continue;
            }
            cleanList.add(p);
        }
        return cleanList;
    }

    // =================================================================
    // [STEP 2] 데이터 분류 및 네이버 검색어 추출
    // =================================================================
    private void processAndSearchNaver(List<Product> fetchedProducts) {
        groupedPublicData.clear();
        groupedNaverData.clear();
        pendingNaverRequests = 0; // 요청 카운터 초기화

        // 사용자가 선택한 증상(Chip)들을 하나씩 순회
        for (String chipText : globalChipTexts) {
            // 해당 증상에 맞는 검색 키워드 가져오기 (예: "간 건강" -> ["간", "피로"])
            List<String> keywords = keywordMap != null ? keywordMap.get(chipText) : null;

            // 2-1. 공공데이터에서 해당 증상과 관련된 제품 찾기
            List<Product> productsInGroup = new ArrayList<>();
            if (keywords != null && !fetchedProducts.isEmpty()) {
                for (Product product : fetchedProducts) {
                    if (product.mainFunction == null) continue;
                    for (String symptom : keywords) {
                        // 제품 기능에 내 증상 키워드가 포함되어 있으면 추가
                        if (product.mainFunction.contains(symptom)) {
                            productsInGroup.add(product);
                            break;
                        }
                    }
                }
            }
            groupedPublicData.put(chipText, productsInGroup);

            // 2-2. 네이버 검색에 쓸 '최적의 검색어' 결정
            // 찾은 제품들의 이름 중에서 가장 많이 등장한 단어를 추출 (예: "밀크씨슬")
            String searchKeyword = getBestKeyword(productsInGroup);

            // 만약 공공데이터에서 제품을 하나도 못 찾았다면? -> 그냥 증상 이름("간 건강")으로 검색
            if (searchKeyword.isEmpty()) {
                searchKeyword = chipText;
            }

            // 2-3. 네이버 쇼핑 API 호출 준비 (요청 수 +1)
            pendingNaverRequests++;
            fetchNaverShoppingForGroup(chipText, searchKeyword + " 영양제");
        }

        // 만약 선택한 증상이 없거나 해서 요청할 게 0개라면 바로 로딩 종료
        if (pendingNaverRequests == 0) {
            updateFinalList();
        }
    }

    // =================================================================
    // [STEP 3] 네이버 쇼핑 API 호출
    // =================================================================
    private void fetchNaverShoppingForGroup(String chipText, String keyword) {
        String clientId = BuildConfig.NAVER_CLIENT_ID; // 네이버 개발자 ID
        String clientSecret = BuildConfig.NAVER_CLIENT_SECRET;     // 네이버 개발자 Secret

        // 네이버 검색 요청 (유사도순 정렬, 20개 가져오기)
        Call<NaverResponse> call = RetrofitClient.getNaverInstance().searchItems(clientId, clientSecret, keyword, 20, "sim");

        call.enqueue(new Callback<NaverResponse>() {
            @Override
            public void onResponse(@NonNull Call<NaverResponse> call, @NonNull Response<NaverResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<NaverItem> rawItems = response.body().items;
                    List<NaverItem> itemsToShow = new ArrayList<>();

                    // 받아온 결과 중 상위 5개만 추려서 저장
                    if (rawItems != null) {
                        for (NaverItem item : rawItems) {
                            itemsToShow.add(item);
                            if (itemsToShow.size() >= 5) break;
                        }
                    }
                    groupedNaverData.put(chipText, itemsToShow);
                }
                // 요청 하나 끝났음을 보고
                checkAndLoadAllData();
            }

            @Override
            public void onFailure(Call<NaverResponse> call, Throwable t) {
                Log.e("NaverAPI", "검색 실패: " + chipText);
                // 실패했어도 보고는 해야 무한 로딩에 안 걸림
                checkAndLoadAllData();
            }
        });
    }

    // 모든 네이버 요청이 끝났는지 확인하는 동기화 함수
    private synchronized void checkAndLoadAllData() {
        pendingNaverRequests--; // 남은 요청 수 감소
        if (pendingNaverRequests <= 0) {
            // 모든 요청 처리가 끝났으면 최종 화면 업데이트!
            updateFinalList();
        }
    }

    // =================================================================
    // [STEP 4] 최종 화면 업데이트
    // =================================================================
    private void updateFinalList() {
        // ★ 로딩이 끝났으므로 로딩창 닫기
        if (loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }

        List<DisplayableItem> displayList = new ArrayList<>();

        // 칩 순서대로 결과 리스트 생성
        for (String chipText : globalChipTexts) {
            // 헤더 추가 (예: "'간 건강' 추천 영양제")
            displayList.add(new HeaderItem("'" + chipText + "' 추천 영양제"));

            // 해당 증상의 네이버 검색 결과 아이템들 추가
            List<NaverItem> naverItems = groupedNaverData.get(chipText);
            if (naverItems != null && !naverItems.isEmpty()) {
                for (NaverItem item : naverItems) {
                    displayList.add(new NaverProductItem(item));
                }
            }
        }

        if (displayList.isEmpty()) {
            Toast.makeText(RecommendActivity.this, "추천 결과를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
        } else {
            // 어댑터에 데이터 전달 -> 화면 갱신
            productAdapter.setItems(displayList);
        }
    }

    // [유틸] 제품 리스트에서 가장 많이 등장하는 단어(키워드) 추출
    private String getBestKeyword(List<Product> products) {
        if (products == null || products.isEmpty()) return "";

        HashMap<String, Integer> wordCount = new HashMap<>();
        for (Product p : products) {
            if (p.productName == null) continue;
            // 특수문자 제거
            String cleanName = p.productName.replaceAll("[^가-힣a-zA-Z0-9]", " ");
            String[] words = cleanName.split("\\s+");

            for (String w : words) {
                if (w.length() < 2) continue; // 1글자짜리는 무시
                // 검색에 도움 안 되는 일반 명사들은 제외 (불용어 처리)
                if (w.equals("비타민") || w.equals("캡슐") || w.equals("정") || w.equals("개월") ||
                        w.equals("분") || w.equals("코리아") || w.equals("제약") || w.equals("바이오") ||
                        w.equals("건강") || w.equals("기능성") || w.equals("식품") || w.equals("의약") ||
                        w.equals("플러스") || w.equals("골드") || w.equals("프리미엄") || w.equals("케어") ||
                        w.equals("박스") || w.equals("세트") || w.equals("리필") || w.equals("주식회사")) {
                    continue;
                }
                // 단어 등장 횟수 카운트
                wordCount.put(w, wordCount.getOrDefault(w, 0) + 1);
            }
        }

        // 가장 많이 나온 단어 선정
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