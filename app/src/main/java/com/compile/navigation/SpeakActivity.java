package com.compile.navigation;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;

public class SpeakActivity extends AppCompatActivity {
    private static final String TAG = "SpeakActivity";
    // 语音合成对象
    private SpeechSynthesizer mTts;
    // 引擎类型
    private String mEngineType = SpeechConstant.TYPE_CLOUD;
    // 默认发音人
    private String voicer = "xiaoyan";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }





    private void init() {
        //这行代码本应该在application里执行的。结果初始化失败导致SpeechSynthesizer为空。
        SpeechUtility.createUtility(SpeakActivity.this, "appid=5ad5ee1e");//=号后面写自己应用的APPID
        requestPermissions();
        //初始化合成对象
        mTts= SpeechSynthesizer.createSynthesizer(SpeakActivity.this,mTtsInitListener);
        setParam();
        String text="1234567890";
        if(TextUtils.isEmpty(text)){
            return;
        }
        int code=mTts.startSpeaking(text,mTtsListener);
        if(code!= ErrorCode.SUCCESS){
            Toast.makeText(SpeakActivity.this, "语音合成失败，错误码"+code, Toast.LENGTH_SHORT).show();
        }
    }

    private void requestPermissions(){
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int permission = ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if(permission!= PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,new String[]
                            {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.LOCATION_HARDWARE,Manifest.permission.READ_PHONE_STATE,
                                    Manifest.permission.WRITE_SETTINGS,Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.RECORD_AUDIO,Manifest.permission.READ_CONTACTS},0x0010);
                }

                if(permission != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,new String[] {
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION},0x0010);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void setParam() {
        if(mTts==null){
            Toast.makeText(this, "mTts为空", Toast.LENGTH_SHORT).show();
        }
        //清空参数
        mTts.setParameter(SpeechConstant.PARAMS,null);
        //根据合成引擎设置相应参数
        if(mEngineType.equals(SpeechConstant.TYPE_CLOUD)){
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
            //设置在线合成发音人
            mTts.setParameter(SpeechConstant.VOICE_NAME,voicer);
            //设置合成语速
            mTts.setParameter(SpeechConstant.SPEED,"50");
            //设置合成音调
            mTts.setParameter(SpeechConstant.PITCH,"50");
            //设置合成音量
            mTts.setParameter(SpeechConstant.VOLUME,"50");
        }else{
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
            // 设置本地合成发音人 voicer为空，默认通过语记界面指定发音人。
            mTts.setParameter(SpeechConstant.VOICE_NAME, "");
            //设置播放器音频流类型
            mTts.setParameter(SpeechConstant.STREAM_TYPE,  "3");
            // 设置播放合成音频打断音乐播放，默认为true
            mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");

            // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
            // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
            mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
            mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/tts.wav");
        }
    }

    /**
     * 初始化监听
     */
    private InitListener mTtsInitListener=new InitListener() {
        @Override
        public void onInit(int code) {
            if(code!= ErrorCode.SUCCESS){
                Toast.makeText(SpeakActivity.this, "初始化失败，错误码："+code, Toast.LENGTH_SHORT).show();
            }else{
                // 初始化成功，之后可以调用startSpeaking方法
                // 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
                // 正确的做法是将onCreate中的startSpeaking调用移至这里
            }

        }
    };

    /**
     * 合成回调监听
     */
    SynthesizerListener mTtsListener=new SynthesizerListener() {
        @Override
        public void onSpeakBegin() {
            Toast.makeText(SpeakActivity.this, "开始播放", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos, String info) {
            Log.d(TAG, "onBufferProgress:合成进度 "+percent);

        }

        @Override
        public void onSpeakPaused() {
            Log.d(TAG, "onSpeakPaused: 暂停播放");

        }

        @Override
        public void onSpeakResumed() {
            Log.d(TAG, "onSpeakResumed: 继续播放");

        }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
            Log.d(TAG, "onSpeakProgress: 播放进度"+percent);

        }

        @Override
        public void onCompleted(SpeechError speechError) {
            Log.d(TAG, "onCompleted: 播放完成");

        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {

        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(null!=mTts){
            //退出时停止播放，释放资源
            mTts.stopSpeaking();
            mTts.destroy();
        }
    }
}
