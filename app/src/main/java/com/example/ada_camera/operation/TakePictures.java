package com.example.ada_camera.operation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class TakePictures {
    private String storepath;
    private String state;
    private File appDir = null;

    private static final TakePictures takepictures = new TakePictures();

    private TakePictures(){
        state = Environment.getExternalStorageState();
        storepath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Ada_Camera";
        if(permission()){
            appDir = new File(storepath);
            Log.d("making","mdiring");
            if(!appDir.exists()){
                appDir.mkdir();
                Log.d("making","mdir");
            }
        }
    }

    public static TakePictures getInstance(){
        return takepictures;
    }

    private boolean permission(){
        if(!state.equals(Environment.MEDIA_MOUNTED)){
            return false;
        }
        return true;
    }

    public boolean saveImage(Context context, byte[] bytes, Camera.Size size) {
        if(permission()){
            String fileName = System.currentTimeMillis() + ".jpg";
            File file = new File(appDir, fileName);
            try{
                FileOutputStream fos = new FileOutputStream(file);
                //通过io流的方式来压缩保存图片
                final byte[] byteArray = yuv2Jpeg(bytes, size.width, size.height);
                Bitmap bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                bmp = rotateBitmap(bmp, 90);

                boolean isSuccess = bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.flush();
                fos.close();

                Uri uri = Uri.fromFile(file);
                context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));

                if(isSuccess){
                    return true;
                }else {return false;}
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        return false;
    }

    public Bitmap rotateBitmap(Bitmap sourceBitmap, int degree) {
        Matrix matrix = new Matrix();
        //旋转90度，并做镜面翻转
        matrix.setRotate(degree);
        matrix.postScale(1, 1);
        return Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight(), matrix, true);
    }

    public byte[] yuv2Jpeg(byte[] yuvBytes, int width, int height){
        YuvImage yuvImage = new YuvImage(yuvBytes, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, baos);
        return baos.toByteArray();
    }

}
