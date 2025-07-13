package com.b07group32.relationsafe;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.google.firebase.auth.FirebaseAuth;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class LoginFragment extends Fragment {

    private EditText emailField, passwordField, pinField;
    private Button loginButton, registerButton, pinLoginButton;
    private TextView switchToPin, switchToEmail;
    private LinearLayout emailPasswordLayout, pinLayout;
    private FirebaseAuth auth;

    private static final String PREFS_NAME = "secure_prefs";
    private static final String PIN_KEY = "user_pin";
    private static final String PIN_SET_KEY = "pin_is_set";
    private static final String USER_EMAIL_KEY = "user_email";
    private static final String USER_PASSWORD_KEY = "user_password";


    private boolean isPinLoginMode = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        //gets the variables for us to compare logins to
        //not sure if this is good practices but it works and the user wouldn't
        //be able to access this when it's compiled
        emailField = view.findViewById(R.id.editTextEmail);
        passwordField = view.findViewById(R.id.editTextPassword);
        pinField = view.findViewById(R.id.editTextPin);
        loginButton = view.findViewById(R.id.buttonLogin);
        registerButton = view.findViewById(R.id.buttonRegister);
        pinLoginButton = view.findViewById(R.id.buttonPinLogin);
        switchToPin = view.findViewById(R.id.textSwitchToPin);
        switchToEmail = view.findViewById(R.id.textSwitchToEmail);
        emailPasswordLayout = view.findViewById(R.id.layoutEmailPassword);
        pinLayout = view.findViewById(R.id.layoutPin);

        auth = FirebaseAuth.getInstance();

        //click listeners for buttons
        loginButton.setOnClickListener(v -> loginUser());
        registerButton.setOnClickListener(v -> registerUser());
        pinLoginButton.setOnClickListener(v -> loginWithPin());
        switchToPin.setOnClickListener(v -> switchToPinLogin());
        switchToEmail.setOnClickListener(v -> switchToEmailLogin());

        //shows logins based on if a pin exists
        checkInitialLoginMode();

        return view;
    }

    private void checkInitialLoginMode() {
        try {
            if (isPinSet()) {
                switchToPin.setVisibility(View.VISIBLE);
                showEmailPasswordLogin();
            }
            else {
                switchToPin.setVisibility(View.GONE);
                showEmailPasswordLogin();
            }
        } catch (GeneralSecurityException | IOException e) {
            //in the case the PIN fails, just serve the user the standard firebase login
            switchToPin.setVisibility(View.GONE);
            showEmailPasswordLogin();
            e.printStackTrace();
        }
    }

    //The following methods are a bit stupid imo
    //in the spirit of 'efficient' and factored code, we use this
    //means we don't need separate fragments for pin and firebase login
    private void switchToPinLogin() {
        isPinLoginMode = true;
        showPinLogin();
    }

    private void switchToEmailLogin() {
        isPinLoginMode = false;
        showEmailPasswordLogin();
    }

    private void showEmailPasswordLogin() {
        emailPasswordLayout.setVisibility(View.VISIBLE);
        pinLayout.setVisibility(View.GONE);
        switchToPin.setVisibility(View.VISIBLE);
        switchToEmail.setVisibility(View.GONE);
    }

    private void showPinLogin() {
        emailPasswordLayout.setVisibility(View.GONE);
        pinLayout.setVisibility(View.VISIBLE);
        switchToPin.setVisibility(View.GONE);
        switchToEmail.setVisibility(View.VISIBLE);
    }

    private void loginUser() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(getContext(), "Email and password required", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Login successful", Toast.LENGTH_SHORT).show();
                        // Save user credentials for PIN login reference
                        saveUserCredentials(email, password);
                        checkPinStatus();
                    }
                    else {
                        Toast.makeText(getContext(), "Login failed: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void loginWithPin() {
        String enteredPin = pinField.getText().toString().trim();

        if (TextUtils.isEmpty(enteredPin)) {
            Toast.makeText(getContext(), "Please enter your PIN", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String savedPin = getSavedPin();
            if (savedPin != null && savedPin.equals(enteredPin)) {
                // if PIN matches - authenticate with Firebase using saved credentials
                authenticateWithSavedCredentials();
            }
            else {
                Toast.makeText(getContext(), "Invalid PIN", Toast.LENGTH_SHORT).show();
                pinField.setText("");
            }
        }
        catch (GeneralSecurityException | IOException e) {
            Toast.makeText(getContext(), "Error verifying PIN", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void authenticateWithSavedCredentials() {
        try {
            String savedEmail = getSavedUserEmail();
            String savedPassword = getSavedUserPassword();

            if (savedEmail != null && savedPassword != null) {
                // Authenticate with Firebase using saved credentials
                auth.signInWithEmailAndPassword(savedEmail, savedPassword)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(getContext(), "PIN login successful", Toast.LENGTH_SHORT).show();
                                loadFragment(new EmergencyInfoStorageFragment());
                            }
                            else {
                                Toast.makeText(getContext(), "Authentication failed. Please use email login.",
                                        Toast.LENGTH_LONG).show();
                                switchToEmailLogin();
                            }
                        });
            }
            else {
                Toast.makeText(getContext(), "No saved credentials found. Please use email login.",
                        Toast.LENGTH_LONG).show();
                switchToEmailLogin();
            }
        }
        catch (GeneralSecurityException | IOException e) {
            Toast.makeText(getContext(), "Security error occurred. Please try again.",
                    Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void registerUser() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(getContext(), "Email and password required", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Registration successful", Toast.LENGTH_SHORT).show();
                        saveUserCredentials(email, password);
                        checkPinStatus();
                    }
                    else {
                        Toast.makeText(getContext(), "Registration failed: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkPinStatus() {
        try {
            if(isPinSet()) {
                // if PIN already exists, go to main app
                loadFragment(new EmergencyInfoStorageFragment());
            }
            else {
                // if PIN not set, prompts user to create one
                showPinSetupDialog();
            }
        }
        catch (GeneralSecurityException | IOException e) {
            Toast.makeText(getContext(), "Error checking PIN status", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private boolean isPinSet() throws GeneralSecurityException, IOException {
        MasterKey masterKey = new MasterKey.Builder(getContext())
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        android.content.SharedPreferences sharedPreferences = EncryptedSharedPreferences.create(
                getContext(),
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );

        return sharedPreferences.getBoolean(PIN_SET_KEY, false);
    }

    private String getSavedPin() throws GeneralSecurityException, IOException {
        MasterKey masterKey = new MasterKey.Builder(getContext())
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        android.content.SharedPreferences sharedPreferences = EncryptedSharedPreferences.create(
                getContext(),
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );

        return sharedPreferences.getString(PIN_KEY, null);
    }


    private String getSavedUserEmail() throws GeneralSecurityException, IOException {
        MasterKey masterKey = new MasterKey.Builder(getContext())
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        android.content.SharedPreferences sharedPreferences = EncryptedSharedPreferences.create(
                getContext(),
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );

        return sharedPreferences.getString(USER_EMAIL_KEY, null);
    }

    private void saveUserCredentials(String email, String password) {
        try {
            MasterKey masterKey = new MasterKey.Builder(getContext())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            android.content.SharedPreferences sharedPreferences = EncryptedSharedPreferences.create(
                    getContext(),
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            sharedPreferences.edit()
                    .putString(USER_EMAIL_KEY, email)
                    .putString(USER_PASSWORD_KEY, password)
                    .apply();
        }
        catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    private String getSavedUserPassword() throws GeneralSecurityException, IOException {
        MasterKey masterKey = new MasterKey.Builder(getContext())
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        android.content.SharedPreferences sharedPreferences = EncryptedSharedPreferences.create(
                getContext(),
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );

        return sharedPreferences.getString(USER_PASSWORD_KEY, null);
    }

    private void showPinSetupDialog() {
        // creates dialog for PIN setup
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("Set Up PIN");
        builder.setMessage(
                "Create a 4 or 6 digit PIN to secure your app and login easily in the future. " +
                        "We recommend using a 6 digit PIN for a more secure experience." +
                        "If someone knows your device PIN, please use a different device PIN.");
        builder.setCancelable(false);
        //FOR TESTING (hello team members!)
        //ran into the issue of your pin being saved even when recompiling when testing this
        //look for Device Manager, and wipe data to erase PIN settings

        // custom layout for PIN input, looks nice enough, feel free to pad a bit more
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);

        // PIN input field
        final EditText pinInput = new EditText(getContext());
        pinInput.setHint("Enter 4 or 6 digit PIN");
        pinInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER |
                android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pinInput.setMaxLines(1);
        layout.addView(pinInput);

        //PIN confirmation
        final EditText confirmPinInput = new EditText(getContext());
        confirmPinInput.setHint("Confirm PIN");
        confirmPinInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER |
                android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        confirmPinInput.setMaxLines(1);
        layout.addView(confirmPinInput);

        builder.setView(layout);

        builder.setPositiveButton("Set PIN", (dialog, which) -> {
            String pin = pinInput.getText().toString().trim();
            String confirmPin = confirmPinInput.getText().toString().trim();

            if (isValidPin(pin) && pin.equals(confirmPin)) {
                savePin(pin);
            }
            else {
                Toast.makeText(getContext(), "PIN must be 4 or 6 digits and match confirmation",
                        Toast.LENGTH_LONG).show();
                showPinSetupDialog(); // Show dialog again if invalid PIN
            }
        });

        builder.show();
    }

    private boolean isValidPin(String pin) {
        if (TextUtils.isEmpty(pin)) return false;
        return (pin.length() == 4 || pin.length() == 6) && TextUtils.isDigitsOnly(pin);
    }

    private void savePin(String pin) {
        try {
            MasterKey masterKey = new MasterKey.Builder(getContext())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            android.content.SharedPreferences sharedPreferences = EncryptedSharedPreferences.create(
                    getContext(),
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            sharedPreferences.edit()
                    .putString(PIN_KEY, pin)
                    .putBoolean(PIN_SET_KEY, true)
                    .apply();

            Toast.makeText(getContext(), "PIN set successfully", Toast.LENGTH_SHORT).show();
            loadFragment(new EmergencyInfoStorageFragment());

        }
        catch (GeneralSecurityException | IOException e) {
            Toast.makeText(getContext(), "Error saving PIN", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}