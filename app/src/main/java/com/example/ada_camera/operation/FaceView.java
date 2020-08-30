package com.example.ada_camera.operation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;


public class FaceView extends View {

    private Paint paint1, paint2, paint3;
    private Matrix matrix = new Matrix();
    private RectF mRect = new RectF();
    private RectF objRect = new RectF();
    private Camera.Face[] mfaces;
    private PointF cpt = new PointF();
    private Matrix matrix1 = new Matrix();

    public FaceView(Context context) {
        super(context);
        init(context);
    }

    public FaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        cpt.x = 0;
        cpt.y = 0;
        paint1 = new Paint();
        paint1.setColor(Color.GREEN);
        paint1.setStrokeWidth(5);
        paint1.setStyle(Paint.Style.STROKE);
        paint2 = new Paint();
        paint2.setColor(Color.BLUE);
        paint2.setStrokeWidth(5);
        paint2.setStyle(Paint.Style.STROKE);
        //其实可以不要，只是为了可视化，最终版将删除
        paint3 = new Paint();
        paint3.setColor(Color.RED);
        paint3.setStrokeWidth(5);
        paint3.setStyle(Paint.Style.STROKE);
    }

    public void init_objRect(){
        // 当前为人脸坐标系，left是顶部，right是底部
        // top是右边，right是左边
        objRect.left = -655f;
        objRect.top = 5f;
        objRect.right = -645f;
        objRect.bottom = -5f;
        prepareMatrix(matrix1, 90, getWidth(), getHeight());
        matrix1.mapRect(objRect);
    }


    public void setFaces(Camera.Face[] faces) {
        mfaces = faces;
        invalidate();
    }

    public void prepareMatrix(Matrix matrix, int displayOrientation,
                              int viewWidth, int viewHeight) {

        matrix.setScale(1, 1);
        matrix.postRotate(displayOrientation);
        matrix.postScale(viewWidth / 2000f, viewHeight / 2000f);
        matrix.postTranslate(viewWidth / 2f, viewHeight / 2f);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        try{
            Log.i("draw", "drawing");
            if(mfaces != null){
                prepareMatrix(matrix, 90, getWidth(), getHeight());
                cpt.x = 0;
                cpt.y = 0;
                for (Camera.Face face: mfaces) {
                    mRect.set(face.rect);
                    matrix.mapRect(mRect);
                    canvas.drawRect(mRect, paint1);
                    cpt.x = cpt.x + mRect.centerX() / mfaces.length;
                    cpt.y = cpt.y + mRect.centerY() / mfaces.length;
                }

                canvas.drawCircle(cpt.x, cpt.y, 5, paint2);
                canvas.drawRect(objRect, paint3);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public PointF geterror(){
        PointF error = new PointF();
        if((cpt.x > objRect.left) && (cpt.x < objRect.right)){error.x = 0;}
        if((cpt.y > objRect.bottom) && (cpt.y < objRect.top)){error.y = 0;}
        if(cpt.x <= objRect.left){error.x = objRect.left - cpt.x;}
        if(cpt.x >= objRect.right){error.x = objRect.right - cpt.x;}
        if(cpt.y <= objRect.top){error.y = cpt.y - objRect.top;}
        if(cpt.y >= objRect.bottom){error.y = cpt.y - objRect.bottom;}
        return error;
    }
}