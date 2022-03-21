package com.jiuqu.tools.ad.hwad;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import com.huawei.hms.ads.AdListener;
import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.BannerAdSize;
import com.huawei.hms.ads.banner.BannerView;
import com.jiuqu.tools.ad.BannerAdBase;

import java.util.ArrayList;

public class HWBannerAd extends BannerAdBase {

    private HWBannerAdListener _listener = null;

    private BannerView _curBanner = null;

    private BannerView _popBanner = null;

    @Override
    public void InitAd(Activity context, String adId, FrameLayout bannerView) {
        super.InitAd(context, adId,bannerView);
        _listener = new HWBannerAdListener();
        bannerAdList = new ArrayList();
        OnInitFinished();
    }

    @Override
    public void LoadAd() {
        super.LoadAd();
        _context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                _curBanner = new BannerView(_context);
                _curBanner.setAdId(_adUnit);
                _curBanner.setBannerAdSize(BannerAdSize.BANNER_SIZE_360_57);
                _curBanner.setAdListener(_listener);
                _curBanner.loadAd(new AdParam.Builder().build());
            }
        });
    }

    @Override
    public void ShowAd() {
        super.ShowAd();
        if(IsReady()){
            _popBanner = (BannerView) bannerAdList.get(0);
            _context.runOnUiThread(()->{
                _bannerView.addView(_popBanner);
                _bannerView.setVisibility(View.VISIBLE);
            });
            bannerAdList.remove(0);
        }
    }

    @Override
    public void HideAd() {
        super.HideAd();
        if(_popBanner != null){
            _context.runOnUiThread(()->{
                _bannerView.removeAllViews();
                _bannerView.setVisibility(View.INVISIBLE);
            });
            _popBanner.destroy();
            OnAdClose();
        }
    }

    private class HWBannerAdListener extends AdListener{
        @Override
        public void onAdLoaded() {
            // 广告加载成功时调用
            bannerAdList.add(_curBanner);
            OnLoadSuccess();
            if(bannerAdList.size() <= 5){
                StartLoadAdTimer();
            }else{
                StopLoadAdTimer();
            }
        }
        @Override
        public void onAdFailed(int errorCode) {
            OnLoadFailed();
        }
        @Override
        public void onAdOpened() {
            // 广告打开时调用
        }
        @Override
        public void onAdClicked() {
            // 广告点击时调用
        }
        @Override
        public void onAdLeave() {
            // 广告离开应用时调用
        }
        @Override
        public void onAdClosed() {

        }
    }
}
