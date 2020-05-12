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

import edu.iastate.snapcrack.MainActivity;
import edu.iastate.snapcrack.R;


public class SettingsFragment extends Fragment {

    private int num;

    /**
     *
     * @return A new instance of fragment SettingsFragment.
     */
    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button saveButton = view.findViewById(R.id.save_button);
        saveButton.setOnClickListener(v -> getFragmentManager().beginTransaction()
                .remove(SettingsFragment.this)
                .commit()
        );

        EditText numberText = view.findViewById(R.id.image_speed_edit_text);
        numberText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                num =  Integer.parseInt(s.toString());

                ((MainActivity)getActivity()).updateImageSpeed(num);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }
}
