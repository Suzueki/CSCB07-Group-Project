package com.b07group32.relationsafe;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;

public class SupportConnectionFragment extends Fragment {
    private static final String TAG = "SupportConnectionFragment";

    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseRef;

    private LinearLayout supportLinksContainer;
    private TextView cityHeaderText;

    private String userCity = "Toronto"; // Default city
    private HashMap<String, HashMap<String, SupportService[]>> supportDirectory;

    // SupportService class to hold service information
    public static class SupportService {
        public String name;
        public String phone;
        public String description;
        public String website;

        public SupportService(String name, String phone, String description, String website) {
            this.name = name;
            this.phone = phone;
            this.description = description;
            this.website = website;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        databaseRef = FirebaseDatabase.getInstance().getReference();

        loadSupportDirectoryFromJson();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_support_connection, container, false);

        supportLinksContainer = view.findViewById(R.id.support_links_container);
        cityHeaderText = view.findViewById(R.id.city_header_text);
        loadUserCity();

        return view;
    }

    private void loadSupportDirectoryFromJson() {
        try {
            InputStream inputStream = getContext().getAssets().open("support_directory.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();

            String jsonString = new String(buffer, "UTF-8");
            JSONObject jsonObject = new JSONObject(jsonString);

            supportDirectory = new HashMap<>();

            // Parse JSON and populate HashMap
            Iterator<String> cityKeys = jsonObject.keys();
            while (cityKeys.hasNext()) {
                String cityName = cityKeys.next();
                JSONObject cityData = jsonObject.getJSONObject(cityName);

                HashMap<String, SupportService[]> cityServices = new HashMap<>();

                Iterator<String> categoryKeys = cityData.keys();
                while (categoryKeys.hasNext()) {
                    String categoryName = categoryKeys.next();
                    JSONArray servicesArray = cityData.getJSONArray(categoryName);

                    SupportService[] services = new SupportService[servicesArray.length()];
                    for (int i = 0; i < servicesArray.length(); i++) {
                        JSONObject serviceObj = servicesArray.getJSONObject(i);
                        services[i] = new SupportService(
                                serviceObj.getString("name"),
                                serviceObj.getString("phone"),
                                serviceObj.getString("description"),
                                serviceObj.optString("website", "")
                        );
                    }
                    cityServices.put(categoryName, services);
                }
                supportDirectory.put(cityName, cityServices);
            }

            Log.d(TAG, "Support directory loaded successfully with " + supportDirectory.size() + " cities");

        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error loading support directory from JSON", e);
            // Fallback to default Toronto services if JSON in accessible
            createFallbackDirectory();
        }
    }

    //In the case the user is offline after logging in, we give them Toronto
    private void createFallbackDirectory() {
        supportDirectory = new HashMap<>();
        HashMap<String, SupportService[]> toronto = new HashMap<>();
        toronto.put("Emergency Hotlines", new SupportService[]{
                new SupportService("Emergency Services", "911", "Emergency Only", ""),
        });
        supportDirectory.put("Toronto", toronto);
    }

    private void loadUserCity() {
        if (currentUser != null) {

            DatabaseReference userCityRef = databaseRef.child("users")
                    .child(currentUser.getUid())
                    .child("questionnaire")
                    .child("city");

            userCityRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    if (snapshot.exists()) {
                        userCity = snapshot.getValue(String.class);
                        if (userCity == null) {
                            userCity = "Toronto";
                        }
                    }

                    Log.d(TAG, "User city loaded: " + userCity);
                    displaySupportServices();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to load user city", error.toException());
                    Toast.makeText(getContext(), "Failed to load city information", Toast.LENGTH_SHORT).show();
                    displaySupportServices(); // Display with default city
                }
            });
        } else {
            displaySupportServices(); // Display with default city if not logged in
        }
    }

    private void displaySupportServices() {
        if (supportLinksContainer == null) return;

        supportLinksContainer.removeAllViews();
        cityHeaderText.setText("Support Services in " + userCity);

        HashMap<String, SupportService[]> cityServices = supportDirectory.get(userCity);
        if (cityServices == null) {
            // Fallback to Toronto if city not found
            cityServices = supportDirectory.get("Toronto");
            if (cityServices == null) {
                displayNoServicesMessage();
                return;
            }
        }

        // Display services by category
        for (String category : cityServices.keySet()) {
            addCategorySection(category, cityServices.get(category));
        }
    }

    private void addCategorySection(String categoryName, SupportService[] services) {
        if (getContext() == null) return;

        // Category header
        TextView categoryHeader = new TextView(getContext());
        categoryHeader.setText(categoryName);
        categoryHeader.setTextSize(18);
        categoryHeader.setTextColor(getResources().getColor(android.R.color.black));
        categoryHeader.setPadding(0, 32, 0, 16);
        categoryHeader.setTypeface(null, android.graphics.Typeface.BOLD);
        supportLinksContainer.addView(categoryHeader);

        // Add services in this category
        for (SupportService service : services) {
            addServiceItem(service);
        }
    }

    private void addServiceItem(SupportService service) {
        if (getContext() == null) return;

        LinearLayout serviceLayout = new LinearLayout(getContext());
        serviceLayout.setOrientation(LinearLayout.VERTICAL);
        serviceLayout.setPadding(16, 12, 16, 12);
        serviceLayout.setBackgroundColor(getResources().getColor(android.R.color.white));

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, 0, 0, 8);
        serviceLayout.setLayoutParams(layoutParams);

        // Service name
        TextView nameText = new TextView(getContext());
        nameText.setText(service.name);
        nameText.setTextSize(16);
        nameText.setTextColor(getResources().getColor(android.R.color.black));
        nameText.setTypeface(null, android.graphics.Typeface.BOLD);
        serviceLayout.addView(nameText);

        // Service description
        TextView descText = new TextView(getContext());
        descText.setText(service.description);
        descText.setTextSize(14);
        descText.setTextColor(getResources().getColor(android.R.color.darker_gray));
        serviceLayout.addView(descText);

        // Phone number and buttons layout
        LinearLayout buttonLayout = new LinearLayout(getContext());
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setPadding(0, 8, 0, 0);

        // Phone text
        TextView phoneText = new TextView(getContext());
        phoneText.setText(service.phone);
        phoneText.setTextSize(16);
        phoneText.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        phoneText.setPadding(0, 0, 16, 0);
        buttonLayout.addView(phoneText);

        // Visit Website button
        if (service.website != null && !service.website.isEmpty()) {
            Button websiteButton = new Button(getContext());
            websiteButton.setText("Visit Website");
            websiteButton.setTextSize(12);
            websiteButton.setPadding(24, 8, 24, 8);
            websiteButton.setOnClickListener(v -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(service.website));
                startActivity(browserIntent);
            });
            buttonLayout.addView(websiteButton);
        }

        // Copy phone number button
        Button copyButton = new Button(getContext());
        copyButton.setText("Copy Phone");
        copyButton.setTextSize(12);
        copyButton.setPadding(24, 8, 24, 8);
        copyButton.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Phone Number", service.phone);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getContext(), "Phone number copied", Toast.LENGTH_SHORT).show();
        });
        buttonLayout.addView(copyButton);

        serviceLayout.addView(buttonLayout);
        supportLinksContainer.addView(serviceLayout);
    }

    private void displayNoServicesMessage() {
        TextView noServicesText = new TextView(getContext());
        noServicesText.setText("No support services available for your city. Please contact emergency services at 911 if you need immediate help.");
        noServicesText.setTextSize(16);
        noServicesText.setPadding(16, 32, 16, 32);
        noServicesText.setTextColor(getResources().getColor(android.R.color.darker_gray));
        supportLinksContainer.addView(noServicesText);
    }
}