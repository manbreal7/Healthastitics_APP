package com.example.doctor;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class ViewAppointment extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view_appointment);

        // Get the LinearLayout where appointments will be displayed
        LinearLayout linearLayout = findViewById(R.id.dynamicAppointments);

        // Fetch doctor name from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("shared_prefs", Context.MODE_PRIVATE);
        String doctorName = sharedPreferences.getString("username", "");

        // Fetch booking details using getBookingData method
        Database db = new Database(this);
        String[][] bookings = db.getBookingData(doctorName);

        if (bookings.length > 0) {
            for (String[] booking : bookings) {
                if(booking[4].equals("Pending")) {
                    String username = booking[0];
                    String appointmentDate = booking[1];
                    String appointmentTime = booking[2];
                    String fees = booking[3];
                    String status = booking[4];
                    String userEmail = db.getUserEmail(username);
                    // Display appointment details
                    TextView textView = new TextView(this);
                    textView.setText("Patient: " + username + "\nDate: " + appointmentDate +
                            "\nTime: " + appointmentTime + "\nFees: $" + fees + "\nStatus: " + status);
                    textView.setPadding(20, 20, 20, 20);
                    textView.setTextColor(0xFFFFFFFF);
                    // Accept button
                    Button acceptButton = new Button(this);
                    acceptButton.setText("Accept");
                    acceptButton.setOnClickListener(v -> {
                        db.updateAppointmentStatus(username, appointmentDate, appointmentTime, doctorName, "Accepted");
//                    MailSender.sendEmail(userEmail, "Appointment Accepted",
//                            "Your appointment on " + appointmentDate + " at " + appointmentTime + " has been accepted.");
                        Toast.makeText(this, "Appointment Accepted", Toast.LENGTH_SHORT).show();
                        recreate();
                    });

                    // Reject button
                    Button rejectButton = new Button(this);
                    rejectButton.setText("Reject");
                    rejectButton.setOnClickListener(v -> {
                        db.updateAppointmentStatus(username, appointmentDate, appointmentTime, doctorName, "Rejected");
//                    MailSender.sendEmail(userEmail, "Appointment Rejected",
//                            "Your appointment on " + appointmentDate + " at " + appointmentTime + " has been rejected.");
                        Toast.makeText(this, "Appointment Rejected", Toast.LENGTH_SHORT).show();
                        recreate();
                    });

                    // Add views dynamically
                    LinearLayout appointmentLayout = new LinearLayout(this);
                    appointmentLayout.setOrientation(LinearLayout.VERTICAL);
                    appointmentLayout.setPadding(20, 20, 20, 20);
                    appointmentLayout.addView(textView);
                    appointmentLayout.addView(acceptButton);
                    appointmentLayout.addView(rejectButton);

                    linearLayout.addView(appointmentLayout);
                }
            }
        } else {
            TextView noAppointments = new TextView(this);
            noAppointments.setText("No Appointments Found.");
            noAppointments.setPadding(20, 20, 20, 20);
            linearLayout.addView(noAppointments);
        }
    }
}