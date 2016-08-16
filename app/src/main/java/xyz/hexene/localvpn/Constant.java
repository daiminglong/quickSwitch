package xyz.hexene.localvpn;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Created by daiminglong on 2016/7/25.
 */
public class Constant {

    public static String DAIMINGLONGTAG = "daiminglongtag";

    public static Map<String,ByteBuffer> requestBufferMap;
    public static int FLAG;
    public static final int WIFI_TRANSMISSION = 0;
    public static final int MOBILE_DATA_TRANSMISSION = 1;

    public static int DEFAULT_TRANSMISSION;

    public static int WIFI_RTT_THRESHOLD = 5;//unit mm

    public static Context context;
}
