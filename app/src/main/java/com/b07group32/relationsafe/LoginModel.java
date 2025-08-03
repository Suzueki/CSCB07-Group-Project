package com.b07group32.relationsafe;

import android.content.Context;
import android.text.TextUtils;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import com.google.firebase.auth.FirebaseAuth;

import java.io.IOException;
import java.security.GeneralSecurityException;

//This is the model component of MVP
public class LoginModel implements LoginContract.Model {

    private FirebaseAuth auth;
    private Context context;

    private static final String PREFS_NAME = "secure_prefs";
    private static final String PIN_KEY = "user_pin";
    private static final String PIN_SET_KEY = "pin_is_set";
    private static final String USER_EMAIL_KEY = "user_email";
    private static final String USER_PASSWORD_KEY = "user_password";

    public LoginModel(Context context) {
        this.context = context;
        this.auth = FirebaseAuth.getInstance();
    }

    @Override
    public void signInWithEmailAndPassword(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Login failed";
                        callback.onFailure(error);
                    }
                });
    }

    @Override
    public void createUserWithEmailAndPassword(String email, String password, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Registration failed";
                        callback.onFailure(error);
                    }
                });
    }

    @Override
    public void authenticateWithSavedCredentials(AuthCallback callback) {
        try {
            String savedEmail = getSavedUserEmail();
            String savedPassword = getSavedUserPassword();

            if (savedEmail != null && savedPassword != null) {
                auth.signInWithEmailAndPassword(savedEmail, savedPassword)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                callback.onSuccess();
                            } else {
                                callback.onFailure("Authentication failed. Please use email login.");
                            }
                        });
            } else {
                callback.onFailure("No saved credentials found. Please use email login.");
            }
        } catch (GeneralSecurityException | IOException e) {
            callback.onFailure("Security error occurred. Please try again.");
        }
    }

    @Override
    public void checkIfPinIsSet(PinCallback callback) {
        try {
            boolean isPinSet = isPinSet();
            callback.onResult(isPinSet);
        } catch (GeneralSecurityException | IOException e) {
            callback.onError("Error checking PIN status");
        }
    }

    @Override
    public void saveUserCredentials(String email, String password, SaveCallback callback) {
        try {
            android.content.SharedPreferences sharedPreferences = getEncryptedSharedPreferences();
            sharedPreferences.edit()
                    .putString(USER_EMAIL_KEY, email)
                    .putString(USER_PASSWORD_KEY, password)
                    .apply();
            callback.onSuccess();
        } catch (GeneralSecurityException | IOException e) {
            callback.onError("Error saving credentials");
        }
    }

    @Override
    public void savePin(String pin, SaveCallback callback) {
        try {
            android.content.SharedPreferences sharedPreferences = getEncryptedSharedPreferences();
            sharedPreferences.edit()
                    .putString(PIN_KEY, pin)
                    .putBoolean(PIN_SET_KEY, true)
                    .apply();
            callback.onSuccess();
        } catch (GeneralSecurityException | IOException e) {
            callback.onError("Error saving PIN");
        }
    }

    @Override
    public void verifyPin(String enteredPin, PinVerificationCallback callback) {
        try {
            String savedPin = getSavedPin();
            boolean isValid = savedPin != null && savedPin.equals(enteredPin);
            callback.onResult(isValid);
        } catch (GeneralSecurityException | IOException e) {
            callback.onError("Error verifying PIN");
        }
    }

    @Override
    public boolean isValidPin(String pin) {
        if (TextUtils.isEmpty(pin)) return false;
        return (pin.length() == 4 || pin.length() == 6) && TextUtils.isDigitsOnly(pin);
    }

    // Keep this method for backward compatibility but mark as deprecated
    @Deprecated
    public boolean verifyPin(String enteredPin) throws GeneralSecurityException, IOException {
        String savedPin = getSavedPin();
        return savedPin != null && savedPin.equals(enteredPin);
    }

    private boolean isPinSet() throws GeneralSecurityException, IOException {
        android.content.SharedPreferences sharedPreferences = getEncryptedSharedPreferences();
        return sharedPreferences.getBoolean(PIN_SET_KEY, false);
    }

    private String getSavedPin() throws GeneralSecurityException, IOException {
        android.content.SharedPreferences sharedPreferences = getEncryptedSharedPreferences();
        return sharedPreferences.getString(PIN_KEY, null);
    }

    private String getSavedUserEmail() throws GeneralSecurityException, IOException {
        android.content.SharedPreferences sharedPreferences = getEncryptedSharedPreferences();
        return sharedPreferences.getString(USER_EMAIL_KEY, null);
    }

    private String getSavedUserPassword() throws GeneralSecurityException, IOException {
        android.content.SharedPreferences sharedPreferences = getEncryptedSharedPreferences();
        return sharedPreferences.getString(USER_PASSWORD_KEY, null);
    }

    private android.content.SharedPreferences getEncryptedSharedPreferences()
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