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

    private final Plan_RecyclerViewInterface planInterface;
    public Plan_RecyclerViewAdapter(Context context, ArrayList<PlanModel> plan, Plan_RecyclerViewInterface planInterface) {
        this.context = context;
        this.plan = plan;
        this.planInterface = planInterface;
    }

    @NonNull

    @Override
    public Plan_RecyclerViewAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.plan_view_item, parent, false);
        return new Plan_RecyclerViewAdapter.MyViewHolder(view, planInterface);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
       String tip = plan.get(position).getTip();
       String answer = plan.get(position).getAnswer().trim();
       String sub = plan.get(position).getSub().trim();
       String text = tip.replace("{answer}", answer);
       text = text.replace("{sub}", sub);

       holder.tvTip.setText(text);
    }

    @Override
    public int getItemCount() {
        return plan.size();
    }

    public PlanModel getPlanAtPosition(int position) {
        return plan.get(position);
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tvTip;

        public MyViewHolder(@NonNull View itemView, Plan_RecyclerViewInterface planInterface) {
            super(itemView);
            tvTip = itemView.findViewById(R.id.tipContent);
            editBtn = itemView.findViewById(R.id.editButton);
            editBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (planInterface != null) {
                        int pos = getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            planInterface.onEditClick(pos);
                        }
                    }
                }
            });
        }
    }
}
