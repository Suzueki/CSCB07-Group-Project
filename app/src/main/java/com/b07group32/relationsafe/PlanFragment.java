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

public class PlanFragment extends Fragment implements Plan_RecyclerViewInterface {
    ArrayList<PlanModel> plan = new ArrayList<>();
    DatabaseReference databaseReference;
    FirebaseUser user;
    private Plan_RecyclerViewAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        user = FirebaseAuth.getInstance().getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference("users").child(user.getUid()).child("plan"); // add .child(list of tips)
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_plan, container, false);
        RecyclerView rv = view.findViewById(R.id.planRecyclerView);
        adapter = new Plan_RecyclerViewAdapter(requireContext(), plan, this);
        rv.setAdapter(adapter);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                plan.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    PlanModel planItem = dataSnapshot.getValue(PlanModel.class);
                    if (planItem != null) {
                        if (!planItem.getRef().isEmpty()) {
                            planItem.setSub(findRef(planItem.getRef()));
                        }
                        plan.add(planItem);
                    }
                }
                adapter.notifyDataSetChanged();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        return view;
    }

    private String findRef(String ref) {
        for (PlanModel planItem : plan) {
            if (planItem.getQid().equals(ref)) {
                return planItem.getAnswer();
            }
        }
        return "";
    }

    @Override
    public void onEditClick(int position) {
        String mode;
        PlanModel item = adapter.getPlanAtPosition(position);
        String qid = item.getQid();
        if (qid.equals("w1")) {
            mode = "change branch";
        } else {
            mode = "edit";
        }
        Fragment fragment = QuestionnaireFragment.newInstance(mode, qid);
        getParentFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).addToBackStack(null).commit();
    }
}