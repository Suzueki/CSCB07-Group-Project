package com.b07group32.relationsafe;

public class MultipleChoiceResponseAnswer extends MultipleChoiceAnswer implements Respondable{

    private String response;
    public MultipleChoiceResponseAnswer(String label, String response) {
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
