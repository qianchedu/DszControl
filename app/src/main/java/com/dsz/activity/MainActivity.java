package com.dsz.activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.Toast;


import com.dsz.view.CustomDialog;
import com.dsz.wifi.WifiSetup;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;


public class MainActivity extends Activity {
	
	public final static String CONTROLER_POSITION = "/remoteCamera/";
    public final String TAG = "Webcam";
    private boolean mIsServiceRunning = false;
    private Thread openWifiThread;         //声明一个子线程
    SharedPreferences mSharedPreferences;
    private ImageButton enterControlerBtn = null;

	
    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        if (!initialize()) {
            Toast.makeText(this, "Can not initialize parameters", Toast.LENGTH_LONG).show();
        }
        findView();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
//        mIsServiceRunning = isServiceRunning();
    }
    
    @Override
    public void onStop() {
        super.onStop();
        if (mIsServiceRunning) {
            finish();
        }
    }
    //在退出软件时将热点关闭
    @Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		//关闭wifi
		WifiSetup wifiSet = new WifiSetup(MainActivity.this);
		wifiSet.closeWifi();

       	
	}

    /**图片点击事件**/
	public void onImageButtonClick(View view) {
        switch (view.getId()) {
        

            
        case R.id.enterControlerBtn:		//遥控器界面
        	WifiSetup wifiSet = new WifiSetup(MainActivity.this);	//wifi设置
        	if(wifiSet.isWifiConnected()){
        		startActivity(new Intent(this , com.dsz.activity.MyVideo.class));//进入遥控界面
        	}else{
        		//进入系统wifi设置界面
        		showAlertDialog();
        	}
        	break;
        	

        }
    }
    
	

    
    private void findView(){
    	enterControlerBtn = (ImageButton)findViewById(R.id.enterControlerBtn);


		//设置按钮触摸监听函数
    	enterControlerBtn.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				 if(event.getAction() == MotionEvent.ACTION_DOWN){
                     //更改为按下时的背景图片     
					 ((ImageButton)v).setImageDrawable(getResources().getDrawable(R.mipmap.enter_controler_down));
	             }else if(event.getAction() == MotionEvent.ACTION_UP){
	                 //改为抬起时的图片     
	            	 ((ImageButton)v).setImageDrawable(getResources().getDrawable(R.mipmap.enter_controler));
	             }     
				return false;
			}
		});
    	
		
    } 
    
    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    private boolean initialize() {

       mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        
       
       File fileFolder = new File(Environment.getExternalStorageDirectory()
               + CONTROLER_POSITION);  
       if (!fileFolder.exists()) { // 如果目录不存在，则创建一个目录  
           fileFolder.mkdir();  
       }
       boolean firstRun = ! mSharedPreferences.contains("settings_camera");
       if (firstRun) {
           Log.v(TAG, "First run");
           
           SharedPreferences.Editor editor = mSharedPreferences.edit();
           
           int cameraNumber = Camera.getNumberOfCameras();
           Log.v(TAG, "Camera number: " + cameraNumber);
           
           /*
            * Get camera name set 
            */
           TreeSet<String> cameraNameSet = new TreeSet<String>();
           if (cameraNumber == 1) {
               cameraNameSet.add("back");
           } else if (cameraNumber == 2) {
               cameraNameSet.add("back");
               cameraNameSet.add("front");
           } else if (cameraNumber > 2) {           // rarely happen
               for (int id = 0; id < cameraNumber; id++) {
                   cameraNameSet.add(String.valueOf(id));
               }
           } else {                                 // no camera available
               Log.v(TAG, "No camrea available");
               Toast.makeText(this, "No camera available", Toast.LENGTH_SHORT).show();
               
               return false;
           }

           /* 
            * Get camera id set
            */
           String[] cameraIds = new String[cameraNumber];
           TreeSet<String> cameraIdSet = new TreeSet<String>();
           for (int id = 0; id < cameraNumber; id++) {
               cameraIdSet.add(String.valueOf(id));
           }
           
           /*
            * Save camera name set and id set
            */
           editor.putStringSet("camera_name_set", cameraNameSet);
           editor.putStringSet("camera_id_set", cameraIdSet);
           
           /*
            * Get and save camera parameters
            */
           for (int id = 0; id < cameraNumber; id++) {
               Camera camera = Camera.open(id);
               if (camera == null) {
                   String msg = "Camera " + id + " is not available";
                   Log.v(TAG, msg);
                   Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                   
                   return false;
               }
               
               Parameters parameters = camera.getParameters();
               
               /*
                * Get and save preview sizes
                */
               List<Size> sizes = parameters.getSupportedPreviewSizes();
               
               TreeSet<String> sizeSet = new TreeSet<String>(new Comparator<String>() {
                   @Override
                   public int compare(String s1, String s2) {
                       int spaceIndex1 = s1.indexOf(" ");
                       int spaceIndex2 = s2.indexOf(" ");
                       int width1 = Integer.parseInt(s1.substring(0, spaceIndex1));
                       int width2 = Integer.parseInt(s2.substring(0, spaceIndex2));
                       
                       return width2 - width1;
                   }
               });
               for (Size size : sizes) {
                   sizeSet.add(size.width + " x " + size.height);
               }
               editor.putStringSet("preview_sizes_" + id, sizeSet);
               
               Log.v(TAG, sizeSet.toString());
               
               /*
                * Set default preview size, use camera 0
                */
               if (id == 0) {
                   Log.v(TAG, "Set default preview size");
                   
                   Size defaultSize = parameters.getPreviewSize();
                   editor.putString("settings_size", defaultSize.width + " x " + defaultSize.height);
               }
               
               /*
                * Get and save 
                */
               List<int[]> ranges = parameters.getSupportedPreviewFpsRange();
               TreeSet<String> rangeSet = new TreeSet<String>();
               for (int[] range : ranges) {
                   rangeSet.add(range[0] + " ~ " + range[1]);
               }
               editor.putStringSet("preview_ranges_" + id, rangeSet);
               
               if (id == 0) {
                   Log.v(TAG, "Set default fps range");
                   
                   int[] defaultRange = new int[2];
                   parameters.getPreviewFpsRange(defaultRange);
                   editor.putString("settings_range", defaultRange[0] + " ~ " + defaultRange[1]);
               }
               
               camera.release();
               
           }
           
           editor.putString("settings_camera", "0");
           editor.commit();
       }
       
       return true;
    }
    
	/**
	 * Function:到系统中设置wifi<br>
	 * <br>
	 */
	private void gotoSysWifi() {
		Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
		startActivityForResult(intent, 0);
	}
	
	//对话框----没有连接到被遥控端的wifi热点
	public void showAlertDialog() {

		CustomDialog.Builder builder = new CustomDialog.Builder(this);
		builder.setMessage("   亲，尚未连接相机！\n   为您跳转到连接设置？");
		builder.setTitle("提示");
		builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				
				//一下是按确认键响应函数
        		//在切到系统之前先打开wifi，（创建一个线程去做，以提高UI的响应,好像没有什么效果，后边再找找问题）
        		openWifiThread = new Thread(new Runnable() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						try {
							//必须先延时，等待系统跳转到wifi界面之后再打开wifi
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						WifiSetup wifiSet = new WifiSetup(MainActivity.this);
						wifiSet.openWifi();
					}
				});
				openWifiThread.start();
				//进入系统wifi设置界面
				gotoSysWifi();
			}
		});
		builder.setNegativeButton("取消",
				new android.content.DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
		builder.create().show();

	}
    
	

	
	
}
