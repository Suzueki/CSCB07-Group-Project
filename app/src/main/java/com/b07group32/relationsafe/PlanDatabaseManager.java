package com.b07group32.relationsafe;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

public class PlanDatabaseManager {
    private DatabaseReference ref;
    private FirebaseUser user;

    public interface PlanUpdateCallback {
        void onSuccess(String action, String qid, String response);
        void onFailure(String action);
    }

    public PlanDatabaseManager() {
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("User is not authenticated");
        }
        ref = FirebaseDatabase.getInstance()
                .getReference()
                .child("users")
                .child(user.getUid())
                .child("plan");
    }

    public void addResponse(final Map<String, Object> response, final PlanUpdateCallback callback) {
        if (!response.containsKey("qid")) {
            if (callback != null) callback.onFailure("responseData must contain a 'qid' field");
            return;
        }
        final String questionId = response.get("qid").toString();

        Query query = ref.orderByChild("qid").equalTo(questionId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        data.getRef().setValue(response)
                                .addOnSuccessListener(aVoid -> {
                                    if (callback != null) callback.onSuccess("updated",
                                            questionId,
                                            response.get("answer").toString());
                                })
                                .addOnFailureListener(e -> {
                                    if (callback != null) callback.onFailure(e.getMessage());
                                });
                        break;
                    }
                } else {
                    // Add new response
                    ref.push().setValue(response)
                            .addOnSuccessListener(aVoid -> {
                                if (callback != null) callback.onSuccess("added",
                                        questionId,
                                        response.get("answer").toString());
                            })
                            .addOnFailureListener(e -> {
                                if (callback != null) callback.onFailure(e.getMessage());
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (callback != null) callback.onFailure(error.getMessage());
            }
        });
    }

    public DatabaseReference getDatabase() {
        return ref;
    }
}
