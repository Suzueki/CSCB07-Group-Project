package com.b07group32.relationsafe;

public class MedicationItem {
    private String id;
    private String drugName;
    private String dosage;
    private String prescription;

    public MedicationItem() {}

    public MedicationItem(String id, String drugName, String dosage, String prescription) {
        this.id = id;
        this.drugName = drugName;
        this.dosage = dosage;
        this.prescription = prescription;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDrugName() { return drugName; }
    public void setDrugName(String drugName) { this.drugName = drugName; }

    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }

    public String getPrescription() { return prescription; }
    public void setPrescription(String prescription) { this.prescription = prescription; }
}