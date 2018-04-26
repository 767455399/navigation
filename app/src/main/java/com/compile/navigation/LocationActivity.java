package com.compile.navigation;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.MyLocationStyle;

public class LocationActivity extends AppCompatActivity implements LocationSource, AMapLocationListener {
    private static final String TAG = "LocationActivity";
    public static final int ACCESS_FINE_LOCATION_PERMISSION=0;
    private static final String PACKAGE_URL_SCHEME = "package:";
    private MapView mapView;
    private AMap aMap;
    private MyLocationStyle myLocationStyle;
    OnLocationChangedListener mListener;
    AMapLocationClient mlocationClient;
    AMapLocationClientOption mLocationOption;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);
        requestPermissions();
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        init();
    }

    private void init() {
        //初始化地图控制器对象
        if (aMap == null) {
            aMap = mapView.getMap();
        }
        //显示实时交通情况
        aMap.setTrafficEnabled(true);
        //卫星地图模式
        aMap.setMapType(AMap.MAP_TYPE_NORMAL);
        aMap.setLocationSource(this);
        myLocationStyle = new MyLocationStyle();
        //初始化定位蓝点样式类
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);
        //连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
        myLocationStyle.interval(2000); //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
        myLocationStyle.showMyLocation(true);
        aMap.setMyLocationStyle(myLocationStyle);//设置定位蓝点的Style
        aMap.getUiSettings().setMyLocationButtonEnabled(true);//设置默认定位按钮是否显示，非必需设置。
        aMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        mlocationClient.startLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if(null!=mlocationClient){
            mlocationClient.onDestroy();
        }
    }

    /**
     * 激活定位
     *
     * @param listener
     */
    @Override
    public void activate(OnLocationChangedListener listener) {
        mListener = listener;
        if(null==mlocationClient){
            //初始化定位
            mlocationClient=new AMapLocationClient(this);
            //初始化定位参数
            mLocationOption=new AMapLocationClientOption();
            //设置定位回调监听
            mlocationClient.setLocationListener(this);
            //设置为高精度定位模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Device_Sensors);
            //设置定位参数
            mlocationClient.setLocationOption(mLocationOption);
            // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
            // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
            // 在定位结束后，在合适的生命周期调用onDestroy()方法
            // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
            mlocationClient.startLocation();
        }

    }

    /**
     * 停止定位
     */
    @Override
    public void deactivate() {
        mListener=null;
        if(mlocationClient!=null){
            mlocationClient.startLocation();
            mlocationClient.onDestroy();
        }
        mlocationClient=null;
    }

    private void requestPermissions() {
        if(ContextCompat.checkSelfPermission(LocationActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            /**
             * 如果权限未被禁止弹框则继续申请，并告知用途以及不允许权限的弊端。
             */
            if(!ActivityCompat.shouldShowRequestPermissionRationale(LocationActivity.this,Manifest.permission.ACCESS_FINE_LOCATION)){
                ActivityCompat.requestPermissions(LocationActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},ACCESS_FINE_LOCATION_PERMISSION);
            }else{
                /**
                 * 完全禁止无法再弹权限申请框，这是提示用户去设置里开启权限。
                 */
                AlertDialog dialog=new AlertDialog.Builder(LocationActivity.this)
                        .setTitle("提示")
                        .setMessage("去设置里开启定位权限")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.parse(PACKAGE_URL_SCHEME+getPackageName()));
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton("取消",null)
                        .create();
                dialog.show();
            }
        }
    }

    /**
     * 定位成功回调函数
     * @param aMapLocation
     */
    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if(mListener!=null&&aMapLocation!=null){
            if(aMapLocation!=null&&aMapLocation.getErrorCode()==0) {
                //显示系统小蓝点
                mListener.onLocationChanged(aMapLocation);
            }else{
                Log.d(TAG, "onLocationChanged: 定位失败"+aMapLocation.getErrorCode()+":"+aMapLocation.getErrorInfo());
            }
        }

    }
}

