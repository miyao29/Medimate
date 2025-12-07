package com.example.medimate.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.io.Serializable;

@Entity(tableName = "medication_table")
public class Medication implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String firebaseKey;

    public String name;
    public int doseAmount;
    public int dailyCount;
    public int totalDays;
    public long startDate;
    public String alarmTimes; // JSON 변환된 시간 문자열
    public String daysOfWeek; // JSON 변환된 요일 문자열

    public Medication() {}

    public Medication(String name, int doseAmount, int dailyCount, int totalDays, long startDate, String alarmTimes, String daysOfWeek) {
        this.name = name;
        this.doseAmount = doseAmount;
        this.dailyCount = dailyCount;
        this.totalDays = totalDays;
        this.startDate = startDate;
        this.alarmTimes = alarmTimes;
        this.daysOfWeek = daysOfWeek;
    }
}
