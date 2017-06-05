package com.example.tlsconn;

import okhttp3.Call;

import com.example.tlsconn.TLSOKHttpConn.HttpGetCallBack;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends Activity implements HttpGetCallBack, OnClickListener{
	
	private TLSOKHttpConn conn;
	private String url;
	private EditText et;
//	private String res;
	private Button btn1, btn2, btn3;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//1.获取单例
		conn = TLSOKHttpConn.getSingleton();
		//2.是否绕过认证
		conn.setBypassAuthen(false);
		//3.设置证书以及密码
		conn.setCertificates(null, null, null);//setCertificates(certificates, bksFile, password);
		//4.初始化工具类
		conn.initHttpsClient();
		//5.设置回调监听
		conn.setHttpGetCallBackListener(this);
		
		et = (EditText) findViewById(R.id.et);
		btn1 = (Button) findViewById(R.id.btn1);
		btn2 = (Button) findViewById(R.id.btn2);
		btn3 = (Button) findViewById(R.id.btn3);
		btn1.setOnClickListener(this);
		btn2.setOnClickListener(this);
		btn3.setOnClickListener(this);
		
//		String url = "https://www.zhihu.com/";
//		String url = "https://www.mfbuluo.com";
		url = "https://blogs.technet.microsoft.com";
		conn.httpsGet(url);
	}

	@Override
	public void getResponseStr(String res) {
		if(res != null && !"".equals(res)) {
			et.setText(res);
		} else {
			et.setText("No response");
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn1:
			url = "https://www.zhihu.com/";
			et.setText("");
			conn.httpsGet(url);
			break;
		case R.id.btn2:
			url = "https://www.mfbuluo.com";
			et.setText("");
			conn.httpsGet(url);
			break;
		case R.id.btn3:
			url = "https://blogs.technet.microsoft.com";
			et.setText("");
			conn.httpsGet(url);
			break;
		default:
			break;
		}
	}
}
