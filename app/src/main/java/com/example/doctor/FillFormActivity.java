package com.example.doctor;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class FillFormActivity extends AppCompatActivity {
    EditText patientNameText,doctorNameText,diagnosisText,symptomsText,prescriptionText,adviceText;
    Button submitButton;
    Button dateButton,timeButton;

    private DatePickerDialog datePickerDialog;
    private TimePickerDialog timePickerDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_fill_form);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Intent intent = getIntent();
        String username = intent.getStringExtra("username");
        String appointmentDate = intent.getStringExtra("appointmentDate");
        String appointmentTime = intent.getStringExtra("appointmentTime");
        SharedPreferences sharedPreferences = getSharedPreferences("shared_prefs", Context.MODE_PRIVATE);
        String doctorName = sharedPreferences.getString("username", "");
        patientNameText = findViewById(R.id.patientName);
        doctorNameText = findViewById(R.id.doctorName);
        diagnosisText = findViewById(R.id.diagnosis);
        symptomsText = findViewById(R.id.patientSymptoms);
        prescriptionText = findViewById(R.id.prescription);
        adviceText = findViewById(R.id.furtherInfo);
        patientNameText.setText(username);
        doctorNameText.setText(doctorName);
        submitButton = findViewById(R.id.submit_record);
        dateButton = findViewById(R.id.bookAppointmentDate);
        timeButton = findViewById(R.id.bookAppointmentTime);
        dateButton.setText(appointmentDate);
        timeButton.setText(appointmentTime);
        submitButton.setOnClickListener(view -> {
            if(diagnosisText.getText().toString().isEmpty() || symptomsText.getText().toString().isEmpty() || prescriptionText.getText().toString().isEmpty() || adviceText.getText().toString().isEmpty()){
                Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show();
            }else{
                String patientName = patientNameText.getText().toString();
                String docName = doctorNameText.getText().toString();
                String diagnosis = diagnosisText.getText().toString();
                String symptoms = symptomsText.getText().toString();
                String prescription = prescriptionText.getText().toString();
                String advice = adviceText.getText().toString();
                String apptDate = dateButton.getText().toString();
                String appTime = timeButton.getText().toString();
                Database db = new Database(this);
                db.insertRecords(patientName,docName,diagnosis,symptoms,prescription,advice,apptDate,appTime);
                Toast.makeText(this, "Record Submitted", Toast.LENGTH_SHORT).show();
            }
        });
        dateButton.setOnClickListener(view -> {
            datePickerDialog.show();
        });
        timeButton.setOnClickListener(view -> {
            timePickerDialog.show();
        });
        datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
            dateButton.setText(selectedDate);
        }, 2023, 0, 1);
        timePickerDialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            String selectedTime = hourOfDay + ":" + minute;
            timeButton.setText(selectedTime);
        }, 12, 0, true);

    }
}