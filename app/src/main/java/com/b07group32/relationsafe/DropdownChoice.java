package com.b07group32.relationsafe;

public class DropdownChoice extends Choice{

    public DropdownChoice(String choice) {
        super(choice);
    }
    @Override
    public String getType() {
        return "dropdown";
    }
}
