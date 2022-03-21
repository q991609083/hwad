package com.jiuqu.tools.ad.hwad;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import android.widget.ImageView;
import android.widget.TextView;

import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.AudioFocusType;
import com.huawei.hms.ads.splash.SplashView;
import com.jiuqu.tools.ad.AdUtils;

public class SplashActivity extends Activity {
    // "testq6zq98hecj"为测试专用的广告位ID, App正式发布时需要改为正式的广告位ID
    public static String AD_ID = "e75o5n970l";
    private static final int AD_TIMEOUT = 10000;
    private static final int MSG_AD_TIMEOUT = 1001;
    public static String mSplashAppTitle = "MR Golden Bean";
    public static String mSplashAppDesc = "Game";
    /**
     * 暂停标志位。
     * 在开屏广告页面展示时：
     * 按返回键退出应用时需设置为true，以确保应用主界面不被拉起；
     * 切换至其他界面时需设置为false，以确保从其他页面回到开屏广告页面时仍然可以正常跳转至应用主界面；
     */
    private boolean hasPaused = false;

    // 收到广告展示超时消息时的回调处理
    private Handler timeoutHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (SplashActivity.this.hasWindowFocus()) {
                jump();
            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        TextView title = findViewById(R.id.splash_ad_title);
        TextView desc = findViewById(R.id.splash_ad_desc);
        ImageView image = findViewById(R.id.app_icon);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                title.setText(mSplashAppTitle);
                desc.setText(mSplashAppDesc);
                image.setBackground(getResources().getDrawable(AdUtils.getInstance().GetDrawableByName("app_icon")));
            }
        });
        // 加载并展示开屏广告
        loadAd();
    }


    private void loadAd() {
        int orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        AdParam adParam = new AdParam.Builder().build();
        SplashView.SplashAdLoadListener splashAdLoadListener = new SplashView.SplashAdLoadListener() {
            @Override
            public void onAdLoaded() {
                // 广告加载成功时调用
            }
            @Override
            public void onAdFailedToLoad(int errorCode) {
                // 广告加载失败时调用, 跳转至App主界面
                jump();
            }
            @Override
            public void onAdDismissed() {

            }
        };
        // 获取SplashView
        SplashView splashView = findViewById(R.id.splash_ad_view);
        // 设置视频类开屏广告的音频焦点类型
        splashView.setAudioFocusType(AudioFocusType.NOT_GAIN_AUDIO_FOCUS_WHEN_MUTE);
        // 加载广告，其中AD_ID为广告位ID
        splashView.load(AD_ID, orientation, adParam, splashAdLoadListener);
        // 发送延时消息，保证广告显示超时后，APP首页可以正常显示
        timeoutHandler.removeMessages(MSG_AD_TIMEOUT);
        timeoutHandler.sendEmptyMessageDelayed(MSG_AD_TIMEOUT, AD_TIMEOUT);
    }
    /**
     * 广告展示完毕时，从广告界面跳转至App主界面
     */
    private void jump() {
        if (!hasPaused) {
            hasPaused = true;
//            startActivity(new Intent(SplashActivity.this, MyActivity.class));
            finish();
        }
    }
    /**
     * 按返回键退出应用时需设置为true，以确保应用主界面不被拉起
     */
    @Override
    protected void onStop() {
        // 移除消息队列中等待的超时消息
        timeoutHandler.removeMessages(MSG_AD_TIMEOUT);
        hasPaused = true;
        super.onStop();
    }
    /**
     * 切换至其他界面时需设置为false，以确保从其他页面回到开屏广告页面时仍然可以正常跳转至应用主界面
     */
    @Override
    protected void onRestart() {
        super.onRestart();
        hasPaused = false;
        jump();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}