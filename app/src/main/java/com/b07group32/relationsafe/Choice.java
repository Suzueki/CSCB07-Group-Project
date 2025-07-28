package com.b07group32.relationsafe;

public abstract class Choice {
    protected String choice;

    public Choice(String choice) {
        this.choice = choice;
    }

    public String getResponse() {
        return this.choice;
    }
}
