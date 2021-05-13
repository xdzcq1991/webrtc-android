package com.example.kingo.rtcclient;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.example.kingo.rtcclient.util.WebRTCInternetUtil;
import com.example.kingo.rtcclient.util.KingoScreenCapturer;
import com.example.mytoast.ToastUtil;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by kingo on 2019/7/9.
 */

public class WebRtcClient {
	private static final String TAG = "WebRtcClient";
	private Context mContext;
	private PeerConnectionFactory peerConnectionFactory;
	private LinkedList<PeerConnection.IceServer> iceServers = new LinkedList<>();
	private String mClientId;
	private MediaConstraints constraints = new MediaConstraints();
	private SendMsgEvent mMsgEvent;
	private ExecutorService executorService;//线程池管理
	private EglBase.Context egContext;

	private int videoWidth;
	private int videoHeight;
	private int videoFps;


	private MediaStream mediaStream;

	private boolean videoCapturerStopped;
	private boolean isError;
	private boolean isInited;
	private boolean renderVideo;
	private boolean renderScreen;
	private boolean enableAudio;

	private AudioSource audioSource;
	private VideoSource videoSource;
	private VideoSource canvasSource;
	private VideoRenderer.Callbacks remoteCameraRenderer;
	private VideoRenderer.Callbacks remoteScreenRenderer;
	//    private List<VideoRenderer.Callbacks> remoteScreenRenderers;
	private MediaConstraints pcConstraints;
	private MediaConstraints audioConstraints;
	private MediaConstraints sdpMediaConstraints;
	private VideoTrack localCanvasTrack;
	private VideoTrack localVideoTrack;
	private VideoTrack remoteVideoTrack;
	private VideoTrack remoteVideoTrack1;
	private AudioTrack localAudioTrack;


	private Peer peer;

	private VideoCapturer mVideoCapturer;
	private KingoScreenCapturer mCanvasCapturer;

	private SessionDescription mDescription = null;

	private boolean hasExchangeSDP = false;
	private boolean remoteOnline = false;

