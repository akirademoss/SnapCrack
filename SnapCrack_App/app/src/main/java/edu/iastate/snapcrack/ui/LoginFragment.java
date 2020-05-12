package edu.iastate.snapcrack.ui;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONObject;

import edu.iastate.snapcrack.R;
import edu.iastate.snapcrack.util.VolleyRequest;

/**
 * Fragment for user login
 */
public class LoginFragment extends Fragment {

    private EditText usernameText;
    private EditText passwordText;
    private String username;
    private String password;

    private static String TAG = "LoginFragment";


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button createAccountBtn = view.findViewById(R.id.create_account_button);
        createAccountBtn.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.createAccountFragment));

        Button loginBtn = view.findViewById(R.id.login_button);
        loginBtn.setOnClickListener(v -> onLoginClicked());

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
    }

    private void onLoginClicked() {

        if(username == null) {
            usernameText.setError("Please enter your username");
        }
        if(password == null) {
            passwordText.setError("Please enter your password");
        }

        if(username != null && password != null) {
            String json = "{username: " + username + ", password: " + password + "}";

            try {
                JSONObject jsonObject = new JSONObject(json);
                VolleyRequest.loginUser(jsonObject, getContext());
                Log.d(TAG, "Login submitted");
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }


}
