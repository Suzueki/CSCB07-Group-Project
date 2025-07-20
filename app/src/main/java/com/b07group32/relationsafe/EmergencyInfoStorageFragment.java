package com.b07group32.relationsafe;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class EmergencyInfoStorageFragment extends Fragment {

    private FirebaseAuth auth;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.emergency_info_storage_fragment, container, false);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(getContext(), "Authentication error. Returning to home.", Toast.LENGTH_LONG).show();
            getParentFragmentManager().popBackStack();
            return view;
        }

        Button buttonAddItem = view.findViewById(R.id.buttonAddItem);
        Button buttonDeleteItem = view.findViewById(R.id.buttonDeleteItem);
        Button buttonEditItem = view.findViewById(R.id.buttonEditItem);
        Button buttonViewItem = view.findViewById(R.id.buttonViewItem);
        Button buttonBack = view.findViewById(R.id.buttonBack);

        String welcomeName;
        if (currentUser.getDisplayName() != null) {
            welcomeName = currentUser.getDisplayName();
        } else {
            welcomeName = currentUser.getEmail();
        }
        Toast.makeText(getContext(), "Welcome, " + welcomeName, Toast.LENGTH_SHORT).show();


        buttonAddItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isUserAuthenticated()) {
                    loadFragment(new AddItemFragment());
                }
            }
        });

        buttonDeleteItem.setOnClickListener(v -> {
            if (isUserAuthenticated()) {
                loadFragment(new DeleteItemFragment());
            }
        });

        buttonEditItem.setOnClickListener(v -> {
            if (isUserAuthenticated()) {
                // uncomment when EditItemFragment is implemented
                // loadFragment(new EditItemFragment());
                Toast.makeText(getContext(), "Edit functionality coming soon", Toast.LENGTH_SHORT).show();
            }
        });

        buttonViewItem.setOnClickListener(v -> {
            if (isUserAuthenticated()) {
                loadFragment(new ViewItemFragment());
            }
        });

        buttonBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
            getParentFragmentManager().popBackStack();
        }
    }

    private boolean isUserAuthenticated() {
        currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please log in to access this feature", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
            return false;
        }
        return true;
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}