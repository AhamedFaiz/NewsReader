package com.example.newsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class ArticleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article);

        //setting up a web view
        WebView webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);//enabling java script
        webView.setWebViewClient(new WebViewClient());// to show the web data in the same page rather than loading in a default browser

        //getting html data from main activity
        Intent intent = getIntent();
        //loading the webview with the html content got from main activity
        webView.loadData(intent.getStringExtra("content"),"text/html","UTF-8");

    }
}
