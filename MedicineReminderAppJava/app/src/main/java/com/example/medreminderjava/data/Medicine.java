package com.example.medreminderjava.data;

public class Medicine {
    private final long id;
    private final long doctorId;
    private final String name;
    private final int timesPerDay;
    private final String timingRelation; // "Before" or "After"
    private final String timingMeals;     // Comma-separated: "Breakfast,Lunch,Dinner"
    private final int totalQuantity;
    private final int dosagePerTime;
    private final int remainingQuantity;
    private final long startDate;
    
    private final String breakfastTime;
    private final String lunchTime;
    private final String dinnerTime;

    public Medicine(long id, long doctorId, String name, int timesPerDay, 
                    String timingRelation, String timingMeals, 
                    int totalQuantity, int dosagePerTime, int remainingQuantity, long startDate,
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
        this.breakfastTime = breakfastTime;
        this.lunchTime = lunchTime;
        this.dinnerTime = dinnerTime;
    }

    public long getId() {
        return id;
    }

    public long getDoctorId() {
        return doctorId;
    }

    public String getName() {
        return name;
    }

    public int getTimesPerDay() {
        return timesPerDay;
    }

    public String getTimingRelation() {
        return timingRelation;
    }

    public String getTimingMeals() {
        return timingMeals;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public int getDosagePerTime() {
        return dosagePerTime;
    }

    public int getRemainingQuantity() {
        return remainingQuantity;
    }

    public long getStartDate() {
        return startDate;
    }

    public String getBreakfastTime() {
        return breakfastTime;
    }

    public String getLunchTime() {
        return lunchTime;
    }

    public String getDinnerTime() {
        return dinnerTime;
    }

    /**
     * Calculates the remaining days of medicine supply.
     * Formula: remainingQuantity / (timesPerDay * dosagePerTime)
     */
    public int getRemainingDays() {
        int dailyDosage = timesPerDay * dosagePerTime;
        if (dailyDosage <= 0) {
            return 0;
        }
        // Round up to ensure that a partial day counts as 1 day remaining (e.g. 5 pills left with 2 pills/day = 3 days remaining)
        return (int) Math.ceil((double) remainingQuantity / dailyDosage);
    }
}
