package com.example.doctor;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ArticleActivity extends AppCompatActivity {
    private TextView articleContentTextView, articleTitleTextView, articleSubtitleTextView, articleBulletPointsTextView, additionalContentTextView;
    private ImageView articleImageView;
    private Button readMoreBtn, shareBtn;
    private static final String TAG = "ArticleActivity";
    private static final String BASE_URL = "http://10.0.2.2:3000/articles"; // update IP if needed
    private String articleUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article);

        // Initialize UI components
        articleContentTextView = findViewById(R.id.article_content);
        articleTitleTextView = findViewById(R.id.article_title);
        articleSubtitleTextView = findViewById(R.id.article_subtitle);
        articleBulletPointsTextView = findViewById(R.id.article_bullet_points);
        additionalContentTextView = findViewById(R.id.additional_content);
        articleImageView = findViewById(R.id.article_image);
        readMoreBtn = findViewById(R.id.btn_read_more);
        shareBtn = findViewById(R.id.btn_share);

        // Back Button functionality
        Button backButton = findViewById(R.id.btn_back);
        backButton.setOnClickListener(v -> finish());

        // Fetch articles from API
        fetchArticles();

        // Read More button functionality
        readMoreBtn.setOnClickListener(v -> {
            if (articleUrl != null && !articleUrl.isEmpty()) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(articleUrl));
                startActivity(browserIntent);
            }
        });

        // Share button functionality
        shareBtn.setOnClickListener(v -> {
            if (articleUrl != null && !articleUrl.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_SUBJECT, "Check out this article");
                intent.putExtra(Intent.EXTRA_TEXT, articleUrl);
                startActivity(Intent.createChooser(intent, "Share via"));
            }
        });
    }

    private void fetchArticles() {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(BASE_URL) // Or add ?topic=diabetes etc.
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "❌ API call failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "❌ Unexpected code " + response);
                    return;
                }

                String json = response.body().string();
                try {
                    JSONObject jsonObject = new JSONObject(json);
                    JSONArray articlesArray = jsonObject.getJSONArray("articles");
                    if (articlesArray.length() > 0) {
                        JSONObject article = articlesArray.getJSONObject(0); // Assuming we want the first article

                        // Parsing article details
                        String title = article.getString("title");
                        String subtitle = article.getString("subtitle");
                        String content = article.getString("content");
                        String bulletPoints = article.getString("bulletPoints");
                        String additionalContent = article.getString("additionalContent");
                        String imageUrl = article.getString("imageUrl");
                        articleUrl = article.getString("url");

                        // Update UI with the article details
                        runOnUiThread(() -> {
                            articleTitleTextView.setText(title);
                            articleSubtitleTextView.setText(subtitle);
                            articleContentTextView.setText(content);
                            articleBulletPointsTextView.setText(bulletPoints);
                            additionalContentTextView.setText(additionalContent);

                            // Load image using Glide
                            Glide.with(ArticleActivity.this)
                                    .load(imageUrl)
                                    .into(articleImageView);
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "❌ JSON Parsing Error: " + e.getMessage());
                }
            }
        });
    }
}
