/*
 * Copyright (C) 2016 Simon Norberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.awsomefox.sprocket.ui;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.widget.ContentLoadingProgressBar;

import com.awsomefox.sprocket.R;
import com.awsomefox.sprocket.SprocketApp;
import com.awsomefox.sprocket.data.LoginManager;
import com.awsomefox.sprocket.data.api.PlexService;
import com.awsomefox.sprocket.util.Rx;
import com.awsomefox.sprocket.util.Strings;
import com.awsomefox.sprocket.util.Views;
import com.bluelinelabs.conductor.RouterTransaction;

import javax.inject.Inject;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import okhttp3.Credentials;

import static com.bluelinelabs.conductor.rxlifecycle2.ControllerEvent.DETACH;

public class LoginController extends BaseController {

    @BindView(R.id.login_form)
    LinearLayout loginForm;
    @BindView(R.id.username_edit)
    EditText usernameEdit;
    @BindView(R.id.password_edit)
    EditText passwordEdit;
    @BindView(R.id.content_loading)
    ContentLoadingProgressBar contentLoading;

    @BindView(R.id.login_button)
    Button loginButton;
    @BindString(R.string.app_name)
    String appName;
    @BindString(R.string.invalid_username)
    String invalidUsername;
    @BindString(R.string.invalid_password)
    String invalidPassword;

    @Inject
    PlexService plex;
    @Inject
    LoginManager loginManager;
    @Inject
    InputMethodManager imm;
    @Inject
    Rx rx;

    public LoginController(Bundle args) {
        super(args);
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.controller_login;
    }

    @Override
    protected void injectDependencies() {
        if (getActivity() != null) {
            SprocketApp.get(getActivity()).component().inject(this);
        }
    }

    @NonNull
    @Override
    protected View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
        View view = super.onCreateView(inflater, container);
        usernameEdit.requestFocus();
        contentLoading.hide();
        return view;
    }

    @SuppressWarnings("unused")
    @OnEditorAction(R.id.password_edit)
    boolean onPasswordEditorAction(TextView view, int actionId, KeyEvent event) {
        if ((event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
                || (actionId == EditorInfo.IME_ACTION_DONE)) {
            login();
            return true;
        }
        return false;
    }

    @OnClick(R.id.login_button)
    void onLoginPressed() {
        login();
    }

    private void login() {
        disableInput();

        String username = usernameEdit.getText().toString();
        String password = passwordEdit.getText().toString();

        if (Strings.isBlank(username)) {
            usernameEdit.setError(invalidUsername);
            enableInput();
            return;
        }
        if (Strings.isBlank(password) || password.length() < 8) {
            passwordEdit.setError(invalidPassword);
            enableInput();
            return;
        }

        hideInputMethod();
        Views.invisible(loginForm);
        contentLoading.show();
        disposables.add(plex.signIn(Credentials.basic(username, password))
                .compose(bindUntilEvent(DETACH))
                .compose(rx.singleSchedulers())
                .subscribe(user -> {
                    loginManager.login(user.authenticationToken);
                    if (getActivity() != null) {
                        getActivity().invalidateOptionsMenu();
                    }
                    getRouter().setRoot(RouterTransaction.with(new BrowserController(null)));
                }, e -> {
                    Rx.onError(e);
                    loginManager.logout();
                    contentLoading.hide();
                    Views.visible(loginForm);
                    Toast.makeText(getActivity(), R.string.sign_in_failed,
                            Toast.LENGTH_SHORT).show();
                    enableInput();
                }));
    }

    private void enableInput() {
        usernameEdit.setEnabled(true);
        passwordEdit.setEnabled(true);
    }

    private void disableInput() {
        usernameEdit.setEnabled(false);
        passwordEdit.setEnabled(false);
    }

    private void hideInputMethod() {
        imm.hideSoftInputFromWindow(usernameEdit.getWindowToken(), 0);
        imm.hideSoftInputFromWindow(passwordEdit.getWindowToken(), 0);
    }
}
