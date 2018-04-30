package com.compile.navigation;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMap.InfoWindowAdapter;
import com.amap.api.maps.AMap.OnMarkerClickListener;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.busline.BusLineItem;
import com.amap.api.services.busline.BusLineQuery;
import com.amap.api.services.busline.BusLineQuery.SearchType;
import com.amap.api.services.busline.BusLineResult;
import com.amap.api.services.busline.BusLineSearch;
import com.amap.api.services.busline.BusLineSearch.OnBusLineSearchListener;
import com.amap.api.services.busline.BusStationItem;
import com.amap.api.services.core.AMapException;
import com.compile.voice.TTSUtils;
import com.iflytek.cloud.SpeechUtility;

import java.util.ArrayList;
import java.util.List;

/**
 * AMapV2地图中简单介绍公交线路搜索
 */
public class BuslineActivity extends Activity implements OnMarkerClickListener,
        InfoWindowAdapter, OnItemSelectedListener, OnBusLineSearchListener,
        OnClickListener, AMapLocationListener {
    private static final double EARTH_RADIUS = 6378137.0;
    private static final String TAG = "BuslineActivity";
    private EditText distanceEditText;
    private float zuixiaojuli = 10000;
    private int zuixiao = 0;
    private double juli;
    private AMap aMap;
    private MapView mapView;
    private ProgressDialog progDialog = null;// 进度框
    private EditText searchName;// 输入公交线路名称
    private Spinner selectCity;// 选择城市下拉列表
    private String[] itemCitys = {"北京-010", "郑州-0371", "上海-021"};
    private String cityCode = "";// 城市区号
    private int currentpage = 0;// 公交搜索当前页，第一页从0开始
    private BusLineResult busLineResult;// 公交线路搜索返回的结果
    private List<BusLineItem> lineItems = null;// 公交线路搜索返回的busline
    private BusLineQuery busLineQuery;// 公交线路查询的查询类

    private BusLineSearch busLineSearch;// 公交线路列表查询
    private AMapLocationClient aMapLocationClient;
    private AMapLocationClientOption aMapLocationClientOption;
    private MyLocationStyle myLocationStyle;
    private List<BusLocationModel> busLocationList;
    private List<BusStationItem> mBusStations;
    private String busStationName = "a";

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.busline_activity);
        /*
         * 设置离线地图存储目录，在下载离线地图或初始化地图设置;
         * 使用过程中可自行设置, 若自行设置了离线地图存储的路径，
         * 则需要在离线地图下载和使用地图页面都进行路径设置
         * */
        //Demo中为了其他界面可以使用下载的离线地图，使用默认位置存储，屏蔽了自定义设置
