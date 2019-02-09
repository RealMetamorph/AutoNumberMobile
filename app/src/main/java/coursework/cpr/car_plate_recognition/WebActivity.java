package coursework.cpr.car_plate_recognition;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class WebActivity extends AppCompatActivity {
    private File saved;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        File dirSavedHTML = new File(Environment.getExternalStorageDirectory(), "CarPlateRecognition/saved numbers");
        dirSavedHTML.mkdirs();
        WebView myWebView = (WebView) findViewById(R.id.webview);
        myWebView.setWebViewClient(new WebViewClient());
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        Intent intent = getIntent();
        saved = new File(dirSavedHTML, intent.getStringExtra("number") + ".html");
        if (saved.exists()) {
            myWebView.loadUrl("file://" + saved.getAbsolutePath());
        } else {
            myWebView.addJavascriptInterface(new JSHTMLCatcher(), "catch");
            myWebView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    if (url.contains("proverkaavto")) {
                        view.loadUrl("javascript: window.catch.saveInfo('<html lang=\"ru\">' + window.document.getElementsByTagName('html').item(0).innerHTML + '</html>');");
                    }
                }
            });
            myWebView.loadUrl(intent.getStringExtra("href"));
        }
    }

    private class JSHTMLCatcher {

        @JavascriptInterface
        void saveInfo(String document) {
            document = "<!DOCTYPE html>\n" + document;
            System.out.println("Doc: " + document);
            try {
                OutputStream outputStream = new FileOutputStream(saved);
                outputStream.write(document.getBytes(StandardCharsets.UTF_8));
                outputStream.close();
            } catch (IOException ignored) {
            }
        }

    }
}



