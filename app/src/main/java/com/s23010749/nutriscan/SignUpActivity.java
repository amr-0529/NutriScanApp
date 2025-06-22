package com.s23010749.nutriscan;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

// This code makes the sign-up screen handle new accounts.
public class SignUpActivity extends AppCompatActivity {

    // Screen elements like text boxes and buttons.
    EditText editTextSignUpUsername, editTextSignUpPassword, editTextConfirmPassword;
    Button buttonRegister;
    TextView textViewLoginInsteadLink;
    DatabaseHelper databaseHelper; // For talking to the database.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up); // Use the sign-up layout.

        // Linking variables to the actual screen parts.
        editTextSignUpUsername = findViewById(R.id.editTextSignUpUsername);
        editTextSignUpPassword = findViewById(R.id.editTextSignUpPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewLoginInsteadLink = findViewById(R.id.textViewLoginInsteadLink);

        // Get the database ready.
        databaseHelper = new DatabaseHelper(this);

        // What happens when the Register button is tapped.
        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get all the typed text.
                String username = editTextSignUpUsername.getText().toString();
                String password = editTextSignUpPassword.getText().toString();
                String confirmPassword = editTextConfirmPassword.getText().toString();

                // Check if any fields are empty.
                if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(SignUpActivity.this, "Fill everything, please!", Toast.LENGTH_SHORT).show();
                }
                // Check if passwords match.
                else if (!password.equals(confirmPassword)) {
                    Toast.makeText(SignUpActivity.this, "Passwords don't match!", Toast.LENGTH_SHORT).show();
                } else {
                    // Check if this username is already used.
                    boolean checkUserExist = databaseHelper.checkUsername(username);
                    if (checkUserExist) {
                        Toast.makeText(SignUpActivity.this, "Username already taken!", Toast.LENGTH_SHORT).show();
                    } else {
                        // If everything is good, add the new user to the database.
                        boolean insertUser = databaseHelper.addUser(username, password);
                        if (insertUser) {
                            Toast.makeText(SignUpActivity.this, "Account created! You can now log in.", Toast.LENGTH_SHORT).show();
                            // After signing up, go to the Home screen.
                            Intent intent = new Intent(SignUpActivity.this, HomeActivity.class);
                            startActivity(intent);
                            finish(); // Close this screen.
                        } else {
                            // If something went wrong.
                            Toast.makeText(SignUpActivity.this, "Registration failed!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });

        // What happens when the "Log In Instead" link is tapped.
        textViewLoginInsteadLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Go back to the login screen.
                Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                startActivity(intent);
                finish(); // Close this screen.
            }
        });
    }
}