package cn.yingzhichu.cordova.gwxchat;
import android.app.Activity;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Intent;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;
import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelbiz.SubscribeMessage;
import com.tencent.mm.opensdk.modelbiz.WXLaunchMiniProgram;
import com.tencent.mm.opensdk.modelbiz.WXOpenBusinessView;
import com.tencent.mm.opensdk.modelbiz.WXOpenBusinessWebview;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.modelmsg.ShowMessageFromWX;
import com.tencent.mm.opensdk.modelmsg.WXAppExtendObject;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;
public class WXEntryActivity extends Activity implements IWXAPIEventHandler{
    private IWXAPI api;

    @Override
    public void onReq(BaseReq req) {
        Intent intent = new Intent(Intent.ACTION_MAIN);

        finish();
    }

    @Override
    public void onResp(BaseResp resp) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        String act = "";
        switch (resp.getType()){
            case ConstantsAPI.COMMAND_PAY_BY_WX://支付成功 errCode 0	成功 -1	错误 -2	用户取消
                act = "pay";
                intent.putExtra("code", resp.errCode);
                break;
            case ConstantsAPI.COMMAND_SENDAUTH://微信登录
                //返回值	说明
                //ErrCode	ERR_OK = 0(用户同意) ERR_AUTH_DENIED = -4（用户拒绝授权） ERR_USER_CANCEL = -2（用户取消）
                //code	用户换取 access_token 的 code，仅在 ErrCode 为 0 时有效
                //state	第三方程序发送时用来标识其请求的唯一性的标志，由第三方程序调用 sendReq 时传入，由微信终端回传，state 字符串长度不能超过 1K
                //lang	微信客户端当前语言
                //country	微信用户当前国家信息
                SendAuth.Resp authResp = (SendAuth.Resp)resp;
                act = "code";
                intent.putExtra("code", authResp.code);
            case ConstantsAPI.COMMAND_SENDMESSAGE_TO_WX:
                act = "share";
                intent.putExtra("code", resp.errCode);
        }
        intent.putExtra("action",act);
        startActivity(intent);
//        finish();

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        api = WXAPIFactory.createWXAPI(this, getApplicationInfo().metaData.getString("wechatid"), false);

        try {
            Intent intent = getIntent();
            api.handleIntent(intent, this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        api.handleIntent(intent, this);
    }


}
