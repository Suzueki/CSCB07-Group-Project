package com.b07group32.relationsafe;

public class MultipleResponseChoice extends MultipleChoice implements Respondable{

    private String response;
    private String hint;
    public MultipleResponseChoice(String choice, String hint) {
        super(choice);
        this.hint = hint;
    }

    @Override
    public void setResponse(String response) {
        this.response = response;
    }

    public String getResponse() {
        return response;
    }
    @Override
    public String getHint() { return hint; }
    @Override
    public String getType() { return "multiple_response_choice"; }
}
