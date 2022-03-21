package com.jiuqu.tools.ad.hwad;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.ads.HwAds;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.iap.Iap;
import com.huawei.hms.iap.entity.ConsumeOwnedPurchaseReq;
import com.huawei.hms.iap.entity.ConsumeOwnedPurchaseResult;
import com.huawei.hms.iap.entity.InAppPurchaseData;
import com.huawei.hms.iap.entity.OrderStatusCode;
import com.huawei.hms.iap.entity.OwnedPurchasesReq;
import com.huawei.hms.iap.entity.OwnedPurchasesResult;
import com.huawei.hms.iap.entity.PurchaseIntentReq;
import com.huawei.hms.iap.entity.PurchaseIntentResult;
import com.huawei.hms.iap.entity.PurchaseResultInfo;
import com.huawei.hms.support.account.AccountAuthManager;
import com.huawei.hms.support.account.request.AccountAuthParams;
import com.huawei.hms.support.account.request.AccountAuthParamsHelper;
import com.huawei.hms.support.account.result.AuthAccount;
import com.huawei.hms.support.account.service.AccountAuthService;
import com.huawei.hms.support.api.client.Status;
import com.huawei.hms.support.api.paytask.IapApiException;
import com.huawei.hms.support.log.common.Base64;
import com.jiuqu.tools.ad.AdCoreBase;
import com.jiuqu.tools.ad.BuyProductEvent;

import org.json.JSONException;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.X509EncodedKeySpec;

public class HWAdCore extends AdCoreBase {

    private String _tempProductName = "";
    private int _tempProductType = 0;
    private String _key = "PAY_KEY";

    @Override
    public void Init(Context context) {
        super.Init(context);
        HwAds.init(context);
        //华为自动跳过隐私协议
        final SharedPreferences sp = context.getSharedPreferences("userAgreementResult", 0);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("userAgreementResult", true);
        editor.apply();
    }

    @Override
    public void LoginSdkWhenNeed(Activity activity) {
        super.LoginSdkWhenNeed(activity);
        AccountAuthParams authParams = new AccountAuthParamsHelper(AccountAuthParams.DEFAULT_AUTH_REQUEST_PARAM).setAuthorizationCode().createParams();
        AccountAuthService service = AccountAuthManager.getService(activity, authParams);
        activity.startActivityForResult(service.getSignInIntent(), 8888);
    }

