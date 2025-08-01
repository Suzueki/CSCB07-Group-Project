package com.b07group32.relationsafe;

public class PlanModel {
    String tip;
    String answer;
    String qid;

    public PlanModel(String qid, String tip, String answer) {
        this.tip = tip;
        this.answer = answer;
        this.qid = qid;
    }

    public String getTip() {
        return tip;
    }

    public String getAnswer() {
        return answer;
    }

    public String getQid() {
        return qid;
    }
}
