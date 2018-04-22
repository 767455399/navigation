package com.compile.navigation;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.amap.api.navi.AMapNaviView;

public class MapActivity extends AppCompatActivity {
    public static final int ACCESS_FINE_LOCATION_PERMISSION=0;
    private static final String PACKAGE_URL_SCHEME = "package:";
    private AMapNaviView aMapNaviView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        requestPermissions();
        initView();
    }

    private void initView() {
        aMapNaviView=(AMapNaviView)findViewById(R.id.aMapNaviView);
    }


    private void requestPermissions() {
        if(ContextCompat.checkSelfPermission(MapActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            /**
             * 如果权限未被禁止弹框则继续申请，并告知用途以及不允许权限的弊端。
             */
            if(!ActivityCompat.shouldShowRequestPermissionRationale(MapActivity.this,Manifest.permission.ACCESS_FINE_LOCATION)){
                ActivityCompat.requestPermissions(MapActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},ACCESS_FINE_LOCATION_PERMISSION);
            }else{
                /**
                 * 完全禁止无法再弹权限申请框，这是提示用户去设置里开启权限。
                 */
                AlertDialog dialog=new AlertDialog.Builder(MapActivity.this)
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
}
