package com.example.medreminderjava.data;

import android.provider.BaseColumns;

public final class DbContract {
    
    private DbContract() {}

    public static class DoctorEntry implements BaseColumns {
        public static final String TABLE_NAME = "doctors";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_LOCATION = "location";
        public static final String COLUMN_CONTACT = "contact";
        public static final String COLUMN_EMAIL = "email";
    }

    public static class MedicineEntry implements BaseColumns {
        public static final String TABLE_NAME = "medicines";
        public static final String COLUMN_DOCTOR_ID = "doctor_id";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_TIMES_PER_DAY = "times_per_day";
        public static final String COLUMN_TIMING_RELATION = "timing_relation"; // "Before" or "After"
        public static final String COLUMN_TIMING_MEALS = "timing_meals"; // Comma-separated: "Breakfast,Lunch,Dinner"
        public static final String COLUMN_TOTAL_QUANTITY = "total_quantity";
        public static final String COLUMN_DOSAGE_PER_TIME = "dosage_per_time";
        public static final String COLUMN_REMAINING_QUANTITY = "remaining_quantity";
        public static final String COLUMN_START_DATE = "start_date"; // Timestamp of creation/update
        public static final String COLUMN_BREAKFAST_TIME = "breakfast_time";
        public static final String COLUMN_LUNCH_TIME = "lunch_time";
        public static final String COLUMN_DINNER_TIME = "dinner_time";
    }

    public static class UserEntry implements BaseColumns {
        public static final String TABLE_NAME = "users";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_LOCATION = "location";
        public static final String COLUMN_AGE = "age";
        public static final String COLUMN_BLOOD_GROUP = "blood_group";
        public static final String COLUMN_MOBILE = "mobile_number";
        public static final String COLUMN_PASSWORD = "password";
        public static final String COLUMN_ALT_MOBILE = "alt_mobile_number";
    }
}
