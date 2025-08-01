package com.b07group32.relationsafe;

import java.util.ArrayList;

public class Question {
    private String question;
    private ArrayList<Answer> answers;

    public Question() {};

    public Question(String question, ArrayList<Answer> answers) {
        this.question = question;
        this.answers = answers;
    }

    public String getQuestionText() {
        return question;
    }

    public ArrayList<Answer> getAnswers() {
        return answers;
    }
}
