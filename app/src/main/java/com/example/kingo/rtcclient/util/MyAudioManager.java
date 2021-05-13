package com.example.kingo.rtcclient.util;

import android.content.Context;
import android.media.AudioManager;


/**
 * Created by kingo on 2019/11/5.
 */

public class MyAudioManager {
	private Context mContext;
	private AudioManager audioManager;
	private boolean isSpeakerOpen = true;//默认开启手机扬声器  
	private static int currVolume = 0;//当前音量

	public MyAudioManager(Context context) {
		this.mContext = context;
		audioManager = ((AudioManager) context.getSystemService(context.AUDIO_SERVICE));
	}

	//打开扬声器
	public void OpenSpeaker() {
		try {
			audioManager.setMode(AudioManager.ROUTE_SPEAKER);
			// 获取当前通话音量
			currVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);

			if (!audioManager.isSpeakerphoneOn()) {
				audioManager.setSpeakerphoneOn(true);

				audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
						audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
						AudioManager.STREAM_VOICE_CALL);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//关闭扬声器
	public void CloseSpeaker() {
		try {
			if (audioManager != null) {
				if (audioManager.isSpeakerphoneOn()) {
					audioManager.setSpeakerphoneOn(false);
					audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, currVolume,AudioManager.STREAM_VOICE_CALL);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
