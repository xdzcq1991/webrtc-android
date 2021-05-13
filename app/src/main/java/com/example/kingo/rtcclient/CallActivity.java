package com.example.kingo.rtcclient;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kingo.rtcclient.util.AppRTCAudioManager;
import com.example.kingo.rtcclient.util.CallBackUtil;
import com.example.kingo.rtcclient.util.DimenUtils;
import com.example.kingo.rtcclient.util.WebRTCInternetUtil;
import com.example.kingo.rtcclient.util.KingoScreenCapturer;
import com.example.kingo.rtcclient.util.NetSpeed;
import com.example.kingo.rtcclient.util.NetSpeedTimer;
import com.example.kingo.rtcclient.util.OkhttpUtil;
import com.example.kingo.rtcclient.util.ScreenUtil;
import com.example.kingo.rtcclient.view.ChatView;
import com.example.kingo.rtcclient.view.CircleButton;
import com.example.kingo.rtcclient.view.DragScaleAblePopupWindow;
import com.example.kingo.rtcclient.view.PaletteView;
import com.example.mytoast.ToastUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.EglBase;
import org.webrtc.Logging;
import org.webrtc.RendererCommon;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

import okhttp3.Call;

public class CallActivity extends AppCompatActivity implements WebSocketMSGListener
																,WebRtcClient.SendMsgEvent
																,PaletteView.Callback
																,View.OnClickListener {
	private Context mContext;
	private BaseApplication mApplication;
	private String m_strLoginID = "";
	private String m_strLoginPWD = "123456";
	private String token = "";
	private String user_code = "";
	private String TAG = "MainActivity";

	private WebRtcClient mClient;

	private String uId;
	private String tId;
	private String tName;

	private boolean iceConnected = false;

	private KingoScreenCapturer canvasCapturer = null;
	private VideoCapturer videoCapturer = null;

	/**
	 * 视频音频
	 */

	private String[] MANDATORY_PERMISSIONS = {
			Manifest.permission.CAMERA,
			Manifest.permission.RECORD_AUDIO,
			Manifest.permission.INTERNET,
			Manifest.permission.ACCESS_NETWORK_STATE,
			Manifest.permission.WRITE_EXTERNAL_STORAGE,
			Manifest.permission.MODIFY_AUDIO_SETTINGS
	};


	public static String EXTRA_ROOMID = "";

	/**
	 * 屏幕分辨率参数传递
	 */
	private int width;
	private int height;
	private int[] sizes;
	/**
	 * 视图碎片
	 */
	private boolean callControlFragmentVisible = true;

	/**
	 * 渲染相关参数
	 */
	private class ProxyRenderer implements VideoRenderer.Callbacks {
		private VideoRenderer.Callbacks target;

		synchronized public void renderFrame(VideoRenderer.I420Frame i420Frame) {
			if (target == null) {
				Logging.d(TAG, "dropping frame");
				VideoRenderer.renderFrameDone(i420Frame);
				return;
			}
			target.renderFrame(i420Frame);

		}

		synchronized void setTarget(VideoRenderer.Callbacks target) {
			this.target = target;
		}
	}

	private final ProxyRenderer remoteProxyScreenRenderer = new ProxyRenderer();
	private final ProxyRenderer remoteProxyCameraRenderer = new ProxyRenderer();
	private SurfaceViewRenderer pipRenderer;
	private SurfaceViewRenderer fullscreenRenderer;
	private EglBase rootEglBase;
	//    private final List<VideoRenderer.Callbacks> remoteRenderers = new ArrayList<>();
//	private boolean isSwappedFeeds;


	//画板
	private LinearLayout palette_banner;
	private View mUndoView;
	private View mRedoView;
	private CircleButton mPenView;
	private CircleButton mEraserView;
	private CircleButton mClearView;
	private CircleButton mThickView;
	private CircleButton mThinView;
  private CircleButton mLineView;
  private CircleButton mRectView;
  private CircleButton mCircleView;

	private View mBlackView;
	private View mRedView;
	private PaletteView mPaletteView;
	private TextView paintToolsToggler;
	private TextView hangUpBtn;

	private PopupWindow mPopupWindow,mVideoWindow;

	private Intent permissionIntent = null;
	private MediaProjectionManager mediaProjectionManager;

	private int lHBannerHeight = 0;
	private boolean isOnline = false;
	private boolean isShowPalette = true;

	private ProgressDialog dialog;

	private NetSpeedTimer mNetSpeedTimer;

	private AppRTCAudioManager audioManager = null;

	private ChatView mChatView;

	@Override
	public void onUndoRedoStatusChanged() {
		mUndoView.setEnabled(mPaletteView.canUndo());
		mRedoView.setEnabled(mPaletteView.canRedo());
	}

	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	@Override
	public void onClick(View v) {
    int i = v.getId();
    if (i == R.id.undo) {
      mPaletteView.undo();
      //				mPopupWindow.dismiss();

    } else if (i == R.id.redo) {
      mPaletteView.redo();
      //				mPopupWindow.dismiss();

    } else if (i == R.id.pen) {
      v.setSelected(true);
      ((CircleButton)v).setUsed();
      mEraserView.setUnUsed();
      mLineView.setUnUsed();
      mCircleView.setUnUsed();
      mRectView.setUnUsed();
      mPaletteView.setMode(PaletteView.Mode.DRAW);
      mPopupWindow.dismiss();

    } else if (i == R.id.eraser) {
      v.setSelected(true);
      ((CircleButton)v).setUsed();
      mPenView.setUnUsed();
      mLineView.setUnUsed();
      mCircleView.setUnUsed();
      mRectView.setUnUsed();
      mPaletteView.setMode(PaletteView.Mode.ERASER);
      mPopupWindow.dismiss();

    } else if (i == R.id.clear) {
      mPaletteView.clear();
      mPopupWindow.dismiss();

    } else if (i == R.id.hang_up) {//				dialog = ProgressDialog.show(mContext, null, "正在挂断");
      disconnect();
//      ((TextView)findViewById(R.id.speed)).setText("123");
    } else if (i == R.id.paint_tools_toggler) {
      mPopupWindow.showAtLocation(paintToolsToggler, Gravity.CENTER|Gravity.BOTTOM, 0, 0);
//      Animation animation = AnimationUtils.loadAnimation(mContext,R.anim.pop_push_down_out_y);
//      paintToolsToggler.startAnimation(animation);
//      paintToolsToggler.setVisibility(View.GONE);

    } else if (i == R.id.thick) {
      ((CircleButton)v).setUsed();
      mThinView.setUnUsed();
      mPaletteView.setPaintSize(3);
      mPopupWindow.dismiss();

    } else if (i == R.id.thin) {
      ((CircleButton)v).setUsed();
      mThickView.setUnUsed();
      mPaletteView.setPaintSize(1);
      mPopupWindow.dismiss();

    } else if (i == R.id.black) {
      mPaletteView.setPaintColor(Color.BLACK);

    } else if (i == R.id.red) {
      mPaletteView.setPaintColor(Color.RED);

    }else if (i == R.id.mode_circle){
      ((CircleButton)v).setUsed();
      mEraserView.setUnUsed();
      mLineView.setUnUsed();
      mPenView.setUnUsed();
      mRectView.setUnUsed();
      mPaletteView.setMode(PaletteView.Mode.CIRCLE);
      mPopupWindow.dismiss();

    }else if (i == R.id.mode_line){
      ((CircleButton)v).setUsed();
      mEraserView.setUnUsed();
      mCircleView.setUnUsed();
      mPenView.setUnUsed();
      mRectView.setUnUsed();
      mPaletteView.setMode(PaletteView.Mode.LINE);
      mPopupWindow.dismiss();

    }else if (i == R.id.mode_rect){
      ((CircleButton)v).setUsed();
      mEraserView.setUnUsed();
      mCircleView.setUnUsed();
      mPenView.setUnUsed();
      mLineView.setUnUsed();
      mPaletteView.setMode(PaletteView.Mode.RECT);
      mPopupWindow.dismiss();

    }
	}

	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Thread.setDefaultUncaughtExceptionHandler(new UnhandledExceptionHandler(this));

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
				| WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
				| WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		getWindow().getDecorView().setSystemUiVisibility(getSystemUiVisibility());
		setContentView(R.layout.activity_call);
		mContext = this;
		mApplication = new BaseApplication(this);
		ActivityCompat.requestPermissions(this, this.MANDATORY_PERMISSIONS, 0);
		mApplication.setListener(this);
		initView();
		initScreen();

		startScreenCapture();

		if (ScreenUtil.hasNotchInScreen(this)){
      int resourceId = mContext.getResources().getIdentifier("status_bar_height", "dimen", "android");
      if (resourceId > 0) {
        lHBannerHeight = mContext.getResources().getDimensionPixelSize(resourceId);
        Log.v("TOPDIS123",lHBannerHeight+"");
      }
    }
		Intent intent = getIntent();
		uId = intent.getStringExtra("uId");
		token = intent.getStringExtra("token");
		Log.v("thread","onCreate-->"+Thread.currentThread().getId()+"");

		Handler handler = new Handler(new Handler.Callback() {
			@Override
			public boolean handleMessage(Message msg) {
				switch (msg.what) {
					case NetSpeedTimer.NET_SPEED_TIMER_DEFAULT:
						final String speed = (String)msg.obj;
						//打印你所需要的网速值，单位默认为kb/s
						Log.i(TAG, "current net speed  = " + speed);
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
//								((TextView)findViewById(R.id.speed)).setText(speed);
							}
						});
						break;

					default:
						break;
				}
				return false;
			}
		});

		//创建NetSpeedTimer实例
		mNetSpeedTimer = new NetSpeedTimer(this, new NetSpeed(), handler).setDelayTime(1000).setPeriodTime(500);
		//在想要开始执行的地方调用该段代码
		mNetSpeedTimer.startSpeedTimer();

	}

	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	@Override
	protected void onDestroy() {
		Thread.setDefaultUncaughtExceptionHandler(null);
//		disconnect();
		if(null != mNetSpeedTimer){
			mNetSpeedTimer.stopSpeedTimer();
		}
		rootEglBase.release();
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	@Override
	public void onBackPressed() {
		//重写返回监听
//		super.onBackPressed();
//		disconnect();
	}

	//初始化组件
	private void initView() {
//		this.iceConnected = false;
    ViewGroup view = (ViewGroup) LayoutInflater.from(mContext).inflate(R.layout.view_chat,null);
    mVideoWindow = new DragScaleAblePopupWindow(view,mContext);

		pipRenderer = view.findViewById(R.id.pipvideo); //findViewById(R.id.pip_video_view);
		fullscreenRenderer =  findViewById(R.id.fullscreen_video_view);
		mPaletteView = findViewById(R.id.palette);
		palette_banner = findViewById(R.id.palette_banner);
		paintToolsToggler = findViewById(R.id.paint_tools_toggler);
		hangUpBtn = findViewById(R.id.hang_up);
		paintToolsToggler.setOnClickListener(this);
		hangUpBtn.setOnClickListener(this);
		mPaletteView.setCallback(this);

		pipRenderer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
//				setSwappedFeeds(!isSwappedFeeds);
			}
		});
		fullscreenRenderer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleCallControlFragmentVisibility();
			}
		});

		rootEglBase = EglBase.create();

    //控制是否显示对方图像视频
    if (WebRTCInternetUtil.kShowRemoteVideo){
      pipRenderer.setVisibility(View.VISIBLE);
      pipRenderer.init(rootEglBase.getEglBaseContext(), null);
      pipRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
      pipRenderer.setZOrderMediaOverlay(true);
      pipRenderer.setEnableHardwareScaler(true);
      remoteProxyCameraRenderer.setTarget(pipRenderer);
    }else {
      pipRenderer.setVisibility(View.GONE);
    }

		fullscreenRenderer.init(rootEglBase.getEglBaseContext(), null);
		fullscreenRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
		fullscreenRenderer.setEnableHardwareScaler(true);

		remoteProxyScreenRenderer.setTarget(fullscreenRenderer);
		fullscreenRenderer.setMirror(false);//!isSwappedFeeds



	}

	//初始化数据
	private void initData() {
		//初始化peer对象
		videoCapturer = createVideoCapture();
		canvasCapturer = createCanvasCapture();
		mClient = new WebRtcClient(
				CallActivity.this
				,videoCapturer
				,canvasCapturer
				,CallActivity.this.width
				,CallActivity.this.height
				,rootEglBase.getEglBaseContext()
				,remoteProxyCameraRenderer
				,remoteProxyScreenRenderer
				,mContext
		);
    pipRenderer.setBackgroundResource(0);
		connectWS();
	}

	//初始化屏幕
	@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
  private void initScreen(){
		DisplayMetrics displayMetrics =new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
		this.width = displayMetrics.widthPixels;
		this.height = displayMetrics.heightPixels;

		sizes = getCanvasSize();
		if (sizes[0]==0){
			ToastUtil.showToast(mContext,"暂不支持该设备");
			return;
		}else {
			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(sizes[0],sizes[1]);
			//			params.gravity = Gravity.CENTER;
			palette_banner.setLayoutParams(params);
			fullscreenRenderer.setLayoutParams(params);

			FrameLayout.LayoutParams btnParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			if (sizes[2]==1){
				//高度占满  左右布局
				btnParams.gravity = Gravity.RIGHT|Gravity.CENTER;
				paintToolsToggler.setPadding(DimenUtils.dp2pxInt(5),DimenUtils.dp2pxInt(5),DimenUtils.dp2pxInt(2),DimenUtils.dp2pxInt(5));
				hangUpBtn.setPadding(DimenUtils.dp2pxInt(5),DimenUtils.dp2pxInt(5),DimenUtils.dp2pxInt(2),DimenUtils.dp2pxInt(5));
			}else {
				//宽度占满  上下布局
				btnParams.gravity = Gravity.BOTTOM|Gravity.CENTER;
				paintToolsToggler.setPadding(DimenUtils.dp2pxInt(5),DimenUtils.dp2pxInt(5),DimenUtils.dp2pxInt(5),DimenUtils.dp2pxInt(2));
				hangUpBtn.setPadding(DimenUtils.dp2pxInt(5),DimenUtils.dp2pxInt(5),DimenUtils.dp2pxInt(5),DimenUtils.dp2pxInt(2));
			}

			initPopMenu(sizes[2]==1);
		}
	}

	//初始化画笔工具
	private void initPopMenu(boolean flag){
		View menuView = LayoutInflater.from(mContext).inflate(R.layout.popmenu_paint_tools,null);
		mPopupWindow = new PopupWindow(menuView, ActionBar.LayoutParams.WRAP_CONTENT,ActionBar.LayoutParams.WRAP_CONTENT);
		mUndoView = menuView.findViewById(R.id.undo);
		mRedoView = menuView.findViewById(R.id.redo);
    mBlackView = menuView.findViewById(R.id.black);
    mRedView = menuView.findViewById(R.id.red);

		mEraserView = menuView.findViewById(R.id.eraser);
    mLineView = menuView.findViewById(R.id.mode_line);
    mPenView = menuView.findViewById(R.id.pen);
    mThickView = menuView.findViewById(R.id.thick);
    mThinView = menuView.findViewById(R.id.thin);
		mClearView = menuView.findViewById(R.id.clear);
    mCircleView = menuView.findViewById(R.id.mode_circle);
    mRectView = menuView.findViewById(R.id.mode_rect);

		LinearLayout banner = menuView.findViewById(R.id.pop_banner);
		if (!flag){
			mPopupWindow.setAnimationStyle(R.style.AnimationPreviewHorizon);
			banner.setOrientation(LinearLayout.VERTICAL);
		}else {
			mPopupWindow.setAnimationStyle(R.style.AnimationPreviewVertical);
			banner.setOrientation(LinearLayout.HORIZONTAL);
		}

		mUndoView.setOnClickListener(this);
		mRedoView.setOnClickListener(this);
		mPenView.setOnClickListener(this);
		mEraserView.setOnClickListener(this);
		mClearView.setOnClickListener(this);
		mThickView.setOnClickListener(this);
		mThinView.setOnClickListener(this);
		mBlackView.setOnClickListener(this);
		mRedView.setOnClickListener(this);
    mLineView.setOnClickListener(this);
    mCircleView.setOnClickListener(this);
    mRectView.setOnClickListener(this);

		mUndoView.setEnabled(false);
		mRedoView.setEnabled(false);

		mPopupWindow.setOutsideTouchable(true);
		mPopupWindow.setBackgroundDrawable(new BitmapDrawable());
		mPopupWindow.setFocusable(false);
    mPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
      @Override
      public void onDismiss() {
//        Animation animation = AnimationUtils.loadAnimation(mContext,R.anim.pop_push_up_in_y);
//        paintToolsToggler.startAnimation(animation);
//        paintToolsToggler.setVisibility(View.VISIBLE);
      }
    });

