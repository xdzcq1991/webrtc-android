package com.example.kingo.rtcclient.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.WindowManager;


import org.webrtc.NV21Buffer;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by roy on 2019/7/18.
 */

public class KingoScreenCapturer implements VideoCapturer {
    private static final String TAG = "KingoScreenCapturer";
    private int cropTop;
    private int cropLeft;
    private int cropWidth;
    private int cropHeight;
    private int mWidth;
    private int mHeight;
    private int mDpi;
    private int mFrameRate;

    private Handler cameraThreadHandler;
    private Context applicationContext;
    private CapturerObserver capturerObserver;
    private SurfaceTextureHelper surfaceHelper;
    private final Object stateLock = new Object();
    private final Object mMutex = new Object();
    private HandlerThread mHandlerThread;
    private VideoFrame mLastSendFrame;
    private long       mLastSendTSMs;

    private int mOrientation = -1;
    private MediaProjection mMediaProjection;
    // parameters for the encoder
    private ImageReader mImgReader;
    private Handler mHandler;
    private AtomicBoolean mQuit = new AtomicBoolean(false);
    private VirtualDisplay mVirtualDisplay = null;
    private byte[] outputData = null;
    private Bitmap localScreenBmp;

    public KingoScreenCapturer(int dpi, MediaProjection mp, int width, int height, int paletteLeft, int paletteTop, int paletteWidth, int paletteHeight) {
        mDpi = dpi;
        mWidth = width;
        mHeight = height;
        cropLeft = paletteLeft;
        cropTop = paletteTop;
        cropWidth = paletteWidth;
        cropHeight = paletteHeight;
        mMediaProjection = mp;
        mHandlerThread = new HandlerThread("HandlerThreadxxx");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

	public Bitmap getLocalScreenBmp() {
		return localScreenBmp;
	}

	public synchronized void setLocalScreenBmp(Bitmap localScreenBmp) {
		this.localScreenBmp = localScreenBmp;
	}

	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initImageReader1(int width, int height, int frameRate) {
        mImgReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3);
        mImgReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image img;
                try {
                    img = mImgReader.acquireLatestImage();
                    if (img != null) {
                        img.setCropRect(new Rect(cropLeft, cropTop, cropLeft + cropWidth, cropTop + cropHeight));
                        Image.Plane[] planes = img.getPlanes();
                        if (planes.length > 0) {
                            final ByteBuffer buffer = planes[0].getBuffer();
                            int pixelStride = planes[0].getPixelStride();
                            int rowStride = planes[0].getRowStride();
                            int rowPadding = rowStride - pixelStride * cropWidth;
                            Bitmap bmp = Bitmap.createBitmap(cropWidth + rowPadding / pixelStride,
                                    cropHeight, Bitmap.Config.ARGB_8888);
                            bmp.copyPixelsFromBuffer(buffer);
                            Bitmap croppedBitmap = Bitmap.createBitmap(bmp, cropLeft, cropTop, cropWidth, cropHeight);
//                            outputData =ImageUtils.bitmapToNv21(bmp, cropWidth, cropWidth);
//                            outputData =ImageUtils.bitmapToNv21(croppedBitmap, cropWidth, cropHeight);
                            setLocalScreenBmp(Bitmap.createBitmap(bmp, cropLeft, cropTop, cropWidth, cropHeight));
                            outputData =ImageUtils.getNV21(croppedBitmap, cropWidth, cropHeight);
                        }
                        long captureTimeNs = TimeUnit.MILLISECONDS.toNanos(SystemClock.elapsedRealtime());
//                        VideoFrame.Buffer frameBuffer = new NV21Buffer(outputData, mWidth, mHeight,null);
                        VideoFrame.Buffer frameBuffer = new NV21Buffer(outputData, cropWidth, cropHeight,null);
                        VideoFrame frame = new VideoFrame(frameBuffer, 0, captureTimeNs);
                        if (capturerObserver != null) {
                            capturerObserver.onFrameCaptured(frame);
                            mLastSendTSMs = System.currentTimeMillis();
                        }
                        frame.release();
                        if (getDeviceOrientation()!=mOrientation){
                            mOrientation = getDeviceOrientation();
                            mHandler.post(vhrun);
                        }
                        img.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        },mHandler);
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void createVirtualDisplay(int width, int height) {
      mVirtualDisplay = mMediaProjection.createVirtualDisplay(TAG + "-display", width, height, mDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, mImgReader.getSurface(),null,null);
    }

    @Override
    public void initialize(SurfaceTextureHelper surfaceTextureHelper, Context context, CapturerObserver capturerObserver) {
        this.applicationContext = context;
        this.capturerObserver = capturerObserver;
        this.surfaceHelper = surfaceTextureHelper;
        this.cameraThreadHandler = surfaceTextureHelper == null ? null : surfaceTextureHelper.getHandler();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void startCapture(int width, int height, int framerate) {
//        heavySet(width,height);
        if (this.applicationContext == null) {
            throw new RuntimeException("ScreenRecorder must be initialized before calling startCapture.");
        } else {
//            synchronized(stateLock) {
                mFrameRate = framerate;
                initImageReader1(mWidth, mHeight, framerate);
//                createVirtualDisplay(cropWidth, cropHeight);
                createVirtualDisplay(mWidth, mHeight);
                if (capturerObserver!=null){
                    capturerObserver.onCapturerStarted(true);
                }
//            }
        }
        mQuit.set(false);
        Thread thread =  new Thread(new Runnable() {
            @Override
            public void run() {
                long preTS = 0;
                long intervalTS = 25;  // 10 fps
                while (!mQuit.get()){
                    long startTS = System.currentTimeMillis();
                    if (preTS == 0) {
                        preTS = startTS;
                    }
                    if (startTS - mLastSendTSMs > 500 && capturerObserver != null) {
                        long captureTimeNs = TimeUnit.MILLISECONDS.toNanos(SystemClock.elapsedRealtime());
//                        VideoFrame.Buffer frameBuffer = new NV21Buffer(outputData, mWidth, mHeight, null);
                        VideoFrame.Buffer frameBuffer = new NV21Buffer(outputData, cropWidth, cropHeight, null);
                        VideoFrame frame = new VideoFrame(frameBuffer, 0, captureTimeNs);
                        if (capturerObserver != null && outputData!=null && outputData.length>0) {
                            capturerObserver.onFrameCaptured(frame);
                        }
                        frame.release();
                    }
                    long diffTS = startTS - preTS;
                    long waitTime = Math.max(intervalTS + intervalTS - diffTS, 0);
                    synchronized(mMutex){
                        try {
                            waitTime = Math.max(waitTime,50);
                            mMutex.wait(waitTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    //TODO
                    preTS = startTS;
                }
            }
        });
        thread.start();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void stopCapture(){
        synchronized(stateLock){
            mQuit.set(true);
            synchronized (mMutex){
                mMutex.notify();
            }
            if (mVirtualDisplay != null) {
                mVirtualDisplay.release();
                mVirtualDisplay = null;
            }
            if (mImgReader!=null){
                mImgReader.close();
                mImgReader = null;
            }
            if (mLastSendFrame!=null){
                mLastSendFrame.release();
                mLastSendFrame = null;
            }
        }
    }

    @Override
    public void changeCaptureFormat(int width, int height, int framerate) {
        synchronized(stateLock) {
            this.stopCapture();
            this.startCapture(width, height, framerate);
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void dispose() {
        this.stopCapture();
//		mHandler.removeCallbacks(vhrun);
        if (mMediaProjection != null) {
            mMediaProjection.stop();
        }
        if (mHandlerThread!=null){
            boolean b = mHandlerThread.quitSafely();
            Log.v("HandlerThreadxx",b?"true":"false");
            mHandlerThread = null;
        }

		mHandler.removeCallbacksAndMessages(null);
		mHandler.getLooper().quit();
    }

    @Override
    public boolean isScreencast() {
        return true;
    }

    private int getDeviceOrientation() {
        int orientation = 1;
        WindowManager wm = (WindowManager)this.applicationContext.getSystemService(Context.WINDOW_SERVICE);
        switch(wm.getDefaultDisplay().getRotation()) {
            case 0:
            default:
                orientation = 0;
                break;
            case 1:
                orientation = 90;
                break;
            case 2:
                orientation = 180;
                break;
            case 3:
                orientation = 270;
        }
        return orientation;
    }

    private void heavySet(int width, int height){
        mOrientation = getDeviceOrientation();
        if (mOrientation == 0||mOrientation==180){
            mWidth = Math.min(width,height);
            mHeight = Math.max(width,height);
        }else {
            mWidth = Math.max(width,height);
            mHeight = Math.min(width,height);
        }
    }

    private Runnable vhrun = new Runnable() {
        @Override
        public void run() {
            changeCaptureFormat(mWidth,mHeight,mFrameRate);
        }
    };
}
