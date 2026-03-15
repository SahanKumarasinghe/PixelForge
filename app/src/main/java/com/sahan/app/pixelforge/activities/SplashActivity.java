package com.sahan.app.pixelforge.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.sahan.app.pixelforge.R;


@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private ProgressBar splashProgressBar;
    private ImageView splashLogoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();

            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            } else {
                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }

        }
        this.splashProgressBar = findViewById(R.id.splashProgressBar);
        this.splashLogoView = findViewById(R.id.splashLogoView);

        Glide.with(this)
                .asBitmap()
                .load(R.drawable.pixelforgelogodark)
                .override(200)
                .fitCenter()
                .into(splashLogoView);

        splashProgressBar.post(this::barProgress);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.splashSpinner).setVisibility(View.VISIBLE);
            }
        }, 500);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                forwardToHome();
            }
        }, 2000);


    }

    private void forwardToHome() {
        findViewById(R.id.splashSpinner).setVisibility(View.INVISIBLE);
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private synchronized void barProgress() {

        Thread t = new Thread(() -> {
            int progress = 0;
            while (progress <= 100) {
                if (progress == 33) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                if (progress == 70) {
                    progress = 100;
                }
                splashProgressBar.setProgress(progress);
                progress++;
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        t.start();
    }


}