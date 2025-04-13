package com.example.doctor;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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

    private static final int REQUEST_STORAGE_PERMISSION = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_generate_pdf);

        database = new Database(this);
        backButton = findViewById(R.id.btn_Back);

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(GeneratePdfActivity.this, HomeActivity.class);
            startActivity(intent);
        });

        SharedPreferences sharedPreferences = getSharedPreferences("shared_prefs", Context.MODE_PRIVATE);
        username = sharedPreferences.getString("username", "");

        LinearLayout recordsContainer = findViewById(R.id.recordsContainer);
        loadMedicalRecords(recordsContainer);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_STORAGE_PERMISSION);
            }
        }
    }

    private void loadMedicalRecords(LinearLayout container) {
        SQLiteDatabase db = database.getReadableDatabase();

        Cursor cursor = db.query(
                "medical_records",
                null,
                "username = ?",
                new String[]{username},
                null, null,
                "appointment_date DESC, appointment_time DESC"
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
            int recordId = cursor.getInt(cursor.getColumnIndexOrThrow("record_id"));
            String doctorName = cursor.getString(cursor.getColumnIndexOrThrow("doctor_name"));
            String symptoms = cursor.getString(cursor.getColumnIndexOrThrow("symptoms"));
            String diagnosis = cursor.getString(cursor.getColumnIndexOrThrow("diagnosis"));
            String prescription = cursor.getString(cursor.getColumnIndexOrThrow("prescription"));
            String notes = cursor.getString(cursor.getColumnIndexOrThrow("notes"));
            String appointmentDate = cursor.getString(cursor.getColumnIndexOrThrow("appointment_date"));
            String appointmentTime = cursor.getString(cursor.getColumnIndexOrThrow("appointment_time"));

            LinearLayout recordLayout = new LinearLayout(this);
            recordLayout.setOrientation(LinearLayout.VERTICAL);
            recordLayout.setPadding(20, 20, 20, 20);
            recordLayout.setBackgroundResource(R.drawable.card_background);

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(0, 0, 0, 20);
            recordLayout.setLayoutParams(layoutParams);

            recordLayout.addView(createTextView("Doctor: " + doctorName, 16, true));
            recordLayout.addView(createTextView("Date: " + appointmentDate + " at " + appointmentTime, 14, false));
            recordLayout.addView(createTextView("Symptoms: " + symptoms, 14, false));
            recordLayout.addView(createTextView("Diagnosis: " + diagnosis, 14, false));
            recordLayout.addView(createTextView("Prescription: " + prescription, 14, false));
            recordLayout.addView(createTextView("Notes: " + notes, 14, false));

            Button downloadBtn = new Button(this);
            downloadBtn.setText("Download PDF");
            downloadBtn.setOnClickListener(v -> generatePdf(
                    recordId, doctorName, symptoms, diagnosis,
                    prescription, notes, appointmentDate, appointmentTime));
            recordLayout.addView(downloadBtn);

            container.addView(recordLayout);
        }

        cursor.close();
    }

    private TextView createTextView(String text, float textSize, boolean isBold) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(textSize);
        textView.setPadding(0, 8, 0, 8);
        if (isBold) textView.setTypeface(null, Typeface.BOLD);
        return textView;
    }

    private void generatePdf(int recordId, String doctorName, String symptoms,
                             String diagnosis, String prescription, String notes,
                             String appointmentDate, String appointmentTime) {
        try {
            PdfDocument document = new PdfDocument();
            int pageWidth = 595;

            View content = createMedicalReportView(doctorName, symptoms, diagnosis,
                    prescription, notes, appointmentDate, appointmentTime);

            content.measure(View.MeasureSpec.makeMeasureSpec(pageWidth, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            int contentHeight = content.getMeasuredHeight();

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, Math.max(842, contentHeight), 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            content.layout(0, 0, content.getMeasuredWidth(), contentHeight);
            content.draw(canvas);
            document.finishPage(page);

            String fileName = "Medical_Report_" + username + "_" + recordId + ".pdf";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                values.put(MediaStore.Downloads.IS_PENDING, 1);

                ContentResolver resolver = getContentResolver();
                Uri collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                Uri fileUri = resolver.insert(collection, values);

                if (fileUri != null) {
                    OutputStream out = resolver.openOutputStream(fileUri);
                    document.writeTo(out);
                    out.close();

                    values.clear();
                    values.put(MediaStore.Downloads.IS_PENDING, 0);
                    resolver.update(fileUri, values, null, null);

                    Toast.makeText(this, "PDF saved to Downloads", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Failed to create file", Toast.LENGTH_SHORT).show();
                }

            } else {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File file = new File(downloadsDir, fileName);
                FileOutputStream fos = new FileOutputStream(file);
                document.writeTo(fos);
                fos.close();
                Toast.makeText(this, "PDF saved to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            }

            document.close();

        } catch (IOException e) {
            Toast.makeText(this, "PDF error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private View createMedicalReportView(String doctorName, String symptoms, String diagnosis,
                                         String prescription, String notes, String appointmentDate,
                                         String appointmentTime) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        layout.setBackgroundColor(Color.WHITE);

        ImageView logoView = new ImageView(this);
        logoView.setImageResource(R.drawable.logo);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(100, 100);
        logoParams.gravity = Gravity.CENTER_HORIZONTAL;
        logoParams.setMargins(0, 0, 0, 20);
        logoView.setLayoutParams(logoParams);
        layout.addView(logoView);

        String[] docDetails = database.getDoctorDetails(doctorName);
        if (docDetails.length > 0) {
            HospitalAddr = docDetails[1];
            Exp = docDetails[2];
            Specialization = docDetails[5];
            Email = docDetails[6];
        }

        TextView header = new TextView(this);
        header.setText("MEDICAL REPORT");
        header.setTextSize(20);
        header.setTypeface(null, Typeface.BOLD);
        header.setGravity(Gravity.CENTER);
        header.setTextColor(Color.parseColor("#2E86C1"));
        header.setPadding(0, 10, 0, 30);
        layout.addView(header);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.parseColor("#F2F3F4"));
        card.setPadding(30, 30, 30, 30);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 20);
        card.setLayoutParams(cardParams);

        card.addView(createSection("Patient", username));
        card.addView(createSection("Date", formatDate(appointmentDate) + " | Time: " + formatTime(appointmentTime)));

        if (!TextUtils.isEmpty(HospitalAddr)) {
            card.addView(createSection("Hospital Info", HospitalAddr + "\n" + Exp + " Years\n" + Specialization + "\n" + Email));
        }

        if (!TextUtils.isEmpty(doctorName)) {
            card.addView(createSection("Attending Doctor", doctorName));
        }

        addReportSection(card, "Symptoms", symptoms);
        addReportSection(card, "Diagnosis", diagnosis);
        addReportSection(card, "Prescription", prescription);
        addReportSection(card, "Notes", notes);

        layout.addView(card);

        return layout;
    }

    private TextView createSection(String title, String content) {
        TextView section = new TextView(this);
        section.setText(title + ": " + content);
        section.setTextSize(7);
        section.setPadding(0, 0, 0, 20);
        return section;
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
            return dateStr;
        }
    }

    private String formatTime(String timeStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date time = inputFormat.parse(timeStr);
            return outputFormat.format(time);
        } catch (Exception e) {
            return timeStr;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (database != null) {
            database.close();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission denied. Cannot save PDF to external storage.", Toast.LENGTH_LONG).show();
            }
        }
    }
}