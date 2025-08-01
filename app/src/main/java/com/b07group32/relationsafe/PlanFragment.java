package com.b07group32.relationsafe;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;

public class PlanFragment extends Fragment {
    ArrayList<PlanModel> plan = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_plan, container, false);
        RecyclerView rv = view.findViewById(R.id.planRecyclerView);
        setUpPlan();
        return view;
    }

    private void setUpPlan() {
        ArrayList<HashMap<String, String>> tips = new ArrayList<>();

        // sample data (actual data will be from database)
        HashMap<String, String> t1 = new HashMap<>();
        t1.put("qid", "sr3");
        t1.put("tip", "Save {answer} in your phone under a code-word so you can quietly call them when you need help.");
        t1.put("answer", "mother");

        HashMap<String, String> t2 = new HashMap<>();
        t2.put("qid", "sr2");
        t2.put("tip", "Log each incident (date, time, and a brief note) so you have clear records if needed later.");

        tips.add(t2);
        tips.add(t1);

        for (int i=0; i<tips.size(); i++) {
            HashMap<String, String> tip = tips.get(i);
            plan.add(new PlanModel(tip.get("qid"), tip.get("tip"), tip.getOrDefault("answer", "")));
        }
    }
}