	public WebRtcClient(SendMsgEvent event,VideoCapturer capturer,KingoScreenCapturer canvascapturer,int width,int height,final EglBase.Context renderEGLContext,final VideoRenderer.Callbacks remoteCameraRenderer,
						final VideoRenderer.Callbacks remoteScreenRenderer,Context context) {
		this.executorService = Executors.newSingleThreadExecutor();
		this.mMsgEvent = event;
		this.renderVideo = true;
		this.renderScreen = true;
		this.enableAudio = true;
		this.mVideoCapturer = capturer;
		this.mCanvasCapturer = canvascapturer;
		this.videoWidth = width;
		this.videoHeight = height;
		this.egContext = renderEGLContext;
		this.videoFps = 30;
		this.remoteCameraRenderer = remoteCameraRenderer;
		this.remoteScreenRenderer = remoteScreenRenderer;
		this.mContext = context;

		peerConnectionFactory = new PeerConnectionFactory(new PeerConnectionFactory.Options());
		// create AudioSource
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				createMediaConstraintsInternal();
				createPeerConnectionInternal();
			}
		});
	}

	public VideoCapturer getVideoCapturer() {
		return mVideoCapturer;
	}

	public void setVideoCapturer(VideoCapturer videoCapturer) {
		mVideoCapturer = videoCapturer;
	}

	public Context getContext() {
		return mContext;
	}

	public void setContext(Context context) {
		mContext = context;
	}

	public void stopCanvasCapture(){
		this.mCanvasCapturer.stopCapture();
	}

	public void startCanvasCapture(){
		this.mCanvasCapturer.startCapture(videoWidth, videoHeight, videoFps);
	}

	public void stopVideoCapture(){
		try {
			this.mVideoCapturer.stopCapture();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void startVideoCapture(){
		this.mVideoCapturer.startCapture(videoWidth, videoHeight, videoFps);
	}


	private VideoTrack createVideoTrack(VideoCapturer videoCapturer) {
		videoSource = peerConnectionFactory.createVideoSource(videoCapturer);
		videoCapturer.startCapture(videoWidth, videoHeight, videoFps);
		localVideoTrack = peerConnectionFactory.createVideoTrack(WebRTCInternetUtil.kVideoLabel, videoSource);
		localVideoTrack.setEnabled(renderVideo);
//		localVideoTrack.addRenderer(new VideoRenderer(remoteCameraRenderer));
		return localVideoTrack;
	}


	private VideoTrack createCanvasTrack(VideoCapturer videoCapturer){
		canvasSource = peerConnectionFactory.createVideoSource(videoCapturer);
		videoCapturer.startCapture(videoWidth , videoHeight, videoFps);//DimenUtils.dp2pxInt(114)
		localCanvasTrack = peerConnectionFactory.createVideoTrack(WebRTCInternetUtil.kCanvasLabel,canvasSource);
		localCanvasTrack.setEnabled(renderScreen);
//		localCanvasTrack.addRenderer(new VideoRenderer(remoteCameraRenderer));
		return localCanvasTrack;
	}


	private AudioTrack createAudioTrack() {
		audioSource = peerConnectionFactory.createAudioSource(audioConstraints);
		localAudioTrack = peerConnectionFactory.createAudioTrack(WebRTCInternetUtil.kAudioLabel, audioSource);
		localAudioTrack.setEnabled(enableAudio);
		return localAudioTrack;
	}

	private void createMediaConstraintsInternal() {
		pcConstraints = new MediaConstraints();
//		pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googCpuOveruseDetection", "true"));
		//Enable DTLS for normal calls and disable for loopback calls.
		pcConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));

		//创建视频约束
		if (videoWidth == 0 || videoHeight == 0) {
			videoWidth = 1280;
			videoHeight = 720;
		}
		if (videoFps == 0) {
			videoFps = 30;
		}

		//创建音频约束
		audioConstraints = new MediaConstraints();

		//创建SDP约束
		sdpMediaConstraints = new MediaConstraints();
		//接收远程音频
		sdpMediaConstraints.mandatory.add(
				new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true")
		);
		//接收远程视频
		sdpMediaConstraints.mandatory.add(
				new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true")
		);
	}

	private void createPeerConnectionInternal(){
		iceServers.add(new PeerConnection.IceServer("turn:123.56.95.218:3478", "name", "pass"));

		peerConnectionFactory.setVideoHwAccelerationOptions(this.egContext,this.egContext);
		peer = getPeerInstance();
		mediaStream = peerConnectionFactory.createLocalMediaStream(WebRTCInternetUtil.kStreamLabel);
		mediaStream.addTrack(createVideoTrack(WebRtcClient.this.mVideoCapturer));
		mediaStream.addTrack(createCanvasTrack(WebRtcClient.this.mCanvasCapturer));
		mediaStream.addTrack(createAudioTrack());
		peer.mPeerConnection.addStream(mediaStream);

		DataChannel.Init init = new DataChannel.Init();
		peer.mDataChannel = peer.mPeerConnection.createDataChannel("data_label", init);
		peer.mDataChannel.registerObserver(peer);
	}

	//CreateOffer
	public void createOffer(){
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				peer.mPeerConnection.createOffer(peer,constraints);
			}
		});
	}

	//CreateAnswer
	public void createAnswer(final String type, final String remote){
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				try {
					SessionDescription sdp = new SessionDescription(
							SessionDescription.Type.fromCanonicalForm(type),
							remote);

					peer.mPeerConnection.setRemoteDescription(peer,sdp);
					peer.mPeerConnection.createAnswer(peer,constraints);
					Log.v("createAnswer","createAnswer");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	//存储对方sdp
	public void setRemoteSDP(final String type, final String remote){
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				SessionDescription sdp = new SessionDescription(
						SessionDescription.Type.fromCanonicalForm(type),
						remote);
				hasExchangeSDP = true;
				peer.mPeerConnection.setRemoteDescription(peer,sdp);
			}
		});
	}

	//设置RemoteCandidate
	public void setRemoteCandidate(final JSONObject payload){
		executorService.execute(new Runnable() {
			@Override
			public void run() {
				try {
					IceCandidate candidate = new IceCandidate(
							payload.getString("sdpMid"),
							payload.getInt("sdpMLineIndex"),
							payload.getString("candidate")
					);
					peer.mPeerConnection.addIceCandidate(candidate);
					Log.v("candidate","candidate");
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		});
	}


	//发送dataChannel信息
	public void sendDataChannelMsg(String message){
		Log.v("DataChannel","send-->"+message);
		byte[] msg = message.getBytes();
		DataChannel.Buffer buffer = new DataChannel.Buffer(
				ByteBuffer.wrap(msg),
				false);
		peer.mDataChannel.send(buffer);
	}

	private void addStreamInternal(MediaStream mediaStream) {
		Log.v("thread","addStreamInternal-->"+Thread.currentThread().getId());
		if (mediaStream.videoTracks.size() == 1) {
			remoteVideoTrack = mediaStream.videoTracks.get(0);
			remoteVideoTrack.setEnabled(renderVideo);
			remoteVideoTrack.addRenderer(new VideoRenderer(remoteScreenRenderer));
		}else if(mediaStream.videoTracks.size() == 2) {//0、视频 1、画板
      if (WebRTCInternetUtil.kShowRemoteVideo){
        remoteVideoTrack1 = mediaStream.videoTracks.get(0);
        remoteVideoTrack1.setEnabled(renderVideo);
        remoteVideoTrack1.addRenderer(new VideoRenderer(remoteCameraRenderer));//控制小视频窗口显示对方摄像头
      }

			remoteVideoTrack = mediaStream.videoTracks.get(1);
			remoteVideoTrack.setEnabled(renderVideo);
			remoteVideoTrack.addRenderer(new VideoRenderer(remoteScreenRenderer));
		}
	}

	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	public void close() {
		executorService.execute(new Runnable() {
			@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
			@Override
			public void run() {
				closeInternal(true);
			}
		});
	}

	//对方断开连接
	private void remoteDisconnect() {
		executorService.execute(new Runnable() {
			@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
			@Override
			public void run() {
//				closeInternal(false);

				mMsgEvent.onRemoteDisConnect();
			}
		});
	}

	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	public void closeInternal(boolean back){
		//Closing peer connection.
//		peerConnectionFactory.stopAecDump();
		if (peer!=null&&peer.mDataChannel!=null){
			peer.mDataChannel.dispose();
			peer.mDataChannel = null;
		}
		if (peer!=null&&peer.mPeerConnection!=null){
			peer.mPeerConnection.dispose();
			peer.mPeerConnection = null;
			Log.v("closeRtc","Closing peer connection.");
//			peer = null;
		}
		//Closing audio source.
		if (audioSource != null) {
			audioSource.dispose();
			Log.v("closeRtc","Closing audio source.");
			audioSource = null;
		}
		//Stopping videocapture.
		if (mVideoCapturer != null) {
			try {
				mVideoCapturer.stopCapture();
				Log.v("closeRtc","Stopping videocapture.");
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			videoCapturerStopped = true;
			mVideoCapturer.dispose();
			mVideoCapturer = null;
		}
		//Closing video source.
		if (videoSource != null) {
			videoSource.dispose();
			Log.v("closeRtc","Closing video source.");
			videoSource = null;
		}
		//Stopping screencapture.
		if (mCanvasCapturer!=null){
			try {
				mCanvasCapturer.stopCapture();
				Log.v("closeRtc","Stopping screencapture.");
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			mCanvasCapturer.dispose();
			mCanvasCapturer = null;
		}
		//Closing screen source.
		if (canvasSource != null){
//			canvasSource.dispose();
			Log.v("closeRtc","Closing screen source.");
			canvasSource = null;
		}
		localAudioTrack = null;
		localCanvasTrack = null;
		localVideoTrack = null;
		remoteVideoTrack = null;
		remoteVideoTrack1 = null;
		remoteCameraRenderer = null;
		remoteScreenRenderer = null;
		//Closing peer connection factory.
		if (peerConnectionFactory != null) {
			peerConnectionFactory.dispose();
			Log.v("closeRtc","Closing peer connection factory.");
			peerConnectionFactory = null;
		}
		PeerConnectionFactory.stopInternalTracingCapture();
		PeerConnectionFactory.shutdownInternalTracer();
		if(peer!=null){
			peer = null;
		}
		Log.v("time1","finishclose-->"+new Date().getTime());
		System.gc();
		if (back){
			//直接返回
			mMsgEvent.onResReleased();
		}else {
			//重新创建资源
			mMsgEvent.reCreateRes();
		}
	}

	public Peer getPeerInstance(){
		return new Peer();
	}

	public class Peer implements SdpObserver,PeerConnection.Observer,DataChannel.Observer {
		PeerConnection mPeerConnection;
		DataChannel mDataChannel;

		public Peer() {
			mPeerConnection = peerConnectionFactory.createPeerConnection(
					iceServers
					,constraints
					,this
			);
		}

		@Override
		public void onBufferedAmountChange(long l) {

		}

		@Override
		public void onStateChange() {
//			Log.v("dataChannelstate",peer.mDataChannel.state()+"@");
		}

		@Override
		public void onMessage(DataChannel.Buffer buffer) {
			ByteBuffer data = buffer.data;
			byte[] bytes = new byte[data.capacity()];
			data.get(bytes);
			String msg = new String(bytes);
			mMsgEvent.onReceiveDataChannelMessage(msg);
		}

		@Override
		public void onSignalingChange(PeerConnection.SignalingState signalingState) {

		}

		@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
		@Override
		public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
			/**
			 * NEW,--ICE 代理正在搜集地址或者等待远程候选可用。
			 CHECKING,-- ICE 代理已收到至少一个远程候选，并进行校验，无论此时是否有可用连接。同时可能在继续收集候选。
			 CONNECTED,--ICE代理至少对每个候选发现了一个可用的连接，此时仍然会继续测试远程候选以便发现更优的连接。同时可能在继续收集候选。
			 COMPLETED,--ICE代理已经发现了可用的连接，不再测试远程候选。
			 FAILED,--ICE候选测试了所有远程候选没有发现匹配的候选。也可能有些候选中发现了一些可用连接。
			 DISCONNECTED,--测试不再活跃，这可能是一个暂时的状态，可以自我恢复。
			 CLOSED;-- ICE代理关闭，不再应答任何请求。
			 */
			Log.v("iceConnectionState",iceConnectionState.name());
			switch (iceConnectionState){
				case NEW:
					break;
				case CHECKING:
					break;
				case CONNECTED:
					//此时已经建立远程连接
					break;
				case COMPLETED:
					break;
				case FAILED:
					break;
				case DISCONNECTED:
					//远程连接断开
					((CallActivity)mContext).runOnUiThread(new Runnable() {
						@Override
						public void run() {
							ToastUtil.showToast(mContext,"对方断开连接，会话结束");
						}
					});
					remoteDisconnect();
					break;
				case CLOSED:
					Log.v("time1","iceConnectionState<CLOSED>-->"+new Date().getTime());
					break;
			}

		}

		@Override
		public void onIceConnectionReceivingChange(boolean b) {

		}

		@Override
		public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

		}

		@Override
		public void onIceCandidate(IceCandidate iceCandidate) {
			JSONObject payload = new JSONObject();
			try {
				payload.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
				payload.put("sdpMid", iceCandidate.sdpMid);
				payload.put("candidate", iceCandidate.sdp);
				Log.v("iceCandidate",payload.toString());

				mMsgEvent.sendMSG("iceCandidate",payload.toString());
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
			Log.v("iceCandidate","onIceCandidatesRemoved");
		}

		@Override
		public void onAddStream(final MediaStream mediaStream) {
//			executorService.execute(new Runnable() {
//				@Override
//				public void run() {
//
//				}
//			});

			((CallActivity)mContext).runOnUiThread(new Runnable() {
				@Override
				public void run() {
					addStreamInternal(mediaStream);
				}
			});
		}

		@Override
		public void onRemoveStream(MediaStream mediaStream) {
			Log.v("MediaStream","MediaStreamRemoved");
		}

		@Override
		public void onDataChannel(DataChannel dataChannel) {
			Log.v("dataChannelstate",dataChannel.state()+"");
//			dataChannel.registerObserver(this);
			remoteOnline = true;
			mMsgEvent.onDataChannelCollected();
		}

		@Override
		public void onRenegotiationNeeded() {

		}

		@Override
		public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {

		}

		@Override
		public void onCreateSuccess(SessionDescription sessionDescription) {
			String description = sessionDescription.description;
			description = preferCodec(description, "ISAC", true);
			description = SdpParser.setExpectedCodec(description, "VP8", false);
			//sdpDescription = SdpParser.setExpectedCodec(sdpDescription, "ISAC", true);
			final SessionDescription sdp = new SessionDescription(sessionDescription.type, description);
			Log.v("sessionDescription",description);
			executorService.execute(new Runnable() {
				@Override
				public void run() {
					mPeerConnection.setLocalDescription(Peer.this,sdp);//接下来响应onSetSuccess或者onSetFailure
				}
			});
			mDescription = sdp;
			//sendmsg
		}

		@Override
		public void onSetSuccess() {
			if (!hasExchangeSDP&&mDescription!=null){
				//没有完成sdp交换
				executorService.execute(new Runnable() {
					@Override
					public void run() {
						Log.v("sessionDescription","sessionDescription存到本地成功了");
						mMsgEvent.sendMSG("sessionDescription",mDescription);
					}
				});

			}else {
				//sdp交换已完成

			}
		}

		@Override
		public void onCreateFailure(String s) {

		}

		@Override
		public void onSetFailure(String s) {
			Log.v("sessionDescription","sessionDescription存到本地失败了");
		}
	}



	private static String preferCodec(String sdpDescription, String codec, boolean isAudio) {
		final String[] lines = sdpDescription.split("\r\n");
		final int mLineIndex = findMediaDescriptionLine(isAudio, lines);
		if (mLineIndex == -1) {
			Log.w(TAG, "No mediaDescription line, so can't prefer " + codec);
			return sdpDescription;
		}
		// A list with all the payload types with name |codec|. The payload types are integers in the
		// range 96-127, but they are stored as strings here.
		final List<String> codecPayloadTypes = new ArrayList<>();
		// a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding parameters>]
		final Pattern codecPattern = Pattern.compile("^a=rtpmap:(\\d+) " + codec + "(/\\d+)+[\r]?$");
		for (int i = 0; i < lines.length; ++i) {
			Matcher codecMatcher = codecPattern.matcher(lines[i]);
			if (codecMatcher.matches()) {
				codecPayloadTypes.add(codecMatcher.group(1));
			}
		}
		if (codecPayloadTypes.isEmpty()) {
			Log.w(TAG, "No payload types with name " + codec);
			return sdpDescription;
		}

		final String newMLine = movePayloadTypesToFront(codecPayloadTypes, lines[mLineIndex]);
		if (newMLine == null) {
			return sdpDescription;
		}
		Log.d(TAG, "Change media description from: " + lines[mLineIndex] + " to " + newMLine);
		lines[mLineIndex] = newMLine;
		return joinString(Arrays.asList(lines), "\r\n", true /* delimiterAtEnd */);
	}

	private static int findMediaDescriptionLine(boolean isAudio, String[] sdpLines) {
		final String mediaDescription = isAudio ? "m=audio " : "m=video ";
		for (int i = 0; i < sdpLines.length; ++i) {
			if (sdpLines[i].startsWith(mediaDescription)) {
				return i;
			}
		}
		return -1;
	}

	private static String movePayloadTypesToFront(List<String> preferredPayloadTypes, String mLine) {
		// The format of the media description line should be: m=<media> <port> <proto> <fmt> ...
		final List<String> origLineParts = Arrays.asList(mLine.split(" "));
		if (origLineParts.size() <= 3) {
			Log.e(TAG, "Wrong SDP media description format: " + mLine);
			return null;
		}
		final List<String> header = origLineParts.subList(0, 3);
		final List<String> unpreferredPayloadTypes =
				new ArrayList<String>(origLineParts.subList(3, origLineParts.size()));
		unpreferredPayloadTypes.removeAll(preferredPayloadTypes);
		// Reconstruct the line with |preferredPayloadTypes| moved to the beginning of the payload
		// types.
		final List<String> newLineParts = new ArrayList<String>();
		newLineParts.addAll(header);
		newLineParts.addAll(preferredPayloadTypes);
		newLineParts.addAll(unpreferredPayloadTypes);
		return joinString(newLineParts, " ", false /* delimiterAtEnd */);
	}

	private static String joinString(
			Iterable<? extends CharSequence> s, String delimiter, boolean delimiterAtEnd) {
		Iterator<? extends CharSequence> iter = s.iterator();
		if (!iter.hasNext()) {
			return "";
		}
		StringBuilder buffer = new StringBuilder(iter.next());
		while (iter.hasNext()) {
			buffer.append(delimiter).append(iter.next());
		}
		if (delimiterAtEnd) {
			buffer.append(delimiter);
		}
		return buffer.toString();
	}

	public interface SendMsgEvent{
		void sendMSG(String flag,Object message);
		void onReceiveDataChannelMessage(String msg);
		void onResReleased();
		void onDataChannelCollected();
		void onRemoteDisConnect();
		void reCreateRes();
	}
}
