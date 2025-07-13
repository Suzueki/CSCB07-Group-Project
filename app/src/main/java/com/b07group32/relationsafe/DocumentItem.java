package com.b07group32.relationsafe;

public class DocumentItem {
    private String id;
    private String fileName;
    private String shortNote;
    private String pdfPath;

    public DocumentItem() {
    }

    public DocumentItem(String id, String fileName, String shortNote, String pdfPath) {
        this.id = id;
        this.fileName = fileName;
        this.shortNote = shortNote;
        this.pdfPath = pdfPath;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    //for future changes, no current uses
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getShortNote() { return shortNote; }
    public void setShortNote(String shortNote) { this.shortNote = shortNote; }

    public String getPdfPath() { return pdfPath; }
    public void setPdfPath(String pdfPath) { this.pdfPath = pdfPath; }
}
