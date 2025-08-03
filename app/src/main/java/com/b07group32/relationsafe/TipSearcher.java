package com.b07group32.relationsafe;

import java.util.List;

public class TipSearcher {

    public static String findMatchingTip(List<TipContainer> containers, String qid, String response) {
        for (TipContainer container : containers) {
            if (container.getQid().equals(qid)) {
                for (TipContainer.Tip tip : container.getTips()) {
                    if (tip.getResponse() != null && tip.getResponse().equals(response)) {
                        return tip.getTip();
                    } else { // If tip response is empty, assumed to be single tip for all responses
                        return tip.getTip();
                    }
                }
                break;
            }
        }
        return null;
    }
}
