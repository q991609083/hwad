package com.jiuqu.tools.ad.hwad;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.Nullable;

import com.huawei.hms.ads.AdListener;
import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.InterstitialAd;
import com.jiuqu.tools.ad.AdEventListener;
import com.jiuqu.tools.ad.AdUtils;
import com.jiuqu.tools.ad.InterAdBase;

import java.util.ArrayList;

public class HWInterAd extends InterAdBase {
    private HWInterAdListener _listener = null;

    @Override
    public void InitAd(Activity context, String adId, @Nullable AdEventListener listener) {
        super.InitAd(context, adId,listener);
        interAdList = new ArrayList();
        OnInitFinished();
    }

    @Override
    public void LoadAd() {
        super.LoadAd();
        InterstitialAd interAd = new InterstitialAd(_context);
        interAd.setAdId(_adUnit);
        _listener = new HWInterAdListener(interAd);
        interAd.setAdListener(_listener);
        interAd.loadAd(new AdParam.Builder().build());
    }

    @Override
    public void ShowAd() {
        super.ShowAd();
        if(IsReady()){
            _context.runOnUiThread(() ->{
                InterstitialAd inter = (InterstitialAd) interAdList.get(0);
                inter.show(_context);
                interAdList.remove(0);
            });
        }
    }

    private class HWInterAdListener extends AdListener{
        private InterstitialAd _ad = null;
        public HWInterAdListener(InterstitialAd ad){
            _ad = ad;
        }
        @Override
        public void onAdLoaded() {
            // 广告加载成功时调用
            OnInterLoaded();

            isReady = true;
            OnLoadSuccess();
            interAdList.add(_ad);
            Log.d(AdUtils.INTER_AD_TAG,"广告缓存数量 :" + interAdList.size());
            if(interAdList.size() < 2){
                StartLoadAdTimer();
            }else{
                StopLoadAdTimer();
            }
        }
        @Override
        public void onAdFailed(int errorCode) {
            // 广告加载失败时调用
            Log.d(AdUtils.INTER_AD_TAG,"加载插屏视频失败" + errorCode);
            isReady = false;
            OnLoadFailed();
        }
        @Override
        public void onAdClosed() {
            // 广告关闭时调用
            CloseInterEvent();
            isReady = false;
            OnAdClose();
        }
        @Override
        public void onAdClicked() {
            // 广告点击时调用
        }
        @Override
        public void onAdLeave() {
            // 广告离开时调用
        }
        @Override
        public void onAdOpened() {
            // 广告打开时调用
        }
        @Override
        public void onAdImpression() {
            // 广告曝光时调用
            ShowInterEvent();
        }
    }
}
