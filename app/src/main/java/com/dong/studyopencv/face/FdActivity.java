package com.dong.studyopencv.face;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.dong.studyopencv.R;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.File;

public class FdActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "人脸识别页面：";

    private CameraBridgeViewBase openCvCameraView;
    private TextView hint;

    private Mat mRgba; //图像容器
    private Mat mGray;

    //解决横屏问题
    private Mat Matlin;
    private Mat gMatlin;
    private int absoluteFaceSize;

    //记录的时间，秒级别
    private long mRecodeTime;
    //记录的时间，毫秒级别，空闲时间，用来计数，实现0.5秒一次检查
    private long mRecodeFreeTime;
    //当前的时间，秒级别
    private long mCurrentTime = 0;
    //当前的时间，毫秒级别
    private long mMilliCurrentTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fd);
        checkPermission();
        //初始化控件
        initComponent();
        //初始化摄像头
        initCamera();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "OpenCV init error");
        }
        //初始化opencv资源
        initOpencv();
    }

    /**
     * @Description 初始化opencv资源
     */
    protected void initOpencv() {
        //initFrontalFace();
        // 显示
        openCvCameraView.enableView();
    }

    /**
     * @Description 初始化组件
     */
    protected void initComponent() {
        openCvCameraView = findViewById(R.id.javaCameraView);
        hint = findViewById(R.id.hint);
    }

    /**
     * @Description 初始化摄像头
     */
    protected void initCamera() {
        int camerId = 1;
        Camera.CameraInfo info = new Camera.CameraInfo();
        int numCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            Log.v("notice", "在 CameraRenderer 类的 openCamera 方法 中执行了开启摄像 Camera.open 方法,cameraId:" + i);
            camerId = i;
            break;
        }
        openCvCameraView.setCameraIndex(1); //摄像头索引        -1/0：后置双摄     1：前置
        openCvCameraView.enableFpsMeter(); //显示FPS
        openCvCameraView.setCvCameraViewListener(this);//监听
        openCvCameraView.setMaxFrameSize(950, 500);//设置帧大小
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat();
        mGray = new Mat();
        //解决横屏问题
        Matlin = new Mat(width, height, CvType.CV_8UC3);
        gMatlin = new Mat(width, height, CvType.CV_8UC3);

        absoluteFaceSize = (int) (height * 0.6);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
        gMatlin.release();
        Matlin.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mMilliCurrentTime = System.currentTimeMillis() / 100;//获取当前时间毫秒级别
        mCurrentTime = mMilliCurrentTime / 10;//获取当前时间，秒级别
        int rotation = openCvCameraView.getDisplay().getRotation();

        //使前置的图像也是正的
        mRgba = inputFrame.rgba(); //RGBA
        mGray = inputFrame.gray(); //单通道灰度图
        //解决  前置摄像头旋转显示问题
        Core.flip(mRgba, mRgba, 1); //旋转,变成镜像
        Core.flip(mGray, mGray, 1);
        return mRgba;
    }

    /**
     * 检查权限
     */
    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            int write = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int read = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            if (write != PackageManager.PERMISSION_GRANTED || read != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 300);
            } else {
                String name = "CrashDirectory";
                File file1 = new File(Environment.getExternalStorageDirectory(), name);
                if (file1.mkdirs()) {
                    Log.i(TAG, "permission -------------> " + file1.getAbsolutePath());
                } else {
                    Log.i(TAG, "permission -------------fail to make file ");
                }
            }
        } else {
            Log.i("wytings", "------------- Build.VERSION.SDK_INT < 23 ------------");
        }
    }
}
