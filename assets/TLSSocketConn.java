package com.example.tlsconn;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.KeyStore;
import java.util.Hashtable;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import android.content.Context;
import android.util.Log;

public class TLSSocketConn {

	static String TAG = "TLS12Conn";
	private Context mContext;
	private Hashtable<String, String> decodeData;
	private Hashtable<String, String> otherInfo;
	static String clientCertFileName;
	static String clientCertPassword;
	
	public TLSSocketConn(Context mContext,Hashtable<String, String> decodeData,Hashtable<String, String> otherInfo) {
		super();
		this.mContext = mContext;
		this.decodeData = decodeData;
		this.otherInfo = otherInfo;
	}
	
	public void setCertFilePath(String clientCertFileName) {
		this.clientCertFileName = clientCertFileName;
	}
	
	public void setClientCertPassword(String clientCertPassword) {
		this.clientCertPassword = clientCertPassword;
	}
	
	// Create an SSL socket factory to use to connect to the Apriva server with
	// Read the appropriate certificate chains and keys from files into the SSL factory
	protected javax.net.ssl.SSLSocketFactory createSSLFactory () {
		Log.i(TAG, "TLSConn:createSSLFactory");
		try {
			InputStream inputFile = null;
			// *** Client Side Certificate *** //
			System.out.println ("2. Loading p12 file");

			// Load the certificate file into the keystore
			KeyStore keystore = KeyStore.getInstance("BKS");
			if(!clientCertFileName.equals("") && clientCertFileName != null) {
				inputFile = mContext.getAssets().open(clientCertFileName);
			} else {
				System.out.println ("ClientCertFileName is null.");
			}

			char [] clientPassphrase = clientCertPassword.toCharArray ();
			keystore.load (inputFile, clientPassphrase);

			// Create the factory
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("X509");
			keyManagerFactory.init (keystore, clientPassphrase);

			//The following section demonstrates how to configure the server trust for production.
			//It is not required for test environments and that is why the code is commented out.
			//Each line required will have the term "JKS line needed for production" following it.
			//The AprivaTrust.jks file included in this project can be used for production.
			
			// *** Server Trust *** //
			//System.out.println ("3. Loading JKS file");
			//KeyStore truststore = KeyStore.getInstance("JKS"); //JKS line needed for production
			//FileInputStream trustInputFile = new FileInputStream (serverTrustFileName); //JKS line needed for production

			//char [] serverTrustPassphrase = serverTrustPassword.toCharArray (); //JKS line needed for production
			//truststore.load (trustInputFile, serverTrustPassphrase); //JKS line needed for production

			//TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()); //JKS line needed for production
			//tmf.init (truststore); //JKS line needed for production

			//TrustManager[] trustManagers = tmf.getTrustManagers (); //JKS line needed for production

			// Create the SSL context and use it to initialize the factory
			SSLContext ctx = SSLContext.getInstance("TLSv1.2");
			//ctx.init (keyManagerFactory.getKeyManagers(), trustManagers, null); //JKS line needed for production
			ctx.init (keyManagerFactory.getKeyManagers(), null, null); //This line should be removed in production, the line above replaces it

			SSLSocketFactory sslFactory = ctx.getSocketFactory();
			return sslFactory;
		} catch (Exception e) {
			e.printStackTrace ();
		}
		return null;
	}
	
	// Perform the test by connecting to the Apriva server
	protected String conn (String host, int port) {
		Log.i(TAG, "TSLConn:test()");

		try {
			// Create an SSL factory and use it to create an SSL socket
			SSLSocketFactory sslFactory = createSSLFactory ();

//			System.out.println ("4. Connecting to " + host +  " port " + port);
			SSLSocket socket = (SSLSocket) sslFactory.createSocket (host, port);

			// Connect
			socket.startHandshake();

			// Send the XML request to the server
			OutputStream outputstream = socket.getOutputStream();
			OutputStreamWriter outputstreamwriter = new OutputStreamWriter(outputstream);

			BufferedWriter bufferedWriter = new BufferedWriter(outputstreamwriter);

			String result = "";

			System.out.println ("5. Sending Request --->>>>>>");
			System.out.println (formatPrettyXML(testXML));

			bufferedWriter.write (testXML);
			bufferedWriter.flush ();

			System.out.println ("6. Waiting for Response <<<<<<--------");
			InputStream inputstream = socket.getInputStream();
			InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
			BufferedReader bufferedReader = new BufferedReader(inputstreamreader);

			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				System.out.println(formatPrettyXML(line));
				result += formatPrettyXML(line);
			}
			inputstream.close();
			outputstream.close();
			socket.close();
			sslFactory = null;

			return result;

		} catch (Exception e) {
			e.printStackTrace ();
			return null;
		}

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

	// Main Function (EntryPoint)
	public  String connect() throws IOException
	{	

		// Display the current local directory
		String current = new java.io.File( "." ).getCanonicalPath();
		System.out.println("Current dir: "+current);

		String HostName = "aibapp53.aprivaeng.com";
		String HostPort = "11098";

		// The file containing the client certificate, private key, and chain
		clientCertFileName = "AprivaDeveloperBKS.p12";
		clientCertPassword = "P@ssword";

		// The file containing the server trust chain
//		serverTrustFileName = "cert/AprivaTrust.jks";
//		serverTrustPassword = "P@ssword";

		String host = HostName;
		int port = Integer.parseInt(HostPort);
		System.out.println ("Java Sample App v1.2 - AIB .53");
		System.out.println ("1. Running Test");
		return test (host, port);
	}
	
}
