package com.b07group32.relationsafe;

public class PlanModel {
    String tip;
    String answer;
    String qid;
    String ref;
    String sub;

    public PlanModel(String qid, String tip, String answer, String ref) {
        this.tip = tip;
        this.answer = answer;
        this.qid = qid;
        this.ref = ref;
        sub = "";
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

    public void setSub(String sub) {
        this.sub = sub;
    }

    public String getRef() {
        return ref;
    }

    public String getSub() {
        return sub;
    }
}
