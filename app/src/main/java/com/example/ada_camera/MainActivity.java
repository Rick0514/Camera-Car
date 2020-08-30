package com.example.ada_camera;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleMtuChangedCallback;
import com.clj.fastble.callback.BleRssiCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.BleScanRuleConfig;
import com.example.ada_camera.Permissions.OnPermission;
import com.example.ada_camera.Permissions.PermissionApply;
import com.example.ada_camera.activity.ActivityManager;
import com.example.ada_camera.adapter.DeviceAdapter;
import com.example.ada_camera.comm.ObserverManager;
import com.example.ada_camera.operation.OperationActivity;

import java.util.List;
import java.util.UUID;

public class MainActivity extends BaseActivity implements View.OnClickListener, OnPermission {

    private static final String TAG = MainActivity.class.getSimpleName();

    private int REQUEST_CODE_OPEN_GPS = 1;
    private int REQUEST_CODE_OPEN_BLUETOOTH = 2;

    private boolean scan_state = false;

    private LinearLayout layout_setting;
    private Button btn_scan, up_info;
    private EditText et_name, et_mac, et_uuid;
    private Switch sw_auto;
    private ImageView img_loading, blue_settings;

    private Animation operatingAnim;
    private DeviceAdapter mDeviceAdapter;
    private ProgressDialog progressDialog;

    private BleDevice getBledevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

        //添加到活动管理器
        ActivityManager.getInstance().addActivity(this);
        //实例化接口
        setOnPermission(this);

