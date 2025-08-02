package com.b07group32.relationsafe;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class Notification {
//    static int numNotifications = 0;
    private boolean ranBefore = false;
    private long interval;
    private int notificationID;
//    private Intent notifyIntent;
//    private PendingIntent notifyPendingIntent;
    private Calendar cal;
    private String period;
    private int amount;
    public Notification(int notificationID, Calendar cal, long interval, Context context, String period, int amount){
        this.notificationID = notificationID;
        this.setCal(cal);
        this.setInterval(interval);
        this.setPeriod(period);
        this.setAmount(amount);
//        setNotifyIntent(new Intent(context, AlarmReceiver.class));
//        getNotifyIntent().putExtra("interval", interval);
//        setNotifyPendingIntent(PendingIntent.getBroadcast(context, getNotificationID(), getNotifyIntent(), PendingIntent.FLAG_IMMUTABLE));
    }

    public void setNotificationID(int notificationID){
        this.notificationID = notificationID;
    }

    public int getNotificationID() {
        return notificationID;
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

//    public Intent getNotifyIntent() {
//        return notifyIntent;
//    }
//
//    public void setNotifyIntent(Intent notifyIntent) {
//        this.notifyIntent = notifyIntent;
//    }
//
//    public PendingIntent getNotifyPendingIntent() {
//        return notifyPendingIntent;
//    }
//
//    public void setNotifyPendingIntent(PendingIntent notifyPendingIntent) {
//        this.notifyPendingIntent = notifyPendingIntent;
//    }

    public Calendar getCal() {
        return cal;
    }

    public void setCal(Calendar cal) {
        this.cal = cal;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
    public String getStringInterval(){
        return getAmount()+" "+getPeriod();
    }

    public String getTimeOfDay(){
        int min = cal.get(Calendar.MINUTE);
        String min_string = "";
        if(min < 10){
            min_string = "0"+min;
        }
        return cal.get(Calendar.HOUR_OF_DAY)+":"+min_string;
    }
    public String getDate(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd 'at' HH:mm");
        if(!ranBefore && System.currentTimeMillis() < cal.getTimeInMillis()){
            return simpleDateFormat.format(cal.getTimeInMillis());
        }
        if(!ranBefore && System.currentTimeMillis() > cal.getTimeInMillis()) {
            Log.d("Date", "never ran before");
            ranBefore = true;
        }
        long nextTrigger = cal.getTimeInMillis() + interval;
        if (System.currentTimeMillis() > nextTrigger) {
            cal.setTimeInMillis(nextTrigger);
            nextTrigger = cal.getTimeInMillis() + interval;
            Log.d("Date", "past the trigger");
            return simpleDateFormat.format(nextTrigger);

        }
        Log.d("Date", "else");
        return simpleDateFormat.format(nextTrigger);
//        return ""+(cal.get(Calendar.DATE));
//        return cal.get(Calendar.YEAR)+"/"+(cal.get(Calendar.MONTH)+1)+"/"+cal.get(Calendar.DAY_OF_MONTH);
    }
}
