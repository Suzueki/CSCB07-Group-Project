package com.b07group32.relationsafe;

import java.util.List;

public class TipContainer {
    private String qid;
    private List<Tip> tips;
    private class Tip {
        private String response;
        private String tip;
        private String ref;
    }
}
