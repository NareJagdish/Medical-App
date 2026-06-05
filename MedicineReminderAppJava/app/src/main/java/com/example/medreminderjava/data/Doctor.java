package com.example.medreminderjava.data;

public class Doctor {
    private final long id;
    private final String name;
    private final String location;
    private final String contact;
    private final String email;

    public Doctor(long id, String name, String location, String contact, String email) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.contact = contact;
        this.email = email;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public String getContact() {
        return contact;
    }

    public String getEmail() {
        return email;
    }
}
