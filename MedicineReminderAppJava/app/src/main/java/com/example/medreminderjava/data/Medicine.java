package com.example.medreminderjava.data;

public class Medicine {
    private final long id;
    private final long doctorId;
    private final String name;
    private final int timesPerDay;
    private final String timingRelation;
    private final String timingMeals;
    private final int totalQuantity;
    private final int dosagePerTime;
    private final int remainingQuantity;
    private final String startDate;
    private final String endDate;
    private final String breakfastTime;
    private final String lunchTime;
    private final String dinnerTime;

    public Medicine(long id, long doctorId, String name, int timesPerDay, 
                    String timingRelation, String timingMeals, 
                    int totalQuantity, int dosagePerTime, int remainingQuantity,
                    String startDate, String endDate,
                    String breakfastTime, String lunchTime, String dinnerTime) {
        this.id = id;
        this.doctorId = doctorId;
        this.name = name;
        this.timesPerDay = timesPerDay;
        this.timingRelation = timingRelation;
        this.timingMeals = timingMeals;
        this.totalQuantity = totalQuantity;
        this.dosagePerTime = dosagePerTime;
        this.remainingQuantity = remainingQuantity;
        this.startDate = startDate;
        this.endDate = endDate;
        this.breakfastTime = breakfastTime;
        this.lunchTime = lunchTime;
        this.dinnerTime = dinnerTime;
    }

    public long getId() { return id; }
    public long getDoctorId() { return doctorId; }
    public String getName() { return name; }
    public int getTimesPerDay() { return timesPerDay; }
    public String getTimingRelation() { return timingRelation; }
    public String getTimingMeals() { return timingMeals; }
    public int getTotalQuantity() { return totalQuantity; }
    public int getDosagePerTime() { return dosagePerTime; }
    public int getRemainingQuantity() { return remainingQuantity; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public String getBreakfastTime() { return breakfastTime; }
    public String getLunchTime() { return lunchTime; }
    public String getDinnerTime() { return dinnerTime; }

    public int getRemainingDays() {
        int dailyDosage = timesPerDay * dosagePerTime;
        if (dailyDosage <= 0) return 0;
        return (int) Math.ceil((double) remainingQuantity / dailyDosage);
    }
}
