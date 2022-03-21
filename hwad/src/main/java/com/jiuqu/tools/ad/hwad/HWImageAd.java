package com.jiuqu.tools.ad.hwad;

import android.app.Activity;
import android.util.Log;

import com.huawei.hms.ads.AdListener;
import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.InterstitialAd;
import com.jiuqu.tools.ad.AdUtils;
import com.jiuqu.tools.ad.ImageAdBase;
import com.jiuqu.tools.ad.InterAdBase;

public class HWImageAd extends ImageAdBase {

    private HWImageAdListener _listener = null;

    @Override
    public void InitAd(Activity context, String adId) {
        super.InitAd(context, adId);
        OnInitFinished();
    }

    @Override
    public void LoadAd() {
        super.LoadAd();
        InterstitialAd imageAd = new InterstitialAd(_context);
        imageAd.setAdId(_adUnit);
        _listener = new HWImageAdListener(imageAd);
        imageAd.setAdListener(_listener);
        imageAd.loadAd(new AdParam.Builder().build());
    }

    @Override
    public void ShowAd() {
        super.ShowAd();
        if(IsReady()){
            _context.runOnUiThread(() -> {
                InterstitialAd imageAd = (InterstitialAd) imageAdList.get(0);
                imageAd.show(_context);
                imageAdList.remove(0);
            });
        }
    }

    private class HWImageAdListener extends AdListener{
        private InterstitialAd _ad;
        public HWImageAdListener(InterstitialAd ad){
            _ad = ad;
        }

        @Override
        public void onAdLoaded() {
            // 广告加载成功时调用
            isReady = true;
            OnLoadSuccess();
            imageAdList.add(_ad);
            Log.d(AdUtils.IMAGE_AD_TAG,"广告缓存数量 :" + imageAdList.size());
            if(imageAdList.size() < 2){
                StartLoadAdTimer();
            }else{
                StopLoadAdTimer();
            }
        }
        @Override
        public void onAdFailed(int errorCode) {
            // 广告加载失败时调用
            isReady = false;
            OnLoadFailed();
        }
        @Override
        public void onAdClosed() {
            // 广告关闭时调用
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
        }
    }
}
