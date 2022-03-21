package com.jiuqu.tools.ad.hwad;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
import com.jiuqu.tools.ad.TopNativeAdBase;

import java.util.ArrayList;

public class HWTopNativeAd extends TopNativeAdBase {
    private HWNativeAdDataLoadListener _dataListener = null;
    private HWNativeAdLoadListener _loadListener = null;

    private NativeAd _curNativeAd = null;
    private NativeAd _popNativeAd = null;

    private boolean _btnUp;
    private boolean _showBg = true;
    private int _offset;
    private boolean nativeCloseSwitch = false;

    @Override
    public void InitAd(Activity context, String adId, FrameLayout nativeView) {
        super.InitAd(context, adId, nativeView);
        _dataListener = new HWNativeAdDataLoadListener();
        _loadListener = new HWNativeAdLoadListener();
        nativeAdList = new ArrayList();
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
                .setChoicesPosition(NativeAdConfiguration.ChoicesPosition.INVISIBLE)
                .build();

        NativeAdLoader nativeAdLoader = builder.setNativeAdOptions(adConfiguration).build();
        nativeAdLoader.loadAd(new AdParam.Builder().build());
    }

    @Override
    public void ShowAd() {
        super.ShowAd();
        if(IsReady()){
            Log.d(AdUtils.NATIVE_AD_TAG,"展示广告");
            if(_popNativeAd != null){
                _popNativeAd.destroy();
                _popNativeAd = null;
            }
            _popNativeAd = (NativeAd) nativeAdList.get(0);
            _context.runOnUiThread(()->{
                AdUtils.getInstance().HideBannerAd();
                _nativeView.removeAllViews();
                _nativeView.setVisibility(View.VISIBLE);
                ShowNativeAdView();
            });
            nativeAdList.remove(0);
        }
    }

    @Override
    public void ShowAd(boolean btnup, boolean showBg, int offset) {
        super.ShowAd(btnup, showBg, offset);
        _btnUp = btnup;
        _showBg = showBg;
        _offset = offset;
        if(IsReady()){
            Log.d(AdUtils.NATIVE_AD_TAG,"自定义参数展示广告");
            if(_popNativeAd != null){
                _popNativeAd.destroy();
                _popNativeAd = null;
            }
            _popNativeAd = (NativeAd) nativeAdList.get(0);
            _context.runOnUiThread(()->{
                AdUtils.getInstance().HideBannerAd();
                _nativeView.removeAllViews();
                _nativeView.setVisibility(View.VISIBLE);
                ShowNativeAdView();
            });
            nativeAdList.remove(0);
        }
    }

    @Override
    public void ShowAd(boolean btnup, boolean showBg, int offset,boolean switchClose) {
        super.ShowAd(btnup, showBg, offset,switchClose);
        _btnUp = btnup;
        _showBg = showBg;
        _offset = offset;
        nativeCloseSwitch = switchClose;
        if(IsReady()){
            Log.d(AdUtils.NATIVE_AD_TAG,"自定义参数展示广告");
            if(_popNativeAd != null){
                _popNativeAd.destroy();
                _popNativeAd = null;
            }
            _popNativeAd = (NativeAd) nativeAdList.get(0);
            _context.runOnUiThread(()->{
                AdUtils.getInstance().HideBannerAd();
                _nativeView.removeAllViews();
                _nativeView.setVisibility(View.VISIBLE);
                ShowNativeAdView();
            });
            nativeAdList.remove(0);
        }
    }
    @Override
    public void HideAd() {
        super.HideAd();
        _context.runOnUiThread(()-> {
            _nativeView.setVisibility(View.INVISIBLE);
            OnAdClose();
        });

    }

