package xyz.hexene.localvpn;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by daiminglong on 2016/7/10.
 */
public class NewNetworkManager {


    protected Context context;


    public NewNetworkManager(Context context){
        this.context = context;
    }

    /**
     * this function used to get root permission for the application
     * @param pkeCodePath
     * @return
     */
    public static boolean upgradeRootPermission(String pkeCodePath){
        Process process = null;
        DataOutputStream os = null;
        try{
            //get root permission success
            String cmd = "chmod 777" + pkeCodePath;
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(cmd + "\n");
            os.writeBytes("exit"+"\n");
            os.flush();
            process.waitFor();
        }catch (Exception e){
            Log.e("mobile data", "upgrade root permission error" + e.toString());
            return false;
        }finally {
            try{
                if(os!=null){
                    os.close();
                }
                process.destroy();
            }catch (Exception e){
                Log.e("mobile data", "process destroy error" + e.toString());
            }
        }
        return true;
    }


    /**
     * this function used to execute a cmd line bypass the android framework
     *
     * @param cmd : cmd line
     * @return true: execute success
     *         false: execute failed
     */
    public boolean executeCmdLine(String cmd){
        Process process = null;
        DataOutputStream os = null;
        String s = "";

        try{
            //command line execute
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(cmd + "\n");
            os.writeBytes("exit" + "\n");
            os.flush();
            process.waitFor();
            Log.e("-------", "555555555");
        }catch (Exception e){
            e.printStackTrace();
            Log.i("cmd line", "cmd line exception" + e.toString());
            return false;
        }finally {
            try{
                if(os!=null){
                    os.close();
                }
                process.destroy();
            }catch (Exception e){
                Log.e("-------", e.toString());
                return false;
            }

        }
        return true;
    }


    /**
     * this function used to transfer int ip address to String ip address
     * @param ipInt
     * @return
     */
    public String fromIntToIP(int ipInt){
        StringBuilder sb = new StringBuilder();
        sb.append(ipInt & 0xFF).append(".");
        sb.append((ipInt >> 8) & 0xFF).append(".");
        sb.append((ipInt >> 16) & 0xFF).append(".");
        sb.append((ipInt >> 24) & 0xFF);
        return sb.toString();
    }


    /**
     * this function used to select a suitable interface
     * @param currentPacket
     */
    public static void selectInterface(Packet currentPacket){

    }


}


class GetRTTThread extends Thread {

    private String netInterface;
    private float rttTime;

    public float getRttTime() {
        return rttTime;
    }

    public GetRTTThread(int type){

        this.rttTime = 0;
        switch (type){
            case Constant.WIFI_TRANSMISSION:
                this.netInterface = "wlan0";
                break;
            case Constant.MOBILE_DATA_TRANSMISSION:
                this.netInterface = "rmnet0";
                break;
        }
    }
    @Override
    public void run(){

        //create 3 thread to get average ping is the best way!
        List<PingThread> pingThreadList = new ArrayList();
        PingThread pingThread1 = new PingThread(netInterface,"123.57.209.174");
        PingThread pingThread2 = new PingThread(netInterface,"123.57.209.174");
        PingThread pingThread3 = new PingThread(netInterface,"123.57.209.174");
        pingThreadList.add(pingThread1);
        pingThreadList.add(pingThread2);
        pingThreadList.add(pingThread3);

        for(PingThread pt : pingThreadList){
            pt.start();
        }

        for(PingThread pt : pingThreadList){
            try {
                pt.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        int count =0;
        if((!pingThread1.getInterResult().equals("Unreachable"))&&(!pingThread2.getInterResult().equals("Unreachable"))&&
                (!pingThread3.getInterResult().equals("Unreachable"))){
            for(PingThread pt : pingThreadList){
                if(!pt.getInterResult().equals("0")){
                    count++;
                    rttTime += Float.parseFloat(pt.getInterResult());
                }
            }
            rttTime = rttTime/count;
        }else{
            rttTime = 0;
        }

        Log.e("-------", "main thread result " + rttTime);

    }
}


class PingThread extends Thread {
    private String netInterface;
    private String ipAddress;
    private String interResult;

    public PingThread(String netInterface, String ipAddress){
        this.netInterface = netInterface;
        this.ipAddress = ipAddress;
        this.interResult = "";
    }


    public void run() {
        Process process = null;
        DataOutputStream os = null;

        try{
            //command line
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            //os.writeBytes("ping -I wlan0 -c 1 123.57.209.174 "+"\n");
            os.writeBytes("ping -c 1 -I "+netInterface+" "+ipAddress+ "\n");
            os.writeBytes("exit" + "\n");
            os.flush();
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = null;
            //resultLineArray used to save ping command result
            String[] resultLineArray = new String[10];
            int enumLength = 0;
            for(int i=0; (line = in.readLine())!= null; i++ ){
                resultLineArray[i] = line;
                enumLength ++;
            }
            //result analyze
            String[] arr1 = resultLineArray[1].split(" ");
            if(enumLength == 6){
                if(arr1[arr1.length-1].equals("Unreachable")){
                    interResult = "Unreachable";
                }else{
                    String[] arr2 = resultLineArray[enumLength-1].split(" ");
                    String[] arr3 = arr2[arr2.length-2].split("/");
                    interResult = arr3[1];
                }
                Log.e("-----", "result " + interResult);
            }else{
                interResult = "0";
            }
            process.waitFor();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try{
                if(os!=null){
                    os.close();
                }
                process.destroy();
            }catch (Exception e){
                Log.e("------", "process destroy error" + e.toString());
            }

        }
    }
    public String getInterResult(){
        //return one intermediate result of this thread
        return this.interResult;
    }
}

class MonitorWifiRTTThread extends Thread{

    // this algorithm works under the assumption that 3G interface is always on
    @Override
    public void run(){
        float currentWifiRTT = 0;
        //monitor wifi's RTT every 2 seconds
        while(true){
            //sleep()
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //is wifi enable
            if(!NewWifiManager.isWifiEnabled()){
                //wifi is disabled
                //switch and retry
                Constant.DEFAULT_TRANSMISSION = Constant.MOBILE_DATA_TRANSMISSION;
            }else{
                //wifi is enabled
                //get wifi RTT actively
                currentWifiRTT = NewWifiManager.getWifiRTT("123.57.209.174");
                Log.e("------","current Wifi RTT "+ currentWifiRTT);
                if(currentWifiRTT > Constant.WIFI_RTT_THRESHOLD){
                    //wifi's RTT reach the threshold
                    //switch and retry
                    Constant.DEFAULT_TRANSMISSION = Constant.MOBILE_DATA_TRANSMISSION;
                    //retry
                }else{
                    //set default transmission to wifi
                    Constant.DEFAULT_TRANSMISSION = Constant.WIFI_TRANSMISSION;
                    //retry
                }
            }
        }
    }
}

