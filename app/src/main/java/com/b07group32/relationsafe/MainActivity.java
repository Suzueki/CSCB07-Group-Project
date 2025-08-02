package com.b07group32.relationsafe;

import static com.b07group32.relationsafe.R.*;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    FirebaseDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        db = FirebaseDatabase.getInstance("https://relationsafe-20cde-default-rtdb.firebaseio.com/");
        DatabaseReference myRef = db.getReference("testDemo");

        FirebaseAuth auth = FirebaseAuth.getInstance();
        // auth.signOut(); // maintain auth for debugging purpose

        if (auth.getCurrentUser() != null) {
            loadFragment(new HomeFragment());
            bottomNavigationView.setVisibility(View.VISIBLE);
        } else {
            loadFragment(new LoginFragment());
            bottomNavigationView.setVisibility(View.GONE);
        }

        setupEmergencyExitFAB();

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if(item.getItemId() == R.id.nav_home){
                setCurrentFragment(new HomeFragment());
            }
            if(item.getItemId() == R.id.nav_settings){
                setCurrentFragment(new SettingsFragment());
            }
            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    private void setupEmergencyExitFAB() {
        FloatingActionButton fab = findViewById(R.id.fab_exit);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emergencyExit();
            }
        });
    }

    private void emergencyExit() {
        // TODO: Configurable URL
        try{
            FirebaseAuth.getInstance().signOut();}
        catch(Exception e)
        {
            e.printStackTrace();
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"));
        startActivity(intent);
        finishAffinity();
    }

    private void setCurrentFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}