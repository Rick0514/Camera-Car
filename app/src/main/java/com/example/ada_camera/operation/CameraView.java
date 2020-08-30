package com.example.ada_camera.operation;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.os.Handler;
import android.widget.Toast;

import com.example.ada_camera.R;

import java.io.IOException;
import java.util.List;
import java.util.Locale;


public class CameraView extends SurfaceView implements SurfaceHolder.Callback,
        Camera.PreviewCallback {

    private Context context;
    private Camera camera;
    private Camera.Parameters camerapara;
    private FaceView faceView;
    private boolean curdetect, lastdetect;
    private StateChangeListener listener;

    private int hascount;
    private int nocount;
    private boolean startcount;

    private String receiveNotify;
    private boolean savepicture;
    public Handler mHandler;    //通知保存结束

    // TTS
    private TextToSpeech textToSpeech;
    private byte[] data;

    //拍照音
    public SoundPool kaca = new SoundPool(1, AudioManager.STREAM_SYSTEM, 0);
    private int kacaid;

    //-------------构造 初始化-----------------------

    public CameraView(Context context) {
        super(context);
        init(context);
    }

    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(final Context context) {
        this.context = context;
        curdetect = false;
        lastdetect = false;
        hascount = 0;
        nocount = 0;
        startcount = false;
        receiveNotify = "";
        savepicture = true;
        kacaid = kaca.load(context, R.raw.kaca, 1);

        getHolder().addCallback(this);

        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg){
                if(msg.what == 1){
                    kaca.play(kacaid, 1,1,1,0,1);
                    savepicture = true;
                    Toast.makeText(context, "已保存至相册", Toast.LENGTH_LONG).show();
                }else if(msg.what == 2){
                    savepicture = true;
                    Toast.makeText(context, "保存失败", Toast.LENGTH_LONG).show();
                }
            }
        };

        textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
            @Override
            public void onInit(int status) {
                if(status == textToSpeech.SUCCESS){
                    textToSpeech.setLanguage(Locale.ENGLISH);
                    textToSpeech.setPitch(1.5f);
                    textToSpeech.setOnUtteranceCompletedListener(new TextToSpeech.OnUtteranceCompletedListener() {
                        @Override
                        public void onUtteranceCompleted(String utteranceId) {
                            // 新建一个保存图片的线程
                            if(utteranceId.equals("photo")){
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Message msg = Message.obtain();

                                        if(TakePictures.getInstance().saveImage(context, data, camerapara.getPreviewSize())){
                                            msg.what = 1;
                                        }else{
                                            msg.what = 2;
                                        }
                                        mHandler.sendMessage(msg);
                                    }
                                }).start();
                            }
                        }
                    });
                }
            }
        });

        }

    //-------------构造 初始化-----------------------

    public void setFaceView(FaceView faceView) {
        if (faceView != null) {
            this.faceView = faceView;
            faceView.setVisibility(View.VISIBLE);
        }
    }

    private void openCamera(){
        try{
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            initCameraPara(camera);
            camera.setPreviewCallback(this);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void initCameraPara(Camera mCamera){
        try{
            camerapara = mCamera.getParameters();
            camerapara.setPreviewFormat(ImageFormat.NV21);
            Camera.Size bestSize = getBestSize(getWidth(), getHeight(), camerapara.getSupportedPreviewSizes());
            camerapara.setPreviewSize(bestSize.width, bestSize.height);

            if(isFocusSupported(camerapara)){
                camerapara.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
            camera.setParameters(camerapara);

        }catch (Exception e){
            e.printStackTrace();
            Log.w("initCameraPara", "failed");
        }
    }

    private Camera.Size getBestSize(int width, int height, List<Camera.Size> camSize){
        Camera.Size bestSize = null;
        double objrate = (Double.valueOf(height) / width);
        double minrate = objrate;
        double supportrate;
        for(Camera.Size size : camSize){
            supportrate = Double.valueOf(size.width) / size.height;
            String msg = "{" + size.width + "} * {" + size.height + "} :" + supportrate;
            Log.i("getBestSize:support", msg);
        }

        for (Camera.Size size : camSize){
            if((size.width == height) && (size.height == width)){
                bestSize = size;
                break;
            }
            supportrate = (double) size.width / size.height;
            double diff = Math.abs((supportrate - objrate));
            if(diff < minrate){
                minrate = diff;
                bestSize = size;
            }
        }
        Log.i("getBestSize:", "obj:" + width + "x" + height + "rate: " + objrate);
        Log.i("getBestSize:", "best:" + bestSize.width + "x" + bestSize.height);
        return bestSize;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        openCamera();
        if (camera != null) {
            try {
                listener.showNoInfo();
                camera.setDisplayOrientation(90);
                camera.setPreviewDisplay(holder);
            }catch (IOException e){
                e.printStackTrace();
            }

        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (camera != null) {
            camera.setPreviewCallback(this);
            camera.startPreview();
            camera.startFaceDetection();
            camera.setFaceDetectionListener(new Camera.FaceDetectionListener() {
                @Override
                public void onFaceDetection(Camera.Face[] faces, Camera camera) {
                    faceView.setFaces(faces);
                    curdetect = true;
                    startcount = false;
                    hascount ++;
                    if(hascount >= 5){
                        listener.showHasInfo();
                        hascount = 0;
                    }

                }
            });
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (camera != null) {
            try {
                camera.stopPreview();
                camera.setPreviewDisplay(null);
                camera.setPreviewCallback(null);
                camera.release();
                camera = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onPreviewFrame(final byte[] data, Camera camera) {
        if(camera != null){
            try{
                if(receiveNotify.equals("aa") && savepicture) {     //当收到拍照指令且已保存
                    receiveNotify = "";
                    savepicture = false;
                    this.data = data;
                    textToSpeech.speak("Ready! Three! Two! One!",
                            TextToSpeech.QUEUE_FLUSH, null, "photo");
                }

                if(listener != null) {
                    if (curdetect != lastdetect) {
                        if(!curdetect){
                            startcount = true;
                            nocount = 0;
                        }
                    }
                    lastdetect = curdetect;
                    curdetect = false;
                    if(startcount){
                        nocount ++;
                        if((nocount > 3) && (curdetect == false)){
                            listener.showNoInfo();
                            startcount = false;
                            nocount = 0;
                        }
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public static boolean isFocusSupported(Camera.Parameters params) {
        List<String> modes = params.getSupportedFocusModes();
        return modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
    }

    // 监听状态变化
    public void setStateChangeListener(StateChangeListener listener){
        this.listener = listener;
    }

    public void setReceiveNotify(String str){
        receiveNotify = str;
    }

    public boolean get_savepic(){
        return savepicture;
    }

}