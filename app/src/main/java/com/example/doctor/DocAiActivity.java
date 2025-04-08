package com.example.doctor;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class DocAiActivity extends AppCompatActivity {

    private EditText symptomEditText;
    private TextView resultTextView;
    private Button predictButton,backButton;
    private static final String TAG = "DocAiActivity";
    private static final String BACKEND_URL = "http://10.0.2.2:3000/analyze-symptoms"; // For emulator
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // Enhanced OkHttpClient with timeouts and logging
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doc_ai);

        symptomEditText = findViewById(R.id.symptoms_input);
        resultTextView = findViewById(R.id.textView2);
        predictButton = findViewById(R.id.predict_button);
        backButton = findViewById(R.id.btn_back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DocAiActivity.this, HomeActivity.class);
                startActivity(intent);
            }
        });
        predictButton.setOnClickListener(v -> predictDiseaseWithBackend());
    }

    private void predictDiseaseWithBackend() {
        String symptoms = symptomEditText.getText().toString().trim();
        if (symptoms.isEmpty()) {
            resultTextView.setText("Please enter symptoms");
            return;
        }

        runOnUiThread(() -> {
            resultTextView.setText("Analyzing...");
            predictButton.setEnabled(false);
        });

        try {
            JSONObject json = new JSONObject();
            json.put("symptoms", symptoms);

            RequestBody body = RequestBody.create(json.toString(), JSON);
            Request request = new Request.Builder()
                    .url(BACKEND_URL)
                    .header("Connection", "close") // Prevent connection reuse issues
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Network Failure: ", e);
                    runOnUiThread(() -> {
                        resultTextView.setText("Error: " + getReadableError(e));
                        predictButton.setEnabled(true);
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (ResponseBody responseBody = response.body()) {
                        if (!response.isSuccessful()) {
                            throw new IOException("Unexpected code " + response);
                        }

                        String responseData = responseBody.string();
                        JSONObject jsonResponse = new JSONObject(responseData);
                        String analysis = jsonResponse.getString("analysis");

                        runOnUiThread(() -> {
                            resultTextView.setText(analysis);
                            predictButton.setEnabled(true);
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Response Processing Error: ", e);
                        runOnUiThread(() -> {
                            resultTextView.setText("Error processing response");
                            predictButton.setEnabled(true);
                        });
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "JSON Creation Error: ", e);
            runOnUiThread(() -> {
                resultTextView.setText("Error creating request");
                predictButton.setEnabled(true);
            });
        }
    }

    private String getReadableError(IOException e) {
        if (e instanceof java.net.ConnectException) {
            return "Cannot connect to server. Check if backend is running.";
        } else if (e instanceof java.net.SocketTimeoutException) {
            return "Connection timed out. Check your network.";
        } else if (e instanceof java.net.UnknownHostException) {
            return "Invalid server address. Check backend URL.";
        }
        return "Network error. Please try again.";
    }
}