        //初始化蓝牙
        BleManager.getInstance().init(getApplication());
        BleManager.getInstance()
                .enableLog(true)
                .setReConnectCount(1, 5000)
                .setConnectOverTime(20000)
                .setOperateTimeout(5000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        showConnectedDevice();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BleManager.getInstance().disconnectAllDevice();
        BleManager.getInstance().destroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_scan: {
                if (scan_state == false) {
                    requestPermission(PermissionApply.PERMISSION_BLUETOOTH,
                            PermissionApply.BLUETOOTH_CODE);
                } else if (scan_state) {
                    btn_scan.setBackgroundResource(R.drawable.ic_scan);
                    BleManager.getInstance().cancelScan();
                    scan_state = false;
                }
                break;
            }
            case R.id.blue_settings: {
                PopupMenu popupMenu = new PopupMenu(this, v);
                MenuInflater inflater = popupMenu.getMenuInflater();
                inflater.inflate(R.menu.main_menu, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()){
                            case R.id.help:
                                Intent intent = new Intent(MainActivity.this, HelpActivity.class);
                                startActivity(intent);
                                break;

                            case R.id.settings:
                                if (layout_setting.getVisibility() == View.VISIBLE) {
                                    layout_setting.setVisibility(View.GONE);
                                } else {
                                    layout_setting.setVisibility(View.VISIBLE);
                                }
                                break;
                        }
                        return false;
                    }
                });
                popupMenu.show();
                break;
            }
            case R.id.up:
                layout_setting.setVisibility(View.GONE);
                break;
        }
    }

    private void initView() {
        blue_settings = (ImageView) findViewById(R.id.blue_settings);
        blue_settings.setOnClickListener(this);
        btn_scan = (Button) findViewById(R.id.btn_scan);
        btn_scan.setOnClickListener(this);
        up_info = findViewById(R.id.up);
        up_info.setOnClickListener(this);

        et_name = (EditText) findViewById(R.id.et_name);
        et_mac = (EditText) findViewById(R.id.et_mac);
        et_uuid = (EditText) findViewById(R.id.et_uuid);
        sw_auto = (Switch) findViewById(R.id.sw_auto);

        layout_setting = findViewById(R.id.layout_setting);
        layout_setting.setVisibility(View.GONE);
        img_loading = findViewById(R.id.img_loading);
        operatingAnim = AnimationUtils.loadAnimation(this, R.anim.rotate);
        operatingAnim.setInterpolator(new LinearInterpolator());
        progressDialog = new ProgressDialog(this);

        mDeviceAdapter = new DeviceAdapter(this);
        mDeviceAdapter.setOnDeviceClickListener(new DeviceAdapter.OnDeviceClickListener() {
            @Override
            public void onConnect(BleDevice bleDevice) {
                if (!BleManager.getInstance().isConnected(bleDevice)) {
                    BleManager.getInstance().cancelScan();
                    connect(bleDevice);
                }
            }

            @Override
            public void onDisConnect(final BleDevice bleDevice) {
                if (BleManager.getInstance().isConnected(bleDevice)) {
                    BleManager.getInstance().disconnect(bleDevice);
                }
            }
            @Override
            public void onDetail(BleDevice bleDevice) {
                requestPermission(PermissionApply.PERMISSION_CAMERA,
                        PermissionApply.CAMERA_CODE);
                getBledevice = bleDevice;
            }
        });
        ListView listView_device = findViewById(R.id.list_device);
        listView_device.setAdapter(mDeviceAdapter);
    }

    private void showConnectedDevice() {
        List<BleDevice> deviceList = BleManager.getInstance().getAllConnectedDevice();
        mDeviceAdapter.clearConnectedDevice();
        for (BleDevice bleDevice : deviceList) {
            mDeviceAdapter.addDevice(bleDevice);
        }
        mDeviceAdapter.notifyDataSetChanged();
    }

    private void setScanRule() {
        String[] uuids;
        String str_uuid = et_uuid.getText().toString();
        if (TextUtils.isEmpty(str_uuid)) {
            uuids = null;
        } else {
            uuids = str_uuid.split(",");
        }
        UUID[] serviceUuids = null;
        if (uuids != null && uuids.length > 0) {
            serviceUuids = new UUID[uuids.length];
            for (int i = 0; i < uuids.length; i++) {
                String name = uuids[i];
                String[] components = name.split("-");
                if (components.length != 5) {
                    serviceUuids[i] = null;
                } else {
                    serviceUuids[i] = UUID.fromString(uuids[i]);
                }
            }
        }

        String[] names;
        String str_name = et_name.getText().toString();
        if (TextUtils.isEmpty(str_name)) {
            names = null;
        } else {
            names = str_name.split(",");
        }

        String mac = et_mac.getText().toString();
        boolean isAutoConnect = sw_auto.isChecked();
        BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()
                .setServiceUuids(serviceUuids)      // 只扫描指定的服务的设备，可选
                .setDeviceName(true, names)   // 只扫描指定广播名的设备，可选
                .setDeviceMac(mac)                  // 只扫描指定mac的设备，可选
                .setAutoConnect(isAutoConnect)      // 连接时的autoConnect参数，可选，默认false
                .setScanTimeOut(10000)              // 扫描超时时间，可选，默认10秒
                .build();
        BleManager.getInstance().initScanRule(scanRuleConfig);
    }

    private void startScan() {
        BleManager.getInstance().scan(new BleScanCallback() {
            @Override
            public void onScanStarted(boolean success) {
                mDeviceAdapter.clearScanDevice();
                mDeviceAdapter.notifyDataSetChanged();
                img_loading.startAnimation(operatingAnim);
                img_loading.setVisibility(View.VISIBLE);
                scan_state = true;
                btn_scan.setBackgroundResource(R.drawable.ic_stop);
            }

            @Override
            public void onLeScan(BleDevice bleDevice) {
                super.onLeScan(bleDevice);
            }
            @Override
            public void onScanning(BleDevice bleDevice) {
                mDeviceAdapter.addDevice(bleDevice);
                mDeviceAdapter.notifyDataSetChanged();
            }
            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                img_loading.clearAnimation();
                img_loading.setVisibility(View.INVISIBLE);
                scan_state = false;
                btn_scan.setBackgroundResource(R.drawable.ic_scan);
            }

        });
    }

    private void connect(final BleDevice bleDevice) {
        BleManager.getInstance().connect(bleDevice, new BleGattCallback() {
            @Override
            public void onStartConnect() {
                progressDialog.show();
            }
            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
                img_loading.clearAnimation();
                img_loading.setVisibility(View.INVISIBLE);
                scan_state = false;
                btn_scan.setBackgroundResource(R.drawable.ic_scan);
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, getString(R.string.connect_fail), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                scan_state = false;
                btn_scan.setBackgroundResource(R.drawable.ic_scan);
                progressDialog.dismiss();
                mDeviceAdapter.addDevice(bleDevice);
                mDeviceAdapter.notifyDataSetChanged();
            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice bleDevice, BluetoothGatt gatt, int status) {
                progressDialog.dismiss();
                scan_state = false;
                btn_scan.setBackgroundResource(R.drawable.ic_scan);
                mDeviceAdapter.removeDevice(bleDevice);
                mDeviceAdapter.notifyDataSetChanged();

                if (isActiveDisConnected) {
                    Toast.makeText(MainActivity.this, getString(R.string.active_disconnected), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, getString(R.string.disconnected), Toast.LENGTH_LONG).show();
                    ObserverManager.getInstance().notifyObserver(bleDevice);
                }

            }
        });
    }

    private void readRssi(BleDevice bleDevice) {
        BleManager.getInstance().readRssi(bleDevice, new BleRssiCallback() {
            @Override
            public void onRssiFailure(BleException exception) {
                Log.i(TAG, "onRssiFailure" + exception.toString());
            }

            @Override
            public void onRssiSuccess(int rssi) {
                Log.i(TAG, "onRssiSuccess: " + rssi);
            }
        });
    }

    private void setMtu(BleDevice bleDevice, int mtu) {
        BleManager.getInstance().setMtu(bleDevice, mtu, new BleMtuChangedCallback() {
            @Override
            public void onSetMTUFailure(BleException exception) {
                Log.i(TAG, "onsetMTUFailure" + exception.toString());
            }
            @Override
            public void onMtuChanged(int mtu) {
                Log.i(TAG, "onMtuChanged: " + mtu);
            }
        });
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void permissionSuccess(int requestCode) {
        if(requestCode == PermissionApply.BLUETOOTH_CODE){
            //请求打开蓝牙
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent,REQUEST_CODE_OPEN_BLUETOOTH);
        }else if(requestCode == PermissionApply.CAMERA_CODE){
            if (BleManager.getInstance().isConnected(getBledevice)) {
                Intent intent = new Intent(MainActivity.this, OperationActivity.class);
                intent.putExtra(OperationActivity.KEY_DATA, getBledevice);
                startActivity(intent);
            }
        }
    }
    // 申请权限失败后，不让用户取消，直接继续申请权限
    @Override
    public void permissionFail(int requestCode) {
        if(requestCode == PermissionApply.BLUETOOTH_CODE){
            new AlertDialog.Builder(this).setTitle(R.string.warning)
                    .setMessage(R.string.reason_for_bluetooth)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermission(PermissionApply.PERMISSION_BLUETOOTH,
                                    PermissionApply.BLUETOOTH_CODE);
                        }
                    })
                    .setNegativeButton(R.string.never, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(MainActivity.this, R.string.application_exit,Toast.LENGTH_LONG).show();
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            ActivityManager.getInstance().finishAllActivities();
                        }
                    }).show();
        }else if(requestCode == PermissionApply.CAMERA_CODE){
            new AlertDialog.Builder(this).setTitle(R.string.warning)
                    .setMessage(R.string.reason_for_camera)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermission(PermissionApply.PERMISSION_CAMERA,
                                    PermissionApply.CAMERA_CODE);
                        }
                    })
                    .setNegativeButton(R.string.never, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(MainActivity.this, R.string.application_exit,Toast.LENGTH_LONG).show();
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            ActivityManager.getInstance().finishAllActivities();
                        }
                    }).show();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OPEN_BLUETOOTH){
            // 进一步请求打开GPS
            if(!checkGPSIsOpen()){
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(intent, REQUEST_CODE_OPEN_GPS);
            }else if(!checkBluetoothIsOpen()){
                permissionFail(PermissionApply.BLUETOOTH_CODE);
            }else{
                scan_state = true;
                setScanRule();
                startScan();
            }
        }
        if (requestCode == REQUEST_CODE_OPEN_GPS) {
            if (checkGPSIsOpen() && checkBluetoothIsOpen()) {
                // 同时满足才能进一步扫描
                scan_state = true;
                setScanRule();
                startScan();
            }
            else{
                permissionFail(PermissionApply.BLUETOOTH_CODE);
            }
        }
    }
}
