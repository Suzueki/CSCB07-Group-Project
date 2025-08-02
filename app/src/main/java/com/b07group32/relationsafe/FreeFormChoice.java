package com.b07group32.relationsafe;

public class FreeFormChoice extends Choice implements Respondable{

    private String response;

    public FreeFormChoice(String hint) {
        super(hint);
    }

    @Override
    public String getType() {
        return "freeform";
    }

    public String getChoice() { return response; }
    @Override
    public void setResponse(String response) { this.response = response; }
    @Override
    public String getHint() { return choice; } // use choice as hint
}
