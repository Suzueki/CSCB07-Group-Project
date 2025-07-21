package com.b07group32.relationsafe;

public class ShortAnswer extends Answer implements Respondable{

    private String response;

    public ShortAnswer(String label) {
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