    /**
     * 根据参数展示原生广告
     */
    private void ShowNativeAdView() {
        LayoutInflater inflater = LayoutInflater.from(_nativeView.getContext());
        View adRootView = inflater.inflate(AdUtils.getInstance().GetLayoutByName("native_ad"), null);
        if (_btnUp)
            adRootView = inflater.inflate(AdUtils.getInstance().GetLayoutByName("native_ad_custom"),null);

        //底部原生向上偏移
        RelativeLayout.LayoutParams layoutParams =  (RelativeLayout.LayoutParams)_nativeView.getLayoutParams();
        layoutParams.bottomMargin = 40 + _offset;
        _nativeView.setLayoutParams(layoutParams);

        NativeView nativeView = adRootView.findViewById(AdUtils.getInstance().GetViewIdByName("native_video_view"));

        // 注册和填充标题素材视图
        nativeView.setTitleView(nativeView.findViewById(AdUtils.getInstance().GetViewIdByName("ad_title")));
        ((TextView) nativeView.getTitleView()).setText(_popNativeAd.getTitle());

        // 注册和填充多媒体素材视图
        nativeView.setMediaView((MediaView) adRootView.findViewById(AdUtils.getInstance().GetViewIdByName("ad_media")));
        nativeView.getMediaView().setMediaContent(_popNativeAd.getMediaContent());

        //注册和填充Icon
        nativeView.setImageView(nativeView.findViewById(AdUtils.getInstance().GetViewIdByName("ad_icon")));
        if (null != _popNativeAd.getIcon()) {
            ((ImageView)nativeView.getImageView()).setImageDrawable(_popNativeAd.getIcon().getDrawable());
        }
        nativeView.getImageView()
                .setVisibility(null != _popNativeAd.getIcon() ? View.VISIBLE : View.INVISIBLE);
        //注册和填充来源
        nativeView.setAdSourceView(nativeView.findViewById(AdUtils.getInstance().GetViewIdByName("ad_source")));
        if (null != _popNativeAd.getAdSource()) {
            ((TextView) nativeView.getAdSourceView()).setText(_popNativeAd.getAdSource());
        }
        nativeView.getAdSourceView()
                .setVisibility(null != _popNativeAd.getAdSource() ? View.VISIBLE : View.INVISIBLE);

        //开关背景
        if (!_showBg)
            adRootView.findViewById(AdUtils.getInstance().GetViewIdByName("ad_mid_bg")).setBackground(null);

        // 注册原生广告对象
        nativeView.setNativeAd(_popNativeAd);

        //自定义下载按钮
        AppDownloadButton appDownloadButton = nativeView.findViewById(AdUtils.getInstance().GetViewIdByName("app_download_btn"));
        appDownloadButton.setBackground(_context.getResources().getDrawable(AdUtils.getInstance().GetDrawableByName("btn_normal_blue")));

        appDownloadButton.setAppDownloadButtonStyle(new MyAppDownloadStyle(_nativeView.getContext()));
        if (nativeView.register(appDownloadButton)) {
            appDownloadButton.setVisibility(View.VISIBLE);
            appDownloadButton.refreshAppStatus();
        } else {
            appDownloadButton.setTextColor(Color.BLACK);
            String btnText = _popNativeAd.getCallToAction();
            appDownloadButton.setText(btnText);
            appDownloadButton.setVisibility(View.VISIBLE);
            nativeView.findViewById(AdUtils.getInstance().GetViewIdByName("app_download_btn")).setVisibility(View.VISIBLE);
        }
        //播放背后的流光
        int createType = _popNativeAd.getCreativeType();
        ImageView imageView = nativeView.findViewById(AdUtils.getInstance().GetViewIdByName("media_bg"));
        if (createType == 7 || createType == 107)
        {
            imageView = nativeView.findViewById(AdUtils.getInstance().GetViewIdByName("media_bgs"));
            nativeView.findViewById(AdUtils.getInstance().GetViewIdByName("media_bgs")).setVisibility(View.VISIBLE);
            nativeView.findViewById(AdUtils.getInstance().GetViewIdByName("ad_flags")).setVisibility(View.VISIBLE);
            nativeView.findViewById(AdUtils.getInstance().GetViewIdByName("media_bg")).setVisibility(View.INVISIBLE);
            nativeView.findViewById(AdUtils.getInstance().GetViewIdByName("ad_flag")).setVisibility(View.INVISIBLE);

        }
        else
        {
            nativeView.findViewById(AdUtils.getInstance().GetViewIdByName("media_bg")).setVisibility(View.VISIBLE);
            nativeView.findViewById(AdUtils.getInstance().GetViewIdByName("ad_flag")).setVisibility(View.VISIBLE);
            nativeView.findViewById(AdUtils.getInstance().GetViewIdByName("media_bgs")).setVisibility(View.INVISIBLE);
            nativeView.findViewById(AdUtils.getInstance().GetViewIdByName("ad_flags")).setVisibility(View.INVISIBLE);
        }
        AnimationDrawable animationDrawable = (AnimationDrawable)imageView.getBackground();
        animationDrawable.start();
        ((Button)nativeView.findViewById(AdUtils.getInstance().GetViewIdByName("ad_close"))).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _nativeView.removeAllViews();
                OnAdClose();
                _nativeView.setVisibility(View.INVISIBLE);
            }
        });
        ((Button)nativeView.findViewById(AdUtils.getInstance().GetViewIdByName("ad_i"))).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _popNativeAd.gotoWhyThisAdPage(nativeView.getContext());
            }
        });
        _nativeView.addView(nativeView);
    }


    /**
     * 华为原生广告数据加载监听
     */
    private class HWNativeAdDataLoadListener implements NativeAd.NativeAdLoadedListener {
        @Override
        public void onNativeAdLoaded(NativeAd nativeAd) {
            nativeAdList.add(nativeAd);
            OnLoadSuccess();
            Log.d(AdUtils.NATIVE_AD_TAG,"广告缓存数量 :" + nativeAdList.size());
            if(nativeAdList.size() < 5){
                StartLoadAdTimer();
            }else{
                StopLoadAdTimer();
            }

        }
    }

    private class HWNativeAdLoadListener extends AdListener {
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
