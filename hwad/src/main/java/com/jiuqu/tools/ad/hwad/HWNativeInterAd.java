package com.jiuqu.tools.ad.hwad;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.graphics.Color;
import com.huawei.hms.ads.AdListener;
import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.AppDownloadButton;
import com.huawei.hms.ads.AppDownloadButtonStyle;
import com.huawei.hms.ads.VideoConfiguration;
import com.huawei.hms.ads.nativead.MediaView;
import com.huawei.hms.ads.nativead.NativeAd;
import com.huawei.hms.ads.nativead.NativeAdConfiguration;
import com.huawei.hms.ads.nativead.NativeAdLoader;
import com.huawei.hms.ads.nativead.NativeView;
import com.jiuqu.tools.ad.AdUtils;
import com.jiuqu.tools.ad.NativeInterAdBase;

import java.util.ArrayList;

public class HWNativeInterAd extends NativeInterAdBase {

    private HWNativeInterAdDataLoadListener _dataListener = null;
    private HWNativeInterAdLoadListener _loadListener = null;
    private NativeAd _popNativeAd = null;

    @Override
    public void InitAd(Activity context, String adId, FrameLayout nativeInterView) {
        super.InitAd(context, adId, nativeInterView);
        _dataListener = new HWNativeInterAdDataLoadListener();
        _loadListener = new HWNativeInterAdLoadListener();
        nativeInterAdList = new ArrayList();
        OnInitFinished();
    }

    @Override
    public void LoadAd() {
        super.LoadAd();
        NativeAdLoader.Builder builder = new NativeAdLoader.Builder(_context, _adUnit);
        builder.setNativeAdLoadedListener(_dataListener).setAdListener(_loadListener);

        VideoConfiguration videoConfiguration = new VideoConfiguration.Builder()
                .setStartMuted(true)
                .build();

        NativeAdConfiguration adConfiguration = new NativeAdConfiguration.Builder()
                .setVideoConfiguration(videoConfiguration)
                .setRequestCustomDislikeThisAd(true)
                .setChoicesPosition(NativeAdConfiguration.ChoicesPosition.BOTTOM_LEFT)
                .build();

        NativeAdLoader nativeAdLoader = builder.setNativeAdOptions(adConfiguration).build();
        nativeAdLoader.loadAd(new AdParam.Builder().build());
    }

    @Override
    public void ShowAd() {
        super.ShowAd();
        if(IsReady()){
            Log.d(AdUtils.NATIVE_INTER_AD_TAG,"展示广告");
            if(_popNativeAd != null){
                _popNativeAd.destroy();
            }
            _popNativeAd = (NativeAd) nativeInterAdList.get(0);
            _context.runOnUiThread(()->{
                AdUtils.getInstance().HideBannerAd();
                _nativeInterView.removeAllViews();
                _nativeInterView.setVisibility(View.VISIBLE);
                ShowNativeInterAdView();
            });
            nativeInterAdList.remove(0);
        }
    }

    @Override
    public void HideAd() {
        super.HideAd();
        _context.runOnUiThread(()-> {
            _nativeInterView.setVisibility(View.INVISIBLE);
            OnAdClose();
        });

    }

