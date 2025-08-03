package com.b07group32.relationsafe;

import androidx.fragment.app.Fragment;

/**
 * Contract interface defining the interactions between View, Presenter, and Model
 * for the Login feature following MVP architecture pattern.
 */
public interface LoginContract {

    interface View {
        void showEmailPasswordLogin();
        void showPinLogin();
        void showPinSwitchOption(boolean show);
        void showEmailSwitchOption(boolean show);
        void showToast(String message);
        void showPinSetupDialog();
        void loadFragment(Fragment fragment);
        void clearPinField();
        String getEmail();
        String getPassword();
        String getPin();
        boolean isEmpty(String text);
    }

    interface Presenter {
        void onLoginButtonClicked();
        void onRegisterButtonClicked();
        void onPinLoginButtonClicked();
        void onSwitchToPinClicked();
        void onSwitchToEmailClicked();
        void onViewCreated();
        void onPinSetup(String pin, String confirmPin);
    }

    interface Model {
        interface AuthCallback {
            void onSuccess();
            void onFailure(String error);
        }

        interface PinCallback {
            void onResult(boolean isPinSet);
            void onError(String error);
        }

        interface SaveCallback {
            void onSuccess();
            void onError(String error);
        }

        interface PinVerificationCallback {
            void onResult(boolean isValid);
            void onError(String error);
        }

        void signInWithEmailAndPassword(String email, String password, AuthCallback callback);
        void createUserWithEmailAndPassword(String email, String password, AuthCallback callback);
        void authenticateWithSavedCredentials(AuthCallback callback);
        void checkIfPinIsSet(PinCallback callback);
        void saveUserCredentials(String email, String password, SaveCallback callback);
        void savePin(String pin, SaveCallback callback);
        void verifyPin(String enteredPin, PinVerificationCallback callback);
        boolean isValidPin(String pin);
    }
}