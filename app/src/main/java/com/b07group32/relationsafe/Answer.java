package com.b07group32.relationsafe;

public abstract class Answer {
    protected String label;

    public Answer(String label) {
        this.label = label;
    }

    public String getLabel() {
        return this.label;
    }
}
