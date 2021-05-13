package com.example.kingo.rtcclient;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.kingo.rtcclient.util.CallBackUtil;
import com.example.kingo.rtcclient.util.WebRTCInternetUtil;
import com.example.kingo.rtcclient.util.OkhttpUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import okhttp3.Call;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {
	private Button mTextView,mTextView1,mTextView2,mTextView3;
	private String m_strLoginID = "";
	private String m_strLoginPWD = "123456";
	private String token = "";
	private String uId = "";

	private Context mContext;

	EditText mEditText1,mEditText2;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		mContext = this;

		mTextView = findViewById(R.id.collect);
		mTextView1 = findViewById(R.id.collect1);
    mTextView2 = findViewById(R.id.collect2);
    mTextView3 = findViewById(R.id.collect3);
    mEditText1 = findViewById(R.id.input);
    mEditText2 = findViewById(R.id.input_pwd);

		mTextView.setOnClickListener(this);
		mTextView1.setOnClickListener(this);
    mTextView2.setOnClickListener(this);
    mTextView3.setOnClickListener(this);

    findViewById(R.id.login_btn).setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
    int i = v.getId();
    if (i == R.id.collect) {
      m_strLoginID = "15211073659";//15211073659//13755184967

    } else if (i == R.id.collect1) {
      m_strLoginID = "13755184967";

    } else if (i == R.id.collect2) {
      m_strLoginID = "13627318325";//13627318325//17711649645

    } else if (i == R.id.collect3) {
      m_strLoginID = "17711649645";

    }else if (i == R.id.login_btn){
      m_strLoginID = mEditText1.getText().toString();
      m_strLoginPWD = mEditText2.getText().toString();
    }
		collectKewai365();

//		Intent intent = new Intent(LoginActivity.this,MainActivity.class);
//		intent.putExtra("uId",uId);
//		intent.putExtra("token",token);
//		startActivity(intent);
	}


	//课外网获取用户信息
	private void collectKewai365(){
		HashMap params = new HashMap();
		params.put("loginId", m_strLoginID);
		params.put("loginPwd", m_strLoginPWD);
		OkhttpUtil.okHttpPost(WebRTCInternetUtil.kServerAddress, params, new CallBackUtil.CallBackString() {
			@Override
			public void onFailure(Call call, Exception e) {
				e.printStackTrace();
			}

			@Override
			public void onResponse(String response) {
				try {
					JSONObject jsonObject = new JSONObject(response);
					String success = jsonObject.getString("SUCCESS");
					String num = jsonObject.has("NUM")?jsonObject.getString("NUM"):"1";
					if ("1".equals(success)) {
						if (num.equals("0")){
//							ToastUtil.showToast(mContext,"当前时间没有排课");
							AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
							builder.setMessage("当前时间段没有排课!");
							builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									finish();
								}
							});
							builder.show();
						}else{
							JSONObject json_data = jsonObject.getJSONObject("DATA");
							String user_name = json_data.getString("user_name");
							String user_code = json_data.getString("user_code");
							String user_type = json_data.getString("user_type");
							String user_icon = json_data.getString("user_photo");

							String peer_name = json_data.has("peer_name")?json_data.getString("peer_name"):"";
							String peer_code = json_data.has("peer_code")?json_data.getString("peer_code"):"";
							String peer_icon = "";//json_data.getString("peer_photo");

							String course_name = "";//json_data.getString("course_name");
							String course_content = "";//json_data.getString("course_content");
							String course_begin = "";//json_data.getString("course_begin");
							String course_end = "";//json_data.getString("course_end");

							String arrange_id = "";//json_data.getString("arrange_id");
							String class_number = "";//json_data.getString("class_num");
							String room_number = "";//json_data.getString("room_num");

							//保存值
							saveSharedPre(m_strLoginID, m_strLoginPWD, user_name, user_code, user_type, peer_name, peer_code, room_number);
							//登录应用服务器成功，跳转到呼叫页面，并将相关信息传给呼叫页面
							connectToSignalServer();

							//页面跳转，连接成功
							//finish();
						}

					}else {
//						ToastUtil.showToast(mContext,"当前时间没有排课");
					}
				}catch (JSONException e){
					e.printStackTrace();
				}
			}
		});
	}

	//保存数据
	private void saveSharedPre(String strLoginID, String strLoginPWD,
							   String user_name, String user_code, String user_type,
							   String peer_name, String peer_code, String room_number) {
		SharedPreferences preferences = getSharedPreferences("info", MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString("userID", strLoginID);
		editor.putString("userPWD", strLoginPWD);
		editor.putString("user_name", user_name);
		editor.putString("user_code", user_code);
		editor.putString("user_type", user_type);
		editor.putString("peer_name", peer_name);
		editor.putString("peer_code", peer_code);
		editor.putString("room_num", room_number);
		editor.apply();
	}

	//连接信令服务器
	private void connectToSignalServer() {
		HashMap params = new HashMap();
		params.put("userAccount", getSharedPreferences("info",MODE_PRIVATE).getString("user_code",""));
		params.put("appKey", WebRTCInternetUtil.APP_KEY);
		OkhttpUtil.okHttpGet(WebRTCInternetUtil.kSignalHttpAddress + "getJWT", params, new CallBackUtil.CallBackString() {
			@Override
			public void onFailure(Call call, Exception e) {
				Toast.makeText(mContext,"connectToSignalServer-失败",Toast.LENGTH_LONG).show();
			}

			@Override
			public void onResponse(String response) {
				try {
					JSONObject object = new JSONObject(response);
					token = object.getString("result");
					isUserRegistered();
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		});
	}

	//判断是否注册
	private void isUserRegistered() {
		HashMap params = new HashMap();
		params.put("userAccount", getSharedPreferences("info",MODE_PRIVATE).getString("user_code",""));
		params.put("appKey", WebRTCInternetUtil.APP_KEY);
		HashMap header = new HashMap();
		header.put("authorization","bearer " + token);
		OkhttpUtil.okHttpPost(WebRTCInternetUtil.kSignalHttpAddress + "auth/isUserRegistered", params, header, new CallBackUtil.CallBackString() {
			@Override
			public void onFailure(Call call, Exception e) {
				Toast.makeText(mContext,"isUserRegistered-失败",Toast.LENGTH_LONG).show();
			}
			@Override
			public void onResponse(String response) {
				try {
					JSONObject object = new JSONObject(response);
					if (object.getString("flg").equals("success")){
						uId = object.getJSONObject("des").getString("uid");
						Intent intent = new Intent(LoginActivity.this,CallActivity.class);
						intent.putExtra("uId",uId);
						intent.putExtra("token",token);
						startActivity(intent);
//						connectWS();
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return super.onKeyDown(keyCode, event);
	}
}