//        MapsInitializer.sdcardDir =OffLineMapUtils.getSdCacheDir(this);
        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(bundle);// 此方法必须重写
        init();
    }

    /**
     * 初始化AMap对象
     */
    @SuppressLint("InlinedApi")
    private void init() {
        if (aMap == null) {
            aMap = mapView.getMap();
            setUpMap();
        }
        aMapLocationClient = new AMapLocationClient(this);
        aMapLocationClientOption = new AMapLocationClientOption();
        aMapLocationClient.setLocationListener(this);
        aMapLocationClientOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Battery_Saving);
        aMapLocationClientOption.setInterval(2000);
        myLocationStyle = new MyLocationStyle();
        //初始化定位蓝点样式类
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);
        //连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
        myLocationStyle.interval(2000); //设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
        myLocationStyle.showMyLocation(true);
        aMap.setMyLocationStyle(myLocationStyle);//设置定位蓝点的Style
        aMap.getUiSettings().setMyLocationButtonEnabled(true);//设置默认定位按钮是否显示，非必需设置。
        aMap.setMyLocationEnabled(true);// 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。
        aMapLocationClient.setLocationOption(aMapLocationClientOption);
        aMapLocationClient.startLocation();
        Button searchByName = (Button) findViewById(R.id.searchbyname);
        searchByName.setOnClickListener(this);
        selectCity = (Spinner) findViewById(R.id.cityName);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, itemCitys);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        selectCity.setAdapter(adapter);
        selectCity.setPrompt("请选择城市：");
        selectCity.setOnItemSelectedListener(this);
        searchName = (EditText) findViewById(R.id.busName);
        busLocationList = new ArrayList<>();
        mBusStations = new ArrayList<>();
        distanceEditText = (EditText) findViewById(R.id.distanceEditText);
    }

    /**
     * 设置marker的监听和信息窗口的监听
     */
    private void setUpMap() {
        aMap.setOnMarkerClickListener(this);
        aMap.setInfoWindowAdapter(this);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        SpeechUtility.createUtility(BuslineActivity.this, "appid=5ad5ee1e");//=号后面写自己应用的APPID
        TTSUtils.getInstance().speak(busStationName + "开始公交站点播报");
        mapView.onResume();
        aMapLocationClient.startLocation();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        busStationName = "";
        mapView.onDestroy();
        if (null != aMapLocationClient) {
            aMapLocationClient.onDestroy();
        }
    }

    /**
     * 公交线路搜索
     */
    public void searchLine() {
        currentpage = 0;// 第一页默认从0开始
        showProgressDialog();
        String search = searchName.getText().toString().trim();
        if ("".equals(search)) {
            search = "641";
            searchName.setText(search);
        }
        busLineQuery = new BusLineQuery(search, SearchType.BY_LINE_NAME,
                cityCode);// 第一个参数表示公交线路名，第二个参数表示公交线路查询，第三个参数表示所在城市名或者城市区号
        busLineQuery.setPageSize(10);// 设置每页返回多少条数据
        busLineQuery.setPageNumber(currentpage);// 设置查询第几页，第一页从0开始算起
        busLineSearch = new BusLineSearch(this, busLineQuery);// 设置条件
        busLineSearch.setOnBusLineSearchListener(this);// 设置查询结果的监听
        busLineSearch.searchBusLineAsyn();// 异步查询公交线路名称
        // 公交站点搜索事例
        /*
         * BusStationQuery query = new BusStationQuery(search,cityCode);
		 * query.setPageSize(10); query.setPageNumber(currentpage);
		 * BusStationSearch busStationSearch = new BusStationSearch(this,query);
		 * busStationSearch.setOnBusStationSearchListener(this);
		 * busStationSearch.searchBusStationAsyn();
		 */
    }

    /**
     * 显示进度框
     */
    private void showProgressDialog() {
        if (progDialog == null)
            progDialog = new ProgressDialog(this);
        progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progDialog.setIndeterminate(false);
        progDialog.setCancelable(true);
        progDialog.setMessage("正在搜索:\n");
        progDialog.show();
    }

    /**
     * 隐藏进度框
     */
    private void dissmissProgressDialog() {
        if (progDialog != null) {
            progDialog.dismiss();
        }
    }

    /**
     * 提供一个给默认信息窗口定制内容的方法
     */
    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }

    /**
     * 提供一个个性化定制信息窗口的方法
     */
    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

    /**
     * 点击marker回调函数
     */
    @Override
    public boolean onMarkerClick(Marker marker) {
        return true;// 点击marker时把此marker显示在地图中心点
    }

    /**
     * 选择城市
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position,
                               long id) {
        String cityString = itemCitys[position];
        cityCode = cityString.substring(cityString.indexOf("-") + 1);
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        cityCode = "010";
    }

    /**
     * 公交线路搜索返回的结果显示在dialog中
     */
    public void showResultList(List<BusLineItem> busLineItems) {
        BusLineDialog busLineDialog = new BusLineDialog(this, busLineItems);
        busLineDialog.onListItemClicklistener(new OnListItemlistener() {
            @Override
            public void onListItemClick(BusLineDialog dialog,
                                        final BusLineItem item) {
                showProgressDialog();

                String lineId = item.getBusLineId();// 得到当前点击item公交线路id
                busLineQuery = new BusLineQuery(lineId, SearchType.BY_LINE_ID,
                        cityCode);// 第一个参数表示公交线路id，第二个参数表示公交线路id查询，第三个参数表示所在城市名或者城市区号
                BusLineSearch busLineSearch = new BusLineSearch(
                        BuslineActivity.this, busLineQuery);
                busLineSearch.setOnBusLineSearchListener(BuslineActivity.this);
                busLineSearch.searchBusLineAsyn();// 异步查询公交线路id
            }
        });
        busLineDialog.show();

    }

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (null != aMapLocation) {
            if (0 == aMapLocation.getErrorCode()) {
                if (!TextUtils.isEmpty(distanceEditText.getText().toString())) {
                    juli = Double.parseDouble(distanceEditText.getText().toString());
                } else {
                    juli = 30;
                }
                aMapLocation.getLocationType();
                double currentLatitude = aMapLocation.getLatitude();
                double currentLongitude = aMapLocation.getLongitude();
                aMapLocation.getAccuracy();
                cityCode = aMapLocation.getCityCode();
                LatLng startLatLng = new LatLng(currentLatitude, currentLongitude);
                if (mBusStations != null && mBusStations.size() > 0) {
                    isArriveStation(mBusStations);
                    for (int i = 0; i < mBusStations.size(); i++) {
                        LatLng endLatLng = new LatLng(mBusStations.get(i).getLatLonPoint().getLatitude(), mBusStations.get(i).getLatLonPoint().getLongitude());
                        float aa = AMapUtils.calculateLineDistance(startLatLng, endLatLng);
                        if (aa < zuixiaojuli) {
                            zuixiaojuli = aa;
                            zuixiao = i;
                        }

                        double busStationLatitude = mBusStations.get(i).getLatLonPoint().getLatitude();
                        double busStationLongitude = mBusStations.get(i).getLatLonPoint().getLongitude();
                        double bb = getDistance(currentLongitude, currentLatitude, busStationLongitude, busStationLatitude);
//                        bobao();
                        if (getDistance(currentLongitude, currentLatitude, busStationLongitude, busStationLatitude) < juli) {
                            Log.d(TAG, "onLocationChanged: 到了" + mBusStations.get(i).getBusStationName());
                            if (mBusStations.get(zuixiao).getBusStationName() != busStationName) {
                                Toast.makeText(BuslineActivity.this, mBusStations.get(i).getBusStationName(), Toast.LENGTH_SHORT).show();
                                busStationName = mBusStations.get(zuixiao).getBusStationName();
                                if (!TextUtils.isEmpty(busStationName)) {
                                    SpeechUtility.createUtility(BuslineActivity.this, "appid=5ad5ee1e");//=号后面写自己应用的APPID
                                    TTSUtils.getInstance().speak(busStationName + "站到了");
                                    TTSUtils.getInstance().speak(busStationName + "站到了");
                                    busStationName=mBusStations.get(zuixiao).getBusStationName();
                                }
                            }
                        }


                    }


                }

                Log.d(TAG, "onLocationChanged: 获取位置成功");
            } else {
                //显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。
                Log.e("AmapError", "location Error, ErrCode:"
                        + aMapLocation.getErrorCode() + ", errInfo:"
                        + aMapLocation.getErrorInfo());
                Log.d(TAG, "onLocationChanged: 获取位置失败");
            }
        }


    }

    private void bobao() {
        if (zuixiaojuli < juli) {
            if (!TextUtils.isEmpty(mBusStations.get(zuixiao).getBusStationName()) && busStationName != mBusStations.get(zuixiao).getBusStationName()) {
                for (int m = 0; m < 2; m++) {
                    SpeechUtility.createUtility(BuslineActivity.this, "appid=5ad5ee1e");//=号后面写自己应用的APPID
                    TTSUtils.getInstance().speak(busStationName + "站到了");
                    Toast.makeText(BuslineActivity.this, mBusStations.get(zuixiao).getBusStationName(), Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "onLocationChanged: 到了" + mBusStations.get(zuixiao).getBusStationName());
                }
                busStationName = mBusStations.get(zuixiao).getBusStationName();

            }
        }
    }

    // 返回单位是米
    public static double getDistance(double longitude1, double latitude1, double longitude2, double latitude2) {
        double Lat1 = rad(latitude1);
        double Lat2 = rad(latitude2);
        double a = Lat1 - Lat2;
        double b = rad(longitude1) - rad(longitude2);
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2)
                + Math.cos(Lat1) * Math.cos(Lat2)
                * Math.pow(Math.sin(b / 2), 2)));
        s = s * EARTH_RADIUS;
        s = Math.round(s * 10000) / 10000;
        return s;
    }

    private static double rad(double d) {
        return d * Math.PI / 180.0;
    }


    private void isArriveStation(List<BusStationItem> mBusStations) {
    }

    /**
     * BusLineDialog ListView 选项点击回调
     */
    interface OnListItemlistener {
        public void onListItemClick(BusLineDialog dialog, BusLineItem item);
    }

    /**
     * 所有公交线路显示页面
     */
    class BusLineDialog extends Dialog implements OnClickListener {

        private List<BusLineItem> busLineItems;
        private BusLineAdapter busLineAdapter;
        private Button preButton, nextButton;
        private ListView listView;
        protected OnListItemlistener onListItemlistener;

        public BusLineDialog(Context context, int theme) {
            super(context, theme);
        }

        public void onListItemClicklistener(
                OnListItemlistener onListItemlistener) {
            this.onListItemlistener = onListItemlistener;

        }

        public BusLineDialog(Context context, List<BusLineItem> busLineItems) {
            this(context, android.R.style.Theme_NoTitleBar);
            this.busLineItems = busLineItems;
            busLineAdapter = new BusLineAdapter(context, busLineItems);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.busline_dialog);
            preButton = (Button) findViewById(R.id.preButton);
            nextButton = (Button) findViewById(R.id.nextButton);
            listView = (ListView) findViewById(R.id.listview);
            listView.setAdapter(busLineAdapter);
            listView.setOnItemClickListener(new OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1,
                                        int arg2, long arg3) {
                    onListItemlistener.onListItemClick(BusLineDialog.this,
                            busLineItems.get(arg2));
                    dismiss();

                }
            });
            preButton.setOnClickListener(this);
            nextButton.setOnClickListener(this);
            if (currentpage <= 0) {
                preButton.setEnabled(false);
            }
            if (currentpage >= busLineResult.getPageCount() - 1) {
                nextButton.setEnabled(false);
            }

        }

        @Override
        public void onClick(View v) {
            this.dismiss();
            if (v.equals(preButton)) {
                currentpage--;
            } else if (v.equals(nextButton)) {
                currentpage++;
            }
            showProgressDialog();
            busLineQuery.setPageNumber(currentpage);// 设置公交查询第几页
            busLineSearch.setOnBusLineSearchListener(BuslineActivity.this);
            busLineSearch.searchBusLineAsyn();// 异步查询公交线路名称
        }

    }


    /**
     * 公交线路查询结果回调
     */
    @Override
    public void onBusLineSearched(BusLineResult result, int rCode) {
        dissmissProgressDialog();
        if (rCode == AMapException.CODE_AMAP_SUCCESS) {
            if (result != null && result.getQuery() != null
                    && result.getQuery().equals(busLineQuery)) {
                if (result.getQuery().getCategory() == SearchType.BY_LINE_NAME) {
                    if (result.getPageCount() > 0
                            && result.getBusLines() != null
                            && result.getBusLines().size() > 0) {
                        busLineResult = result;
                        lineItems = result.getBusLines();
                        if (lineItems != null) {
                            showResultList(lineItems);
                        }
                    }
                } else if (result.getQuery().getCategory() == SearchType.BY_LINE_ID) {
                    aMap.clear();// 清理地图上的marker
                    busLineResult = result;
                    lineItems = busLineResult.getBusLines();
                    if (lineItems != null && lineItems.size() > 0) {
                        BusLineOverlay busLineOverlay = new BusLineOverlay(this,
                                aMap, lineItems.get(0));
                        mBusStations.clear();
                        mBusStations = lineItems.get(0).getBusStations();
                        busLineOverlay.removeFromMap();
//						BusStationName: 江汉路公交站 LatLonPoint: 30.19602,120.216431 BusLines:  CityCode: null AdCode: null
                        busLineOverlay.addToMap();
                        busLineOverlay.zoomToSpan();
                    }
                }
            } else {
                Toast.makeText(this, "对不起，没有搜索到相关数据！", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, rCode + "对不起，没有搜索到相关数据！", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 查询公交线路
     */
    @Override
    public void onClick(View v) {
        searchLine();
    }
}
