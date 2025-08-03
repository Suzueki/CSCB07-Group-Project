package com.b07group32.relationsafe;

import java.util.List;

public class TipContainer {
    private String qid;
    private List<Tip> tips;
    public static class Tip {
        private String response;
        private String tip;
        private String ref;

        public String getResponse() {
            return response;
        }

        public String getTip() {
            return tip;
        }
    }

    public String getQid() {
        return qid;
    }

    public List<Tip> getTips() {
        return tips;
    }
}
