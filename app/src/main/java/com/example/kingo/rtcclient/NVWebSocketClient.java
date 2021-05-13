package com.example.kingo.rtcclient;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Message;

import com.example.kingo.rtcclient.util.MyLog;
import com.example.mytoast.ToastUtil;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by kinggo on 2018/7/23.
 */

public class NVWebSocketClient {
    private static final int DEFAULT_SOCKET_CONNECTTIMEOUT = 3000;
    private static int DEFAULT_SOCKET_RECONNECTINTERVAL = 2000;
    private static final int FRAME_QUEUE_SIZE = 5;
    WebSocketListener mWebSocketListener;
    WebSocketFactory mWebSocketFactory;
    public WebSocket mWebSocket;
	
    private ConnectStatus mConnectStatus = ConnectStatus.CONNECT_DISCONNECT;
    private Timer mReconnectTimer = new Timer();

    private String mUri;

    public interface WebSocketListener {
    	
    	void onConnected();

        void onTextMessage(WebSocket websocket, String text) throws Exception;
    }

    public enum ConnectStatus {
        CONNECT_DISCONNECT,// 断开连接
        CONNECT_SUCCESS,//连接成功
        CONNECT_FAIL,//连接失败
        CONNECTING;//正在连接
    }

    public Context mContext;

    public NVWebSocketClient(String uri, Context context) {
        this(uri, DEFAULT_SOCKET_CONNECTTIMEOUT);
        mContext = context;
    }

    public NVWebSocketClient(String uri, int timeout) {
        mUri = uri;
        mWebSocketFactory = new WebSocketFactory().setConnectionTimeout(timeout);
    }

    public void setWebSocketListener(WebSocketListener webSocketListener) {
        mWebSocketListener = webSocketListener;
    }

    public void connect() {
        try {
            setConnectStatus(ConnectStatus.CONNECTING);
            mWebSocket = mWebSocketFactory.createSocket(mUri)
                    .setFrameQueueSize(FRAME_QUEUE_SIZE)//设置帧队列最大值为5
                    .setMissingCloseFrameAllowed(false)//设置不允许服务端关闭连接却未发送关闭帧
                    .addListener(new NVWebSocketListener())
                    .setPingInterval(270000) //发送心跳  每270秒一次
//                    .setPongInterval(30000)
                    .connectAsynchronously();
        } catch (IOException e) {
            e.printStackTrace();
            reconnect();
        }
    }
    
    public void sendText(String text){
    	mWebSocket.sendText(text);
	}

    private void setConnectStatus(ConnectStatus connectStatus) {
        mConnectStatus = connectStatus;
    }

    public ConnectStatus getConnectStatus() {
        return mConnectStatus;
    }

    public void disconnect() {
        if (mWebSocket != null) {
            mWebSocket.disconnect();
        }
        setConnectStatus(ConnectStatus.CONNECT_DISCONNECT);
    }


    class NVWebSocketListener extends WebSocketAdapter {

        String TAG = "WebSocket";
        @SuppressLint("HandlerLeak")
        android.os.Handler handler = new android.os.Handler(mContext.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 3:
                        String respose1 = (String) msg.obj;
                        try {
                            JSONObject jsonObject = new JSONObject(respose1);
                            String flag = jsonObject.getString("flg");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        break;
                }
            }
        };

        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
            super.onConnected(websocket, headers);
            System.out.println(TAG + "===OS. WebSocket onConnected11");
            System.out.println(TAG + "===OS. WebSocket onConnected22");
            setConnectStatus(ConnectStatus.CONNECT_SUCCESS);
			mWebSocketListener.onConnected();
        }


        @Override
        public void onConnectError(WebSocket websocket, WebSocketException exception) throws Exception {
            super.onConnectError(websocket, exception);
            System.out.println(TAG + "===OS. WebSocket onConnectError");
            setConnectStatus(ConnectStatus.CONNECT_FAIL);
            reconnect();
        }

        @Override
        public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
            super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer);
            System.out.println(TAG + "===OS. WebSocket onDisconnected");
            setConnectStatus(ConnectStatus.CONNECT_DISCONNECT);
            ToastUtil.showToast(mContext, "断开连接了");
        }

        @Override
        public void onTextMessage(WebSocket websocket, String text) throws Exception {
            super.onTextMessage(websocket, text);
            System.out.println(TAG + "===OS. WebSocket onTextMessage");
            MyLog.i(TAG, text);
            if (mWebSocketListener != null && text != null) {
                num = 0;
                DEFAULT_SOCKET_RECONNECTINTERVAL = 2000;
                mWebSocketListener.onTextMessage(websocket, text);
            } else {
                MyLog.i(TAG, "22222222222222222222222222");
            }
        }
    }

    int num = 0;

    public void reconnect() {
        TimerTask mReconnectTimerTask = new TimerTask() {
            @Override
            public void run() {
                MyLog.i("NVWebSocketClient中，重连线程里面", DEFAULT_SOCKET_RECONNECTINTERVAL * 2 + "秒一次");
                if (mWebSocket != null && !mWebSocket.isOpen()
                        && getConnectStatus() != ConnectStatus.CONNECTING
                        && getConnectStatus() != ConnectStatus.CONNECT_SUCCESS) {
                    num++;
                    connect();
                    DEFAULT_SOCKET_RECONNECTINTERVAL = DEFAULT_SOCKET_RECONNECTINTERVAL * 2;
                } else {
                    num = 0;
                    DEFAULT_SOCKET_RECONNECTINTERVAL = 2000;
                }
                if (num > 4) {
                    mReconnectTimer.cancel();
                    num = 0;
                    DEFAULT_SOCKET_RECONNECTINTERVAL = 2000;
                    MyLog.i("NVWebSocketClient中", "重连线程停止");
                }
            }
        };
        mReconnectTimer.schedule(mReconnectTimerTask, DEFAULT_SOCKET_RECONNECTINTERVAL);
    }

}


