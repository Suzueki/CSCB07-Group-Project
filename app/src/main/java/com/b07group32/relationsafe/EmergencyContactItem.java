package com.b07group32.relationsafe;

public class EmergencyContactItem {
    private String id;
    private String personName;
    private String phoneNumber;

    public EmergencyContactItem() {
    }

    public EmergencyContactItem(String id, String personName, String phoneNumber) {
        this.id = id;
        this.personName = personName;
        this.phoneNumber = phoneNumber;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}