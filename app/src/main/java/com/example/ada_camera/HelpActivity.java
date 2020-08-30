package com.example.ada_camera;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.zzhoujay.richtext.RichText;
import com.zzhoujay.richtext.RichType;

public class HelpActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private TextView textView;
    private final String readme = "### Ada_Camera：自适应人脸定位的拍照应用程序\n" +
            "\n" +
            "\n" +
            "\n" +
            "#### 首先在此感谢以下开源库的支持\n" +
            "\n" +
            "* [@zzhoujay](https://github.com/zzhoujay/RichText) Android平台上的富文本解析器\n" +
            "* [@Jasonchenlijian](https://github.com/Jasonchenlijian/FastBle) Android低功耗蓝牙开源库\n" +
            "\n" +
            "\n" +
            "\n" +
            "#### 以及所有博客以及书籍的帮助，以下是主要博客\n" +
            "\n" +
            "* [@涛声依旧Cjt](https://blog.csdn.net/u010898329/article/details/82424273) \n" +
            "* [@Smashing](https://www.jianshu.com/p/331af6dc2772) \n" +
            "* 《第一行代码：Android》\n" +
            "* b站 **天哥在奔跑** 系列安卓教学视频\n" +
            "\n" +
            "\n" +
            "\n" +
            "#### 硬件依赖\n" +
            "\n" +
            "- [x] 安卓6.0++（最好）\n" +
            "- [x] BLE蓝牙模块\n" +
            "- [x] 支持Usart串口通信的单片机\n" +
            "\n" +
            "\n" +
            "\n" +
            "#### 简要功能说明\n" +
            "\n" +
            "连接蓝牙后，可以进入摄像头操作，后置摄像头捕获环境中的人脸，并返回与最佳拍照位置人脸的偏差，把偏差数据通过**Writenoresponse**方式发送给蓝牙。单片机通过决策，可以通过**Notify**方式通知上位机，进行拍照保存。\n" +
            "\n" +
            "\n" +
            "\n" +
            "#### 使用说明\n" +
            "\n" +
            "* 点击 **扫描** 按键后，手机动态申请蓝牙和位置权限，请放心授权，否则程序会强制退出。\n" +
            "* 扫描后出现所需蓝牙最好按下 **停止** 按键停止扫描后再进入相机操作。\n" +
            "* 人脸识别速度与手机硬件有关，适合低速场景，如果告诉移动手机将得不到良好效果。\n" +
            "\n" +
            "\n" +
            "\n" +
            "#### 联系方式 \n" +
            "\n" +
            "后续会发布具体使用博客，请关注：[@gy_rick](https://me.csdn.net/blog/gy_Rick) \n" +
            "\n" +
            "或通过邮件私信哦：gy_rick@foxmail.com\n" +
            "\n" +
            "拍照小车硬件实现可私信：zl_keepmoving@163.com";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Drawable upArrow = ContextCompat.getDrawable(this, R.drawable.abc_ic_ab_back_material);
        upArrow.setColorFilter(getResources().getColor(R.color.colorWhite), PorterDuff.Mode.SRC_ATOP);
        getSupportActionBar().setHomeAsUpIndicator(upArrow);


        RichText.initCacheDir(this);
        RichText.debugMode = true;

        textView = findViewById(R.id.app_info);
        RichText
                .from(readme)
                .type(RichType.markdown)
                .into(textView);

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

}
