package com.sahan.app.pixelforge.activities;

import static com.sahan.app.pixelforge.utils.WishlistManager.mergeLocalWishlistToFirebase;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sahan.app.pixelforge.databinding.ActivityLoginBinding;

import org.jetbrains.annotations.NotNull;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding loginBinding;
    private TextView signuplink;
    private FirebaseAuth mAuth;
    final static String TAG = "LoginActivity";
    private TextView emailInput;
    private TextView passwordInput;
    private TextView forgotPassword;

    private Button loginBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.loginBinding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(loginBinding.getRoot());

        this.emailInput = loginBinding.loginEmailInput;
        this.passwordInput = loginBinding.loginPasswordInput;
        this.loginBtn = loginBinding.loginBtn;

        this.signuplink = loginBinding.signUpLink;
        this.forgotPassword = loginBinding.loginForgotPassword;

        this.mAuth = FirebaseAuth.getInstance();

        this.signuplink.setOnClickListener((View view) -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });

        this.forgotPassword.setOnClickListener(v -> {
            forgotPasswordProcess();
        });

        this.loginBtn.setOnClickListener((View view) -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty()) {
                emailInput.setError("Email is Required");
                emailInput.requestFocus();
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInput.setError("Invalid Email");
                emailInput.requestFocus();
            } else if (password.isBlank()) {
                passwordInput.setError("Password is Required");
                passwordInput.requestFocus();
            } else {
                authenticate(email, password);
            }
        });
    }

    private void forgotPasswordProcess() {

        FirebaseAuth auth = FirebaseAuth.getInstance();
        String email = this.emailInput.getText().toString();

        if (email.isBlank()) {
            emailInput.setError("Email is Required");
            emailInput.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Invalid Email");
            emailInput.requestFocus();
            return;
        }

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "Password reset email sent",
                                Toast.LENGTH_SHORT).show();

                        new AlertDialog.Builder(this)
                                .setTitle("Open Email")
                                .setMessage("Do you want to open the email app?")
                                .setPositiveButton("Open", (dialog, which) -> {
                                    Intent intent = new Intent(Intent.ACTION_MAIN);
                                    intent.addCategory(Intent.CATEGORY_APP_EMAIL);
                                    try {
                                        startActivity(intent);
                                    } catch (ActivityNotFoundException e) {
                                        Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .setNegativeButton("Cancel", null)
                                .show();

                    } else {
                        Log.w("LoginActivity", task.getException());
                        Toast.makeText(this,
                                "Something went wrong",
                                Toast.LENGTH_SHORT).show();
                    }

                });
    }

    private void authenticate(@NotNull String email, @NotNull String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                mergeLocalWishlistToFirebase(LoginActivity.this);
                                updateUI(user);
                            }
                        } else {
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void updateUI(@NotNull FirebaseUser user) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("userName", user.getDisplayName());
        intent.putExtra("userEmail", user.getEmail());
        intent.putExtra("userMobile", user.getPhoneNumber());
        startActivity(intent);
        finish();
    }
}