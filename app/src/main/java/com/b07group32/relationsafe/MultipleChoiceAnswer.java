package com.b07group32.relationsafe;

public class MultipleChoiceAnswer extends Answer{
    public MultipleChoiceAnswer(String label) {
        super(label);
    };

    public String getResponse() {
        return label;
    }

}
