package com.b07group32.relationsafe;

public class MultipleChoiceResponseAnswer extends MultipleChoiceAnswer implements Respondable{

    private String response;
    public MultipleChoiceResponseAnswer(String label) {
        super(label);
    }

    @Override
    public void setResponse(String response) {
        this.response = response;
    }

    public String getResponse() {
        return response;
    }
}
