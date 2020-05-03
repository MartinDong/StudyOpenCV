package com.dong.studyopencv.face;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.dong.studyopencv.R;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class FdActivity extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "人脸识别页面：";

    private CameraBridgeViewBase openCvCameraView;
    private Handler              mHandler;
    private CascadeClassifier    mFrontalFaceClassifier = null; //正脸 级联分类器
    private Mat                  mRgba; //图像容器
    private Mat                  mGray;
    private TextView             hint;
    //解决横屏问题
    private Mat                  Matlin;
    private Mat                  gMatlin;
    private int                  absoluteFaceSize;
    //人脸矩形框
    private Rect                 faceRect               = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fd);
        mHandler = new Handler();
        // 检查权限
        checkPermission();
        // 初始化控件
        initComponent();
        // 初始化摄像头
        initCamera();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG , "OpenCV init error");
        }
        //初始化opencv资源
        initOpencv();
    }

    /**
     * @Description 初始化opencv资源
     */
    protected void initOpencv() {
        initFrontalFace();
        // 显示
        openCvCameraView.enableView();
    }

    /**
     * @Description 初始化正脸分类器
     */
    public void initFrontalFace() {
        try {
            //OpenCV的人脸模型文件： lbpcascade_frontalface_improved
            InputStream      is           = getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
            File             cascadeDir   = getDir("cascade" , Context.MODE_PRIVATE);
            File             mCascadeFile = new File(cascadeDir , "haarcascade_frontalface_alt.xml");
            FileOutputStream os           = new FileOutputStream(mCascadeFile);
            byte[]           buffer       = new byte[4096];
            int              bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer , 0 , bytesRead);
            }
            is.close();
            os.close();
            // 加载 正脸分类器
            mFrontalFaceClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG , e.toString());
        }
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
        openCvCameraView.setCameraIndex(1); //摄像头索引        -1/0：后置双摄     1：前置
        openCvCameraView.enableFpsMeter(); //显示FPS
        openCvCameraView.setCvCameraViewListener(this);//监听
        openCvCameraView.setMaxFrameSize(950 , 500);//设置帧大小
    }

    @Override
    public void onCameraViewStarted(int width , int height) {
        mRgba = new Mat();
        mGray = new Mat();
        //解决横屏问题
        Matlin = new Mat(width , height , CvType.CV_8UC3);
        gMatlin = new Mat(width , height , CvType.CV_8UC3);

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
        int rotation = openCvCameraView.getDisplay().getRotation();

        //使前置的图像也是正的
        mRgba = inputFrame.rgba(); //RGBA
        mGray = inputFrame.gray(); //单通道灰度图
        //解决  前置摄像头旋转显示问题
        Core.flip(mRgba , mRgba , 1); //旋转,变成镜像
        Core.flip(mGray , mGray , 1);

        Rect[] faceArray;
        if (rotation == Surface.ROTATION_0) {
            MatOfRect faces = new MatOfRect();
            Core.rotate(mGray , gMatlin , Core.ROTATE_90_CLOCKWISE);
            Core.rotate(mRgba , Matlin , Core.ROTATE_90_CLOCKWISE);
            if (mFrontalFaceClassifier != null) {
                mFrontalFaceClassifier.detectMultiScale(gMatlin , faces , 1.1 , 5 , 2 , new Size(absoluteFaceSize , absoluteFaceSize) , new Size());
            }
            faceArray = faces.toArray();

            for (int i = 0 ;i < faceArray.length ;i++) {
                Imgproc.rectangle(Matlin , faceArray[i].tl() , faceArray[i].br() , new Scalar(255 , 255 , 255) , 2);
                if (i == faceArray.length - 1) {
                    faceRect = new Rect(faceArray[i].x , faceArray[i].y , faceArray[i].width , faceArray[i].height);
                }
            }
            Core.rotate(Matlin , mRgba , Core.ROTATE_90_COUNTERCLOCKWISE);
        }

        return mRgba;
    }

    /**
     * 检查权限
     */
    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            int write = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int read  = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            if (write != PackageManager.PERMISSION_GRANTED || read != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE , Manifest.permission.READ_EXTERNAL_STORAGE} , 300);
            } else {
                String name  = "CrashDirectory";
                File   file1 = new File(Environment.getExternalStorageDirectory() , name);
                if (file1.mkdirs()) {
                    Log.i(TAG , "permission -------------> " + file1.getAbsolutePath());
                } else {
                    Log.i(TAG , "permission -------------fail to make file ");
                }
            }
        } else {
            Log.i(TAG , "------------- Build.VERSION.SDK_INT < 23 ------------");
        }

        if (ContextCompat.checkSelfPermission(this , Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
        } else {
            //否则去请求相机权限
            ActivityCompat.requestPermissions(this , new String[]{Manifest.permission.CAMERA} , 100);
        }
    }
}
