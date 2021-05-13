package com.example.kingo.rtcclient;

/**
 * Created by kingo on 2019/7/9.
 */

public interface WebSocketMSGListener {
	void onConnect();
	void onMessage(String msg);
	
}
