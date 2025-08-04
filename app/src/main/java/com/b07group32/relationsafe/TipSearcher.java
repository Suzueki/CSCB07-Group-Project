package com.b07group32.relationsafe;

import java.util.List;

public class TipSearcher {

    public static String findMatchingTip(List<TipContainer> containers, String qid, String response) {
        for (TipContainer container : containers) {
            if (container.getQid().equals(qid)) {
                for (TipContainer.Tip tip : container.getTips()) {
                    if (tip.getResponse() != null && tip.getResponse().equals(response)) {
                        return tip.getTip();
                    }
                }
                return container.getTips().get(0).getTip(); // Default to first tip if no matching response is found
            }
        }
        return null;
    }
}
