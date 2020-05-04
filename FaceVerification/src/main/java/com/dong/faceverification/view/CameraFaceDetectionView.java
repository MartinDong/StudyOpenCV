package com.dong.faceverification.view;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;

import com.dong.faceverification.R;
import com.dong.faceverification.interfaces.OnFaceDetectorListener;
import com.dong.faceverification.interfaces.OnOpenCVInitListener;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.InstallCallbackInterface;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
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
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by donghongyu on 2020/5/4.
 * CameraFaceDetectionView
 */
public class CameraFaceDetectionView extends JavaCameraView implements LoaderCallbackInterface,
        CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String                 TAG                = "RobotCameraView";
    private              OnFaceDetectorListener mOnFaceDetectorListener;
    private              OnOpenCVInitListener   mOnOpenCVInitListener;
    private              CascadeClassifier      mFrontalFaceClassifier;
    // 记录切换摄像头点击次数
    private              int                    mCameraSwitchCount = 0;
    // 记录原始的摄像头数据，提供给分析器使用
    private              Mat                    mRgba;
    private              Mat                    mGray;
    // 解决横屏问题，用來展示用的
    private              Mat                    Matlin;
    private              Mat                    gMatlin;
    private              int                    mAbsoluteFaceSize  = 0;
    // 脸部占屏幕多大面积的时候开始识别
    private static final float                  RELATIVE_FACE_SIZE = 0.2f;
    private              boolean                isLoadSuccess      = false;
    //人脸矩形框
    private              Rect                   faceRect           = null;
    private              Scalar                 FACE_RECT_COLOR    = new Scalar(0 , 255 , 0 , 255);

    public CameraFaceDetectionView(Context context , AttributeSet attrs) {
        super(context , attrs);
    }

    /**
     * 加载OpenCV
     *
     * @param context context
     */
    public void loadOpenCV(Context context) {
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG , "OpenCV init error");
        }
        enableView();
        onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    @Override
    public void enableView() {
        if (isLoadSuccess) {
            super.enableView();
            super.setCameraIndex(1); //摄像头索引  -1/0：后置双摄 1：前置
            super.enableFpsMeter(); //显示FPS
            super.setCvCameraViewListener(this);//监听
//            super.setMaxFrameSize(950 , 500);//设置帧大小
        }
    }

    @Override
    public void disableView() {
        if (isLoadSuccess) {
            super.disableView();
        }
    }

    @Override
    public void onCameraViewStarted(int width , int height) {
        mGray = new Mat();
        mRgba = new Mat();
        //解决横屏问题
        Matlin = new Mat(width , height , CvType.CV_8UC3);
        gMatlin = new Mat(width , height , CvType.CV_8UC3);

        mAbsoluteFaceSize = (int) (height * 0.6);
    }

    @Override
    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();

        Matlin.release();
        gMatlin.release();
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        int rotation = getDisplay().getRotation();

        //使前置的图像也是正的,子线程（非UI线程）
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
                mFrontalFaceClassifier.detectMultiScale(
                        gMatlin , faces , 1.1 , 2 , 2 ,
                        new Size(mAbsoluteFaceSize , mAbsoluteFaceSize) ,
                        new Size());
            }
            faceArray = faces.toArray();

            for (int i = 0 ;i < faceArray.length ;i++) {
                Imgproc.rectangle(Matlin , faceArray[i].tl() , faceArray[i].br() , FACE_RECT_COLOR , 2);
                if (i == faceArray.length - 1) {
                    faceRect = new Rect(faceArray[i].x , faceArray[i].y , faceArray[i].width , faceArray[i].height);
                    if (null != mOnFaceDetectorListener) {
                        mOnFaceDetectorListener.onFace(mRgba , faceRect);
                    }
                }
            }
            Core.rotate(Matlin , mRgba , Core.ROTATE_90_COUNTERCLOCKWISE);
        }

        return mRgba;
    }

    /**
     * 切换摄像头
     *
     * @return 切换摄像头是否成功
     */
    public boolean switchCamera() {
        // 摄像头总数
        int numberOfCameras = Camera.getNumberOfCameras();
        // 2个及以上摄像头
        if (1 < numberOfCameras) {
            // 设备没有摄像头
            int index = ++mCameraSwitchCount % numberOfCameras;
            disableView();
            setCameraIndex(index);
            enableView();
            return true;
        }
        return false;
    }

    /**
     * 添加人脸识别额监听
     *
     * @param listener 回调接口
     */
    public void setOnFaceDetectorListener(OnFaceDetectorListener listener) {
        mOnFaceDetectorListener = listener;
    }

    /**
     * 添加加载OpenCV的监听
     *
     * @param listener 回调接口
     */
    public void setOnOpenCVInitListener(OnOpenCVInitListener listener) {
        mOnOpenCVInitListener = listener;
    }

    @Override
    public void onManagerConnected(int status) {
        switch (status) {
            case LoaderCallbackInterface.SUCCESS:
                Log.i(TAG , "onManagerConnected: OpenCV加载成功");
                if (null != mOnOpenCVInitListener) {
                    mOnOpenCVInitListener.onLoadSuccess();
                }
                isLoadSuccess = true;
                try {
                    //OpenCV的人脸模型文件： lbpcascade_frontalface_improved
                    InputStream      is          = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                    File             cascadeDir  = getContext().getApplicationContext().getDir("cascade" , Context.MODE_PRIVATE);
                    File             cascadeFile = new File(cascadeDir , "haarcascade_frontalface_alt.xml");
                    FileOutputStream os          = new FileOutputStream(cascadeFile);
                    byte[]           buffer      = new byte[4096];
                    int              bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer , 0 , bytesRead);
                    }
                    is.close();
                    os.close();
                    mFrontalFaceClassifier = new CascadeClassifier(cascadeFile.getAbsolutePath());
                    if (mFrontalFaceClassifier.empty()) {
                        Log.e(TAG , "级联分类器加载失败");
                        mFrontalFaceClassifier = null;
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG , "没有找到级联分类器");
                }
                enableView();

                break;
            case LoaderCallbackInterface.INCOMPATIBLE_MANAGER_VERSION: // Application is incompatible with this version of OpenCV Manager. Possibly, a service update is required.
                Log.i(TAG , "onManagerConnected: 版本不正确");
                if (null != mOnOpenCVInitListener) {
                    mOnOpenCVInitListener.onIncompatibleManagerVersion();
                }
                break;
            default: // Other status,
                Log.i(TAG , "onManagerConnected: 其他错误");
                if (null != mOnOpenCVInitListener) {
                    mOnOpenCVInitListener.onOtherError();
                }
                // super.onManagerConnected(status);
                break;
        }
    }

    @Override
    public void onPackageInstall(int operation , InstallCallbackInterface callback) {

    }
}
