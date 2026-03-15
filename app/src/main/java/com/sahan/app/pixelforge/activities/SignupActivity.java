package com.sahan.app.pixelforge.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sahan.app.pixelforge.databinding.ActivitySignupBinding;
import com.sahan.app.pixelforge.models.User;

public class SignupActivity extends AppCompatActivity {

    private ActivitySignupBinding signupBinding;
    private FirebaseAuth firebaseAuth;
    private static final String TAG = "SignUpActivity";
    private FirebaseFirestore firestoreDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        signupBinding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(signupBinding.getRoot());

        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firestoreDB = FirebaseFirestore.getInstance();

        signupBinding.signInLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        signupBinding.signupBtn.setOnClickListener((View view) -> {

            String name = signupBinding.signupNameInput.getText().toString().trim();
            String email = signupBinding.signupEmailInput.getText().toString().trim();
            String password = signupBinding.signupPasswordInput.getText().toString().trim();
            String confirmationPassword = signupBinding.confirmPasswordInput.getText().toString().trim();

            if (name.isBlank()) {
                signupBinding.signupNameInput.setError("Name is Required");
            } else if (email.isBlank()) {
                signupBinding.signupEmailInput.setError("Email is Required");
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                signupBinding.signupEmailInput.setError("Invalid Email");
            } else if (password.isBlank()) {
                signupBinding.signupPasswordInput.setError("Password is Required");
            } else if (password.length() < 6) {
                signupBinding.signupPasswordInput.setError("Password must be at-least 6 characters");
            } else if (!confirmationPassword.equals(password)) {
                signupBinding.confirmPasswordInput.setError("Passwords don't match");
            } else {
                signupProcess(name, email, confirmationPassword);
            }
        });
    }

    private void signupProcess(String name, String email, String password) {

        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    String uid = task.getResult().getUser().getUid();
                    User user = User.builder().uId(uid).name(name).email(email).build();

                    firestoreDB.collection("users")
                            .document(uid)
                            .set(user)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    Toast.makeText(getApplicationContext(), "Your Registration was Successful", Toast.LENGTH_SHORT).show();
                                    assert firebaseAuth.getCurrentUser() != null;
                                    updateUI(firebaseAuth.getCurrentUser());
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e(TAG, e.toString());
                                }
                            });

                } else {

                }
            }
        });
    }

    private void updateUI(FirebaseUser user) {
        Intent intent = new Intent(SignupActivity.this, MainActivity.class);
        intent.putExtra("userName", user.getDisplayName());
        intent.putExtra("userEmail", user.getEmail());
        intent.putExtra("userMobile", user.getPhoneNumber());
        startActivity(intent);
    }
}