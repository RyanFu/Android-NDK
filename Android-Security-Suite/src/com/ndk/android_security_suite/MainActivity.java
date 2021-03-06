package com.ndk.android_security_suite;

//import com.ndk.android_security_suite.R;
import com.ndk.android_security_suite.support.Support;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private String SYSTEM_ARCHITECTURE;
	private final String LOG_TAG = "[ANDROID_SECURITY_SUITE] ===> ";

	private TextView ip_address_view, interface_view;

	AssetManager assetManager;
	File sdCard;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		String exePath = null;
		ip_address_view = (TextView) findViewById(R.id.ip_address_main2);
		interface_view = (TextView) findViewById(R.id.interface_name_main2);

		this.SYSTEM_ARCHITECTURE = Support.getCPUArch();
		Toast.makeText(getApplicationContext(), this.SYSTEM_ARCHITECTURE, Toast.LENGTH_SHORT).show();

		this.assetManager = getAssets();
		this.sdCard = getExternalFilesDir(null);

		WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
		WifiInfo wifiInfo = wm.getConnectionInfo();
		int ip = wifiInfo.getIpAddress();
		String ip_address = Formatter.formatIpAddress(ip);

		ip_address_view.setText(ip_address);

		try {
			NetworkInterface net_interface = getActiveWifiInterface(getApplicationContext(), ip_address);
			String interface_name = net_interface.getName();

			if (interface_name != null) {
				interface_view.setText(interface_name);
			}
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		/*
		 * try { String[] asset = assetManager.list(this.SYSTEM_ARCHITECTURE);
		 * for (String f : asset) { textView.setText(f + " "); } } catch
		 * (IOException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); }
		 */

		/*
		 * getAssetsFiles(this.SYSTEM_ARCHITECTURE, "AndroDump");
		 * 
		 * exePath = getSdcardFiles(); textView.setText(exePath); if
		 * (exePath.isEmpty()) { textView.setText("File Not Found");
		 * Log.d(this.LOG_TAG + "MainActivity.onCreate()",
		 * "Executable File not found in the Sdcard."); } else {
		 * Toast.makeText(getApplicationContext(), exePath,
		 * Toast.LENGTH_SHORT).show(); moveToBin(exePath, "AndroDump"); }
		 */

		InitialSetup is = new InitialSetup(this.assetManager, this.sdCard, this.SYSTEM_ARCHITECTURE);
		String filePath = is.executeSetup();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public static NetworkInterface getActiveWifiInterface(Context context, String ip_address)
			throws SocketException, UnknownHostException {
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		// Return dynamic information about the current Wi-Fi connection, if any
		// is active.
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		if (wifiInfo == null)
			return null;
		// InetAddress address = intToInet(wifiInfo.getIpAddress());
		InetAddress address = InetAddress.getByName(ip_address);
		return NetworkInterface.getByInetAddress(address);
	}

	public void startAndroDumpActivity(View view) {
		Intent intent = new Intent(this, AndroDumpActivity.class);
		startActivity(intent);
	}

	public void startPacketCrafterActivity(View view) {
		Intent intent = new Intent(this, PacketCrafterActivity.class);
		startActivity(intent);
	}

	public void startDNSSpooferActivity(View view) {
		Intent intent = new Intent(this, DNSSpooferActivity.class);
		startActivity(intent);
	}

	public void startARPSpooferActivity(View view) {
		Intent intent = new Intent(this, ARPSpooferActivity.class);
		startActivity(intent);
	}

	/**
	 * public void getAssetsFiles(String arhc, String filename) { String[] files
	 * = null; AssetManager assetManager = getAssets();
	 * 
	 * try { files = assetManager.list(""); } catch (IOException e) {
	 * Log.e(this.LOG_TAG + "getAssetsFile", "Failed to get asset files list.",
	 * e); }
	 * 
	 * if (files != null) { for (String file : files) { if
	 * (file.equalsIgnoreCase(arhc)) { InputStream in = null; OutputStream out =
	 * null; String filePath = arhc + "/" + filename; //
	 * Toast.makeText(getApplicationContext(), filePath, //
	 * Toast.LENGTH_LONG).show();
	 * 
	 * try { in = assetManager.open(filePath); File outFile = new
	 * File(getExternalFilesDir(null), filename); out = new
	 * FileOutputStream(outFile); copyFile(in, out); } catch (IOException e) {
	 * Log.e(this.LOG_TAG + "getAssetsFile", "Failed to copy asset file: " +
	 * filename, e); } finally { if (in != null) { try { in.close(); } catch
	 * (IOException e) { Log.e(this.LOG_TAG + "getAssetsFile",
	 * "Failed to close the Input Stream", e); } } if (out != null) { try {
	 * out.close(); } catch (IOException e) { Log.e(this.LOG_TAG +
	 * "getAssetsFile", "Failed to close the Output Stream", e); } } } } } } }
	 * 
	 * 
	 * private void copyFile(InputStream in, OutputStream out) throws
	 * IOException { byte[] buffer = new byte[1024]; int read; while ((read =
	 * in.read(buffer)) != -1) { out.write(buffer, 0, read); } }
	 * 
	 * 
	 * private String getSdcardFiles() { String fileLocation = null;
	 * 
	 * try { File fileDir = new File(getExternalFilesDir(null), ""); for (File f
	 * : fileDir.listFiles()) { if (f.getName().equalsIgnoreCase("AndroDump")) {
	 * fileLocation = f.getAbsolutePath(); break; } } } catch (Exception e) {
	 * Log.e(this.LOG_TAG + "getSdcardFiles()",
	 * "Failed to Get the Directory Listing", e); }
	 * 
	 * return fileLocation; }
	 * 
	 * public void moveToBin(String filePath, String fileName) { boolean exitSu
	 * = false; java.lang.Process suProcess;
	 * 
	 * try { // Open Root Shell suProcess = Runtime.getRuntime().exec("su");
	 * 
	 * // Get the Output Stream from the Shell DataOutputStream os = new
	 * DataOutputStream(suProcess.getOutputStream()); // Get Input Stream to the
	 * Shell DataInputStream osRes = new
	 * DataInputStream(suProcess.getInputStream());
	 * 
	 * if (os != null && osRes != null) { os.writeBytes("id\n"); os.flush();
	 * 
	 * String curUid = osRes.readUTF().toString(); if (curUid == null) {
	 * Log.e(this.LOG_TAG + "moveToBin()",
	 * "Can't get root access or denied by user"); } else if
	 * (curUid.contains("uid=0") == true) { Log.e(this.LOG_TAG + "moveToBin()",
	 * "Root Acess Granted: " + curUid);
	 * 
	 * // os.writeBytes("chown -v root " + filePath); os.flush();
	 * //os.writeBytes(filePath);
	 *
	 * 
	 * os.writeBytes("mkdir /data/app/android_security_suite\n"
	 * );os.flush();os.writeBytes("mv -v "+filePath+" /system/bin/. \n"
	 * );os.flush();
	 * 
	 * if(osRes.readUTF().toString().contains("system"))
	 * 
	 * { Log.d(this.LOG_TAG + "moveToBin()",
	 * "Executable moved to /system/bin directory"); os.writeBytes(
	 * "chown -v root /system/bin/" + fileName + "\n"); os.flush(); if
	 * (osRes.readUTF().toString().contains("changed ownership")) {
	 * Log.d(this.LOG_TAG + "moveToBin()", "root permissions granted."); } }
	 * 
	 * exitSu=true; // Copy files from Sdcard to /system/bin directory
	 * }else{Log.e(this.LOG_TAG+"moveToBin()","Root Access Rejected: "+curUid);}
	 * 
	 * if(exitSu){os.writeBytes("exit\n");os.flush();Log.e(this.LOG_TAG+
	 * "moveToBin()","SU Shell Terminated: "+curUid);}
	 * 
	 * }}catch(
	 * 
	 * Exception e)
	 * 
	 * { Log.e(this.LOG_TAG + "moveToBin()", "Unable to move the file", e); }
	 * 
	 * }
	 * 
	 * public final boolean execute() { boolean retVal = false;
	 * 
	 * return retVal; }
	 */

}
