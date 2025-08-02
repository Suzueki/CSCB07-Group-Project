package com.b07group32.relationsafe;

public class MultipleChoice extends Choice {
    public MultipleChoice(String label) {
        super(label);
    };
    @Override
    public String getType() {
        return "multiple_choice";
    }

}
