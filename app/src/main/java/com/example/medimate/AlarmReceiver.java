package com.example.medimate;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import java.util.Calendar; // Calendar import 필수

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // 1. 정보 받기
        String medName = intent.getStringExtra("medName");
        int dayOfWeek = intent.getIntExtra("dayOfWeek", -1);
        int hour = intent.getIntExtra("hour", -1);
        int minute = intent.getIntExtra("minute", -1);

        if (medName == null) medName = "약";

        // 2. 알림 띄우기 (기존 코드 유지)
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "medication_alarm_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "복용 알림", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        Intent appIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, appIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_supplement_recommend) // 상태바에 뜨는 작은 아이콘 (흰색)
                .setContentTitle("약 드실 시간입니다!")
                .setContentText("[" + medName + "] 복용 잊지 마세요.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());

        // 3. 다음 주 알람 재등록
        if (dayOfWeek != -1 && hour != -1 && minute != -1) {
            scheduleNextWeekAlarm(context, medName, dayOfWeek, hour, minute);
        }
    }

    // 다음 주 알람 예약 함수
    private void scheduleNextWeekAlarm(Context context, String medName, int dayOfWeek, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // 무조건 7일 뒤로 설정
        calendar.add(Calendar.DAY_OF_YEAR, 7);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("medName", medName);
        intent.putExtra("dayOfWeek", dayOfWeek);
        intent.putExtra("hour", hour);
        intent.putExtra("minute", minute);

        int alarmId = (dayOfWeek * 10000) + (hour * 100) + minute;

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
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
            }
        }
    }
}