package cn.yingzhichu.cordova.gwxchat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.modelmsg.WXImageObject;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.modelmsg.WXTextObject;
import com.tencent.mm.opensdk.modelmsg.WXVideoObject;
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject;
import com.tencent.mm.opensdk.modelpay.PayReq;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

/**
 * This class echoes a string called from JavaScript.
 */
public class GWxChat extends CordovaPlugin {

    private IWXAPI api;
    private String wxappid;
    private CallbackContext payCallBack;
    private CallbackContext loginCallBack;
    private CallbackContext shareCallBack;

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        switch (intent.getStringExtra("act")) {
            case "pay":
                if(payCallBack != null){
                    payCallBack.success(intent.getStringExtra("code"));
                    return;
                }
            case "code":
                if(loginCallBack != null){
                    try {
                        JSONObject ret = new JSONObject();
                        ret.put("code",intent.getStringExtra("code"));
                        ret.put("appid",wxappid);
                        loginCallBack.success(ret);
                    }catch (Exception e){}

                    return;
                }
            case "share":
                if(shareCallBack != null){
                    shareCallBack.success(intent.getStringExtra("code"));
                    return;
                }
        }

    }

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        wxappid = cordova.getActivity().getApplicationInfo().metaData.getString("wechatid");
        api = WXAPIFactory.createWXAPI(cordova.getContext(), wxappid, true);
        api.registerApp(wxappid);
        cordova.getActivity().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                api.registerApp(wxappid);
            }
        }, new IntentFilter(ConstantsAPI.ACTION_REFRESH_WXAPP));
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("pay")) {//支付
            payCallBack = callbackContext;
            pay(args.getJSONObject(0));
        } else if (action.equals("login")) {//获得code
            loginCallBack = callbackContext;
            login();
        } else if (action.equals("share")) {
            shareCallBack = callbackContext;
            share(args.getJSONObject(0));
        }
        return true;
    }

    private void share(JSONObject obj) {
        switch (obj.optString("act")) {
            case "text":
                shareText(obj);
                break;
            case "img":
                shareImg(obj);
                break;
            case "video":
                shareVideo(obj);
                break;
            case "url":
                shareUrl(obj);
                break;
        }

    }

    private void shareText(JSONObject obj) {
        String text = obj.optString("title");
        if (text.equals("")) {
            return;
        }
        //初始化一个 WXTextObject 对象，填写分享的文本内容
        WXTextObject textObj = new WXTextObject();
        textObj.text = text;

        //用 WXTextObject 对象初始化一个 WXMediaMessage 对象
        WXMediaMessage msg = new WXMediaMessage();
        msg.mediaObject = textObj;
        msg.description = obj.optString("desc");

        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = ("text");
        req.message = msg;
        req.scene = getSence(obj);
        //调用api接口，发送数据到微信
        api.sendReq(req);
    }

    private void shareImg(JSONObject obj) {
        Bitmap bmp = urlBuildBitmap(obj);
        if (bmp == null) {
            return;
        }

        //初始化 WXImageObject 和 WXMediaMessage 对象
        WXImageObject imgObj = new WXImageObject(bmp);
        WXMediaMessage msg = new WXMediaMessage();
        msg.mediaObject = imgObj;

        //设置缩略图
        Bitmap thumbBmp = Bitmap.createScaledBitmap(bmp, 200, 200, true);
        bmp.recycle();
        msg.thumbData = bmpToByteArray(thumbBmp, true);
        msg.title = obj.optString("title");
        msg.description = obj.optString("desc");

        //构造一个Req
        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = buildTransaction("img");
        req.message = msg;

        req.scene = getSence(obj);
        //调用api接口，发送数据到微信
        api.sendReq(req);
    }


    private void shareVideo(JSONObject obj) {
        String videoUrl = obj.optString("video");
        if (videoUrl.equals("")) {
            return;
        }
        //初始化一个WXVideoObject，填写url
        WXVideoObject video = new WXVideoObject();
        video.videoUrl = videoUrl;

        //用 WXVideoObject 对象初始化一个 WXMediaMessage 对象
        WXMediaMessage msg = new WXMediaMessage(video);
        msg.title = obj.optString("title");
        msg.description = obj.optString("desc");;
        Bitmap thumbBmp = urlBuildBitmap(obj);
        if(thumbBmp != null){
            msg.thumbData = bmpToByteArray(thumbBmp, true);
        }
        //构造一个Req
        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = buildTransaction("video");
        req.message = msg;
        req.scene = getSence(obj);
        //调用api接口，发送数据到微信
        api.sendReq(req);
    }

    private void shareUrl(JSONObject obj) {
        String web = obj.optString("url");
        if(web.equals("")){
            return;
        }
        //初始化一个WXWebpageObject，填写url
        WXWebpageObject webpage = new WXWebpageObject();
        webpage.webpageUrl = web;

        //用 WXWebpageObject 对象初始化一个 WXMediaMessage 对象
        WXMediaMessage msg = new WXMediaMessage(webpage);
        msg.title = obj.optString("title");
        msg.description = obj.optString("desc");
        Bitmap thumbBmp = urlBuildBitmap(obj);
        if(thumbBmp != null){
            msg.thumbData = bmpToByteArray(thumbBmp, true);
        }

        //构造一个Req
        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = buildTransaction("webpage");
        req.message = msg;
        req.scene = getSence(obj);

        //调用api接口，发送数据到微信
        api.sendReq(req);
    }

    //微信支付
    private void pay(JSONObject obj) {
        try {
            PayReq request = new PayReq();
            request.appId = obj.optString("appId");
            request.partnerId = obj.optString("partnerId");
            request.prepayId = obj.optString("prepayId");
            request.packageValue = "Sign=WXPay";
            request.nonceStr = obj.optString("nonceStr");
            request.timeStamp = obj.optString("timeStamp");
            request.sign = obj.optString("sign");
            api.sendReq(request);
        } catch (Exception e) {
        }
    }

    //登录
    private void login() {
        SendAuth.Req req = new SendAuth.Req();
        req.scope = "snsapi_userinfo"; // 只能填 snsapi_userinfo
        req.state = "wechat_sdk_demo_test";
        api.sendReq(req);

    }

    private String buildTransaction(final String type) {
        return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
    }

    private int getSence(JSONObject obj) {
        String type = obj.optString("sence");
        switch (type) {
            case "timeline":
                return SendMessageToWX.Req.WXSceneTimeline;
            default:
                return SendMessageToWX.Req.WXSceneSession;
        }
    }

    private Bitmap urlBuildBitmap(JSONObject obj) {
        try {
            String url = obj.optString("img");
            if (url.equals("")) {
                return null;
            }
            InputStream is = new URL(url).openStream();
            return BitmapFactory.decodeStream(is);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] bmpToByteArray(final Bitmap bmp, final boolean needRecycle) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, output);
        if (needRecycle) {
            bmp.recycle();
        }

        byte[] result = output.toByteArray();
        try {
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
}
