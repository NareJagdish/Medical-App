package com.example.medreminderjava.data;

public class Appointment {
    private final long id;
    private final String userMobile;
    private final long doctorId;
    private final String date;
    private final String time;
    private final String reason;
    private final String priority;

    public Appointment(long id, String userMobile, long doctorId, String date, String time, String reason, String priority) {
        this.id = id;
        this.userMobile = userMobile;
        this.doctorId = doctorId;
        this.date = date;
        this.time = time;
        this.reason = reason;
        this.priority = priority;
    }

    public long getId() { return id; }
    public String getUserMobile() { return userMobile; }
    public long getDoctorId() { return doctorId; }
    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getReason() { return reason; }
    public String getPriority() { return priority; }
}
