package com.dong.studyopencv.face;

import android.Manifest;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.dong.faceverification.interfaces.OnFaceDetectorListener;
import com.dong.faceverification.interfaces.OnOpenCVInitListener;
import com.dong.faceverification.util.FaceUtil;
import com.dong.faceverification.view.CameraFaceDetectionView;
import com.dong.studyopencv.PermissionsManager;
import com.dong.studyopencv.R;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.osgi.OpenCVNativeLoader;

public class Fd2Activity extends AppCompatActivity
        implements OnFaceDetectorListener {

    private static final String                  TAG           = "Fd2Activity";
    private static final String                  FACE1         = "face1";
    private static final String                  FACE2         = "face2";
    private static       boolean                 isGettingFace = false;
    private              Bitmap                  mBitmapFace1;
    private              Bitmap                  mBitmapFace2;
    private              ImageView               mImageViewFace1;
    private              ImageView               mImageViewFace2;
    private              TextView                mCmpPic;
    private              double                  cmp;
    private              CameraFaceDetectionView mCameraFaceDetectionView;
    private              PermissionsManager      mPermissionsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fd2);
        // 检测人脸的View
        mCameraFaceDetectionView = findViewById(R.id.cameraFaceDetectionView);
        if (mCameraFaceDetectionView != null) {
            mCameraFaceDetectionView.setOnFaceDetectorListener(this);
            mCameraFaceDetectionView.setOnOpenCVInitListener(new OnOpenCVInitListener() {
                @Override
                public void onLoadSuccess() {
                    Log.i(TAG , "onLoadSuccess: ");
                }

                @Override
                public void onLoadFail() {
                    Log.i(TAG , "onLoadFail: ");
                }

                @Override
                public void onIncompatibleManagerVersion() {
                    Log.i(TAG , "onIncompatibleManagerVersion: ");
                }

                @Override
                public void onOtherError() {
                    Log.i(TAG , "onOtherError: ");
                }
            });
        }
        // 显示的View
        mImageViewFace1 = findViewById(R.id.face1);
        mImageViewFace2 = findViewById(R.id.face2);
        mCmpPic = findViewById(R.id.text_view);
        Button bn_get_face = findViewById(R.id.bn_get_face);
        // 抓取一张人脸
        bn_get_face.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isGettingFace = true;
            }
        });
        Button switch_camera = findViewById(R.id.switch_camera);
        // 切换摄像头（如果有多个）
        switch_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 切换摄像头
                boolean isSwitched = mCameraFaceDetectionView.switchCamera();
                Toast.makeText(getApplicationContext() , isSwitched ? "摄像头切换成功" : "摄像头切换失败" , Toast.LENGTH_SHORT).show();
            }
        });

        // 动态权限检查器
        mPermissionsManager = new PermissionsManager(this) {
            @Override
            public void authorized(int requestCode) {
                Toast.makeText(getApplicationContext() , "权限通过！" , Toast.LENGTH_SHORT).show();
            }

            @Override
            public void noAuthorization(int requestCode , String[] lacksPermissions) {
                AlertDialog.Builder builder = new AlertDialog.Builder(Fd2Activity.this);
                builder.setTitle("提示");
                builder.setMessage("缺少相机权限！");
                builder.setPositiveButton("设置权限" , new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog , int which) {
                        PermissionsManager.startAppSettings(getApplicationContext());
                    }
                });
                builder.create().show();
            }

            @Override
            public void ignore() {
                AlertDialog.Builder builder = new AlertDialog.Builder(Fd2Activity.this);
                builder.setTitle("提示");
                builder.setMessage("Android 6.0 以下系统不做权限的动态检查\n如果运行异常\n请优先检查是否安装了 OpenCV Manager\n并且打开了 CAMERA 权限");
                builder.setPositiveButton("确认" , null);
                builder.setNeutralButton("设置权限" , new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog , int which) {
                        PermissionsManager.startAppSettings(getApplicationContext());
                    }
                });
                builder.create().show();
            }
        };
    }

    /**
     * 设置应用权限
     *
     * @param view view
     */
    public void setPermissions(View view) {
        PermissionsManager.startAppSettings(getApplicationContext());
    }

    /**
     * 检测到人脸
     *
     * @param mat  Mat
     * @param rect Rect
     */
    @Override
    public void onFace(Mat mat , Rect rect) {
        Log.e(TAG , "检测到人脸>>>>>>>>");
        if (isGettingFace) {
            if (null == mBitmapFace1 || null != mBitmapFace2) {
                mBitmapFace1 = null;
                mBitmapFace2 = null;

                // 保存人脸信息并显示
                FaceUtil.saveImage(this , mat , rect , FACE1);
                mBitmapFace1 = FaceUtil.getImage(this , FACE1);
                cmp = 0.0d;
            } else {
                FaceUtil.saveImage(this , mat , rect , FACE2);
                mBitmapFace2 = FaceUtil.getImage(this , FACE2);

                // 计算相似度
                cmp = FaceUtil.compare(this , FACE1 , FACE2);
                Log.e(TAG , "onFace: 相似度 : " + cmp);
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (null == mBitmapFace1) {
                        mImageViewFace1.setImageResource(R.mipmap.ic_contact_picture);
                    } else {
                        mImageViewFace1.setImageBitmap(mBitmapFace1);
                    }
                    if (null == mBitmapFace2) {
                        mImageViewFace2.setImageResource(R.mipmap.ic_contact_picture);
                    } else {
                        mImageViewFace2.setImageBitmap(mBitmapFace2);
                    }
                    mCmpPic.setText(String.format("相似度 :  %.2f" , cmp) + "%");
                }
            });

            isGettingFace = false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode , @NonNull String[] permissions , @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode , permissions , grantResults);
        mPermissionsManager.recheckPermissions(requestCode , permissions , grantResults);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mCameraFaceDetectionView != null) {
            mCameraFaceDetectionView.disableView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 要校验的权限
        String[] PERMISSIONS = new String[]{Manifest.permission.CAMERA};
        // 检查权限
        mPermissionsManager.checkPermissions(0 , PERMISSIONS);

        if (mCameraFaceDetectionView != null) {
            mCameraFaceDetectionView.loadOpenCV(getApplicationContext());
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mCameraFaceDetectionView != null)
            mCameraFaceDetectionView.disableView();
    }
}
