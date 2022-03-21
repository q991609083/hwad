package com.jiuqu.tools.ad.hwad;


import android.app.Activity;
import android.content.Intent;
import com.jiuqu.tools.ad.SplashAdBase;



public class HWSplashAd extends SplashAdBase {
    @Override
    public void InitAd(Activity context, String adId) {
        super.InitAd(context,adId);
        SplashActivity.AD_ID = adId;
        SplashActivity.mSplashAppTitle = _context.getResources().getString(_context.getApplicationInfo().labelRes);
        SplashActivity.mSplashAppDesc = "Game";
        Intent intent = new Intent(context, SplashActivity.class);
        context.startActivity(intent);
    }
}
