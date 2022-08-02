package com.satyam.VideoCalling.ui.boarding;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.auth.FirebaseAuth;

import com.satyam.VideoCalling.R;
import com.satyam.VideoCalling.application.MyApplication;
import com.satyam.VideoCalling.managers.ActivitySwitchManager;
import com.satyam.VideoCalling.managers.SharedPrefManager;
import com.satyam.VideoCalling.ui.MainActivity;
import com.satyam.VideoCalling.ui.auth.AuthActivity;


public class SplashScreenActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private SharedPrefManager sharedPrefManager;
    private Handler handler;

    private LottieAnimationView splashLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        initConfig();

    }

    private void initConfig(){
        firebaseAuth = MyApplication.getInstance().getFirebaseAuth();
        sharedPrefManager = MyApplication.getInstance().getSharedPrefManager();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        splashLoader = findViewById(R.id.splash_loader);
        splashLoader.setProgress(0);
        splashLoader.addAnimatorListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                splashLoader.setProgress(0);
            }
        });

        handler=new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(firebaseAuth.getCurrentUser()!=null){
                    new ActivitySwitchManager(SplashScreenActivity.this, MainActivity.class).openActivity();
                }else{
                    new ActivitySwitchManager(SplashScreenActivity.this, AuthActivity.class).openActivity();
                }
            }
        },500);

    }
}
