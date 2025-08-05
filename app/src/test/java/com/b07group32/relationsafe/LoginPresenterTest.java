package com.b07group32.relationsafe;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class LoginPresenterTest {
    private LoginContract.View mockView;
    private LoginContract.Model mockModel;
    private LoginPresenter presenter;

    @Before
    public void setup() {
        mockView = mock(LoginContract.View.class);
        mockModel = mock(LoginContract.Model.class);
        presenter = new LoginPresenter(mockView, mockModel);
    }

    @Test
    public void testLoginEmpty() {
        when(mockView.getEmail()).thenReturn("");
        when(mockView.getPassword()).thenReturn("");
        when(mockView.isEmpty("")).thenReturn(true);
        presenter.onLoginButtonClicked();
        verify(mockView).showToast("Email and password required");
    }

    @Test
    public void testLoginSuccess() {
        when(mockView.getEmail()).thenReturn("rawad@utoronto.ca");
        when(mockView.getPassword()).thenReturn("rawad");
        when(mockView.isEmpty(anyString())).thenReturn(false);
        presenter.onLoginButtonClicked();
        ArgumentCaptor<LoginContract.Model.AuthCallback> authCaptor = ArgumentCaptor.forClass(LoginContract.Model.AuthCallback.class);
        verify(mockModel).signInWithEmailAndPassword(eq("rawad@utoronto.ca"), eq("rawad"), authCaptor.capture());
        authCaptor.getValue().onSuccess();
        verify(mockView).showToast("Login successful");
    }

    @Test
    public void testLoginFailure() {
        when(mockView.getEmail()).thenReturn("rawad@utoronto.ca");
        when(mockView.getPassword()).thenReturn("rawad");
        when(mockView.isEmpty(anyString())).thenReturn(false);
        presenter.onLoginButtonClicked();
        ArgumentCaptor<LoginContract.Model.AuthCallback> authCaptor = ArgumentCaptor.forClass(LoginContract.Model.AuthCallback.class);
        verify(mockModel).signInWithEmailAndPassword(eq("rawad@utoronto.ca"), eq("rawad"), authCaptor.capture());
        authCaptor.getValue().onFailure("Invalid credentials");
        verify(mockView).showToast("Login failed: Invalid credentials");
    }

    @Test
    public void testRegisterEmpty() {
        when(mockView.getEmail()).thenReturn("");
        when(mockView.getPassword()).thenReturn("");
        when(mockView.isEmpty("")).thenReturn(true);
        presenter.onRegisterButtonClicked();
        verify(mockView).showToast("Email and password required");
    }

    @Test
    public void testRegisterSuccess() {
        when(mockView.getEmail()).thenReturn("rawad@utoronto.ca");
        when(mockView.getPassword()).thenReturn("rawad");
        when(mockView.isEmpty(anyString())).thenReturn(false);
        presenter.onRegisterButtonClicked();
        ArgumentCaptor<LoginContract.Model.AuthCallback> authCaptor = ArgumentCaptor.forClass(LoginContract.Model.AuthCallback.class);
        verify(mockModel).createUserWithEmailAndPassword(eq("rawad@utoronto.ca"), eq("rawad"), authCaptor.capture());
        authCaptor.getValue().onSuccess();
        verify(mockView).showToast("Registration successful");
    }

    @Test
    public void testRegisterFailure() {
        when(mockView.getEmail()).thenReturn("rawad@utoronto.ca");
        when(mockView.getPassword()).thenReturn("rawad");
        when(mockView.isEmpty(anyString())).thenReturn(false);
        presenter.onRegisterButtonClicked();
        ArgumentCaptor<LoginContract.Model.AuthCallback> authCaptor = ArgumentCaptor.forClass(LoginContract.Model.AuthCallback.class);
        verify(mockModel).createUserWithEmailAndPassword(eq("rawad@utoronto.ca"), eq("rawad"), authCaptor.capture());
        authCaptor.getValue().onFailure("Rawad is banned");
        verify(mockView).showToast("Registration failed: Rawad is banned");
    }

    @Test
    public void testPinLoginEmpty() {
        when(mockView.getPin()).thenReturn("");
        when(mockView.isEmpty("")).thenReturn(true);
        presenter.onPinLoginButtonClicked();
        verify(mockView).showToast("Please enter your PIN");
    }

    @Test
    public void testPinLoginSuccess() {
        when(mockView.getPin()).thenReturn("1234");
        when(mockView.isEmpty(anyString())).thenReturn(false);
        presenter.onPinLoginButtonClicked();
        ArgumentCaptor<LoginContract.Model.PinVerificationCallback> pinVerifierCaptor = ArgumentCaptor.forClass(LoginContract.Model.PinVerificationCallback.class);
        verify(mockModel).verifyPin(eq("1234"), pinVerifierCaptor.capture());
        pinVerifierCaptor.getValue().onResult(true);

        ArgumentCaptor<LoginContract.Model.AuthCallback> authCaptor = ArgumentCaptor.forClass(LoginContract.Model.AuthCallback.class);
        verify(mockModel).authenticateWithSavedCredentials(authCaptor.capture());
        authCaptor.getValue().onSuccess();
        verify(mockView).showToast("PIN login successful");
    }

    @Test
    public void testPinLoginFailure() {
        when(mockView.getPin()).thenReturn("1234");
        when(mockView.isEmpty(anyString())).thenReturn(false);
        presenter.onPinLoginButtonClicked();

        ArgumentCaptor<LoginContract.Model.PinVerificationCallback> pinVerifierCaptor = ArgumentCaptor.forClass(LoginContract.Model.PinVerificationCallback.class);
        verify(mockModel).verifyPin(eq("1234"), pinVerifierCaptor.capture());
        pinVerifierCaptor.getValue().onResult(false);
        verify(mockView).showToast("Invalid PIN");
    }

    @Test
    public void testPinLoginAuthFailure() {
        when(mockView.getPin()).thenReturn("1234");
        when(mockView.isEmpty(anyString())).thenReturn(false);
        presenter.onPinLoginButtonClicked();
        ArgumentCaptor<LoginContract.Model.PinVerificationCallback> pinVerifierCaptor = ArgumentCaptor.forClass(LoginContract.Model.PinVerificationCallback.class);
        verify(mockModel).verifyPin(eq("1234"), pinVerifierCaptor.capture());
        pinVerifierCaptor.getValue().onResult(true);

        ArgumentCaptor<LoginContract.Model.AuthCallback> authCaptor = ArgumentCaptor.forClass(LoginContract.Model.AuthCallback.class);
        verify(mockModel).authenticateWithSavedCredentials(authCaptor.capture());
        authCaptor.getValue().onFailure("Authentication error");
        verify(mockView).showToast("Authentication error");
    }

    @Test
    public void testSwitchToPin(){
        presenter.onSwitchToPinClicked();
        verify(mockView).showPinLogin();
    }

    @Test
    public void testSwitchToEmail(){
        presenter.onSwitchToEmailClicked();
        verify(mockView).showEmailPasswordLogin();
    }

    @Test
    public void testPinSetupSuccess() {
        when(mockModel.isValidPin("1234")).thenReturn(true);
        presenter.onPinSetup("1234", "1234");
        ArgumentCaptor<LoginContract.Model.SaveCallback> saveCaptor = ArgumentCaptor.forClass(LoginContract.Model.SaveCallback.class);
        verify(mockModel).savePin(eq("1234"), saveCaptor.capture());
        saveCaptor.getValue().onSuccess();
        verify(mockView).showToast("PIN set successfully");
    }

    @Test
    public void testPinSetupFailure() {
        when(mockModel.isValidPin("1234")).thenReturn(true);
        presenter.onPinSetup("1234", "1234");
        ArgumentCaptor<LoginContract.Model.SaveCallback> saveCaptor = ArgumentCaptor.forClass(LoginContract.Model.SaveCallback.class);
        verify(mockModel).savePin(eq("1234"), saveCaptor.capture());
        saveCaptor.getValue().onError("PIN has been leaked");
        verify(mockView).showToast("PIN has been leaked");
    }

    @Test
    public void testInitialLoginPinSet(){
        presenter.onViewCreated(); // no special actions - but need view loaded
        ArgumentCaptor<LoginContract.Model.PinCallback> pinCaptor = ArgumentCaptor.forClass(LoginContract.Model.PinCallback.class);
        verify(mockModel).checkIfPinIsSet(pinCaptor.capture());
        pinCaptor.getValue().onResult(true);
        verify(mockView, times(2)).showPinSwitchOption(true);
    }

    @Test
    public void testInitialLoginPinNotSet(){
        presenter.onViewCreated();
        ArgumentCaptor<LoginContract.Model.PinCallback> pinCaptor = ArgumentCaptor.forClass(LoginContract.Model.PinCallback.class);
        verify(mockModel).checkIfPinIsSet(pinCaptor.capture());
        pinCaptor.getValue().onResult(false);
        verify(mockView).showPinSwitchOption(false);
    }

    @Test
    public void testInitialLoginPinError(){
        presenter.onViewCreated();
        ArgumentCaptor<LoginContract.Model.PinCallback> pinCaptor = ArgumentCaptor.forClass(LoginContract.Model.PinCallback.class);
        verify(mockModel).checkIfPinIsSet(pinCaptor.capture());
        pinCaptor.getValue().onError("Something went wrong");
        verify(mockView).showPinSwitchOption(false);
    }

    @Test
    public void testCheckPinStatusSet(){
        presenter.checkPinStatus();
        ArgumentCaptor<LoginContract.Model.PinCallback> pinCaptor = ArgumentCaptor.forClass(LoginContract.Model.PinCallback.class);
        verify(mockModel).checkIfPinIsSet(pinCaptor.capture());
        pinCaptor.getValue().onResult(true);
        verify(mockView).loadFragment(any(HomeFragment.class));
    }

    @Test
    public void testCheckPinStatusNotSet(){
        presenter.checkPinStatus();
        ArgumentCaptor<LoginContract.Model.PinCallback> pinCaptor = ArgumentCaptor.forClass(LoginContract.Model.PinCallback.class);
        verify(mockModel).checkIfPinIsSet(pinCaptor.capture());
        pinCaptor.getValue().onResult(false);
        verify(mockView).showPinSetupDialog();
    }

    @Test
    public void testCheckPinStatusError(){
        presenter.checkPinStatus();
        ArgumentCaptor<LoginContract.Model.PinCallback> pinCaptor = ArgumentCaptor.forClass(LoginContract.Model.PinCallback.class);
        verify(mockModel).checkIfPinIsSet(pinCaptor.capture());
        pinCaptor.getValue().onError("Something went wrong");
        verify(mockView).showToast("Something went wrong");
    }
}
