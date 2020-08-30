package com.example.ada_camera.Permissions;

public interface OnPermission {
    void permissionSuccess(int requestCode);
    void permissionFail(int requestCode);
}
