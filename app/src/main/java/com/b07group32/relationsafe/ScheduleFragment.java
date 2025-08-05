package com.b07group32.relationsafe;

import java.util.ArrayList;
import android.Manifest;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.function.Consumer;



public class ScheduleFragment extends Fragment {
    private final int columnPadding = 16;
    private AlarmManager alarmManager;
    private ActivityResultLauncher<String> notifPerm;
    private ArrayList<Notification> notifications = new ArrayList<Notification>();
    private TableLayout tableLayout;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        notifPerm = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted){
                        queryUser(0);
                    }
                });
    }

    @Override
    public void onPause(){
        super.onPause();
        saveNotifications();
    }

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        View view = inflater.inflate(R.layout.fragment_schedule, container, false);
        Button buttonSendReminder = view.findViewById(R.id.buttonSendNotification);
        buttonSendReminder.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                            requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else{
                queryUser(0);
            }
        });
        tableLayout = view.findViewById(R.id.tableID);
        loadNotifications();
        Log.d("NotificationList", ""+notifications.size());
        for(int i = 0; i<notifications.size(); i++){
            Log.d("NotificationList", ""+notifications.get(i).getNotificationID());
        }
        return view;
    }

    public void saveNotifications(){
        SharedPreferences preferences = requireContext().getSharedPreferences("notification_preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(notifications);
        editor.putString("notification_preferences", json);
        editor.apply();
    }

    public void loadNotifications(){
        SharedPreferences preferences = requireContext().getSharedPreferences("notification_preferences", Context.MODE_PRIVATE);
        String json = preferences.getString("notification_preferences", null);
        if(json != null){
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<Notification>>() {}.getType();
            ArrayList<Notification> loadedList = gson.fromJson(json, type);
            notifications.addAll(loadedList);
            for(Notification n:notifications){
                addToTable(n);
            }
        }
    }
    public void queryUser(int id){
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.period_dialog, null);
        Spinner spinner = dialogView.findViewById(R.id.customSpinner);

        String[] options = {"Daily", "Weekly", "Monthly", "Custom"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                options);
        spinner.setAdapter(adapter);
        Spinner periodSpinner = dialogView.findViewById(R.id.spinner2);
        EditText customText = dialogView.findViewById(R.id.customInputBox);
        String[] periodOptions = {"Day(s)", "Week(s)", "Month(s)", "Year(s)"};
        ArrayAdapter<String> periodAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                periodOptions);
        periodSpinner.setAdapter(periodAdapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedOption = options[position];
                if(selectedOption.equals("Custom")){
                    periodSpinner.setVisibility(View.VISIBLE);
                    customText.setVisibility(View.VISIBLE);
                }
                else{
                    periodSpinner.setVisibility(View.GONE);
                    customText.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                periodSpinner.setVisibility(View.GONE);
                customText.setVisibility(View.GONE);
            }
        });
        new AlertDialog.Builder(getContext())
            .setTitle("Choose how often you'd like to be reminded")
            .setView(dialogView)
            .setPositiveButton("OK", (dialog, which) -> {
                String selected = spinner.getSelectedItem().toString();
                String period;
                int amount = 1;
                long intervalMS = TimeUnit.DAYS.toMillis(1L);
                period = "Day(s)";
                switch (selected) {
                    case "Weekly":
                        intervalMS = TimeUnit.DAYS.toMillis(7L);
                        period = "Week(s)";
                        break;
                    case "Monthly":
                        intervalMS = TimeUnit.DAYS.toMillis(30L);
                        period = "Month(s)";
                        break;
                    case "Custom":
                        String unit = periodSpinner.getSelectedItem().toString();
                        period = unit;
                        if(customText.getText().toString().isEmpty()){
                            Toast.makeText(getContext(), "Error: amount cannot be empty", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        amount = Integer.parseInt(customText.getText().toString());

                        if (unit.equals("Day(s)")) {
                            intervalMS = TimeUnit.DAYS.toMillis(amount);
                        }
                        if (unit.equals("Week(s)")) {
                            intervalMS = TimeUnit.DAYS.toMillis(amount * 7L);
                        }
                        if (unit.equals("Month(s)")) {
                            intervalMS = TimeUnit.DAYS.toMillis(amount * 30L);
                        }
                        if (unit.equals("Year(s)")) {
                            intervalMS = TimeUnit.DAYS.toMillis(amount * 365L);
                        }
                        break;
                }

                long finalInterval = intervalMS;
                String finalPeriod = period;
                int finalAmount = Math.max(1, amount);

                showDateTimePicker(getParentFragmentManager(), dateTime->{
                    Calendar cal = getCalendar(dateTime);
                    if(cal.getTimeInMillis() < System.currentTimeMillis()){
                        Toast.makeText(getContext(), "Error: time picked must be in future", Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (alarmManager != null) {
                        Notification notif;
                        if(id == 0) {
                            notif = new Notification(notifications.size()+1, cal, finalInterval, requireContext(), finalPeriod, finalAmount);
                            notifications.add(notif);
                            addToTable(notif);
                            Toast.makeText(getContext(), "Reminder successfully scheduled", Toast.LENGTH_SHORT).show();
                        }
                        else{
                             notif = notifications.get(id-1);
                             notif.setCal(cal);
                             notif.setInterval(finalInterval);
                             notif.setPeriod(finalPeriod);
                            notif.setAmount(finalAmount);
                            Toast.makeText(getContext(), "Reload screen to see changes", Toast.LENGTH_SHORT).show();
                        }
                        queueNotification(notif);
                        saveNotifications();
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void queueNotification(Notification n){
        PendingIntent temp_PendingIntent = createTempIntent(n);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, n.getCal().getTimeInMillis(), temp_PendingIntent);
        Log.d("ScheduleFragment", "Trigger at millis: " + n.getCal().getTimeInMillis() + ", Current: " + System.currentTimeMillis());
        Log.d("ScheduleFragment", "notification queued");
    }

    private void dequeueNotification(Notification n){
        PendingIntent temp_PendingIntent = createTempIntent(n);
        if(alarmManager != null)
            alarmManager.cancel(temp_PendingIntent);
    }

    private PendingIntent createTempIntent(Notification n){
        Intent temp_intent = new Intent(requireContext(), AlarmReceiver.class);
        temp_intent.putExtra("interval", n.getInterval());
        temp_intent.putExtra("id", n.getNotificationID());
        return PendingIntent.getBroadcast(requireContext(), n.getNotificationID(), temp_intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    @SuppressLint("SetTextI18n")
    public void addToTable(Notification n){
        TableRow tableRow = new TableRow(requireContext());
        TextView nameCell = new TextView(requireContext());
        nameCell.setText("Notification "+n.getNotificationID());
        nameCell.setPadding(columnPadding, columnPadding, columnPadding, columnPadding);
        tableRow.addView(nameCell);
        TextView frequencyCell = new TextView(requireContext());
        frequencyCell.setText(n.getStringInterval());
        frequencyCell.setPadding(columnPadding, columnPadding, columnPadding, columnPadding);
        tableRow.addView(frequencyCell);
        TextView startingDateCell = new TextView(requireContext());
        startingDateCell.setText(n.getDate());
        startingDateCell.setPadding(columnPadding, columnPadding, columnPadding, columnPadding);
        tableRow.addView(startingDateCell);
        ImageButton editButton = new ImageButton(requireContext());
        editButton.setImageResource(R.drawable.baseline_edit_24);
        editButton.setBackgroundColor(Color.TRANSPARENT);
        editButton.setPadding(columnPadding,columnPadding,8,columnPadding);
        editButton.setOnClickListener(v->{
            queryUser(n.getNotificationID());
        });
        tableRow.addView(editButton);
        ImageButton deleteButton = new ImageButton(requireContext());
        deleteButton.setImageResource(R.drawable.baseline_delete_24);
        deleteButton.setBackgroundColor(Color.TRANSPARENT);
        deleteButton.setPadding(8,columnPadding,columnPadding,columnPadding);
        deleteButton.setOnClickListener(v->{
                tableLayout.removeView(tableRow);

                notifications.remove(n);
                dequeueNotification(n);
                Toast.makeText(getContext(), "Reminder successfully cancelled", Toast.LENGTH_SHORT).show();
                for(int i = 0; i<notifications.size(); i++) {
                    dequeueNotification(notifications.get(i));
                    int pastID = notifications.get(i).getNotificationID();
                    notifications.get(i).setNotificationID(pastID-1);
                    queueNotification(notifications.get(i));
                }
                saveNotifications();
        });
        tableRow.addView(deleteButton);
        tableLayout.addView(tableRow);
    }
    @NonNull
    private static Calendar getCalendar(LocalDateTime dateTime) {
        Calendar cal = Calendar.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            cal.set(Calendar.YEAR, dateTime.getYear());
            cal.set(Calendar.MONTH, dateTime.getMonthValue() - 1);
            cal.set(Calendar.DAY_OF_MONTH, dateTime.getDayOfMonth());
            cal.set(Calendar.HOUR_OF_DAY, dateTime.getHour());
            cal.set(Calendar.MINUTE, dateTime.getMinute());
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
        }
        return cal;
    }

    public void showDateTimePicker(@NonNull FragmentManager fragmentManager, @NonNull Consumer<LocalDateTime> onDateTimePicked){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_12H)
                    .setHour(LocalTime.now().getHour())
                    .setMinute(LocalTime.now().getMinute()+1)
                    .setTitleText("Select time")
                    .build();
            timePicker.addOnPositiveButtonClickListener(v->{
                LocalTime time = LocalTime.of(timePicker.getHour(), timePicker.getMinute());


                MaterialDatePicker<Long> datePicker = MaterialDatePicker
                        .Builder
                        .datePicker()
                        .setTitleText("Select date")
                        .build();

                datePicker.addOnPositiveButtonClickListener(selection -> {
                   LocalDate date = Instant.ofEpochMilli(selection)
                           .atZone(ZoneId.of("UTC"))
                           .toLocalDate();
                    onDateTimePicked.accept(LocalDateTime.of(date,time));

                });
                datePicker.show(fragmentManager, "DATE_PICKER");
            });
            timePicker.show(fragmentManager, "TIME_PICKER");
        }
    }
}
