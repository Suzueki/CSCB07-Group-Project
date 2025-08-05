package com.b07group32.relationsafe;

//This is the presenter component of MVP
public class LoginPresenter implements LoginContract.Presenter {

    private LoginContract.View view;
    private LoginContract.Model model;
    private boolean isPinLoginMode = false;

    public LoginPresenter(LoginContract.View view, LoginContract.Model model) {
        this.view = view;
        this.model = model;
    }

    @Override
    public void onViewCreated() {
        checkInitialLoginMode();
    }

    @Override
    public void onLoginButtonClicked() {
        String email = view.getEmail();
        String password = view.getPassword();

        if (view.isEmpty(email) || view.isEmpty(password)) {
            view.showToast("Email and password required");
            return;
        }

        model.signInWithEmailAndPassword(email, password, new LoginContract.Model.AuthCallback() {
            @Override
            public void onSuccess() {
                view.showToast("Login successful");
                model.saveUserCredentials(email, password, new LoginContract.Model.SaveCallback() {
                    @Override
                    public void onSuccess() {
                        checkPinStatus();
                    }

                    @Override
                    public void onError(String error) {
                        // Continue even if credential saving fails
                        checkPinStatus();
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                view.showToast("Login failed: " + error);
            }
        });
    }

    @Override
    public void onRegisterButtonClicked() {
        String email = view.getEmail();
        String password = view.getPassword();

        if (view.isEmpty(email) || view.isEmpty(password)) {
            view.showToast("Email and password required");
            return;
        }

        model.createUserWithEmailAndPassword(email, password, new LoginContract.Model.AuthCallback() {
            @Override
            public void onSuccess() {
                view.showToast("Registration successful");
                model.saveUserCredentials(email, password, new LoginContract.Model.SaveCallback() {
                    @Override
                    public void onSuccess() {
                        checkPinStatus();
                    }

                    @Override
                    public void onError(String error) {
                        // Continue even if credential saving fails
                        checkPinStatus();
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                view.showToast("Registration failed: " + error);
            }
        });
    }

    @Override
    public void onPinLoginButtonClicked() {
        String enteredPin = view.getPin();

        if (view.isEmpty(enteredPin)) {
            view.showToast("Please enter your PIN");
            return;
        }

        model.verifyPin(enteredPin, new LoginContract.Model.PinVerificationCallback() {
            @Override
            public void onResult(boolean isValid) {
                if (isValid) {
                    model.authenticateWithSavedCredentials(new LoginContract.Model.AuthCallback() {
                        @Override
                        public void onSuccess() {
                            view.showToast("PIN login successful");
                            view.loadFragment(new HomeFragment());
                        }

                        @Override
                        public void onFailure(String error) {
                            view.showToast(error);
                            switchToEmailLogin();
                        }
                    });
                } else {
                    view.showToast("Invalid PIN");
                    view.clearPinField();
                }
            }

            @Override
            public void onError(String error) {
                view.showToast("Error verifying PIN");
            }
        });
    }

    @Override
    public void onSwitchToPinClicked() {
        switchToPinLogin();
    }

    @Override
    public void onSwitchToEmailClicked() {
        switchToEmailLogin();
    }

    @Override
    public void onPinSetup(String pin, String confirmPin) {
        if (model.isValidPin(pin) && pin.equals(confirmPin)) {
            model.savePin(pin, new LoginContract.Model.SaveCallback() {
                @Override
                public void onSuccess() {
                    view.showToast("PIN set successfully");
                    view.loadFragment(new HomeFragment());
                }

                @Override
                public void onError(String error) {
                    view.showToast(error);
                }
            });
        } else {
            view.showToast("PIN must be 4 or 6 digits and match confirmation");
            view.showPinSetupDialog(); // Show dialog again if invalid PIN
        }
    }

    public void checkInitialLoginMode() {
        model.checkIfPinIsSet(new LoginContract.Model.PinCallback() {
            @Override
            public void onResult(boolean isPinSet) {
                if (isPinSet) {
                    view.showPinSwitchOption(true);
                    showEmailPasswordLogin();
                } else {
                    view.showPinSwitchOption(false);
                    showEmailPasswordLogin();
                }
            }

            @Override
            public void onError(String error) {
                // In case PIN check fails, serve standard Firebase login
                view.showPinSwitchOption(false);
                showEmailPasswordLogin();
            }
        });
    }

    public void switchToPinLogin() {
        isPinLoginMode = true;
        showPinLogin();
    }

    public void switchToEmailLogin() {
        isPinLoginMode = false;
        showEmailPasswordLogin();
    }

    public void showEmailPasswordLogin() {
        view.showEmailPasswordLogin();
        view.showPinSwitchOption(true);
        view.showEmailSwitchOption(false);
    }

    public void showPinLogin() {
        view.showPinLogin();
        view.showPinSwitchOption(false);
        view.showEmailSwitchOption(true);
    }

    public void checkPinStatus() {
        model.checkIfPinIsSet(new LoginContract.Model.PinCallback() {
            @Override
            public void onResult(boolean isPinSet) {
                if (isPinSet) {
                    // If PIN already exists, go to main app
                    view.loadFragment(new HomeFragment());
                } else {
                    // If PIN not set, prompt user to create one
                    view.showPinSetupDialog();
                }
            }

            @Override
            public void onError(String error) {
                view.showToast(error);
            }
        });
    }
}