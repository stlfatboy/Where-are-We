package com.sp.wherearewe;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.TextView;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.TextureMapView;
import com.baidu.mapapi.model.LatLng;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by stlfa on 11/15/2016.
 */

public class ChatMapBetaActivity extends Activity {

    LocationClient mLocClient;
    public MyLocationListenner myListener = new MyLocationListenner();
    private TextureMapView mMapView;
    private BaiduMap mBaiduMap;

    private static double Long, Glat, r;
    private static double locDirection;
    boolean isFirstLoc = true;

    EditText ip;
    EditText msg;
    TextView text;
    Socket socket;
    BufferedReader reader = null;
    BufferedWriter writer= null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrollmap_beta);
        mMapView = (TextureMapView) findViewById(R.id.mTexturemap);
        mBaiduMap = mMapView.getMap();

        ip = (EditText) findViewById(R.id.ip);
        msg = (EditText) findViewById(R.id.msg);
        text = (TextView) findViewById(R.id.text);

        TabHost tabHost = (TabHost) findViewById(R.id.tabhost);
        // 如果没有继承TabActivity时，通过该种方法加载启动tabHost
        tabHost.setup();
        tabHost.addTab(tabHost.newTabSpec("texturehint").setIndicator("聊天室",
                null).setContent(R.id.chatroom));

        tabHost.addTab(tabHost.newTabSpec("mTexturemap").setIndicator("地图")
                .setContent(R.id.mTexturemap));


        mBaiduMap.setMyLocationEnabled(true);

        InitLocation();

        mLocClient.start();

        //initOverlay();
    }

    public void InitLocation(){
        // 定位初始化
        mLocClient = new LocationClient(this);
        mLocClient.registerLocationListener(myListener);
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(1000);
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setNeedDeviceDirect(true);//可选，设置是否需要设备方向结果
        option.setLocationNotify(true);
        option.setIgnoreKillProcess(false);
        //可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        mLocClient.setLocOption(option);
    }

    public class MyLocationListenner implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            // map view 销毁后不在处理新接收的位置
            if (location == null || mMapView == null) {
                return;
            }
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(location.getDirection()).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();

            Long = location.getLongitude();
            Glat = location.getLatitude();
            r = location.getRadius();
            /*TextView geo = (TextView) findViewById(R.id.GeoText);
            TextView geo2 = (TextView) findViewById(R.id.GeoText2);
            geo2.setText(Glat+", "+Long);
            geo.setText("卫星数："+location.getSatelliteNumber()+"精度："+r);*/

            mBaiduMap.setMyLocationData(locData);
            if (isFirstLoc) {
                isFirstLoc = false;
                LatLng ll = new LatLng(location.getLatitude(),
                        location.getLongitude());
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(ll).zoom(18.0f);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
            }
        }

        public void onReceivePoi(BDLocation poiLocation) {
        }
    }



    public void ConnectServerButtonClick(View v){
        final String ipaddr = ip.getText().toString();
        AsyncTask<Void, String, Void> read = new AsyncTask<Void, String, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                boolean flag = false;
                try {
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(ipaddr, 25566), 5000);
                    //设置连接端口，超时5s
                    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                } catch (IOException e) {
                    e.printStackTrace();
                    flag = true;
                }

                if (!flag) {
                    try {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            publishProgress(line);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else {
                    publishProgress("连接服务器超时");
                }

                return null;
            }

            @Override
            protected void onProgressUpdate(String... values) {
                text.append(values[0]+"\n");
                super.onProgressUpdate(values);
            }
        };
        read.execute();

    }

    public void MsgSendButtonClick(View v){
        try {
            writer.write(msg.getText().toString()+"\n");
            writer.flush();
            text.append("本机:"+msg.getText().toString()+'\n');
            msg.setText("");
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    @Override
    protected void onPause() {
        super.onPause();
        // activity 暂停时同时暂停地图控件
        mMapView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // activity 恢复时同时恢复地图控件
        mMapView.onResume();
    }

    @Override
    protected void onDestroy() {

        // activity 销毁时同时销毁地图控件
        mMapView.onDestroy();
        // 退出时销毁定位
        mLocClient.stop();
        // 关闭定位图层
        mBaiduMap.setMyLocationEnabled(false);
        super.onDestroy();
        // 回收 bitmap 资源
        /*bdA.recycle();
        bdB.recycle();
        bdC.recycle();*/
        super.onDestroy();
    }

}
