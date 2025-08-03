package com.b07group32.relationsafe;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
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
import androidx.core.content.ContextCompat;
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
        categoryHeader.setTextSize(20);
        categoryHeader.setTextColor(ContextCompat.getColor(getContext(), android.R.color.black));
        categoryHeader.setPadding(0, 24, 0, 12);
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
        serviceLayout.setPadding(20, 16, 20, 16);

        // Create rounded background
        GradientDrawable background = new GradientDrawable();
        background.setColor(ContextCompat.getColor(getContext(), android.R.color.white));
        background.setCornerRadius(12f);
        background.setStroke(1, ContextCompat.getColor(getContext(), android.R.color.darker_gray));
        serviceLayout.setBackground(background);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, 0, 0, 12);
        serviceLayout.setLayoutParams(layoutParams);

        // Service name
        TextView nameText = new TextView(getContext());
        nameText.setText(service.name);
        nameText.setTextSize(18);
        nameText.setTextColor(ContextCompat.getColor(getContext(), android.R.color.black));
        nameText.setTypeface(null, android.graphics.Typeface.BOLD);
        serviceLayout.addView(nameText);

        // Service description
        TextView descText = new TextView(getContext());
        descText.setText(service.description);
        descText.setTextSize(14);
        descText.setTextColor(ContextCompat.getColor(getContext(), android.R.color.darker_gray));
        descText.setPadding(0, 4, 0, 0);
        serviceLayout.addView(descText);

        // Phone and website layout
        LinearLayout contactLayout = new LinearLayout(getContext());
        contactLayout.setOrientation(LinearLayout.HORIZONTAL);
        contactLayout.setPadding(0, 12, 0, 0);

        // Phone number (clickable)
        TextView phoneText = new TextView(getContext());
        phoneText.setText(service.phone);
        phoneText.setTextSize(16);
        phoneText.setTextColor(ContextCompat.getColor(getContext(), android.R.color.holo_blue_dark));
        phoneText.setTypeface(null, android.graphics.Typeface.BOLD);
        phoneText.setOnClickListener(v -> {
            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(Uri.parse("tel:" + service.phone));
            startActivity(callIntent);
        });
        contactLayout.addView(phoneText);

        // Add website if available
        if (service.website != null && !service.website.isEmpty()) {
            // Separator
            TextView separator = new TextView(getContext());
            separator.setText(" • ");
            separator.setTextSize(16);
            separator.setTextColor(ContextCompat.getColor(getContext(), android.R.color.darker_gray));
            contactLayout.addView(separator);

            // Website link
            TextView websiteText = new TextView(getContext());
            websiteText.setText("Website");
            websiteText.setTextSize(16);
            websiteText.setTextColor(ContextCompat.getColor(getContext(), android.R.color.holo_blue_dark));
            websiteText.setTypeface(null, android.graphics.Typeface.BOLD);
            websiteText.setOnClickListener(v -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(service.website));
                startActivity(browserIntent);
            });
            contactLayout.addView(websiteText);
        }

        serviceLayout.addView(contactLayout);
        supportLinksContainer.addView(serviceLayout);
    }



    private void displayNoServicesMessage() {
        TextView noServicesText = new TextView(getContext());
        noServicesText.setText("No support services available for your city. Please contact emergency services at 911 if you need immediate help.");
        noServicesText.setTextSize(16);
        noServicesText.setPadding(20, 32, 20, 32);
        noServicesText.setTextColor(ContextCompat.getColor(getContext(), android.R.color.darker_gray));

        // Create rounded background for no services message
        GradientDrawable background = new GradientDrawable();
        background.setColor(ContextCompat.getColor(getContext(), android.R.color.white));
        background.setCornerRadius(12f);
        background.setStroke(1, ContextCompat.getColor(getContext(), android.R.color.darker_gray));
        noServicesText.setBackground(background);

        supportLinksContainer.addView(noServicesText);
    }
}