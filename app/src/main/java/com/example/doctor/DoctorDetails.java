package com.example.doctor;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class DoctorDetails extends AppCompatActivity {

    private EditText edUsername, edSpecialization, edHospital;
    private Button btnSubmit;
    private Database db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_details2);

        // Initialize database
        db = new Database(this);

        // Initialize UI components
        edUsername = findViewById(R.id.doctorUsername);
        edSpecialization = findViewById(R.id.doctorSpecialization);
        edHospital = findViewById(R.id.doctorHospital);
        btnSubmit = findViewById(R.id.submitDoctorDetails);

        // Handle button click to add doctor details
        btnSubmit.setOnClickListener(view -> addDoctorDetails());
    }

    private void addDoctorDetails() {
        String username = edUsername.getText().toString().trim();
        String specialization = edSpecialization.getText().toString().trim();
        String hospital = edHospital.getText().toString().trim();

        if (username.isEmpty() || specialization.isEmpty() || hospital.isEmpty()) {
            showToast("All fields are required");
            return;
        }

        // Fetch email using the provided username
        String email = getEmailByUsername(username);
        if (email == null) {
            showToast("No email found for this username");
            return;
        }

        // Insert doctor details into the database
        boolean isInserted = insertDoctorDetails(username, specialization, email, hospital);
        if (isInserted) {
            showToast("Doctor details added successfully");
        } else {
            showToast("Failed to add doctor details");
        }
    }

    // Method to fetch email from users table using the username
    private String getEmailByUsername(String username) {
        SQLiteDatabase database = db.getReadableDatabase();
        String email = null;

        Cursor cursor = database.rawQuery("SELECT email FROM users WHERE username=?", new String[]{username});
        if (cursor.moveToFirst()) {
            email = cursor.getString(0);
        }

        cursor.close();
        database.close();
        return email;
    }

    // Method to insert doctor details into the doctors table
    private boolean insertDoctorDetails(String username, String specialization, String email, String hospital) {
        SQLiteDatabase database = db.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("doctor_name", username);
        values.put("specialization", specialization);
        values.put("email", email);
        values.put("hospital", hospital);

        long result = database.insert("doctors", null, values);
        database.close();

        return result != -1; // Return true if insert was successful
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}
