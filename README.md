# TLSInOkhttp
***Communicate to network by TLS protocol in OkHttpUtils.***
___

## 问题描述
在4.x系统上通过HTTPS进行访问产生如下异常：  
`javax.net.ssl.SSLProtocolException: SSL handshake aborted: ssl=0x65bc0ad8: Failure in SSL library, usually a protocol error
error:1407743E:SSL routines:SSL23_GET_SERVER_HELLO:tlsv1 alert inappropriate fallback (external/openssl/ssl/s23_clnt.c:744 0x5cf4ed74:0x00000000)`  
>javax.net.ssl.SSLProtocolException: SSL handshake aborted: ssl=0x65bc0ad8: Failure in SSL library, usually a protocol error
error:1407743E:SSL routines:SSL23_GET_SERVER_HELLO:tlsv1 alert inappropriate fallback (external/openssl/ssl/s23_clnt.c:744 0x5cf4ed74:0x00000000)

## 原因
Android4.x系统对TLS的支持存在版本差异，具体细节请看以下分析

## 分析
不同Android版本针对于TLS协议的默认配置图如下：
![](http://img.blog.csdn.net/20161229161119662?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvczAwMzYwM3U=/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)  
从上图可以得出如下结论：  
* TLSv1.0从API 1+就被默认打开
* TLSv1.1和TLSv1.2只有在API 20+ 才会被默认打开
* 也就是说低于API 20+的版本是默认关闭对TLSv1.1和TLSv1.2的支持，若要支持则必须自己打开  

我们可以在[QUALYS SSL LABS](https://www.ssllabs.com/ssltest/)测试它对ssl支持的版本 
这里截取SSL报告中对我们有用的一部分，如下图:  
![](http://img.blog.csdn.net/20161229154444392?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQvczAwMzYwM3U=/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/SouthEast)  
* 刚开始服务器配置只支持TLS1.2，SSL报告的结果也验证了这一点
* 可以看出大部分2.x、4.x的Android系统都会报Server sent fatal alert：handshake_failure,而5.0、6.0、7.0的Android系统在Hanshake Simulation中表现正常，因为它们支持TLS1.2  
这就能解释为什么大部分4.xAndroid系统在进行HTTPS访问时产生上述异常。
 
## 解决方案
想办法让Android4.x打开对TLS1.1、TLS1.2的支持
* 假设你的网络请求库使用的是okhttp，在APP中可以这样初始化OkHttpClient，这里通过在AppParams中配置isBypassAuthen，来判断是否绕过认证，也就是无条件信任所有HTTPS网站
在MainActivity中，我们这样初始化TLSOkhttpConn网络连接实例：
  `setContentView(R.layout.activity_main);  
		//1.获取单例  
		conn = TLSOKHttpConn.getSingleton();  
		//2.是否绕过认证  
		conn.setBypassAuthen(false);  
		//3.设置证书以及密码  
		conn.setCertificates(null, null, null);//setCertificates(certificates, bksFile, password);  
		//4.初始化工具类  
		conn.initHttpsClient();  
		//5.设置回调监听  
  conn.setHttpGetCallBackListener(this);`

其中：
`public void initHttpsClient() {  
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
			} catch (NoSuchAlgorithmException e) { e.printStackTrace(); }	
			SSLSocketFactory socketFactory = new Tls12SocketFactory(sslContext.getSocketFactory());
			builder.sslSocketFactory(socketFactory,new HttpUtils.UnSafeTrustManager());
		} else{
			HttpUtils.SSLParams sslParams = HttpUtils.getSslSocketFactory(certificates, bksFile, password); //lu
			builder.sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager);
		}
		OkHttpClient okHttpClient = builder.build();
		OkHttpUtils.initClient(okHttpClient);
	}`

该例子帮助我们初始化了支持TLS1.2的okhttp以及okhttputils，具体的网络访问操作，参考okhttp以及okhttputils的使用。




