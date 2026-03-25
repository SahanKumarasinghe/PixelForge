package com.sahan.app.pixelforge.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sahan.app.pixelforge.databinding.FragmentSettingsBinding;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private FirebaseAuth auth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedPreferences = requireActivity().getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        auth = FirebaseAuth.getInstance();
        boolean isDarkMode = sharedPreferences.getBoolean("isDarkMode", false);
        binding.themeSwitch.setChecked(isDarkMode);

        binding.themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                editor.putBoolean("isDarkMode", true);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                editor.putBoolean("isDarkMode", false);
            }
            editor.apply();
        });
        binding.settingsResetPassword.setOnClickListener(v -> {
            FirebaseUser user = auth.getCurrentUser();

            if (user != null && user.getEmail() != null) {
                String email = user.getEmail();

                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Reset Password")
                        .setMessage("Are you sure?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            auth.sendPasswordResetEmail(email)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(getContext(), "Reset link sent to " + email, Toast.LENGTH_LONG).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            dialog.dismiss();
                        })
                        .show();
            } else {
                Toast.makeText(getContext(), "No email found for this account.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}