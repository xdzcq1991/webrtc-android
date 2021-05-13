package com.example.kingo.rtcclient.util;

/**
 * Created by kingo on 2019/7/8.
 */

public class WebRTCInternetUtil {
	//    public final static String kServerAddress = "https://www.kewai365.com/KingoKww/visitor/verifyLogin.action";
	public final static String kServerAddress = "https://www.kewai365.com/visitor/verifyLogin.action";
//  public final static String kServerAddress = "http://www.kww.com/visitor/verifyLogin.action";
	//public final static String kSignalServerAddress = "ws://192.168.9.215:8082/KwwIMCenter_SSM/websocket/";
	//    public final static String kSignalServerAddress = "ws://192.168.0.210:8090/KwwIMCenter_SSM/websocket/";
	public final static String kSignalHttpAddress = "http://116.128.228.13:21000/common/";
	public final static String kSignalServerAddress = "ws://116.128.228.13:21000/websocket/connection.do";
	public final static String kLgoinCode = "usercode";
	public final static String kLoginName = "username";
	public final static String kLoginRoomCode = "roomcode";
	public final static String kMessageType = "type";
	public final static String kMessageContent = "msgtxt";
	public final static String kFromCode = "fromMemberId";
	public final static String kFromName = "username";
	public final static String kToCode = "toMemId";
	public final static String kRoomCode = "roomcode";
	public final static String kAudioLabel = "audio_label";
	public final static String kVideoLabel = "video_label";
	public final static String kCanvasLabel = "canvas_label";
	public final static String kDataLabel = "data_label";
	public final static String kStreamLabel = "stream_label";

	// Names used for a SessionDescription JSON object.
	public final static String kSessionDescriptionTypeName = "type";
	public final static String kSessionDescriptionSdpName = "sdp";

	// Names used for a IceCandidate JSON object.
	public final static String kCandidateSdpMidName = "sdpMid";
	public final static String kCandidateSdpMlineIndexName = "sdpMLineIndex";
	public final static String kCandidateSdpName = "candidate";
  public static       boolean kShowRemoteVideo = true;  //是否显示对方摄像头视频

  public static String APP_KEY = "5be926897d28d011dca3adf1";
//  public static String APP_KEY = "5bd165947d28d02204406d7b";
//  public static String APP_KEY = "5bda53ad7dc309000105a0ab";
}
