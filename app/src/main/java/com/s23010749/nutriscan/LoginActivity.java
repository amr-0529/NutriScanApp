package com.s23010749.nutriscan;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

// This is the code that makes the login screen actually work.
public class LoginActivity extends AppCompatActivity {

    // These are the things I can see and interact with on the screen.
    EditText editTextUsername, editTextPassword;
    Button buttonLogin;
    TextView textViewSignUpLink;
    DatabaseHelper databaseHelper; // This helps me talk to the database.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // Tells the app to show my login screen layout.

        // Connecting the code stuff to the actual stuff on the screen.
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewSignUpLink = findViewById(R.id.textViewSignUpLink);

        // Setting up the database connection.
        databaseHelper = new DatabaseHelper(this);

        // What happens when the login button is clicked.
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get whatever text the user typed in.
                String username = editTextUsername.getText().toString();
                String password = editTextPassword.getText().toString();

                // If fields are empty, tell the user.
                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Fill everything please!", Toast.LENGTH_SHORT).show();
                } else {
                    // Check if the username and password are correct in the database.
                    boolean checkUser = databaseHelper.checkUser(username, password);
                    if (checkUser) {
                        // If it's correct, go to the main home screen.
                        Toast.makeText(LoginActivity.this, "Logged in! Yay!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class); // Go to HomeActivity
                        startActivity(intent);
                        finish(); // Close the login screen.
                    } else {
                        // If not, tell them it's wrong.
                        Toast.makeText(LoginActivity.this, "Wrong username or password!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // What happens when the "Sign Up" link is clicked.
        textViewSignUpLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Just go to the sign up screen.
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });
    }
}