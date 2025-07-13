package com.b07group32.relationsafe;

public class SafeLocationItem {
    private String id;
    private String address;
    private String note;

    public SafeLocationItem() {
    }

    public SafeLocationItem(String id, String address, String note) {
        this.id = id;
        this.address = address;
        this.note = note;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}