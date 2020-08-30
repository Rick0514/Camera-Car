package com.example.ada_camera.Permissions;

import android.Manifest;

// 权限提供类，管理全局权限
public class PermissionApply {
    //蓝牙操作权限
    public static final int BLUETOOTH_CODE = 1;
    public static final String[] PERMISSION_BLUETOOTH = new String[]{
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    //相机操作权限
    public static final int CAMERA_CODE = 2;
    public static final String[] PERMISSION_CAMERA = new String[]
            {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
            };
}
