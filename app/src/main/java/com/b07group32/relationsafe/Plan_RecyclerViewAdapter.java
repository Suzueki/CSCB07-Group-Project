package com.b07group32.relationsafe;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class Plan_RecyclerViewAdapter extends RecyclerView.Adapter<Plan_RecyclerViewAdapter.MyViewHolder>{
    Context context;
    ArrayList<PlanModel> plan;
    public Plan_RecyclerViewAdapter(Context context, ArrayList<PlanModel> plan) {
        this.context = context;
        this.plan = plan;
    }

    @NonNull

    @Override
    public Plan_RecyclerViewAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.plan_view_item, parent, false);
        return new Plan_RecyclerViewAdapter.MyViewHolder(view);
    }

    public void editResponse(String qid) {

    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
       String tip = plan.get(position).getTip();
       String answer = plan.get(position).getAnswer().trim();

       holder.tvTip.setText(tip.replace("{answer}", answer));
       holder.editBtn.setOnClickListener(v -> editResponse(plan.get(position).getQid()));
    }

    @Override
    public int getItemCount() {
        return plan.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tvTip;
        Button editBtn;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTip = itemView.findViewById(R.id.tipContent);
            editBtn = itemView.findViewById(R.id.editButton);
        }
    }
}
