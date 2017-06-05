package com.example.tlsconn;

import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import android.util.Log;

import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;
import com.zhy.http.okhttp.https.HttpsUtils;
import com.zhy.http.okhttp.log.LoggerInterceptor;

public class TLSOKHttpConn {
	private InputStream[] certificates;
	private InputStream bksFile;
	private String password;
	
	//在AppParams中配置isBypassAuthen，来判断是否绕过认证，也就是无条件信任所有HTTPS网站
	private boolean isBypassAuthen = false;
	private static TLSOKHttpConn conn = null;
	private static final String TAG = "TLS12OKHttpConn";
	
	private TLSOKHttpConn() {
	}
	
	public static TLSOKHttpConn getSingleton() {
		if(conn == null) {
			synchronized(TLSOKHttpConn.class) {
				if(conn == null) {
					conn = new TLSOKHttpConn();
				}
			}
		}
		return conn;
	}

	public void initHttpsClient() {  
		OkHttpClient.Builder builder = new OkHttpClient.Builder()
				.connectTimeout(30000L, TimeUnit.MILLISECONDS)
				.readTimeout(30000L, TimeUnit.MILLISECONDS)
				.addInterceptor(new LoggerInterceptor("OkHttpClient"))
				.hostnameVerifier(new HostnameVerifier() {
					@Override
					public boolean verify(String hostname, SSLSession session) {
						return true;
					}
				});
		if(isBypassAuthen) {
			SSLContext sslContext = null;
			try {
				sslContext = SSLContext.getInstance("TLSv1.2");//SSLv3
				try {
					sslContext.init(null, null, null);
				} catch (KeyManagementException e) { 
					e.printStackTrace();
				}
			} catch (NoSuchAlgorithmException e) { 
				e.printStackTrace();
			}
			
			SSLSocketFactory socketFactory = new Tls12SocketFactory(sslContext.getSocketFactory());
//			SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			builder.sslSocketFactory(socketFactory,new HttpUtils.UnSafeTrustManager());
		} else{
//			HttpsUtils.SSLParams sslParams = HttpsUtils.getSslSocketFactory(certificates, bksFile, password); //zhy
			HttpUtils.SSLParams sslParams = HttpUtils.getSslSocketFactory(certificates, bksFile, password); //lu
			builder.sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager);
		}
		OkHttpClient okHttpClient = builder.build();
		OkHttpUtils.initClient(okHttpClient);
	}
	
	public void setCertificates(InputStream[] certificates, InputStream bksFile, String password) {
		if(certificates == null) this.certificates = null;
		if(bksFile == null) this.bksFile = null;
		if(password == null) this.password = null;
		this.certificates = certificates;
		this.bksFile = bksFile;
		this.password = password;
	}
	
	//GET请求
	public void httpsGet(String url){
//		String url = url;//http://www.csdn.net/ https://183.232.231.173 https://www.zhihu.com/
		OkHttpUtils
			.get()
			.url(url)
//	 		.addParams("username", "hyman")
//	 		.addParams("password", "123")
			.build()
			.execute(new StringCallback()
			{
				@Override
				public void onError(Call arg0, Exception arg1, int arg2) {
					Log.d(TAG, arg1.toString());
				}
	
				@Override
				public void onResponse(String arg0, int arg1) {
					Log.d(TAG, "onResponse");
					if(arg0 != null && !"".equals(arg0)) {
						httpGetCallBack.getResponseStr(arg0);
					}  else {
						httpGetCallBack.getResponseStr("No result");
					}
//					System.out.println("response == " + response);
//					if(response != null && !"".equals(response))
//						MainActivity.setEditText(response);
				}
			});
	}
	
	public void setBypassAuthen(boolean isBypassAuthen) {
		this.isBypassAuthen = isBypassAuthen;
	}
	
	protected static String formatPrettyXML(String unformattedXML) {
		String prettyXMLString = null;
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			StreamResult result = new StreamResult(new StringWriter());
			StreamSource source = new StreamSource(new StringReader(unformattedXML));
			transformer.transform(source, result);
			prettyXMLString = result.getWriter().toString();			
		} catch (TransformerConfigurationException e) {
			System.out.println("Unable to transform XML " + e.getMessage());
		} catch (TransformerFactoryConfigurationError e) {
			System.out.println("Unable to transform XML " + e.getMessage());
		} catch (TransformerException e) {
			System.out.println("Unable to transform XML " + e.getMessage());
		}
		return prettyXMLString;
	}
	
	//Callback of okhttp get method
	private HttpGetCallBack httpGetCallBack;
	public interface HttpGetCallBack {
		public void getResponseStr(String res);
	}
	public void setHttpGetCallBackListener(HttpGetCallBack h) {
		this.httpGetCallBack = h;
	}
}
