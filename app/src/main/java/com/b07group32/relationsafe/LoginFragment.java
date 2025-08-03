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

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

//This is the View component of MVP
public class LoginFragment extends Fragment implements LoginContract.View {

    private EditText emailField, passwordField, pinField;
    private Button loginButton, registerButton, pinLoginButton;
    private TextView switchToPin, switchToEmail;
    private LinearLayout emailPasswordLayout, pinLayout;

    private LoginContract.Presenter presenter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        initializeViews(view);
        initializePresenter();
        setupClickListeners();

        if (presenter != null) {
            presenter.onViewCreated();
        }

        return view;
    }

    private void initializeViews(View view) {
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
    }

    private void initializePresenter() {
        if (getContext() != null) {
            LoginContract.Model model = new LoginModel(getContext());
            presenter = new LoginPresenter(this, model);
        }
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> presenter.onLoginButtonClicked());
        registerButton.setOnClickListener(v -> presenter.onRegisterButtonClicked());
        pinLoginButton.setOnClickListener(v -> presenter.onPinLoginButtonClicked());
        switchToPin.setOnClickListener(v -> presenter.onSwitchToPinClicked());
        switchToEmail.setOnClickListener(v -> presenter.onSwitchToEmailClicked());
    }

    // LoginContract.View implementation
    @Override
    public void showEmailPasswordLogin() {
        emailPasswordLayout.setVisibility(View.VISIBLE);
        pinLayout.setVisibility(View.GONE);
    }

    @Override
    public boolean isEmpty(String text) {
        return TextUtils.isEmpty(text) || text.trim().isEmpty();
    }

    @Override
    public void showPinLogin() {
        emailPasswordLayout.setVisibility(View.GONE);
        pinLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void showPinSwitchOption(boolean show) {
        switchToPin.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void showEmailSwitchOption(boolean show) {
        switchToEmail.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showPinSetupDialog() {
        // Create dialog for PIN setup
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("Set Up PIN");
        builder.setMessage(
                "Create a 4 or 6 digit PIN to secure your app and login easily in the future. " +
                        "We recommend using a 6 digit PIN for a more secure experience." +
                        "If someone knows your device PIN, please use a different device PIN.");
        builder.setCancelable(false);

        // Custom layout for PIN input
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

        // PIN confirmation
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
            presenter.onPinSetup(pin, confirmPin);
        });

        builder.show();
    }

    @Override
    public void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();

        BottomNavigationView bottomNavigationView = getActivity().findViewById(R.id.bottomNavigationView);
        if (fragment instanceof HomeFragment) {
            bottomNavigationView.setVisibility(View.VISIBLE);
        } else {
            bottomNavigationView.setVisibility(View.GONE);
        }
    }

    @Override
    public void clearPinField() {
        pinField.setText("");
    }

    @Override
    public String getEmail() {
        return emailField.getText().toString().trim();
    }

    @Override
    public String getPassword() {
        return passwordField.getText().toString().trim();
    }

    @Override
    public String getPin() {
        return pinField.getText().toString().trim();
    }

    @Override
    public void onStop() {
        super.onStop();
        // Sign out user when fragment stops to ensure login next time
        // For security purposes
        if (getActivity() != null && getActivity().isFinishing()) {
            FirebaseAuth.getInstance().signOut();
        }
    }
}