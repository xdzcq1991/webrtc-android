package com.example.kingo.rtcclient.util;

import android.app.Activity;
import android.os.Build;
import android.view.DisplayCutout;
import android.view.View;
import android.view.WindowInsets;

import java.lang.reflect.Method;

/**
 * Created by kingo on 2019/8/28.
 */

public class ScreenUtil {

	public static boolean hasNotchInScreen(Activity activity){
		if (Build.VERSION.SDK_INT >= 28) {
			View decorView = activity.getWindow().getDecorView();
			WindowInsets windowInsets = decorView.getRootWindowInsets();
			if (windowInsets!=null){
				DisplayCutout displayCutout = windowInsets.getDisplayCutout();
				if (displayCutout != null) {
					// 说明有刘海屏
					return true;
				}
			}else {
				//这里判断有问题?????
				String manufacturer = Build.MANUFACTURER;

				if ("".equals(manufacturer)) {
					return false;
				} else if (manufacturer.equalsIgnoreCase("HUAWEI")) {
					return hasNotchHw(activity);
				} else if (manufacturer.equalsIgnoreCase("xiaomi")) {
					return hasNotchXiaoMi(activity);
				} else if (manufacturer.equalsIgnoreCase("oppo")) {
					return hasNotchOPPO(activity);
				} else if (manufacturer.equalsIgnoreCase("vivo")) {
					return hasNotchVIVO(activity);
				} else {
					return false;
				}
			}
		} else {
			// 通过其他方式判断是否有刘海屏  目前官方提供有开发文档的就 小米，vivo，华为（荣耀），oppo
			String manufacturer = Build.MANUFACTURER;

			if ("".equals(manufacturer)) {
				return false;
			} else if (manufacturer.equalsIgnoreCase("HUAWEI")) {
				return hasNotchHw(activity);
			} else if (manufacturer.equalsIgnoreCase("xiaomi")) {
				return hasNotchXiaoMi(activity);
			} else if (manufacturer.equalsIgnoreCase("oppo")) {
				return hasNotchOPPO(activity);
			} else if (manufacturer.equalsIgnoreCase("vivo")) {
				return hasNotchVIVO(activity);
			} else {
				return false;
			}
		}
		return false;
	}


	/**
	 * 判断vivo是否有刘海屏
	 * 
	 * https://swsdl.vivo.com.cn/appstore/developer/uploadfile/20180328/20180328152252602.pdf
	 *
	 * @param activity
	 * @return
	 */
	private static boolean hasNotchVIVO(Activity activity) {
		try {
			Class<?> c = Class.forName("android.util.FtFeature");
			Method get = c.getMethod("isFeatureSupport", int.class);
			return (boolean) (get.invoke(c, 0x20));
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 判断oppo是否有刘海屏
	 * https://open.oppomobile.com/wiki/doc#id=10159
	 *
	 * @param activity
	 * @return
	 */
	private static boolean hasNotchOPPO(Activity activity) {
		return activity.getPackageManager().hasSystemFeature("com.oppo.feature.screen.heteromorphism");
	}

	/**
	 * 判断xiaomi是否有刘海屏
	 * https://dev.mi.com/console/doc/detail?pId=1293
	 *
	 * @param activity
	 * @return
	 */
	private static boolean hasNotchXiaoMi(Activity activity) {
		try {
			Class<?> c = Class.forName("android.os.SystemProperties");
			Method get = c.getMethod("getInt", String.class, int.class);
			return (int) (get.invoke(c, "ro.miui.notch", 0)) == 1;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 判断华为是否有刘海屏
	 * https://devcenter-test.huawei.com/consumer/cn/devservice/doc/50114
	 *
	 * @param activity
	 * @return
	 */
	private static boolean hasNotchHw(Activity activity) {

		try {
			ClassLoader cl = activity.getClassLoader();
			Class HwNotchSizeUtil = cl.loadClass("com.huawei.android.util.HwNotchSizeUtil");
			Method get = HwNotchSizeUtil.getMethod("hasNotchInScreen");
			return (boolean) get.invoke(HwNotchSizeUtil);
		} catch (Exception e) {
			return false;
		}
	}
}
