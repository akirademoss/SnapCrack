package edu.iastate.snapcrack.ui;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import edu.iastate.snapcrack.R;
import edu.iastate.snapcrack.util.VolleyRequest;

/**
 * Fragment for user account creation
 */
public class CreateAccountFragment extends Fragment {

    private EditText usernameText;
    private EditText passwordText;
    private EditText passwordConfirmText;

    private String username;
    private String password;
    private String confirmedPassword;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button submitBtn = view.findViewById(R.id.submit_button);
        submitBtn.setOnClickListener(v -> comparePasswords());

        usernameText = view.findViewById(R.id.username_edit_text);
        usernameText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                username = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        passwordText = view.findViewById(R.id.password_edit_text);
        passwordText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                password = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        passwordConfirmText = view.findViewById(R.id.password_confirm_edit_text);
        passwordConfirmText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                confirmedPassword = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void comparePasswords() {
        if(password == null) {
            passwordText.setError("Must enter a password");
        }

        if(confirmedPassword == null) {
            passwordConfirmText.setError("Must confirm your password");
        }

        if(username == null) {
            usernameText.setError("Must enter a username");
        }

        if(username != null && password != null && confirmedPassword != null) {
            if (!password.equals(confirmedPassword)) {
                passwordText.setError("Passwords do not match");
                passwordConfirmText.setError("Passwords do not match");
            } else {
                submitUser();
            }
        }
    }

    private void submitUser() { //TODO catch case where two users sign up with the same username
        String json = "{username: " + username + ", password: " + password + "}";
        JSONObject js = null;
        try {
            js = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        VolleyRequest.createUser(js, getContext());
    }
}
