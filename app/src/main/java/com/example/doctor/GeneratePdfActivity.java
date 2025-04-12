package com.example.doctor;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GeneratePdfActivity extends AppCompatActivity {

    private Database database;
    private String username;
    Button backButton;

    String HospitalAddr;
    String Exp;
    String Email;
    String Specialization;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_generate_pdf);

        // Initialize database
        database = new Database(this);
        backButton = findViewById(R.id.btn_Back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GeneratePdfActivity.this, HomeActivity.class);
                startActivity(intent);
            }
        });
        // Get username from shared preferences
        SharedPreferences sharedPreferences = getSharedPreferences("shared_prefs", Context.MODE_PRIVATE);
        username = sharedPreferences.getString("username", "");

        // Get the LinearLayout where records will be displayed
        LinearLayout recordsContainer = findViewById(R.id.recordsContainer);

        // Load and display medical records
        loadMedicalRecords(recordsContainer);
    }

    private void loadMedicalRecords(LinearLayout container) {
        // Get readable database
        SQLiteDatabase db = database.getReadableDatabase();

        // Query the database for all records of this user
        Cursor cursor = db.query(
                "medical_records",
                null,
                "username = ?",
                new String[]{username},
                null, null,
                "appointment_date DESC, appointment_time DESC" // Sort by date and time
        );

        if (cursor.getCount() == 0) {
            TextView noRecords = new TextView(this);
            noRecords.setText("No medical records found");
            noRecords.setTextSize(16);
            noRecords.setPadding(16, 16, 16, 16);
            container.addView(noRecords);
            cursor.close();
            return;
        }

        while (cursor.moveToNext()) {
            // Extract record data
            int recordId = cursor.getInt(cursor.getColumnIndexOrThrow("record_id"));
            String doctorName = cursor.getString(cursor.getColumnIndexOrThrow("doctor_name"));
            String symptoms = cursor.getString(cursor.getColumnIndexOrThrow("symptoms"));
            String diagnosis = cursor.getString(cursor.getColumnIndexOrThrow("diagnosis"));
            String prescription = cursor.getString(cursor.getColumnIndexOrThrow("prescription"));
            String notes = cursor.getString(cursor.getColumnIndexOrThrow("notes"));
            String appointmentDate = cursor.getString(cursor.getColumnIndexOrThrow("appointment_date"));
            String appointmentTime = cursor.getString(cursor.getColumnIndexOrThrow("appointment_time"));

            // Create a container for each record
            LinearLayout recordLayout = new LinearLayout(this);
            recordLayout.setOrientation(LinearLayout.VERTICAL);
            recordLayout.setPadding(20, 20, 20, 20);
            recordLayout.setBackgroundResource(R.drawable.card_background);

            // Set layout margins to create space between records
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(0, 0, 0, 20);
            recordLayout.setLayoutParams(layoutParams);

            // Create TextViews for each field
            TextView tvDoctor = createTextView("Doctor: " + doctorName, 16, true);
            TextView tvDate = createTextView("Date: " + appointmentDate + " at " + appointmentTime, 14, false);
            TextView tvSymptoms = createTextView("Symptoms: " + symptoms, 14, false);
            TextView tvDiagnosis = createTextView("Diagnosis: " + diagnosis, 14, false);
            TextView tvPrescription = createTextView("Prescription: " + prescription, 14, false);
            TextView tvNotes = createTextView("Notes: " + notes, 14, false);

            // Add TextViews to the record layout
            recordLayout.addView(tvDoctor);
            recordLayout.addView(tvDate);
            recordLayout.addView(tvSymptoms);
            recordLayout.addView(tvDiagnosis);
            recordLayout.addView(tvPrescription);
            recordLayout.addView(tvNotes);

            // Create download button
            Button downloadBtn = new Button(this);



            downloadBtn.setGravity(Gravity.CENTER);
            downloadBtn.setText("Download PDF");
            downloadBtn.setOnClickListener(v -> generatePdf(
                    recordId, doctorName, symptoms, diagnosis,
                    prescription, notes, appointmentDate, appointmentTime
            ));

            // Add button to the record layout
            recordLayout.addView(downloadBtn);

            // Add the record layout to the main container
            container.addView(recordLayout);
        }
        cursor.close();
    }

    private TextView createTextView(String text, float textSize, boolean isBold) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(textSize);
        textView.setPadding(0, 8, 0, 8);
        if (isBold) {
            textView.setTypeface(null, android.graphics.Typeface.BOLD);
        }
        return textView;
    }

    private void generatePdf(int recordId, String doctorName, String symptoms,
                             String diagnosis, String prescription, String notes,
                             String appointmentDate, String appointmentTime) {
        try {
            PdfDocument document = new PdfDocument();
            // A4 size in points (595 x 842)
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);

            // Create content view with only the passed parameters
            View content = createMedicalReportView(doctorName, symptoms, diagnosis,
                    prescription, notes, appointmentDate,
                    appointmentTime);
            content.measure(
                    View.MeasureSpec.makeMeasureSpec(pageInfo.getPageWidth(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(pageInfo.getPageHeight(), View.MeasureSpec.AT_MOST));
            content.layout(0, 0, pageInfo.getPageWidth(), pageInfo.getPageHeight());
            content.draw(page.getCanvas());

            document.finishPage(page);

            // Generate filename
            String fileName = "Medical_Report_" + username + "_" + recordId + ".pdf";
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloadsDir, fileName);

            // Save the document
            document.writeTo(new FileOutputStream(file));
            document.close();

            Toast.makeText(this, "Report saved to Downloads", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private View createMedicalReportView(String doctorName, String symptoms, String diagnosis,
                                         String prescription, String notes, String appointmentDate,
                                         String appointmentTime) {
        Database database = new Database(this);
        // Main container
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50); // Padding in points

        String[] docDetails = database.getDoctorDetails(doctorName);
        if (docDetails.length > 0) {
            HospitalAddr = docDetails[1];
            Exp = docDetails[2];
            Specialization = docDetails[5];
            Email = docDetails[6];
        }


        // Report Header
        TextView header = new TextView(this);
        header.setText("MEDICAL REPORT");
        header.setTextSize(18);
        header.setTypeface(null, Typeface.BOLD);
        header.setGravity(Gravity.CENTER);
        header.setPadding(0, 0, 0, 30);
        layout.addView(header);

        // Patient Information
        TextView patientInfo = new TextView(this);
        patientInfo.setText("Patient: " + username + "\nDate: " + formatDate(appointmentDate) +
                " | Time: " + formatTime(appointmentTime));
        patientInfo.setTextSize(7);
        patientInfo.setPadding(0, 0, 0, 20);
        layout.addView(patientInfo);

        // Hospital Information
        if (!TextUtils.isEmpty(HospitalAddr)) {
            TextView hospitalInfo = new TextView(this);
            hospitalInfo.setText("" + HospitalAddr + "\n" + Exp + "Years" + "\n" + Specialization + "\n" + Email);
            hospitalInfo.setTextSize(7);
            hospitalInfo.setPadding(0, 0, 0, 20);
            layout.addView(hospitalInfo);
            //Hospital Details
        }

        // Doctor Information
        if (!TextUtils.isEmpty(doctorName)) {
            TextView doctorInfo = new TextView(this);
            doctorInfo.setText("Attending Doctor: " + doctorName);
            doctorInfo.setTextSize(7);
            doctorInfo.setPadding(0, 0, 0, 30);
            layout.addView(doctorInfo);
        }

        // Medical Sections - only add if content exists
        addReportSection(layout, "Symptoms", symptoms);
        addReportSection(layout, "Diagnosis", diagnosis);
        addReportSection(layout, "Prescription", prescription);
        addReportSection(layout, "Notes", notes);

        return layout;
    }

    private void addReportSection(LinearLayout parent, String title, String content) {
        if (TextUtils.isEmpty(content)) return;

        TextView titleView = new TextView(this);
        titleView.setText(title + ":");
        titleView.setTextSize(7);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setPadding(0, 15, 0, 5);
        parent.addView(titleView);

        TextView contentView = new TextView(this);
        contentView.setText(content);
        contentView.setTextSize(7);
        contentView.setPadding(20, 0, 0, 20);
        parent.addView(contentView);
    }

    private String formatDate(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            Date date = inputFormat.parse(dateStr);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateStr; // Return original if parsing fails
        }
    }

    private String formatTime(String timeStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date time = inputFormat.parse(timeStr);
            return outputFormat.format(time);
        } catch (Exception e) {
            return timeStr; // Return original if parsing fails
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (database != null) {
            database.close();
        }
    }
}