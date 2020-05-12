package org.tensorflow.lite.examples.detection;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);
        Boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        if(isLoggedIn) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
    }


}
