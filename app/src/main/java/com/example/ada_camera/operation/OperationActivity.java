package com.example.ada_camera.operation;


import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.example.ada_camera.BaseActivity;
import com.example.ada_camera.comm.Observer;
import com.example.ada_camera.R;
import com.example.ada_camera.comm.ObserverManager;

import java.util.UUID;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class OperationActivity extends BaseActivity implements Observer {

    public static final String KEY_DATA = "key_data";

    private BleDevice bleDevice;
    private BluetoothGattService bluetoothGattService;
    private BluetoothGattCharacteristic characteristic;

    private CameraView camView;
    private FaceView faceView;
    private TextView centerView;

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operation);
        initData();
        initView();
        ObserverManager.getInstance().addObserver(this);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                faceView.init_objRect();
            }
        }, 1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BleManager.getInstance().clearCharacterCallback(bleDevice);
        ObserverManager.getInstance().deleteObserver(this);
    }

    @Override
    public void disConnected(BleDevice device) {
        if (device != null && bleDevice != null && device.getKey().equals(bleDevice.getKey())) {
            finish();
        }
    }


    private void initView() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Drawable upArrow = ContextCompat.getDrawable(this, R.drawable.abc_ic_ab_back_material);
        upArrow.setColorFilter(getResources().getColor(R.color.colorWhite), PorterDuff.Mode.SRC_ATOP);
        getSupportActionBar().setHomeAsUpIndicator(upArrow);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        camView = (CameraView) findViewById(R.id.cam_view);
        faceView = (FaceView) findViewById(R.id.face_view);
        centerView = (TextView) findViewById(R.id.face_info);
        camView.setFaceView(faceView);
        try{
            camView.setStateChangeListener(new StateChangeListener() {
                @Override
                public void showHasInfo() {
                    PointF cpt = faceView.geterror();
                    @SuppressLint("DefaultLocale") String msg = "X:" + String.format("%.2f", cpt.x) + "   Y:" + String.format("%.2f", cpt.y);
                    centerView.setText(msg);
                    String str = String.format("%.2f", cpt.x) + " " + String.format("%.2f", cpt.y) + "a";
                    BleManager.getInstance().write_b(bleDevice,
                            bluetoothGattService.getUuid().toString(),
                            characteristic.getUuid().toString(),
                            str,
                            new BleWriteCallback(){
                                @Override
                                public void onWriteSuccess(int current, int total, byte[] justWrite) {

                                }
                                @Override
                                public void onWriteFailure(BleException exception) {

                                }
                            });
                }
                @Override
                public void showNoInfo() {
                    String msg = "b";
                    centerView.setText(msg);
                    BleManager.getInstance().write_b(bleDevice,
                            bluetoothGattService.getUuid().toString(),
                            characteristic.getUuid().toString(),
                            msg,
                            new BleWriteCallback() {
                                @Override
                                public void onWriteSuccess(int current, int total, byte[] justWrite) {

                                }
                                @Override
                                public void onWriteFailure(BleException exception) {

                                }
                            });
                }
            });

            BleManager.getInstance().notify(bleDevice,
                    bluetoothGattService.getUuid().toString(),
                    characteristic.getUuid().toString(),
                    new BleNotifyCallback() {
                        @Override
                        public void onNotifySuccess() {
                            Log.d("notifydata","success");
                        }

                        @Override
                        public void onNotifyFailure(BleException exception) {
                            Log.d("notifydata","failed");
                        }

                        @Override
                        public void onCharacteristicChanged(byte[] data) {
                            if(camView.get_savepic()) {
                                String msg = new String(data);
                                camView.setReceiveNotify(msg);
                                Log.d("notifydata", msg);
                            }
                        }
                    });

        }catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    private void initData() {
        UUID uuid;
        String struuid;
        bleDevice = getIntent().getParcelableExtra(KEY_DATA);
        BluetoothGatt gatt = BleManager.getInstance().getBluetoothGatt(bleDevice);
        for(BluetoothGattService service : gatt.getServices()){
            uuid = service.getUuid();
            struuid = uuid.toString();
            if(struuid.startsWith("0000ffe0")){
                bluetoothGattService = service;
                for(BluetoothGattCharacteristic cha : service.getCharacteristics()){
                    uuid = cha.getUuid();
                    struuid = uuid.toString();
                    if(struuid.startsWith("0000ffe1")){
                        characteristic = cha;
                        Log.d("linkblue",struuid);
                        break;
                    }
                }
                break;
            }
        }
        if((bluetoothGattService == null) || (characteristic == null)){
            Toast.makeText(this, "Not objected bluetooth", Toast.LENGTH_LONG).show();
            finish();
        }
        if (bleDevice == null)
            finish();
    }

}
