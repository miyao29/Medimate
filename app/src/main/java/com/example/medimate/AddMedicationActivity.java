package com.example.medimate;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.medimate.database.Medication;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddMedicationActivity extends AppCompatActivity {

    // --- 1. UI 컴포넌트 변수 선언 ---
    private EditText etMedName, etTotalDays; // 약 이름, 총 복용 기간 입력 필드
    private TextView tvCount, btnDelete;     // 하루 복용 횟수 표시, 삭제 버튼
    private LinearLayout layoutTimeContainer; // 복용 시간 입력칸들이 동적으로 추가될 레이아웃

    // --- 2. 데이터 관리 변수 ---
    private int frequencyCount = 0; // 현재 설정된 하루 복용 횟수
    private List<TimeData> timeList = new ArrayList<>(); // 설정된 시간 정보들을 담는 리스트

    // --- 3. 수정 모드 관련 변수 ---
    private boolean isEditMode = false;          // 현재 화면이 '수정' 모드인지 '추가' 모드인지 확인
    private Medication currentMedication = null; // 수정 모드일 때 받아온 기존 약 데이터 객체

    // --- 4. 파이어베이스 관련 변수 ---
    private FirebaseFirestore db; // 데이터베이스 접근 객체
    private FirebaseAuth auth;    // 사용자 인증 객체 (로그인 정보 확인용)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1) 화면 레이아웃 설정 (Edge-to-Edge: 전체 화면 채우기)
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_medication);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            final int originalPaddingLeft = mainView.getPaddingLeft();
            final int originalPaddingTop = mainView.getPaddingTop();
            final int originalPaddingRight = mainView.getPaddingRight();
            final int originalPaddingBottom = mainView.getPaddingBottom();

            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(
                        originalPaddingLeft + systemBars.left,
                        originalPaddingTop + systemBars.top,
                        originalPaddingRight + systemBars.right,
                        originalPaddingBottom + systemBars.bottom
                );
                return insets;
            });
        }

        // 2) 파이어베이스 초기화
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // 3) UI 뷰 연결 (XML ID와 자바 변수 매칭)
        etMedName = findViewById(R.id.etMedName);
        etTotalDays = findViewById(R.id.etTotalDays);
        tvCount = findViewById(R.id.tvCount);
        layoutTimeContainer = findViewById(R.id.layoutTimeContainer);
        btnDelete = findViewById(R.id.btnDelete);

        // 4) 버튼 클릭 리스너 설정
        findViewById(R.id.btnBack).setOnClickListener(v -> finish()); // 뒤로가기 (취소)
        findViewById(R.id.btnSave).setOnClickListener(v -> saveDataAndSchedule()); // 저장 버튼

        // 삭제 버튼 처리
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> deleteMedication());
        }

        // 복용 횟수 조절 버튼 (+ / -) 리스너
        findViewById(R.id.btnPlus).setOnClickListener(v -> updateFrequency(frequencyCount + 1));
        findViewById(R.id.btnMinus).setOnClickListener(v -> {
            if (frequencyCount > 0) updateFrequency(frequencyCount - 1);
        });

        // 5) 수정 모드인지 확인 및 데이터 로딩
        if (getIntent().hasExtra("medication_data")) {
            isEditMode = true;
            currentMedication = (Medication) getIntent().getSerializableExtra("medication_data");

            // 기존 데이터를 화면에 채워 넣음
            loadMedicationData(currentMedication);

            if (btnDelete != null) btnDelete.setVisibility(View.VISIBLE);
        } else {
            if (btnDelete != null) btnDelete.setVisibility(View.GONE);
            updateFrequency(0); // 시간 입력칸 0개로 시작
        }
    }

    // --- UI 업데이트 메소드 ---

    private void updateFrequency(int newCount) {
        while (frequencyCount < newCount) {
            addTimeRow(frequencyCount + 1);
            frequencyCount++;
        }
        while (frequencyCount > newCount) {
            if (!timeList.isEmpty()) {
                timeList.remove(timeList.size() - 1);
                layoutTimeContainer.removeViewAt(layoutTimeContainer.getChildCount() - 1);
            }
            frequencyCount--;
        }
        tvCount.setText(String.valueOf(frequencyCount));
    }

    private void addTimeRow(int index) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View timeView = inflater.inflate(R.layout.item_time_row, layoutTimeContainer, false);

        TextView tvLabel = timeView.findViewById(R.id.tvTimeLabel);
        TextView tvValue = timeView.findViewById(R.id.tvTimeValue);

        tvLabel.setText("복용시간 " + index);

        // 기본 시간 설정 (오전 8:00)
        TimeData newTime = new TimeData(8, 0);
        timeList.add(newTime);
        tvValue.setText(formatTime(8, 0));

        // 시간 텍스트 클릭 시 시계 다이얼로그
        timeView.setOnClickListener(v -> {
            TimePickerDialog dialog = new TimePickerDialog(
                    this,
                    android.R.style.Theme_Holo_Light_Dialog_NoActionBar,
                    (view, hourOfDay, minute) -> {
                        newTime.hour = hourOfDay;
                        newTime.minute = minute;
                        tvValue.setText(formatTime(hourOfDay, minute));
                    },
                    newTime.hour,
                    newTime.minute,
                    false
            );
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
            dialog.show();
        });

        layoutTimeContainer.addView(timeView);
    }

    private String formatTime(int hour, int minute) {
        String amPm = hour < 12 ? "오전" : "오후";
        int displayHour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);
        return String.format(Locale.getDefault(), "%s %d:%02d", amPm, displayHour, minute);
    }

    // --- 데이터 로딩 (수정 모드용) ---

    private void loadMedicationData(Medication med) {
        etMedName.setText(med.name);

        Gson gson = new Gson();
        List<Integer> days = gson.fromJson(med.daysOfWeek, new TypeToken<List<Integer>>() {}.getType());

        // [수정됨] DB에 저장된 '총 기간'을 보여주는 게 아니라,
        // 그 기간 동안 '실제 몇 번 복용하는지'를 역으로 계산해서 보여줌
        if (days != null) {
            int originalCount = calculateDoseCount(med.startDate, med.totalDays, days);
            etTotalDays.setText(String.valueOf(originalCount));
        } else {
            // 요일 정보가 없으면 그냥 기간을 표시 (예외 처리)
            etTotalDays.setText(String.valueOf(med.totalDays));
        }

        if (days != null) {
            if (days.contains(Calendar.MONDAY)) ((CompoundButton) findViewById(R.id.chipMon)).setChecked(true);
            if (days.contains(Calendar.TUESDAY)) ((CompoundButton) findViewById(R.id.chipTue)).setChecked(true);
            if (days.contains(Calendar.WEDNESDAY)) ((CompoundButton) findViewById(R.id.chipWed)).setChecked(true);
            if (days.contains(Calendar.THURSDAY)) ((CompoundButton) findViewById(R.id.chipThu)).setChecked(true);
            if (days.contains(Calendar.FRIDAY)) ((CompoundButton) findViewById(R.id.chipFri)).setChecked(true);
            if (days.contains(Calendar.SATURDAY)) ((CompoundButton) findViewById(R.id.chipSat)).setChecked(true);
            if (days.contains(Calendar.SUNDAY)) ((CompoundButton) findViewById(R.id.chipSun)).setChecked(true);
        }

        List<String> times = gson.fromJson(med.alarmTimes, new TypeToken<List<String>>() {}.getType());
        if (times != null) {
            updateFrequency(times.size());
            for (int i = 0; i < times.size(); i++) {
                String[] parts = times.get(i).split(":");
                int h = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]);

                timeList.get(i).hour = h;
                timeList.get(i).minute = m;

                View row = layoutTimeContainer.getChildAt(i);
                TextView tvValue = row.findViewById(R.id.tvTimeValue);
                tvValue.setText(formatTime(h, m));
            }
        }
    }

    /**
     * [추가됨] 저장된 기간(duration) 동안 실제로 약을 먹는 날이 며칠인지 계산하는 함수
     * (화면에 보여줄 때 원래 입력값 '5'를 복원하기 위함)
     */
    private int calculateDoseCount(long startDate, int totalDurationDays, List<Integer> daysOfWeek) {
        int count = 0;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(startDate);

        // 저장된 기간(예: 8일) 동안 하루씩 넘기면서 체크
        for (int i = 0; i < totalDurationDays; i++) {
            int currentDay = cal.get(Calendar.DAY_OF_WEEK);
            // 복용하는 요일이면 카운트 증가
            if (daysOfWeek.contains(currentDay)) {
                count++;
            }
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        return count;
    }

    // --- 삭제 로직 ---

    private void deleteMedication() {
        FirebaseUser user = auth.getCurrentUser();
        // 안전장치
        if (user == null || currentMedication == null || currentMedication.firebaseKey == null) return;

        // 1. 다이얼로그 생성 (바로 show하지 않고 변수에 담습니다)
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("약 삭제")
                .setMessage("정말 이 약을 삭제하시겠습니까?")
                .setPositiveButton("삭제", (d, which) -> {
                    // --- 삭제 로직 시작 ---
                    cancelAlarms(currentMedication); // 로컬 알람 취소

                    // 파이어베이스 데이터 삭제
                    db.collection("users").document(user.getUid())
                            .collection("medications").document(currentMedication.firebaseKey)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    // --- 삭제 로직 끝 ---
                })
                .setNegativeButton("취소", null)
                .create();

        // 2. 화면에 띄우기
        dialog.show();

        // 3. 버튼 색상 변경
        // "삭제" 버튼 -> 빨간색 (경고 느낌)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.RED);

        // "취소" 버튼 -> 검은색
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.BLACK);
    }

    private void cancelAlarms(Medication med) {
        if (med == null) return;
        Gson gson = new Gson();
        List<Integer> days = gson.fromJson(med.daysOfWeek, new TypeToken<List<Integer>>() {}.getType());
        List<String> times = gson.fromJson(med.alarmTimes, new TypeToken<List<String>>() {}.getType());
        if (days == null || times == null) return;

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);

        for (int day : days) {
            for (String t : times) {
                String[] parts = t.split(":");
                int h = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]);
                int alarmId = (day * 10000) + (h * 100) + m;

                PendingIntent pi = PendingIntent.getBroadcast(
                        this, alarmId, intent,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

                if (alarmManager != null) {
                    alarmManager.cancel(pi);
                }
            }
        }
    }

    // --- 저장 및 알람 등록 ---

    private void saveDataAndSchedule() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = etMedName.getText().toString();
        String daysStr = etTotalDays.getText().toString();

        // 사용자 입력값: 복용 횟수
        int targetDoseCount = daysStr.isEmpty() ? 30 : Integer.parseInt(daysStr);

        if (name.isEmpty()) {
            Toast.makeText(this, "약 이름을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Integer> selectedDays = new ArrayList<>();
        if (((CompoundButton) findViewById(R.id.chipMon)).isChecked()) selectedDays.add(Calendar.MONDAY);
        if (((CompoundButton) findViewById(R.id.chipTue)).isChecked()) selectedDays.add(Calendar.TUESDAY);
        if (((CompoundButton) findViewById(R.id.chipWed)).isChecked()) selectedDays.add(Calendar.WEDNESDAY);
        if (((CompoundButton) findViewById(R.id.chipThu)).isChecked()) selectedDays.add(Calendar.THURSDAY);
        if (((CompoundButton) findViewById(R.id.chipFri)).isChecked()) selectedDays.add(Calendar.FRIDAY);
        if (((CompoundButton) findViewById(R.id.chipSat)).isChecked()) selectedDays.add(Calendar.SATURDAY);
        if (((CompoundButton) findViewById(R.id.chipSun)).isChecked()) selectedDays.add(Calendar.SUNDAY);

        if (selectedDays.isEmpty()) {
            Toast.makeText(this, "요일을 최소 하나 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (timeList.isEmpty()) {
            Toast.makeText(this, "복용 횟수를 최소 1회 이상 설정해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        long finalStartDate;
        if (isEditMode && currentMedication != null) {
            finalStartDate = currentMedication.startDate;
        } else {
            finalStartDate = System.currentTimeMillis();
        }

        // 실제 기간 계산 (예: 5번 먹으려면 8일 필요 -> 8일 저장)
        int realDurationDays = calculateRealDuration(finalStartDate, selectedDays, targetDoseCount);

        List<String> timeStrList = new ArrayList<>();
        for (TimeData t : timeList) {
            timeStrList.add(t.hour + ":" + t.minute);
        }

        // DB에는 계산된 'realDurationDays'를 저장함
        Medication med = new Medication(
                name, 1, timeList.size(),
                realDurationDays,
                finalStartDate,
                new Gson().toJson(timeStrList),
                new Gson().toJson(selectedDays)
        );

        if (isEditMode && currentMedication != null && currentMedication.firebaseKey != null) {
            med.firebaseKey = currentMedication.firebaseKey;
            db.collection("users").document(user.getUid())
                    .collection("medications").document(currentMedication.firebaseKey)
                    .set(med)
                    .addOnSuccessListener(aVoid -> {
                        registerAlarms(med, selectedDays, timeList);
                        Toast.makeText(this, "수정되었습니다.", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "수정 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            db.collection("users").document(user.getUid())
                    .collection("medications")
                    .add(med)
                    .addOnSuccessListener(documentReference -> {
                        String newId = documentReference.getId();
                        documentReference.update("firebaseKey", newId);
                        registerAlarms(med, selectedDays, timeList);
                        Toast.makeText(this, "저장되었습니다.", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * 저장용: 입력한 횟수(targetCount)를 채우기 위해 필요한 기간 계산
     */
    private int calculateRealDuration(long startDate, List<Integer> selectedDays, int targetCount) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(startDate);

        int foundCount = 0;
        int daysPassed = 0;

        while (foundCount < targetCount && daysPassed < 3650) {
            int currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            if (selectedDays.contains(currentDayOfWeek)) {
                foundCount++;
            }
            if (foundCount == targetCount) {
                break;
            }
            cal.add(Calendar.DAY_OF_MONTH, 1);
            daysPassed++;
        }
        return daysPassed + 1;
    }

    private void registerAlarms(Medication med, List<Integer> days, List<TimeData> times) {
        for (Integer dayOfWeek : days) {
            for (TimeData timeData : times) {
                scheduleWeeklyAlarm(med.name, dayOfWeek, timeData.hour, timeData.minute);
            }
        }
    }

    private void scheduleWeeklyAlarm(String medName, int dayOfWeek, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1);
        }

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("medName", medName);
        intent.putExtra("dayOfWeek", dayOfWeek);
        intent.putExtra("hour", hour);
        intent.putExtra("minute", minute);

        int alarmId = (dayOfWeek * 10000) + (hour * 100) + minute;

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                alarmId,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            }
        }
    }

    static class TimeData {
        int hour, minute;
        public TimeData(int hour, int minute) {
            this.hour = hour;
            this.minute = minute;
        }
    }
}