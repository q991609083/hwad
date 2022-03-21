package com.jiuqu.tools.ad.hwad;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.Nullable;

import com.huawei.hms.ads.AdParam;
import com.huawei.hms.ads.reward.Reward;
import com.huawei.hms.ads.reward.RewardAd;
import com.huawei.hms.ads.reward.RewardAdLoadListener;
import com.huawei.hms.ads.reward.RewardAdStatusListener;
import com.jiuqu.tools.ad.AdEventListener;
import com.jiuqu.tools.ad.AdUtils;
import com.jiuqu.tools.ad.RewardAdBase;

import java.util.ArrayList;

/**
 * 华为激励视频广告
 */
public class HWRewardAd extends RewardAdBase {

    private HWRewardAdLoadListener _loadListener = null;
    private HWRewardAdPlayListener _playListener = null;
    private boolean _rewardFinish = false;

    @Override
    public void InitAd(Activity context, String adId, @Nullable AdEventListener listener ) {
        super.InitAd(context,adId,listener);
        _playListener = new HWRewardAdPlayListener();
        rewardAdList = new ArrayList();
        OnInitFinished();
    }

    @Override
    public void LoadAd() {
        super.LoadAd();
        Log.d(AdUtils.REWARD_AD_TAG,"hasLoadAd");
        RewardAd rewardAd = new RewardAd(_context,_adUnit);
        _loadListener = new HWRewardAdLoadListener(rewardAd);
        rewardAd.loadAd(new AdParam.Builder().build(),_loadListener);
    }

    @Override
    public void ShowAd() {
        super.ShowAd();
        if(IsReady()){
            _rewardFinish = false;
            _context.runOnUiThread(
                () -> {
                    RewardAd reward = (RewardAd) rewardAdList.get(0);
                    reward.show(_context,_playListener);
                    rewardAdList.remove(0);
                });
        }else{
            RewardAdFailed();
        }
    }

    /**
     * 实现激励视频广告加载监听
     */
    private class HWRewardAdLoadListener extends RewardAdLoadListener{
        private RewardAd _ad;
        public HWRewardAdLoadListener(RewardAd ad){
            _ad = ad;
        }
        @Override
        public void onRewardedLoaded() {
            // 激励广告加载成功
            Log.d(AdUtils.REWARD_AD_TAG,"加载激励视频成功");
            rewardAdList.add(_ad);
            //加载成功Event
            OnLoadSuccess();
            Log.d(AdUtils.REWARD_AD_TAG,"广告缓存数量 :" + rewardAdList.size());
            if(rewardAdList.size() < 2){
                StartLoadAdTimer();
            }else{
                StopLoadAdTimer();
            }

        }
        @Override
        public void onRewardAdFailedToLoad(int errorCode) {
            // 激励广告加载失败
            Log.d(AdUtils.REWARD_AD_TAG,"加载激励视频失败" + errorCode);
            OnLoadFailed();

        }
    }

    private class HWRewardAdPlayListener extends RewardAdStatusListener{
        @Override
        public void onRewardAdOpened() {
            // 激励广告被打开
            ShowRewardEvent();
        }
        @Override
        public void onRewardAdFailedToShow(int errorCode) {
            // 激励广告展示失败
            RewardAdFailed();
            //重新触发计时器加载
            CloseRewardEvent();
            OnAdClose();
        }
        @Override
        public void onRewardAdClosed() {
            // 激励广告被关闭
            CloseRewardEvent();
            OnAdClose();
        }
        @Override
        public void onRewarded(Reward reward){
            // 激励广告奖励达成，发放奖励
            _rewardFinish = true;
            RewardAdEndPlay();
        }
    }
}