//		mPopupWindow.showAtLocation(mUndoView,Gravity.RIGHT,0,0);
	}

	//断开连接
	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	private void disconnect(){
		mApplication.close();
		remoteProxyScreenRenderer.setTarget(null);
		remoteProxyCameraRenderer.setTarget(null);

		if (pipRenderer != null) {
			pipRenderer.release();
			Log.v("closeRtc","release pipRenderer");
			pipRenderer = null;
		}
		if (fullscreenRenderer != null) {
			fullscreenRenderer.release();
			Log.v("closeRtc","release fullscreenRenderer");
			fullscreenRenderer = null;
		}
		if (mClient!=null){
			mClient.close();
		}
		if (audioManager != null) {
			audioManager.stop();
			audioManager = null;
		}
		Log.v("time1","startclose-->"+new Date().getTime());
//		try {
//			Thread.sleep(800);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//		finish();
	}

	//开启屏幕录制
	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	private void startScreenCapture(){
		mediaProjectionManager = (MediaProjectionManager) getApplication().getSystemService(MEDIA_PROJECTION_SERVICE);
		startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(),1);
	}

	//***
	@TargetApi(19)
	private static int getSystemUiVisibility() {
		int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
		}
		return flags;
	}

	//点击播放器，查询对方画板使用情况
	private void toggleCallControlFragmentVisibility() {
		JSONObject object = new JSONObject();
		try {
			object.put("type","CMD");
			object.put("content","Query_Canvas_Status");
			if (mClient!=null){
				mClient.sendDataChannelMsg(object.toString());
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

	}

	//截图
	private Bitmap createBitmap(View view) {
		view.buildDrawingCache();
		Bitmap bitmap = view.getDrawingCache();
		return bitmap;
	}

	//截图1
	public static Bitmap getBitmapFromView(View v) {
		Bitmap b = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.RGB_565);
		Canvas c = new Canvas(b);
		v.layout(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
		// Draw background
		//		Drawable bgDrawable = v.getBackground();
		//		if (bgDrawable != null)
		//			bgDrawable.draw(c);
		//		else
		//			c.drawColor(Color.WHITE);
		// Draw view to canvas
		v.draw(c);
		return b;
	}

	private void saveBitmap(Bitmap bitmap) {
		FileOutputStream fos;
		try {
			File root = Environment.getExternalStorageDirectory();
			File file = new File(root, "test.png");
			fos = new FileOutputStream(file);
			bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
			fos.flush();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	//连接websocket
	private void connectWS() {
		String signal_server = WebRTCInternetUtil.kSignalServerAddress+"?userAccount="+getSharedPreferences("info",MODE_PRIVATE).getString("user_code","")+"&appKey="+ WebRTCInternetUtil.APP_KEY+"&tok="+token+"&dev=PC" +
      "&env" +
      "=prod";
		Log.v("wsAddr",signal_server);
		mApplication.setSignal_server(signal_server);
	}

	//根据对方peer_code获得对方的id
	private void getOtherUserInfoByAccount(){
		HashMap params = new HashMap();
		params.put("uid", getSharedPreferences("info",MODE_PRIVATE).getString("user_code",""));
		params.put("otherUserAccount", getSharedPreferences("info",MODE_PRIVATE).getString("peer_code",""));
		params.put("appKey", WebRTCInternetUtil.APP_KEY);
		HashMap header = new HashMap();
		header.put("authorization","bearer " + token);
		OkhttpUtil.okHttpGet(WebRTCInternetUtil.kSignalHttpAddress + "auth/getOtherUserInfoByAccount", params,header, new CallBackUtil.CallBackString() {
			@Override
			public void onFailure(Call call, Exception e) {
				Toast.makeText(mContext,"getOtherUserInfoByAccount-失败",Toast.LENGTH_LONG).show();
			}

			@Override
			public void onResponse(String response) {
				try {
					JSONObject object = new JSONObject(response);
					String success = object.getString("SUCCESS");
					if (success.equals("1")){

						JSONObject userInfo = object.getJSONObject("DATA").getJSONObject("userInfo");
						JSONArray userStatus = object.getJSONObject("DATA").getJSONArray("userStatus");
						for (int j=0;j<userStatus.length();j++){
							String state = userStatus.getString(j);
							if (state.equals("PC")){
								//当前为在线状态  dosth..
								isOnline = true;
								break;
							}
						}
						tId = userInfo.getString("uid");
						tName = userInfo.getString("name");
						if (isOnline){
							//在线 -发起ConnectionCall请求
//							mClient.createOffer();

							sendMSG("ConnectionCall","");

						}else {
							//不在线 -等待--显示画板
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									toggleScreenContent(true);
								}
							});
						}

					}
					Log.v(TAG,response);

				} catch (JSONException e) {
					e.printStackTrace();
					ToastUtil.showToast(mContext,"未安排当前时段排课信息");
//					finish();
				}
			}
		});
	}

	//	屏幕抓取
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private KingoScreenCapturer createCanvasCapture() {
		MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getApplication().getSystemService(MEDIA_PROJECTION_SERVICE);
		MediaProjection mp = mediaProjectionManager.getMediaProjection(-1, permissionIntent);

		int si[] = new int[2];
		palette_banner.getLocationInWindow(si);

//		int laletteLeft = (this.width-sizes[0])/2;
//		int laletteTop = (this.height-sizes[1])/2;

		KingoScreenCapturer canvasCapturer = new KingoScreenCapturer(
				400, mp, this.width, this.height,
				si[0]+lHBannerHeight, si[1], sizes[0], sizes[1]);
		return canvasCapturer;
	}

	//创建VideoCapture
	private VideoCapturer createVideoCapture() {
		VideoCapturer videoCapturer;
		//判断是否支持camer2
		if (Camera2Enumerator.isSupported(this)) {
			Log.d(TAG, "Creating capturer using camera2 API.");
			videoCapturer = createCameraCapturer(new Camera2Enumerator(this));
		} else {
			Log.d(TAG, "Creating capturer using camera1 API.");
			videoCapturer = createCameraCapturer(new Camera1Enumerator(true));
		}
		if (videoCapturer == null) {
			//出错操作，打开摄像头失败
			Log.e(TAG, "failed to open camera");
			return null;
		}
		return videoCapturer;
	}

	private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
		final String[] devideNames = enumerator.getDeviceNames();
		//首先，尝试找到前置摄像头
		for (String deviceName : devideNames) {
			if (enumerator.isFrontFacing(deviceName)) {
				VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
				if (videoCapturer != null) {
					return videoCapturer;
				}
			}
		}
		//如果没有找到前置摄像头，搞点别的事情
		for (String deviceName : devideNames) {
			if (!enumerator.isFrontFacing(deviceName)) {
				VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);
				if (videoCapturer != null) {
					return videoCapturer;
				}
			}
		}
		return null;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		permissionIntent = data;
		initData();
	}

	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	@Override
	public void onConnect() {

	}

	@Override
	public void onMessage(String msg) {
		try {
			Log.v("thread","onMessage中-->"+Thread.currentThread().getId());
			JSONObject object = new JSONObject(msg);
			String mtp = object.has("mtp")?object.getString("mtp"):"";
			if (mtp.equals("0")){
				String txt = object.has("txt")?object.getString("txt"):"";
				if (txt.equals("0")){
					getOtherUserInfoByAccount();
				}else if (txt.equals("closed")){
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
              AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
              builder.setMessage("当前账号已在其他设备登录!");
              builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                  finish();
                }
              });
              builder.show();
							Log.v("thread","runOnUiThread-->"+Thread.currentThread().getId()+"");
						}
					});

				}
			}else if (mtp.equals("2")){
				String receiveObj = object.getString("txt");
				if (receiveObj.equals("ConnectionCall")||receiveObj.equals("PCConnectionCall")){
					//对方请求连接
					mClient.createOffer();
					String usertype = getSharedPreferences("info",MODE_PRIVATE).getString("user_type","");
					if (usertype.equals("STU")){
						//学生 显示视频播放器
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								toggleScreenContent(false);
							}
						});
					}
				}else if (receiveObj.equals("")){
					//txt为空 啥也不干
				}else {
					JSONObject receiveMSG = new JSONObject(receiveObj);
					String type= receiveMSG.has("type")?receiveMSG.getString("type"):"";
					if (type.equalsIgnoreCase("OFFER")){//
						String remoteSDP = receiveMSG.getString("sdp");
						mClient.createAnswer(type,remoteSDP);
					}else if(type.equalsIgnoreCase("ANSWER")){
						String remoteSDP = receiveMSG.getString("sdp");
						mClient.setRemoteSDP(type,remoteSDP);
					}else if (type.equals("")){//Candidate
						mClient.setRemoteCandidate(receiveMSG);
					}
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void sendMSG(String flag,Object message) {
		JSONObject param = new JSONObject();
		try {
			param.put("fid",uId);
			param.put("tid",tId);
			param.put("fro",getSharedPreferences("info",MODE_PRIVATE).getString("user_name",""));
			param.put("ton",tName);
			param.put("mtp","2");
			param.put("typ","0");
			param.put("app", WebRTCInternetUtil.APP_KEY);
			param.put("dev","PC");
			switch (flag){
				case "sessionDescription":
					String type = ((SessionDescription)message).type.toString();
					String sdp = ((SessionDescription)message).description;
					JSONObject object = new JSONObject();
					object.put("type",type.toLowerCase());
					object.put("sdp",sdp);
					param.put("txt",object.toString());
					break;
				case "ConnectionCall":
					param.put("txt","ConnectionCall");
					break;
				case "iceCandidate":
					param.put("txt",message);
					break;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		mApplication.sendText(param.toString());
		Log.v("webSocket-MSG","send-->"+param.toString());
	}

	@Override
	public void onReceiveDataChannelMessage(String msg) {
		Log.v("DataChannel","receive-->"+msg);
		msg = msg.trim().replaceAll("\\s*|\t|\r|\n","");
		try {
			JSONObject object = new JSONObject(msg);
			String type = object.getString("type");
			String content = object.getString("content");
			switch (type){
				case "CMD":
					if(content.equals("Query_Canvas_Status")){
						//对方查询画板使用情况
						JSONObject response = new JSONObject();
						response.put("type","CMD");
						if (mPaletteView.isUsing()){
							//正在使用画板
							try {
								response.put("content","Canvas_Using");
								if (mClient!=null){
									mClient.sendDataChannelMsg(response.toString());
								}
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}else {
							//画板处于空闲状态
							try {
								response.put("content","Canvas_UnUsing");
								if (mClient!=null){
									mClient.sendDataChannelMsg(response.toString());
								}
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
					}else if (content.equals("Canvas_Using")){
						//表示画布正在被使用，自己什么都不做
					}else if (content.equals("Canvas_UnUsing")) {
						//表示画布没有被使用，自己取得画布使用权，保存当前视频图片在画布上，并且通知对方更改画布状态
						getPaletteUsing(false);
						JSONObject response = new JSONObject();
						response.put("type","CMD");
						try {
							response.put("content","Release_Canvas");
							if (mClient!=null){
								mClient.sendDataChannelMsg(response.toString());
							}
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}else if (content.equals("Release_Canvas")){
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								toggleScreenContent(false);
							}
						});
					}
					break;
				default:
					break;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onResReleased() {
		finish();
	}

	@Override
	public void onDataChannelCollected() {
		String usertype = getSharedPreferences("info",MODE_PRIVATE).getString("user_type","");
		if (usertype.equals("STU")){
			//学生 显示视频播放器
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					toggleScreenContent(false);
				}
			});
		}else {
			//教师 显示画板
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					toggleScreenContent(true);
				}
			});
		}

//		MyAudioManager myAudioManager = new MyAudioManager(mContext);?
//		myAudioManager.OpenSpeaker();
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				audioManager = AppRTCAudioManager.create(getApplicationContext());
				audioManager.start(new AppRTCAudioManager.AudioManagerEvents() {
					// This method will be called each time the number of available audio
					// devices has changed.
					@Override
					public void onAudioDeviceChanged(AppRTCAudioManager.AudioDevice audioDevice, Set<AppRTCAudioManager.AudioDevice> availableAudioDevices) {
            onAudioManagerDevicesChanged(audioDevice, availableAudioDevices);
					}
				});
			}
		});
	}

	// This method is called when the audio manager reports audio device change,
	// e.g. from wired headset to speakerphone.
  private void onAudioManagerDevicesChanged(final AppRTCAudioManager.AudioDevice device, final Set<AppRTCAudioManager.AudioDevice> availableDevices) {
    Log.d(TAG, "onAudioManagerDevicesChanged: " + availableDevices + ", " + "selected: " + device);
    // TODO(henrika): add callback handler.
  }

	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	@Override
	public void onRemoteDisConnect() {
		//对方断了
		if (!isShowPalette){
			getPaletteUsing(true);
			mPaletteView.setUsing(false);
		}else {
//			reCreateClient();
			mClient.closeInternal(false);
		}
	}

	@Override
	public void reCreateRes() {
		reCreateClient();
	}

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    if (WebRTCInternetUtil.kShowRemoteVideo && hasFocus){
      mVideoWindow.showAtLocation(hangUpBtn,Gravity.TOP|Gravity.LEFT,0,0);
    }

  }

  //重新创建client对象
	private void reCreateClient(){
		mClient = null;
		videoCapturer = null;
		canvasCapturer = null;
		videoCapturer = createVideoCapture();
		canvasCapturer = createCanvasCapture();
		mClient = new WebRtcClient(
				CallActivity.this
				,videoCapturer
				,canvasCapturer
				,CallActivity.this.width
				,CallActivity.this.height
				,rootEglBase.getEglBaseContext()
				,remoteProxyCameraRenderer
				,remoteProxyScreenRenderer
				,mContext
		);
	}

	//获取画板使用权
	private void getPaletteUsing(final boolean reCreate) {
		runOnUiThread(new Runnable() {
			@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
			@Override
			public void run() {
				Bitmap bitmap = canvasCapturer.getLocalScreenBmp();
				mPaletteView.drawRemoteView(bitmap);
				toggleScreenContent(true);
//				saveBitmap(bitmap);
				if (reCreate){
//					reCreateClient();
					mClient.closeInternal(false);
				}
			}
		});
	}

	/**
	 *
	 * @param showpalette 是否显示画板
	 */
	private void toggleScreenContent(boolean showpalette){
		isShowPalette = showpalette;
		if (showpalette){
			palette_banner.setVisibility(View.VISIBLE);
			fullscreenRenderer.setVisibility(View.GONE);
//			mClient.startVideoCapture();
		}else {
			fullscreenRenderer.setVisibility(View.VISIBLE);
			palette_banner.setVisibility(View.GONE);
//			mClient.stopVideoCapture();
		}
	}

	//设置画板的宽高
	private int[] getCanvasSize(){
		boolean findSize = false;
		int[] size = new int[]{0,0,1};
		//画布宽高初始化为屏幕宽高
		int canvasHeight  = height;
		int canvasWidth = width;
		if ((canvasWidth * 6 / 10 ) < canvasHeight){
			//如果宽度的60%小于高度，则以宽度为计算基准
			canvasWidth = width / 4 * 4;  //保证是4的整倍数
			//循环遍历
			for(; canvasWidth > width * 9 / 10; canvasWidth=canvasWidth - 4){
				if((canvasHeight * 6) % 10 == 0){
					canvasHeight = canvasWidth * 6 / 10;
					if(canvasHeight % 4 == 0){
						findSize = true;
						size[2] = -1;
						break;
					}
				}
			}
		}else {
			//如果宽度的60%大于等于高度，则以高度为计算基准
			canvasHeight = height / 4 * 4;
			for(; canvasHeight > height * 9 / 10; canvasHeight = canvasHeight - 4){
				if((canvasHeight * 10) % 6 == 0){
					canvasWidth = canvasHeight * 10 / 6;
					if(canvasWidth % 4 == 0){
						findSize = true;
						size[2] = 1;
						break;
					}
				}
			}
		}
		if (findSize){
			size[0] = canvasWidth;
			size[1] = canvasHeight;
		}else {
		}
		return size;
	}
}
