package com.b07group32.relationsafe;

import androidx.annotation.NonNull;
import android.content.Context;
import android.widget.Toast;


import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class CategoryDataManager
{
    private DatabaseReference databaseRef;

    public CategoryDataManager(DatabaseReference databaseRef)
    {
        this.databaseRef = databaseRef;
    }

    public void loadCategoryData(String category, List<String> list, SimpleItemAdapter adapter,
                                 List<String> keys, Runnable onComplete)
    {
        databaseRef.child(category).addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                list.clear();
                keys.clear();
                for (DataSnapshot itemSnapshot : dataSnapshot.getChildren())
                {
                    String displayText = DisplayDataUtils.buildDisplayString(itemSnapshot, category);
                    if (displayText != null)
                    {
                        list.add(displayText);
                        keys.add(itemSnapshot.getKey());
                    }
                }
                adapter.notifyDataSetChanged();
                adapter.clearSelection();
                onComplete.run();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error)
            {
                //This will NEVER come up because I don't allow for it but we must have an implementation.
            }
        });
    }

    // Add metadata because the requirements stated title, etc. but not other metadata that I'd expect
    public void addSimpleItem(String category, String content, Context context)
    {
        String key = databaseRef.child(category).push().getKey();
        if (key != null)
        {
            Map<String, Object> itemData = new HashMap<>();
            itemData.put("content", content);
            itemData.put("timestamp", System.currentTimeMillis());

            databaseRef.child(category).child(key).setValue(itemData)
                    .addOnSuccessListener(aVoid -> {Toast.makeText(context, "Item added successfully", Toast.LENGTH_SHORT).show();})
                    .addOnFailureListener(e -> {Toast.makeText(context, "Failed to add item: " + e.getMessage(), Toast.LENGTH_SHORT).show();});
        }
    }

    // Look for item, if exists, delete, else, serve error msg
    public void deleteItem(String category, String key, FirebaseUser currentUser, Context context, Runnable onSuccess) {
        databaseRef.child(category).child(key).get().addOnSuccessListener(
                dataSnapshot -> {String fileName = dataSnapshot.child("fileName").getValue(String.class);

                    databaseRef.child(category).child(key).removeValue()
                    .addOnSuccessListener(aVoid -> {Toast.makeText(context, "Item deleted successfully", Toast.LENGTH_SHORT).show();onSuccess.run();

                        // Delete file from storage if it exists
                        if (fileName != null)
                        {
                            StorageReference fileRef = FirebaseStorage.getInstance().getReference()
                                    .child("users").child(currentUser.getUid())
                                    .child("emergency_info").child(category).child(fileName);
                            fileRef.delete();
                        }
                    })
                    .addOnFailureListener(e -> {Toast.makeText(context, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();});
        });
    }
}