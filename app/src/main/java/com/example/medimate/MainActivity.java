package com.example.medimate;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// ★ [UI 개선] 알약이 많으면 자동으로 다음 줄로 넘겨주는 레이아웃
import com.google.android.flexbox.FlexboxLayout;

import com.example.medimate.Login.MyInfoActivity;
import com.example.medimate.database.Medication;
import com.example.medimate.recommendation.HealthInputActivity;
import com.example.medimate.DrugActivity.MedimateActivity;
import com.example.medimate.DrugActivity.CameraGuideDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    // --- 1. UI 컴포넌트 변수 ---
    private TextView tvMonthTitle;         // 상단 월 표시 (예: "2025년 5월")
    private RecyclerView rvCalendar;       // 가로 스크롤 달력 리스트
    private RecyclerView rvMedicationList; // 하단 약 목록 리스트

    // --- 2. 어댑터 (데이터와 리스트뷰 연결 관리자) ---
    private MedicationAdapter medicationAdapter; // 약 목록 어댑터
    private CalendarAdapter calendarAdapter;     // 달력 어댑터

    // --- 3. 날짜 및 데이터 관련 변수 ---
    private Calendar currentCalendar = Calendar.getInstance(); // 현재 보고 있는 달(Month)
    private Calendar selectedDate = Calendar.getInstance();    // 사용자가 선택한 날짜 (기본값: 오늘)

    // 서버에서 받아온 '나의 모든 약' 데이터 저장소
    private List<Medication> allMedicationsDB = new ArrayList<>();

    // ★ [핵심] 오늘 복용 완료한(체크된) 약의 ID들을 저장하는 집합
    // "check_약ID_날짜_순서" 형태의 문자열들이 저장됨 (Set을 사용하여 중복 방지)
    private Set<String> completedMedications = new HashSet<>();

    // --- 4. 파이어베이스 인스턴스 ---
    private FirebaseFirestore db; // 데이터베이스 접근 객체
    private FirebaseAuth auth;    // 로그인 사용자 정보 객체

    // --- 5. 권한 요청 (알림) ---
    // 안드로이드 13(Tiramisu) 이상부터는 알림 권한을 사용자에게 직접 허용받아야 함
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "알림 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Edge-to-Edge 설정 (화면 전체 채우기)
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // 2. 파이어베이스 초기화 (가장 먼저 수행)
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // 3. 화면 패딩 설정 (시스템 바에 가려지지 않게 여백 조정)

        // (1) 전체 배경: 좌우 패딩을 0으로 해서 꽉 채움
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            return insets;
        });

        // (2) 상단 달력 영역: 상태바(시계) 높이만큼 아래로 내려서 내용 보호
        View topArea = findViewById(R.id.topCalendarArea);
        int originalTopPadding = topArea.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(topArea, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), originalTopPadding + systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        // (3) 하단 내비게이션 바: 제스처 바 높이만큼 위로 올려서 버튼 보호
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottomNavBar), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
            return insets;
        });

        // 4. 알림 권한 확인 및 요청
        checkNotificationPermission();

        // 5. UI 뷰 연결 (XML ID 찾기)
        tvMonthTitle = findViewById(R.id.tvMonthTitle);
        rvCalendar = findViewById(R.id.rvCalendar);
        rvMedicationList = findViewById(R.id.rvMedicationList);

        // 6. 버튼 클릭 이벤트 설정

        // [ < ] 이전 달로 이동
        findViewById(R.id.btnPrevMonth).setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            setupCalendar(); // 달력 UI 갱신
        });

        // [ > ] 다음 달로 이동
        findViewById(R.id.btnNextMonth).setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            setupCalendar(); // 달력 UI 갱신
        });

        // [ + ] 약 추가 버튼 (플로팅 버튼) -> 약 추가 화면으로 이동
        findViewById(R.id.fabAdd).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AddMedicationActivity.class));
        });

        // 하단 탭 버튼들 (추천, 카메라, 내정보) 설정
        setupClickListeners();

        // 7. 리스트(RecyclerView) 설정
        // 약 목록: 세로 리스트
        rvMedicationList.setLayoutManager(new LinearLayoutManager(this));
        // 어댑터 생성 시 '체크된 목록(completedMedications)'을 함께 전달
        medicationAdapter = new MedicationAdapter(new ArrayList<>(), completedMedications);
        rvMedicationList.setAdapter(medicationAdapter);

        // 달력: 가로 리스트
        rvCalendar.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // 달력 넘길 때 날짜가 중앙에 딱! 멈추게 하는 스냅 기능
        androidx.recyclerview.widget.SnapHelper snapHelper = new androidx.recyclerview.widget.LinearSnapHelper();
        snapHelper.attachToRecyclerView(rvCalendar);
    }

    // 화면이 다시 보일 때마다 호출됨 (데이터 최신화)
    @Override
    protected void onResume() {
        super.onResume();
        setupCalendar(); // 달력 날짜 다시 그리기
        loadMedicationData(); // ★ 서버에서 최신 약 데이터 불러오기
    }

    // --- [서버 로직 1] 파이어베이스에서 '내 약 목록' 불러오기 ---
    private void loadMedicationData() {
        FirebaseUser user = auth.getCurrentUser();
        // 로그인이 안 되어 있다면 리스트를 비우고 종료
        if (user == null) {
            allMedicationsDB.clear();
            medicationAdapter.updateData(new ArrayList<>(), "");
            return;
        }

        // Firestore 경로: "users/{내UID}/medications" 컬렉션에서 모든 약 데이터 가져오기
        db.collection("users").document(user.getUid())
                .collection("medications")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        allMedicationsDB.clear(); // 기존 목록 비우기
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                // 가져온 문서(JSON)를 자바 객체(Medication)로 변환
                                Medication med = document.toObject(Medication.class);
                                med.firebaseKey = document.getId(); // 문서 ID 저장 (식별용 필수)
                                allMedicationsDB.add(med);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        // 약 목록 로딩이 끝나면 -> 현재 날짜에 맞는 복용 기록(체크 여부)을 가져옵니다.
                        loadIntakeStatusForDate(selectedDate);
                    }
                });
    }

    // 날짜가 변경될 때 호출되는 중간 함수
    private void updateMedicationListForDate(Calendar date) {
        // 해당 날짜의 복용 기록(체크된 것들)을 서버에서 새로 가져옵니다.
        loadIntakeStatusForDate(date);
    }

    // --- [서버 로직 2] 서버에서 '해당 날짜의 복용 기록' 가져오기 ---
    private void loadIntakeStatusForDate(Calendar date) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        // 날짜를 "20250530" 형태의 문자열(Key)로 변환
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String dateKey = sdf.format(date.getTime());

        // Firestore 경로: "users/{내UID}/checks" (여기서 해당 날짜인 것만 골라냄)
        db.collection("users").document(user.getUid())
                .collection("checks")
                .whereEqualTo("date", dateKey) // "date" 필드가 선택한 날짜인 것만
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        completedMedications.clear(); // 기존 체크 기록 초기화
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // 체크된 항목들의 ID를 Set에 저장
                            completedMedications.add(document.getId());
                        }
                        // 체크 기록 로딩 완료 -> 최종적으로 화면(리스트) 갱신
                        filterAndShowList(date, dateKey);
                    }
                });
    }

    // --- [로직 3] 데이터 필터링 및 화면 표시 ---
    private void filterAndShowList(Calendar date, String dateKey) {
        List<MedicationModel> uiList = new ArrayList<>();
        Gson gson = new Gson();
        int dayOfWeek = date.get(Calendar.DAY_OF_WEEK); // 선택한 날의 요일 (1:일 ~ 7:토)

        // 시간 정보를 제거한 날짜 객체 생성 (날짜만 비교하기 위함)
        Calendar targetDate = (Calendar) date.clone();
        resetTime(targetDate);

        for (Medication dbMed : allMedicationsDB) {
            try {
                // (1) 요일 체크: 이 약을 먹는 요일인지 확인
                List<Integer> days = gson.fromJson(dbMed.daysOfWeek, new TypeToken<List<Integer>>(){}.getType());
                if (days == null || !days.contains(dayOfWeek)) continue; // 아니면 건너뜀

                // (2) 기간 체크: 복용 시작일 ~ 종료일 사이인지 확인
                Calendar startDate = Calendar.getInstance();
                startDate.setTimeInMillis(dbMed.startDate);
                resetTime(startDate);

                Calendar endDate = (Calendar) startDate.clone();
                // 종료일 = 시작일 + (총 복용일수 - 1)
                endDate.add(Calendar.DAY_OF_YEAR, dbMed.totalDays - 1);

                // 오늘 날짜가 기간 밖이라면 리스트에서 제외
                if (targetDate.before(startDate) || targetDate.after(endDate)) {
                    continue;
                }

                // (3) 통과한 약은 화면용 리스트에 추가
                List<String> times = gson.fromJson(dbMed.alarmTimes, new TypeToken<List<String>>(){}.getType());

                // MedicationModel에 약 정보와 시간 목록을 담음
                uiList.add(new MedicationModel(dbMed, "하루 " + (times != null ? times.size() : 0) + "번", times));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 어댑터에 최종 리스트와 날짜 키를 전달하여 화면 갱신!
        medicationAdapter.updateData(uiList, dateKey);
    }

    // 시간 정보(시/분/초)를 0으로 초기화하는 헬퍼 함수
    private void resetTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    // --- 달력 설정 ---
    private void setupCalendar() {
        // 제목 설정 (예: 2025년 5월)
        SimpleDateFormat format = new SimpleDateFormat("yyyy년 M월", Locale.KOREA);
        tvMonthTitle.setText(format.format(currentCalendar.getTime()));

        List<CalendarDayModel> dayList = new ArrayList<>();
        Calendar tempCal = (Calendar) currentCalendar.clone();
        tempCal.set(Calendar.DAY_OF_MONTH, 1); // 1일부터 시작
        int lastDay = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH); // 그 달의 마지막 날

        // 1일부터 말일까지 리스트 생성
        for (int i = 1; i <= lastDay; i++) {
            String dayOfWeek = new SimpleDateFormat("E", Locale.ENGLISH).format(tempCal.getTime());
            dayList.add(new CalendarDayModel(i, dayOfWeek, tempCal));
            tempCal.add(Calendar.DAY_OF_MONTH, 1);
        }

        if (calendarAdapter == null) {
            calendarAdapter = new CalendarAdapter(dayList);
            rvCalendar.setAdapter(calendarAdapter);
        } else {
            calendarAdapter.setDays(dayList);
        }

        // 달 이동 시 '오늘' 또는 '1일' 자동 선택 로직
        int targetPos = 0;
        Calendar today = Calendar.getInstance();
        if (currentCalendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                currentCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR)) {
            targetPos = today.get(Calendar.DAY_OF_MONTH) - 1; // 이번 달이면 오늘 날짜 선택
        }

        // 해당 위치로 스크롤 및 선택
        rvCalendar.scrollToPosition(targetPos);
        calendarAdapter.setSelectedPosition(targetPos);

        // 선택된 날짜 변수 업데이트
        Calendar targetDate = (Calendar) currentCalendar.clone();
        targetDate.set(Calendar.DAY_OF_MONTH, targetPos + 1);
        selectedDate = targetDate;

        // 데이터가 있다면 필터링 실행
        if (!allMedicationsDB.isEmpty()) {
            loadIntakeStatusForDate(selectedDate);
        }
    }

    // 알림 권한 체크 (Android 13+)
    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    // 하단 탭 버튼 클릭 리스너 설정
    private void setupClickListeners() {
        // 추천받기
        findViewById(R.id.btnRecommend).setOnClickListener(v -> startActivity(new Intent(this, HealthInputActivity.class)));

        // 카메라 촬영
        findViewById(R.id.btnCamera).setOnClickListener(v -> {
            CameraGuideDialog dialog = new CameraGuideDialog(this, new CameraGuideDialog.OnStartCameraListener() {
                @Override
                public void onStartCamera() {
                    Intent intent = new Intent(MainActivity.this, MedimateActivity.class);
                    startActivity(intent);
                }
            });
            dialog.show();
        });

        // 내 정보
        findViewById(R.id.btnMyInfo).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MyInfoActivity.class);
            intent.putExtra("uid", getIntent().getStringExtra("uid")); // UID 전달
            startActivity(intent);
        });
    }

    // --- 내부 데이터 모델 ---

    // 화면에 표시할 약 정보 모델
    class MedicationModel {
        Medication originalData; // 원본 데이터
        String frequency;        // "하루 3번" 텍스트
        List<String> timeList;   // 복용 시간 리스트
        public MedicationModel(Medication data, String frequency, List<String> timeList) {
            this.originalData = data;
            this.frequency = frequency;
            this.timeList = timeList;
        }
    }

    // 달력 날짜 정보 모델
    class CalendarDayModel {
        int day; String dayOfWeek; Calendar fullDate;
        public CalendarDayModel(int day, String dayOfWeek, Calendar fullDate) {
            this.day = day; this.dayOfWeek = dayOfWeek;
            this.fullDate = (Calendar) fullDate.clone();
        }
    }

    // --- 어댑터 (RecyclerView) ---

    // 약 목록 어댑터
    class MedicationAdapter extends RecyclerView.Adapter<MedicationAdapter.ViewHolder> {
        List<MedicationModel> list;
        Set<String> completedSet; // 체크된 항목 ID 모음
        String currentDateKey = ""; // 현재 날짜 키 (예: "20250530")

        public MedicationAdapter(List<MedicationModel> list, Set<String> completedSet) {
            this.list = list;
            this.completedSet = completedSet;
        }

        // 데이터 업데이트 함수
        public void updateData(List<MedicationModel> newList, String dateKey) {
            this.list = newList;
            this.currentDateKey = dateKey;
            notifyDataSetChanged();
        }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medication, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MedicationModel model = list.get(position);
            holder.tvName.setText(model.originalData.name);
            holder.tvCount.setText(model.frequency);

            holder.layoutTimeSlots.removeAllViews();

            // 시간표(알약) 동적 생성
            if (model.timeList != null) {
                LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());

                for (int i = 0; i < model.timeList.size(); i++) {
                    String timeStr = model.timeList.get(i);
                    View pill = inflater.inflate(R.layout.item_time_slot, holder.layoutTimeSlots, false);

                    TextView tvAmPm = pill.findViewById(R.id.tvAmPm);
                    TextView tvTime = pill.findViewById(R.id.tvTime);

                    try {
                        String[] parts = timeStr.split(":");
                        int h = Integer.parseInt(parts[0]);
                        String amPm = h < 12 ? "오전" : "오후";
                        int displayH = h > 12 ? h - 12 : (h == 0 ? 12 : h);
                        tvAmPm.setText(amPm);
                        tvTime.setText(String.format(Locale.getDefault(), "%d:%02d", displayH, Integer.parseInt(parts[1])));
                    } catch (Exception e) { tvTime.setText(timeStr); }

                    // 체크 상태 확인을 위한 고유 키 생성
                    String checkKey = "check_" + model.originalData.firebaseKey + "_" + currentDateKey + "_" + i;

                    // 현재 이 약이 체크되어 있는지 확인
                    boolean isChecked = completedSet.contains(checkKey);
                    setPillSelectedState(pill, tvAmPm, tvTime, isChecked);

                    // [클릭 이벤트] 체크 상태 변경 및 서버 저장
                    pill.setOnClickListener(v -> {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user == null) return;

                        boolean newState = !pill.isSelected(); // 반대 상태로 변경

                        // 1. 화면 즉시 갱신 (빠른 반응 속도)
                        setPillSelectedState(pill, tvAmPm, tvTime, newState);
                        if (newState) completedSet.add(checkKey);
                        else completedSet.remove(checkKey);

                        // 2. 파이어베이스 DB에 저장 또는 삭제 (백그라운드)
                        if (newState) {
                            Map<String, Object> data = new HashMap<>();
                            data.put("date", currentDateKey);
                            data.put("medId", model.originalData.firebaseKey);
                            data.put("timestamp", System.currentTimeMillis());
                            // 체크 기록 저장
                            db.collection("users").document(user.getUid())
                                    .collection("checks").document(checkKey).set(data);
                        } else {
                            // 체크 기록 삭제
                            db.collection("users").document(user.getUid())
                                    .collection("checks").document(checkKey).delete();
                        }
                    });

                    holder.layoutTimeSlots.addView(pill);
                }
            }

            // 카드 클릭 시 수정 화면으로 이동
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), AddMedicationActivity.class);
                intent.putExtra("medication_data", model.originalData); // Serializable 객체 전달
                v.getContext().startActivity(intent);
            });
        }

        // 알약 뷰의 선택 상태(색상 등) 변경 헬퍼
        private void setPillSelectedState(View root, TextView v1, TextView v2, boolean isSelected) {
            root.setSelected(isSelected);
            v1.setSelected(isSelected);
            v2.setSelected(isSelected);
            View divider = ((LinearLayout)root).getChildAt(1);
            if(divider != null) divider.setSelected(isSelected);
        }

        @Override public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvCount;

            // 자동 줄바꿈 처리
            FlexboxLayout layoutTimeSlots;

            public ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvMedName);
                tvCount = v.findViewById(R.id.tvFrequency);
                layoutTimeSlots = v.findViewById(R.id.layoutTimeSlots);
            }
        }
    }

    // 달력 어댑터
    class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {
        List<CalendarDayModel> days;
        int selectedPosition = -1;

        public CalendarAdapter(List<CalendarDayModel> days) { this.days = days; }
        public void setDays(List<CalendarDayModel> days) { this.days = days; notifyDataSetChanged(); }
        public void setSelectedPosition(int pos) { selectedPosition = pos; notifyDataSetChanged(); }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_date, parent, false));
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CalendarDayModel day = days.get(position);
            holder.tvNum.setText(String.valueOf(day.day));
            holder.tvDayOfWeek.setText(day.dayOfWeek);

            // 선택된 날짜 스타일 적용 (동그라미 배경 등)
            if (selectedPosition == position) {
                holder.tvNum.setTextColor(getColor(android.R.color.white));
                holder.tvNum.setBackgroundResource(R.drawable.bg_circle_selector);
                holder.tvNum.setBackgroundTintList(null);
            } else {
                holder.tvNum.setTextColor(getColor(android.R.color.black));
                holder.tvNum.setBackground(null);
            }

            // 날짜 클릭 이벤트
            holder.itemView.setOnClickListener(v -> {
                int previousPos = selectedPosition;
                selectedPosition = holder.getAdapterPosition();
                notifyItemChanged(previousPos);
                notifyItemChanged(selectedPosition);

                selectedDate = day.fullDate;
                // 날짜가 바뀌었으므로 해당 날짜의 '복용 기록'을 다시 불러옴
                loadIntakeStatusForDate(selectedDate);
            });
        }
        @Override public int getItemCount() { return days.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvNum, tvDayOfWeek;
            public ViewHolder(View v) { super(v); tvNum = v.findViewById(R.id.tvDayNumber); tvDayOfWeek = v.findViewById(R.id.tvDayOfWeek); }
        }
    }
}