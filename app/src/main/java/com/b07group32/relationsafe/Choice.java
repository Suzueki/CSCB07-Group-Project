package com.b07group32.relationsafe;

public abstract class Choice {
    protected String label;

    public Choice(String label) {
        this.label = label;
    }

    public String getResponse() {
        return this.label;
    }
}
