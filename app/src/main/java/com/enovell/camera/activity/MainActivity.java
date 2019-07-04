package com.enovell.camera.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.enovell.camera.util.PermissionUtil;
import com.enovell.camera.view.AutoFitTextureView;
import com.enovell.camera.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.System.exit;

public class MainActivity extends AppCompatActivity {
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    //拍照权限请求码
    private static final int REQUEST_PICTURE_PERMISSION = 1;
    //拍照权限
    private static final String[] PICTURE_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private Button btn_take_photo;
    private AutoFitTextureView mTextureView;
    private ImageView iv_mask;


    private String mCameraId;
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCameraCaptureSession;
private CaptureRequest.Builder mPreviewRequestBuilder;

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    private TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            Log.i("tag","调用onSurfaceTextureAvailable");
            openCamera(width,height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            startPreview();
        }

        @Override
        public void onDisconnected( CameraDevice cameraDevice) {

        }

        @Override
        public void onError( CameraDevice cameraDevice, int i) {

        }
    };

    private void startPreview() {
        SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        //设置预览大小
        surfaceTexture.setDefaultBufferSize(mTextureView.getWidth(),mTextureView.getHeight());
        Surface surface = new Surface(surfaceTexture);
        try {
            List<Surface> list = new ArrayList<>();
            list.add(surface);
             mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);
            mCameraDevice.createCaptureSession(list, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured( CameraCaptureSession cameraCaptureSession) {
                    mCameraCaptureSession = cameraCaptureSession;
                    try {
                        Log.i("tag","onConfigured");
                        cameraCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, mBackgroundHandler);
                    } catch (CameraAccessException e) {


                    }
                }

                @Override
                public void onConfigureFailed( CameraCaptureSession cameraCaptureSession) {

                }
            },mBackgroundHandler);
            Log.i("tag","开启预览");


        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }


    ///为了使照片竖直显示
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();
        initData();

    }

    private void initData() {
        btn_take_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePhoto();
            }
        });
        initCamera();
    }

    /**
     * 初始化相机数据
     */
    private void initCamera() {



    }

    /**
     * 点击拍照后处理
     */
    private void takePhoto(){
        try {
            mCameraCaptureSession.capture(mPreviewRequestBuilder.build(), null,null);
            Toast.makeText(getApplicationContext(),"拍照成功",Toast.LENGTH_SHORT).show();
        } catch (CameraAccessException e) {


        }
    }

    /**
     * 开启相机
     */
    private void openCamera(int width,int height) {
        //请求权限
        PermissionUtil permissionUtil = new PermissionUtil(this);
        //若没有权限

        iv_mask.setImageResource(R.mipmap.takephoto);
        if (!permissionUtil.hasPermissionGranted(PICTURE_PERMISSIONS)) {
            //请求所需权限
//            ActivityCompat.requestPermissions(MainActivity.this, PICTURE_PERMISSIONS, REQUEST_PICTURE_PERMISSION);
            permissionUtil.requestRequiredPermissions(PICTURE_PERMISSIONS, R.string.need_permissions, REQUEST_PICTURE_PERMISSION);
            return;
        }

        //设置camera参数

        setCameraInfo(width,height);
    }

    private void setCameraInfo(int width, int height) {
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
//        mCameraManager.getCameraIdList();
        Log.i("tag","11");
        try {
            mCameraManager.openCamera("0",mCameraDeviceStateCallback,mBackgroundHandler);
            Log.i("tag","相机开启成功");
        } catch (CameraAccessException e) {
            Log.i("tag","相机开启失败");
            e.printStackTrace();
        }
    }


    /**
     * 开启后台线程
     */
    private void startBackgroundThread(){
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * 停止后台线程
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            throw new RuntimeException("停止后台线程时中断");
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        if(mTextureView.isAvailable()){
            openCamera(mTextureView.getWidth(),mTextureView.getHeight());
        }else {
            mTextureView.setSurfaceTextureListener(textureListener);
        }

    }

    @Override
    protected void onPause() {

        super.onPause();
        stopBackgroundThread();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PICTURE_PERMISSION:
                if (grantResults.length == PICTURE_PERMISSIONS.length) {
                    for (int grantResult : grantResults) {
                        if (grantResult == PackageManager.PERMISSION_DENIED) {
                            stopApp(this);
                            break;
                        }
                    }
                } else {
                    stopApp(this);
                }
                //程序正常运行
                setCameraInfo(mTextureView.getWidth(),mTextureView.getHeight());

                break;
            default:

        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * 停止Activity：APP停止运行
     */
    private void stopApp(Activity activity) {
        Toast.makeText(activity, R.string.sorry, Toast.LENGTH_SHORT).show();
        activity.finish();
    }

    private void initUI() {
        btn_take_photo = findViewById(R.id.btn_take_photo);
        mTextureView = findViewById(R.id.atv_textureview);
        iv_mask = findViewById(R.id.iv_mask);
    }


}
