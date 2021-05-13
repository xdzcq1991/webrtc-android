package com.example.kingo.rtcclient.util;

import android.net.TrafficStats;

/**
 * Created by kingo on 2019/10/10.
 */

public class NetSpeed {
	private static final String TAG = NetSpeed.class.getSimpleName();
	private long lastTotalRxBytes = 0;
	private long lastTotalTxBytes = 0;
	private long lastTimeStamp = 0;

	public String getNetSpeed(int uid) {
		long[] datas = getTotalRxBytes(uid);
		long nowTimeStamp = System.currentTimeMillis();
		long speed = ((datas[0] - lastTotalRxBytes) * 1000 / (nowTimeStamp - lastTimeStamp));//毫秒转换
		long speed1 = ((datas[1] - lastTotalTxBytes) * 1000 / (nowTimeStamp - lastTimeStamp));//毫秒转换
		lastTimeStamp = nowTimeStamp;
		lastTotalRxBytes = datas[0];
		lastTotalTxBytes = datas[1];
		return "收："+String.valueOf(speed) + " kb/s;发："+String.valueOf(speed1) + " kb/s";
	}

	//getApplicationInfo().uid
	public long[] getTotalRxBytes(int uid) {
		long rxBytes = TrafficStats.getUidRxBytes(uid) == TrafficStats.UNSUPPORTED ? 0 : (TrafficStats.getUidRxBytes(uid) / 1024);
		long txBytes = TrafficStats.getUidTxBytes(uid) == TrafficStats.UNSUPPORTED ? 0 : (TrafficStats.getUidTxBytes(uid) / 1024);
		long[] datas = new long[]{rxBytes,txBytes};
		return datas;//转为KB
	}
}
