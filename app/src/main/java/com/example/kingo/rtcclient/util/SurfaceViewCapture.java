package com.example.kingo.rtcclient.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * Created by kingo on 2019/7/25.
 */

public class SurfaceViewCapture {
	private static final String DEVICE_NAME = "/dev/graphics/fb0";
	private static String TAG = "SurfaceViewCapture";
	
	public static Bitmap acquireScreenshot(Context context) {
		WindowManager mWinManager = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics metrics = new DisplayMetrics();
		Display display = mWinManager.getDefaultDisplay();
		display.getMetrics(metrics);
		// 屏幕高
		int height = metrics.heightPixels;
		// 屏幕的宽
		int width = metrics.widthPixels;

		int pixelformat = display.getPixelFormat();
		PixelFormat localPixelFormat1 = new PixelFormat();
		PixelFormat.getPixelFormatInfo(pixelformat, localPixelFormat1);
		// 位深
		int deepth = localPixelFormat1.bytesPerPixel;

		byte[] arrayOfByte = new byte[height * width * deepth];
		try {
			// 读取设备缓存，获取屏幕图像流
//			InputStream localInputStream = readAsRoot();
//			DataInputStream localDataInputStream = new DataInputStream(
//					localInputStream);
//			localDataInputStream.readFully(arrayOfByte);
//			localInputStream.close();
			// 获取fb0数据输入流
			InputStream stream = new FileInputStream(new File(
					"/dev/graphics/fb0"));
			DataInputStream dStream = new DataInputStream(stream);
			dStream.readFully(arrayOfByte);

			int[] tmpColor = new int[width * height];
			int r, g, b;
			for (int j = 0; j < width * height * deepth; j += deepth) {
				b = arrayOfByte[j] & 0xff;
				g = arrayOfByte[j + 1] & 0xff;
				r = arrayOfByte[j + 2] & 0xff;
				tmpColor[j / deepth] = (r << 16) | (g << 8) | b | (0xff000000);
			}
			// 构建bitmap
			Bitmap scrBitmap = Bitmap.createBitmap(tmpColor, width, height,
					Bitmap.Config.ARGB_8888);
			return scrBitmap;

		} catch (Exception e) {
			Log.d(TAG, "#### 读取屏幕截图失败");
			e.printStackTrace();
		}
		return null;

	}
	
	/**
	 * 屏幕截图
	 * @param activity
	 * @return
	 */
	public static Bitmap screenShot(AppCompatActivity activity, String filePath) {
		if (activity == null){
//			Logger.getLogger().e("screenShot--->activity is null");
			return null;
		}
		View view = activity.getWindow().getDecorView();
		//允许当前窗口保存缓存信息
		view.setDrawingCacheEnabled(true);
		view.buildDrawingCache();

		WindowManager mWinManager = (WindowManager) activity
				.getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics metrics = new DisplayMetrics();
		Display display = mWinManager.getDefaultDisplay();
		display.getMetrics(metrics);
		// 屏幕高
		int height = metrics.heightPixels;
		// 屏幕的宽
		int width = metrics.widthPixels;

		// 全屏不用考虑状态栏，有导航栏需要加上导航栏高度
		Bitmap bitmap = null;
		try {
			bitmap = Bitmap.createBitmap(view.getDrawingCache(), 0, 0, width,
					height + 0);
		} catch (Exception e) {
			// 这里主要是为了兼容异形屏做的处理，我这里的处理比较仓促，直接靠捕获异常处理
			// 其实vivo oppo等这些异形屏手机官网都有判断方法
			// 正确的做法应该是判断当前手机是否是异形屏，如果是就用下面的代码创建bitmap


			String msg = e.getMessage();
			// 部分手机导航栏高度不占窗口高度，不用添加，比如OppoR15这种异形屏
			if (msg.contains("<= bitmap.height()")){
				try {
					bitmap = Bitmap.createBitmap(view.getDrawingCache(), 0, 0, width,
							height);
				} catch (Exception e1) {
					msg = e1.getMessage();
					// 适配Vivo X21异形屏，状态栏和导航栏都没有填充
					if (msg.contains("<= bitmap.height()")) {
						try {
							bitmap = Bitmap.createBitmap(view.getDrawingCache(), 0, 0, width,
									height - 0);
						} catch (Exception e2) {
							e2.printStackTrace();
						}
					}else {
						e1.printStackTrace();
					}
				}
			}else {
				e.printStackTrace();
			}
		}

		//销毁缓存信息
		view.destroyDrawingCache();
		view.setDrawingCacheEnabled(false);

		if (null != bitmap){
			try {
//				compressAndGenImage(bitmap,filePath);
//				Logger.getLogger().d("--->截图保存地址：" + filePath);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return bitmap;
	}
}