    @Override
    public void BuyProduct(Activity acty, String pName, int priceType, BuyProductEvent event) {
        super.BuyProduct(acty,pName, priceType,event);
        _tempProductName = pName;
        _tempProductType = priceType;
        // 构造一个PurchaseIntentReq对象
        PurchaseIntentReq req = new PurchaseIntentReq();
        // 通过createPurchaseIntent接口购买的商品必须是您在AppGallery Connect网站配置的商品。
        req.setProductId(_tempProductName);
        // priceType: 0：消耗型商品; 1：非消耗型商品; 2：订阅型商品
        req.setPriceType(_tempProductType);
        // 获取调用接口的Activity对象
        final Activity activity = acty;
        // 调用createPurchaseIntent接口创建托管商品订单
        Task<PurchaseIntentResult> task = Iap.getIapClient(activity).createPurchaseIntent(req);
        task.addOnSuccessListener(new OnSuccessListener<PurchaseIntentResult>() {
            @Override
            public void onSuccess(PurchaseIntentResult result) {
                // 获取创建订单的结果
                Status status = result.getStatus();
                if (status.hasResolution()) {
                    try {
                        // 6666是您自定义的常量
                        // 启动IAP返回的收银台页面
                        status.startResolutionForResult(activity, 6666);
                    } catch (IntentSender.SendIntentException exp) {
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof IapApiException) {
                    IapApiException apiException = (IapApiException) e;
                    Status status = apiException.getStatus();
                    if (status.getStatusCode() == OrderStatusCode.ORDER_HWID_NOT_LOGIN) {
                        LoginSdkWhenNeed(activity);
                    } else if (status.getStatusCode() == OrderStatusCode.ORDER_ACCOUNT_AREA_NOT_SUPPORTED) {
                        // 用户当前登录的华为帐号所在的服务地不在华为IAP支持结算的国家/地区中
                    }
                } else {
                    // 其他外部错误
                }
            }
        });
    }

    @Override
    public void OnActivityCheck(Activity acty,int requestCode, int resultCode, @Nullable Intent data) {
        super.OnActivityCheck(acty,requestCode, resultCode, data);
        if (requestCode == 8888) {
            Task<AuthAccount> authAccountTask = AccountAuthManager.parseAuthResultFromIntent(data);
            if (authAccountTask.isSuccessful()) {
                //登录成功，获取用户的帐号信息和Authorization Code
                AuthAccount authAccount = authAccountTask.getResult();
                Log.i("ActivityTag", "serverAuthCode:" + authAccount.getAuthorizationCode());
                if (_tempProductName != "")
                    BuyProduct(acty,_tempProductName, _tempProductType,_buyProductEvent);
            } else {
                //登录失败
                Log.e("ActivityTag", "sign in failed:" + ((ApiException) authAccountTask.getException()).getStatusCode());
            }
        }
        else if (requestCode == 6666) {
            if (data == null) {
                Log.e("onActivityResult", "data is null");
                return;
            }
            // 调用parsePurchaseResultInfoFromIntent方法解析支付结果数据
            PurchaseResultInfo purchaseResultInfo = Iap.getIapClient(acty).parsePurchaseResultInfoFromIntent(data);
            switch(purchaseResultInfo.getReturnCode()) {
                case OrderStatusCode.ORDER_STATE_CANCEL:
                    // 用户取消
                    break;
                case OrderStatusCode.ORDER_STATE_FAILED:
                case OrderStatusCode.ORDER_STATE_DEFAULT_CODE:
                    // 检查是否存在未发货商品
                    CompensateProduct(acty);
                    break;
                case OrderStatusCode.ORDER_PRODUCT_OWNED:
                    if(_buyProductEvent != null){
                        _buyProductEvent.OnBuyFinished(_tempProductName);
                    }
                    break;
                case OrderStatusCode.ORDER_STATE_SUCCESS:
                    // 支付成功
                    // 使用您应用的IAP公钥验证签名
                    // 若验签成功，则进行发货
                    // 若用户购买商品为消耗型商品，您需要在发货成功后调用consumeOwnedPurchase接口进行消耗
                    String inAppPurchaseData = purchaseResultInfo.getInAppPurchaseData();
                    String inAppPurchaseDataSignature = purchaseResultInfo.getInAppDataSignature();
                    if (doCheck(inAppPurchaseData, inAppPurchaseDataSignature, _key))
                    {
                        //发货
                        try {
                            InAppPurchaseData inAppPurchaseDataBean = new InAppPurchaseData(inAppPurchaseData);
                            int purchaseState = inAppPurchaseDataBean.getPurchaseState();
                            if (purchaseState == 0) {
                                _tempProductName = inAppPurchaseDataBean.getProductId();
                                _tempProductType = inAppPurchaseDataBean.getKind();
                                FinishBuy(acty,inAppPurchaseData);
                            }
                        } catch (JSONException e) {
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    //补发商品
    private void CompensateProduct(Activity acty)
    {
        // 构造一个OwnedPurchasesReq对象
        OwnedPurchasesReq ownedPurchasesReq = new OwnedPurchasesReq();
        // priceType: 0：消耗型商品; 1：非消耗型商品; 2：订阅型商品
        ownedPurchasesReq.setPriceType(0);
        // 获取调用接口的Activity对象
        final Activity activity = acty;
        // 调用obtainOwnedPurchases接口获取所有已购但未发货的消耗型商品的购买信息
        Task<OwnedPurchasesResult> task = Iap.getIapClient(activity).obtainOwnedPurchases(ownedPurchasesReq);
        task.addOnSuccessListener(new OnSuccessListener<OwnedPurchasesResult>() {
            @Override
            public void onSuccess(OwnedPurchasesResult result) {
                // 获取接口请求成功的结果
                if (result != null && result.getInAppPurchaseDataList() != null) {
                    for (int i = 0; i < result.getInAppPurchaseDataList().size(); i++) {
                        String inAppPurchaseData = result.getInAppPurchaseDataList().get(i);
                        String inAppSignature = result.getInAppSignature().get(i);
                        // 使用应用的IAP公钥验证inAppPurchaseData的签名数据
                        if (doCheck(inAppPurchaseData, inAppSignature, _key)) {
                            // 如果验签成功，确认每个商品的购买状态。确认商品已支付后，检查此前是否已发过货，未发货则进行发货操作。发货成功后执行消耗操作
                            try {
                                InAppPurchaseData inAppPurchaseDataBean = new InAppPurchaseData(inAppPurchaseData);
                                int purchaseState = inAppPurchaseDataBean.getPurchaseState();
                                if (purchaseState == 0) {
                                    _tempProductName = inAppPurchaseDataBean.getProductId();
                                    _tempProductType = inAppPurchaseDataBean.getKind();
                                    FinishBuy(acty,inAppPurchaseData);
                                }
                            } catch (JSONException e) {
                            }
                        }
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof com.huawei.hms.iap.IapApiException) {
                    com.huawei.hms.iap.IapApiException apiException = (com.huawei.hms.iap.IapApiException) e;
                    Status status = apiException.getStatus();
                    int returnCode = apiException.getStatusCode();
                } else {
                    // 其他外部错误
                }
            }
        });
    }

    private void FinishBuy(Activity acty,String inAppPurchaseData)
    {
        // 构造ConsumeOwnedPurchaseReq对象
        ConsumeOwnedPurchaseReq req = new ConsumeOwnedPurchaseReq();
        String purchaseToken = "";
        try {
            // purchaseToken需从购买信息InAppPurchaseData中获取
            InAppPurchaseData inAppPurchaseDataBean = new InAppPurchaseData(inAppPurchaseData);
            purchaseToken = inAppPurchaseDataBean.getPurchaseToken();
        } catch (JSONException e) {
        }
        req.setPurchaseToken(purchaseToken);
        // 获取调用接口的Activity对象
        final Activity activity = acty;
        // 消耗型商品发货成功后，需调用consumeOwnedPurchase接口进行消耗
        Task<ConsumeOwnedPurchaseResult> task = Iap.getIapClient(activity).consumeOwnedPurchase(req);
        task.addOnSuccessListener(new OnSuccessListener<ConsumeOwnedPurchaseResult>() {
            @Override
            public void onSuccess(ConsumeOwnedPurchaseResult result) {
                // 获取接口请求结果
                if(_buyProductEvent != null){
                    _buyProductEvent.OnBuyFinished(_tempProductName);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                if (e instanceof com.huawei.hms.iap.IapApiException) {
                    com.huawei.hms.iap.IapApiException apiException = (com.huawei.hms.iap.IapApiException) e;
                    Status status = apiException.getStatus();
                    int returnCode = apiException.getStatusCode();
                } else {
                    // 其他外部错误
                }
            }
        });
    }

    /**
     * 校验签名信息
     *
     * @param content 结果字符串
     * @param sign 签名字符串
     * @param publicKey IAP公钥
     * @return 是否校验通过
     */
    public static boolean doCheck(String content, String sign, String publicKey) {
        return doCheck(content, sign, publicKey, "SHA256WithRSA");
    }

    /**
     * 校验签名信息
     *
     * @param content 结果字符串
     * @param sign 签名字符串
     * @param publicKey IAP公钥
     * @param signatureAlgorithm 签名算法字段，可从接口返回数据中获取，例如：OwnedPurchasesResult.getSignatureAlgorithm()
     * @return 是否校验通过
     */
    public static boolean doCheck(String content, String sign, String publicKey, String signatureAlgorithm) {
        if (sign == null) {
            return false;
        }
        if (publicKey == null) {
            return false;
        }

        // 当signatureAlgorithm为空时使用默认签名算法
        if (signatureAlgorithm == null || signatureAlgorithm.length() == 0) {
            signatureAlgorithm = "SHA256WithRSA";
            System.out.println("doCheck, algorithm: SHA256WithRSA");
        }
        try {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            // 生成"RSA"的KeyFactory对象
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            byte[] encodedKey = Base64.decode(publicKey);
            // 生成公钥
            PublicKey pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(encodedKey));
            java.security.Signature signature = null;
            // 根据SHA256WithRSA算法获取签名对象实例
            signature = java.security.Signature.getInstance(signatureAlgorithm);
            // 初始化验证签名的公钥
            signature.initVerify(pubKey);
            // 把原始报文更新到签名对象中
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            // 将sign解码
            byte[] bsign = Base64.decode(sign);
            // 进行验签
            return signature.verify(bsign);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
