package com.b07group32.relationsafe;

import java.util.ArrayList;

public class Question {
    private String question;
    private ArrayList<Choice> choices;

    public Question() {};

    public Question(String question, ArrayList<Choice> choices) {
        this.question = question;
        this.choices = choices;
    }

    public String getQuestionText() {
        return question;
    }

    public ArrayList<Choice> getAnswers() {
        return choices;
    }
}
