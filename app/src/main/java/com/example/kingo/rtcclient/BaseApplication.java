package com.example.kingo.rtcclient;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.neovisionaries.ws.client.WebSocket;

import org.webrtc.PeerConnectionFactory;

/**
 * Created by kingo on 2019/7/8.
 */

public class BaseApplication {
	public NVWebSocketClient webSocket;
	String signal_server = "";
	WebSocketMSGListener mListener;
	private Context mContext;
//	@Override
//	public void onCreate() {
//		super.onCreate();
//    PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(this).createInitializationOptions());
//
//	}


  public BaseApplication(Context context) {
    mContext = context;
    PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(mContext).createInitializationOptions());
  }

  public void setListener(WebSocketMSGListener listener) {
		mListener = listener;
	}

	public void setSignal_server(String signal_server) {
		this.signal_server = signal_server;



		webSocket = new NVWebSocketClient(
				signal_server
				,mContext
		);
		webSocket.connect();
		webSocket.setWebSocketListener(new NVWebSocketClient.WebSocketListener() {
			@Override
			public void onConnected() {
				Log.v("webSocket-MSG","WS连接上了");
				mListener.onConnect();
			}

			@Override
			public void onTextMessage(WebSocket websocket, String text) throws Exception {
				Log.v("webSocket-MSG","receive-->"+text);
				mListener.onMessage(text);
			}
		});
	}

	public void close(){
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (webSocket!=null){
					webSocket.disconnect();
					webSocket = null;
					Log.v("closeRtc","try to close webSocket");
				}
			}
		}).start();
	}

	public void sendText(String text){
		webSocket.sendText(text);
	}

}
