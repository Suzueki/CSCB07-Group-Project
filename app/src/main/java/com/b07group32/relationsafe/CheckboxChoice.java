package com.b07group32.relationsafe;

public class CheckboxChoice extends Choice{
    public CheckboxChoice(String choice) {
        super(choice);
    }

    @Override
    public String getType() {
        return "checkbox";
    }
}
