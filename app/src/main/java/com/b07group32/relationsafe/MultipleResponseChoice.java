package com.b07group32.relationsafe;

public class MultipleResponseChoice extends MultipleChoice implements Respondable{

    private String response;
    public MultipleResponseChoice(String label, String response) {
        super(label);
        this.response = response;
    }

    @Override
    public void setResponse(String response) {
        this.response = response;
    }
    @Override

    public String getResponse() {
        return response;
    }
}