    private void ShowNativeInterAdView(){

        LayoutInflater inflater = LayoutInflater.from(_nativeInterView.getContext());
        View adRootView = inflater.inflate(AdUtils.getInstance().GetLayoutByName("native_ad_inter"), null);
        NativeView nativeView = adRootView.findViewById(AdUtils.getInstance().GetViewIdByName("native_video_view"));

        // 注册和填充标题素材视图
        nativeView.setTitleView(nativeView.findViewById(AdUtils.getInstance().GetViewIdByName("ad_title")));
        ((TextView) nativeView.getTitleView()).setText(_popNativeAd.getTitle());

        // 注册和填充多媒体素材视图
        nativeView.setMediaView((MediaView) adRootView.findViewById(AdUtils.getInstance().GetViewIdByName("ad_media")));
        nativeView.getMediaView().setMediaContent(_popNativeAd.getMediaContent());

        // 注册原生广告对象
        nativeView.setNativeAd(_popNativeAd);

        //自定义下载按钮
        AppDownloadButton appDownloadButton = nativeView.findViewById(AdUtils.getInstance().GetViewIdByName("app_download_btn"));
        appDownloadButton.setBackground(_context.getResources().getDrawable(AdUtils.getInstance().GetDrawableByName("btn_red")));

        appDownloadButton.setAppDownloadButtonStyle(new MyAppDownloadStyleInter(_nativeInterView.getContext()));
        if (nativeView.register(appDownloadButton)) {
            appDownloadButton.setVisibility(View.VISIBLE);
            appDownloadButton.refreshAppStatus();
        } else {
            appDownloadButton.setVisibility(View.INVISIBLE);
            nativeView.findViewById(AdUtils.getInstance().GetViewIdByName("app_download_btn")).setVisibility(View.INVISIBLE);
        }
        ((Button)nativeView.findViewById(AdUtils.getInstance().GetViewIdByName("ad_close"))).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _nativeInterView.removeAllViews();
                ((FrameLayout)_context.findViewById(AdUtils.getInstance().GetViewIdByName("native_inter_layer"))).setVisibility(View.INVISIBLE);
                OnAdClose();
                _nativeInterView.setVisibility(View.INVISIBLE);
            }
        });
        _nativeInterView.addView(nativeView);
        ((FrameLayout)_context.findViewById(AdUtils.getInstance().GetViewIdByName("native_inter_layer"))).setVisibility(View.VISIBLE);
        ((FrameLayout)_context.findViewById(AdUtils.getInstance().GetViewIdByName("native_inter_layer"))).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
    }

    /**
     * 华为原生插屏广告数据加载监听
     */
    private class HWNativeInterAdDataLoadListener implements NativeAd.NativeAdLoadedListener {
        @Override
        public void onNativeAdLoaded(NativeAd nativeAd) {
            nativeInterAdList.add(nativeAd);
            OnLoadSuccess();
            Log.d(AdUtils.NATIVE_INTER_AD_TAG,"广告缓存数量 :" + nativeInterAdList.size());
            if(nativeInterAdList.size() < 5){
                StartLoadAdTimer();
            }else{
                StopLoadAdTimer();
            }

        }
    }

    private class HWNativeInterAdLoadListener extends AdListener {
        @Override
        public void onAdLoaded() {
            //updateStatus(getString(R.string.status_load_ad_finish), true);
        }

        @Override
        public void onAdFailed(int errorCode) {
            // Call this method when an ad fails to be loaded.
            //updateStatus(getString(R.string.status_load_ad_fail) + errorCode, true);
            OnLoadFailed();
        }
    }

    /**
     * Custom AppDownloadButton Style
     */
    private static class MyAppDownloadStyle extends AppDownloadButtonStyle {

        public MyAppDownloadStyle(Context context) {
            super(context);
            normalStyle.setTextColor(Color.BLACK);
            normalStyle.setBackground(context.getResources().getDrawable(AdUtils.getInstance().GetDrawableByName("btn_normal_blue")));
            processingStyle.setTextColor(context.getResources().getColor(AdUtils.getInstance().GetColorByName("hiad_90_white")));
        }
    }

    /**
     * Custom AppDownloadButton Style
     */
    private static class MyAppDownloadStyleInter extends AppDownloadButtonStyle {

        public MyAppDownloadStyleInter(Context context) {
            super(context);
            normalStyle.setTextColor(Color.BLACK);
            normalStyle.setBackground(context.getResources().getDrawable(AdUtils.getInstance().GetDrawableByName("btn_red")));
            processingStyle.setTextColor(context.getResources().getColor(AdUtils.getInstance().GetColorByName("hiad_90_white")));
        }
    }
}
