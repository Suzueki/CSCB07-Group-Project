package com.b07group32.relationsafe;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class Pin {

    private static final String PREFS_NAME = "secure_prefs";
    private static final String PIN_KEY = "user_pin";
    private static final String PIN_SET_KEY = "pin_is_set";

    public static boolean isPinSet(Context context) throws GeneralSecurityException, IOException {
        SharedPreferences sharedPreferences = getEncryptedSharedPreferences(context);
        return sharedPreferences.getBoolean(PIN_SET_KEY, false);
    }

    public static void savePin(Context context, String pin) throws GeneralSecurityException, IOException {
        SharedPreferences sharedPreferences = getEncryptedSharedPreferences(context);
        sharedPreferences.edit()
                .putString(PIN_KEY, pin)
                .putBoolean(PIN_SET_KEY, true)
                .apply();
    }

    public static boolean verifyPin(Context context, String inputPin) throws GeneralSecurityException, IOException {
        SharedPreferences sharedPreferences = getEncryptedSharedPreferences(context);
        String storedPin = sharedPreferences.getString(PIN_KEY, "");
        return storedPin.equals(inputPin);
    }


    public static void clearPin(Context context) throws GeneralSecurityException, IOException {
        SharedPreferences sharedPreferences = getEncryptedSharedPreferences(context);
        sharedPreferences.edit()
                .remove(PIN_KEY)
                .putBoolean(PIN_SET_KEY, false)
                .apply();
    }

    public static boolean isValidPin(String pin) {
        if (pin == null || pin.trim().isEmpty()) return false;
        String trimmedPin = pin.trim();
        return (trimmedPin.length() == 4 || trimmedPin.length() == 6) &&
                trimmedPin.matches("\\d+");
    }

    private static SharedPreferences getEncryptedSharedPreferences(Context context)
            throws GeneralSecurityException, IOException {
        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        return EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }
}