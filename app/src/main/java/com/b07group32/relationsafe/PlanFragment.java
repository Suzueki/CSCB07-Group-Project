package com.b07group32.relationsafe;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import java.util.ArrayList;
import java.util.HashMap;

public class PlanFragment extends Fragment {
    ArrayList<PlanModel> plan = new ArrayList<>();
    DatabaseReference databaseReference;
    FirebaseUser user;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        user = FirebaseAuth.getInstance().getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference("users").child(user.getUid()); // add .child(list of tips)
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_plan, container, false);
        RecyclerView rv = view.findViewById(R.id.planRecyclerView);
        setUpPlan();
        Plan_RecyclerViewAdapter adapter = new Plan_RecyclerViewAdapter(requireContext(), plan);
        rv.setAdapter(adapter);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    // add to list
                }
                adapter.notifyDataSetChanged();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
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