package com.b07group32.relationsafe;

public class FreeFormChoice extends Choice implements Respondable{

    private String response;

    public FreeFormChoice(String label) {
        super(label);
    }

    public String getResponse() {
        return response;
    }
    @Override
    public void setResponse(String response) {
        this.response = response;
    }
}
