package com.ping.thingsjournalclient.view;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationClientOption.AMapLocationMode;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.AMap.OnMapClickListener;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.LocationSource;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.Circle;
import com.amap.api.maps2d.model.CircleOptions;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.maps2d.model.MyLocationStyle;
import com.amap.api.maps2d.model.Polygon;
import com.amap.api.maps2d.model.PolygonOptions;
import com.example.thingsjournalclient.R;
import com.ping.thingsjournalclient.common.CircleArea;
import com.ping.thingsjournalclient.common.Vertex;
import com.ping.thingsjournalclient.model.ManageStatic;
import com.ping.thingsjournalclient.model.MessageType;
import com.ping.thingsjournalclient.model.TransMessage;
import com.ping.thingsjournalclient.model.User;
import com.ping.thingsjournalclient.util.ChangTwoBit;
import com.ping.thingsjournalclient.util.CoordinateConversion;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class MainActivity extends Activity
		implements LocationSource, AMapLocationListener, OnMapClickListener, OnSeekBarChangeListener {
	private long tempTime = 0;// 开始查询后的时间
	private int allCount = 3;// 保存收到backquery中的在线好友数量
	private int nowCount = 0;// 收到的答复好友数量
	private List<String> resultsTemp = null;// 临时用来存储在线好友的list
	private String queryTypeTemp = null;// 查询的方式
	private static final long thresoldTime = 10000;// 门限时间
	private String[] tempStrs = null;
	private int tempFlag = 0;// 是否有查询结果返回的标志
	private int eventFlag = 0;// 一个事件是否结束的标志
	private int typeFlag = 0;// 查询事件的类型
	private int clearFlag = 0;// 清除地图的标记
	private int progressValue = 1000;// seekbar初始值
	LatLng tempLatLng = null;// 临时经纬度值
	List<Double> latitude = new ArrayList<Double>();// 临时纬度列表
	List<Double> longitude = new ArrayList<Double>();// 临时经度列表
	List<LatLng> latl = new ArrayList<LatLng>();// 临时经纬度值列表
	CircleArea queryCircle ;
	String queryArea = null;

	private String[] friendsName = null;//好友姓名
	CoordinateConversion coor = new CoordinateConversion();//坐标转换类

	private ProgressDialog progressDialog;
	private MyBroadcastReceiverRetfriends brRetFriends = null;
	private MyBroadcastReceiverAllCount brAllCount = null;
	private MyBroadcastReceiverResponseData brRRD = null;
	private MyBroadcastReceiverAddFriend brAF = null;
	private MyBroadcastReceiverSocketFailed brSF = null;
	private Handler handler = null;
	private AMap aMap;
	private MapView mapView;
	private OnLocationChangedListener mListener;
	private AMapLocationClient mlocationClient;
	private AMapLocationClientOption mLocationOption;
	private Polygon polygon;
	private VerticalSeekBar sbRadius;
	private Circle circle = null;
	private Button btnConfirm = null;
	NotificationManager notificationManager = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);
		/**
		 * 接收好友列表的receiver
		 */
		IntentFilter myIntentFilterRetfriends = new IntentFilter();
		myIntentFilterRetfriends.addAction("org.ping.retfriend");
		brRetFriends = new MyBroadcastReceiverRetfriends();
		registerReceiver(brRetFriends, myIntentFilterRetfriends);
		/**
		 * 接收查询返回数据的receiver
		 */
		IntentFilter myIntentFilterAllCount = new IntentFilter();
		myIntentFilterAllCount.addAction("org.ping.backquery");
		brAllCount = new MyBroadcastReceiverAllCount();
		registerReceiver(brAllCount, myIntentFilterAllCount);
		/**
		 * 处理查询数据的receiver
		 */
		IntentFilter myIntentFilterResponseData = new IntentFilter();
		myIntentFilterResponseData.addAction("org.ping.backresult");
		brRRD = new MyBroadcastReceiverResponseData();
		registerReceiver(brRRD, myIntentFilterResponseData);
		/**
		 * 处理添加好友的receiver
		 */
		IntentFilter myIntentFilterAddFriend = new IntentFilter();
		myIntentFilterAddFriend.addAction("org.ping.addfriend");
		brAF = new MyBroadcastReceiverAddFriend();
		registerReceiver(brAF, myIntentFilterAddFriend);
		IntentFilter myIntentFilterSocketFailed = new IntentFilter();
		myIntentFilterSocketFailed.addAction("org.ping.socketfailed");
		brSF= new MyBroadcastReceiverSocketFailed();
		registerReceiver(brSF, myIntentFilterSocketFailed);
		mapView = (MapView) findViewById(R.id.map);
		mapView.onCreate(savedInstanceState);// 此方法必须重写
		init();

	}

	public void finish() {
		unregisterReceiver(brRetFriends);
		unregisterReceiver(brAllCount);
		unregisterReceiver(brRRD);
		unregisterReceiver(brAF);
		ManageStatic.locationClient.onDestroy();
		super.finish();
	}

	// 初始化的一些方法，以及处理activity生命周期的一些方法
	/**
	 * 进行地图和一些构件的初始化
	 */
	private void init() {

		notificationManager = (NotificationManager) 
	            this.getSystemService(android.content.Context.NOTIFICATION_SERVICE); 
		
		progressDialog = new ProgressDialog(this);
		progressDialog.setTitle("操作中");
		progressDialog.setMessage("请稍后");
		progressDialog.setCanceledOnTouchOutside(false);
		progressDialog.onStart();
		resultsTemp = Collections.synchronizedList(new ArrayList<String>());
		handler = new Handler();
		btnConfirm = (Button) findViewById(R.id.btn_confirm);

		sbRadius = (VerticalSeekBar) findViewById(R.id.sb_radius);
		sbRadius.setMax(10000);
		sbRadius.setProgress(0);

		if (aMap == null) {
			aMap = mapView.getMap();
			setUpMap();
		}
	}

	/**
	 * 设置一些amap的属性
	 */
	private void setUpMap() {
		MyLocationStyle myLocationStyle = new MyLocationStyle();
		myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.location_marker));// 设置小蓝点的图标
		myLocationStyle.strokeColor(Color.BLACK);// 设置圆形的边框颜色
		myLocationStyle.radiusFillColor(Color.argb(100, 0, 0, 180));// 设置圆形的填充颜色
		myLocationStyle.strokeWidth(1.0f);// 设置圆形的边框粗细
		aMap.setMyLocationStyle(myLocationStyle);
		aMap.setLocationSource(this);// 设置定位监听
		aMap.getUiSettings().setMyLocationButtonEnabled(true);// 设置默认定位按钮是否显示
		aMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
		aMap.setOnMapClickListener(this);
		sbRadius.setOnSeekBarChangeListener(this);
	}

	/**
	 * 方法必须重写
	 */
	@Override
	protected void onResume() {
		super.onResume();
		mapView.onResume();
	}

	/**
	 * 方法必须重写
	 */
	@Override
	protected void onPause() {
		showNotification();
		super.onPause();
		mapView.onPause();
		deactivate();
	}

	/**
	 * 方法必须重写
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mapView.onSaveInstanceState(outState);
	}

	/**
	 * 方法必须重写
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mapView.onDestroy();
	}
	
	/**
	 * 添加menu
	 */
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, 1, 0, "退出");
		return true;
	}
	
    /**
     * 添加menu事件
     */
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()){
		case 1:
			closeAllActivity();
			break;
    	}
        return true;
    }
	
	/**
	 * 关闭所有activity
	 */
	public void closeAllActivity(){
	    for(int i = 0; i<ManageActivity.allActiviy.size();i++){
	    	ManageActivity.allActiviy.get(i).finish();
	    }
	    notificationManager.cancelAll();
	    this.finish();
	    android.os.Process.killProcess(android.os.Process.myPid()); 
		System.exit(0);
	}
	
	//按返回键的事件
	public boolean onKeyDown(int keyCode, KeyEvent event) {  
		if (keyCode == KeyEvent.KEYCODE_BACK) {  
			moveTaskToBack(true);  
			return true;  
		}  
		return super.onKeyDown(keyCode, event);  
	}  
	
	/**
	 * 返回键事件
	 */
	public void onBackPressed() {
		Intent i = new Intent(Intent.ACTION_MAIN);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.addCategory(Intent.CATEGORY_HOME);
		startActivity(i);
		super.onBackPressed();
	}
	
	/**
	 * 设置通知栏消息
	 */
	private void showNotification() { 
        // 定义Notification的各种属性 
        Notification notification =new Notification(R.drawable.ic_launcher, 
                "ThingsJournal 正在运行", System.currentTimeMillis()); 
        notification.flags |= Notification.FLAG_ONGOING_EVENT; //加入通知栏的"Ongoing"中 
        notification.flags |= Notification.FLAG_NO_CLEAR; //点击了通知栏中的"清除通知"后不清除
        notification.flags |= Notification.FLAG_SHOW_LIGHTS; 
        notification.defaults = Notification.DEFAULT_LIGHTS; 
        notification.setLatestEventInfo(this, 
        		"ThingsJournal",  // 通知栏标题
        		"您的位置正在被模糊查询中",  // 通知栏内容
        		null); // 点击该通知后要跳转的Activity
        // 把Notification传递给NotificationManager，id为0
        notificationManager.notify(0, notification); 
    }
	// 点击事件处理方法

	/**
	 * 显示好友列表
	 * 
	 * @param source
	 */

	public void showFriends(View source) {
		Dialog dialog = new AlertDialog.Builder(MainActivity.this).setTitle("好友列表")
				.setItems(friendsName, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
					}
				}).setPositiveButton("确定", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
					}
				}).create();
		dialog.show();
	}
	
	/*
	 * 添加好友的方法
	 */
	public void addFriend(View source) {
		final EditText addFriend = new EditText(this);
		addFriend.setSingleLine(true);
		Dialog dialog = new AlertDialog.Builder(this).setTitle("添加好友").setView(addFriend)
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						progressDialog.show();
						if (!addFriend.getText().toString().equals("")) {
							String friendName = addFriend.getText().toString();
							System.out.println(friendName);
							TransMessage tm = new TransMessage();
							tm.setMessageType(MessageType.ADDFRIEND);
							tm.setSender(ManageStatic.user.getUserName());
							tm.setReceiver(friendName);
							try {
								ObjectOutputStream oos = new ObjectOutputStream(
										ManageStatic.getClientConServerThread(ManageStatic.user.getUserName())
												.getSocket().getOutputStream());
								oos.writeObject(tm);
							} catch (IOException e) {
								handler.post(run);
								e.printStackTrace();
							}
							eventFlag = 1;
							tempFlag = 0;
							new Thread() {
								public void run() {
									long tempTimeSec = System.currentTimeMillis();
									while (true) {
										if (tempFlag == 1) {
											break;
										}
										if (System.currentTimeMillis() - tempTimeSec > thresoldTime) {
											handler.post(run);
											eventFlag = 0;
											break;
										}
									}
								}
							}.start();
						} else {
							handler.post(runAddFriend);
						}
					}
				}).setNegativeButton("取消", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
					}
				}).create();
		dialog.show();
	}

	/**
	 * p方法查询
	 * 
	 * @param source
	 */
	public void click_p(View source) {
		typeFlag = 1;
		aMap.clear();
		sbRadius.setVisibility(View.GONE);
		btnConfirm.setVisibility(View.GONE);
	}

	/**
	 * c方法查询
	 * 
	 * @param source
	 */
	public void click_c(View source) {
		typeFlag = 2;
		aMap.clear();
		sbRadius.setVisibility(View.VISIBLE);
		btnConfirm.setVisibility(View.VISIBLE);
	}

	/**
	 * 确认c查询
	 * 
	 * @param source
	 */
	public void confirm_query(View source) {
		if(tempLatLng == null){//未返回数据直接跳过
			return;
		}
		new Thread() {
			@Override
			public void run() {
				String[] vertexString = coor.latLon2UTM(tempLatLng.latitude, tempLatLng.longitude).split(" ");
				queryCircle = new CircleArea(progressValue, Long.parseLong(vertexString[2]),
						Long.parseLong(vertexString[3]));
				String queryMessage = ManageStatic.qU_AGRQ_C.QU_AGRQ_C_QDC(queryCircle);
				TransMessage tm = new TransMessage();
				tm.setMessageType(MessageType.SEND_QUERY);
				tm.setMessageContent(queryMessage);
				tm.setSender(ManageStatic.user.getUserName());
				tm.setQueryType(MessageType.QUERY_C);
				queryArea = vertexString[0]+" "+vertexString[1];
				queryTypeTemp = MessageType.QUERY_C;
				try {
					ObjectOutputStream oos = new ObjectOutputStream(ManageStatic
							.getClientConServerThread(ManageStatic.user.getUserName()).getSocket().getOutputStream());
					oos.writeObject(tm);
				} catch (Exception e) {
					handler.post(runRe);
					e.printStackTrace();
				}
			}
		}.start();
		tempTime = System.currentTimeMillis();
		progressDialog.show();
		eventFlag = 1;
	}

	// Override BroadcastReceiver

	/**
	 * 获得在线好友的广播接收器
	 * 
	 * @author Administrator
	 *
	 */
	public class MyBroadcastReceiverRetfriends extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String[] friendNames = intent.getStringArrayExtra("friendNames");
			friendsName = friendNames;
			ArrayList<String> tempListFriends = new ArrayList<String>();
			for(int i = 0; i < friendsName.length; i++){
				if(!friendsName[i].equals("")){
					tempListFriends.add(friendsName[i]);
				}
			}
			String[] tempString = new String[tempListFriends.size()];
			for(int i = 0; i < tempListFriends.size(); i++){
				tempString[i] = tempListFriends.get(i);
			}
			friendsName = tempString;
		}

	}

	/**
	 * 获得查询返回数据的接收器
	 * 
	 * @author Administrator
	 *
	 */
	public class MyBroadcastReceiverAllCount extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			int allCountTemp = intent.getIntExtra("allCount", 3);
			allCount = allCountTemp;
			if (allCount == 0) {
				Dialog dialog = new AlertDialog.Builder(MainActivity.this).setTitle("查询结果")
						.setItems(new String[] { "您没有好友在当前区域" }, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
							}
						}).setPositiveButton("确定", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
							}
						}).create();
				progressDialog.dismiss();
				dialog.show();
				eventFlag = 0;
			} else {
				tempFlag = 0;
				new Thread() {
					public void run() {
						long tempTimeSec = System.currentTimeMillis();
						while (true) {
							if (tempFlag == 1) {
								break;
							}
							if (System.currentTimeMillis() - tempTimeSec > thresoldTime) {
								handler.post(run);
								eventFlag = 0;
								tempStrs = null;
								resultsTemp.clear();
								;
								allCount = 3;
								nowCount = 0;
								tempTime = 0;
								queryTypeTemp = null;

								eventFlag = 0;

								break;
							}
						}
					}

				}.start();

			}

		}

	}

	/**
	 * 处理查询数据的receiver
	 * 
	 * @author Administrator
	 *
	 */
	public class MyBroadcastReceiverResponseData extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {

			String responseData = intent.getStringExtra("responseData");
			String sender = intent.getStringExtra("sender");
			String resultArea = intent.getStringExtra("resultArea");
			String resultFaild = intent.getStringExtra("resultFaild");
			if(resultFaild == null){
				
				if(resultArea.equals(queryArea)){
					if (queryTypeTemp.equals(MessageType.QUERY_P)) {
						try {
							boolean result = ManageStatic.qU_AGRQ_P.QU_AGRQ_P_QRR(responseData);
							if (result) {
								if(!resultsTemp.contains(sender))
								resultsTemp.add(sender);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else if (queryTypeTemp.equals(MessageType.QUERY_C)) {
						try {
							boolean result = ManageStatic.qU_AGRQ_C.QU_AGRQ_P_QRR(responseData, queryCircle);
							if (result) {
								if(!resultsTemp.contains(sender))
								resultsTemp.add(sender);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				nowCount++;
			}
			
			if (( nowCount >= allCount) && (eventFlag != 0)) {
				tempFlag = 1;
				if(resultsTemp.isEmpty()){
					Dialog dialog = new AlertDialog.Builder(MainActivity.this).setTitle("查询结果")
							.setItems(new String[] { "您没有好友在当前区域" }, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
								}
							}).setPositiveButton("确定", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
								}
							}).create();
					progressDialog.dismiss();
					dialog.show();
					
				}else{
					tempStrs = new String[resultsTemp.size()];
					for (int i = 0; i < resultsTemp.size(); i++) {
						tempStrs[i] = resultsTemp.get(i);
					}
					Dialog dialog = new AlertDialog.Builder(MainActivity.this).setTitle("查询结果")
							.setItems(tempStrs, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
								}
							}).setPositiveButton("确定", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
								}
							}).create();
					progressDialog.dismiss();
					dialog.show();
					
				}
				tempStrs = null;
				resultsTemp.clear();
				allCount = 3;
				nowCount = 0;
				tempTime = 0;
				queryTypeTemp = null;
				queryArea = null;
				eventFlag = 0;
			}
		}
	}

	/**
	 * 处理添加好友的receiver
	 * 
	 * @author Administrator
	 *
	 */
	class MyBroadcastReceiverAddFriend extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			tempFlag = 1;
			if (eventFlag != 0) {
				String type = intent.getStringExtra("type");
				if (type.equals(MessageType.ADDFRIEND_SUCCESS)) {
					String[] friendsNameTemp = intent.getStringExtra("friendsName").split(User.separator);
					Dialog dialog = new AlertDialog.Builder(MainActivity.this).setTitle("操作结果")
							.setMessage("添加好友成功，该好友已在您的好友列表中！")
							.setPositiveButton("确定", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
								}
							}).create();
					progressDialog.dismiss();
					dialog.show();
					friendsName = friendsNameTemp;
					ArrayList<String> tempListFriends = new ArrayList<String>();
					for(int i = 0; i < friendsName.length; i++){
						if(!friendsName[i].equals("")){
							tempListFriends.add(friendsName[i]);
						}
					}
					String[] tempString = new String[tempListFriends.size()];
					for(int i = 0; i < tempListFriends.size(); i++){
						tempString[i] = tempListFriends.get(i);
					}
					friendsName = tempString;
				} else if (type.equals(MessageType.ADDFRIEND_FAIL)) {
					Dialog dialog = new AlertDialog.Builder(MainActivity.this).setTitle("操作结果")
							.setMessage("添加好友失败，该用户不存在！")
							.setPositiveButton("确定", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
								}
							}).create();
					progressDialog.dismiss();
					dialog.show();
				}
				eventFlag = 0;
			}
		}

	}
	
	/**
	 * 处理断开连接事件
	 * @author Administrator
	 *
	 */
	class MyBroadcastReceiverSocketFailed extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Dialog dialog = new AlertDialog.Builder(MainActivity.this).setTitle("操作结果")
					.setItems(new String[] { "服务异常，请退出后重新登录 " }, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
						}
					}).setPositiveButton("确定", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
						}
					}).create();
			dialog.show();
		}
		
	}
	
	// 更新UI的Runnable类

	/**
	 * 查询服务异常时更新UI
	 */
	Runnable run = new Runnable() {
		public void run() {
			Dialog dialog = new AlertDialog.Builder(MainActivity.this).setTitle("操作结果")
					.setItems(new String[] { "服务异常，请重新查询 " }, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
						}
					}).setPositiveButton("确定", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
						}
					}).create();
			progressDialog.dismiss();
			dialog.show();
		}
	};
	
	/**
	 * 处理断开连接是的异常
	 */
	Runnable runRe = new Runnable() {
		public void run() {
			Dialog dialog = new AlertDialog.Builder(MainActivity.this).setTitle("操作结果")
					.setItems(new String[] { "服务异常，请退出后重新登录 " }, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
						}
					}).setPositiveButton("确定", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
						}
					}).create();
			progressDialog.dismiss();
			dialog.show();
		}
	};
	/**
	 * 处理添加耗时异常的更新UI操作
	 */
	Runnable runAddFriend = new Runnable() {
		public void run() {
			Dialog dialog = new AlertDialog.Builder(MainActivity.this).setTitle("操作结果")
					.setItems(new String[] { "添加失败，好友名不能为空！" }, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
						}
					}).setPositiveButton("确定", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
						}
					}).create();
			progressDialog.dismiss();
			dialog.show();
		}
	};

	// 重写监听事件的方法

	/**
	 * 拖动条拖动事件方法
	 */
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		if (circle == null) {
			return;
		}
		aMap.clear();
		MarkerOptions otMarkerOptions = new MarkerOptions();
		otMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.yellowpoint));
		otMarkerOptions.position(tempLatLng);
		aMap.addMarker(otMarkerOptions);
		aMap.moveCamera(CameraUpdateFactory.changeLatLng(tempLatLng));
		// 绘制一个圆形
		progressValue = progress + 1000;
		circle = aMap.addCircle(new CircleOptions().center(tempLatLng).radius(progressValue)
				.strokeColor(Color.argb(50, 1, 1, 1)).fillColor(Color.argb(50, 1, 1, 1)).strokeWidth(1));
		aMap.invalidate();// 刷新地图
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
	}

	/**
	 * 地图点击事件
	 */
	@Override
	public void onMapClick(LatLng latLng) {
		if (typeFlag == 1) {
			aMap.clear();
			if (clearFlag == 1) {
				aMap.clear();
				latitude.clear();//
				longitude.clear();
				latl.clear();
				clearFlag = 0;
			}
			PolygonOptions pOption = new PolygonOptions();
			latl.add(latLng);
			latitude.add(latLng.latitude);
			longitude.add(latLng.longitude);
			if (latitude.size() >= 3) {
				if (Dbx(latitude, longitude) == 0) {
					latl.remove(latl.size() - 1);
					latitude.remove(latitude.size() - 1);
					longitude.remove(longitude.size() - 1);
					clearFlag = 1;
					dealMethod();
				}
			}
			MarkerOptions otMarkerOptions = new MarkerOptions();
			otMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.yellowpoint));
			for (int j = 0; j < latl.size(); j++) {
				otMarkerOptions.position((LatLng) latl.get(j));
				aMap.addMarker(otMarkerOptions);
				aMap.moveCamera(CameraUpdateFactory.changeLatLng(latLng));
			}
			for (int i = 0; i < latitude.size(); i++) {
				pOption.add(new LatLng((double) latitude.get(i), (double) longitude.get(i)));

			}
			polygon = aMap.addPolygon(
					pOption.strokeWidth(4).strokeColor(Color.argb(50, 1, 1, 1)).fillColor(Color.argb(50, 1, 1, 1)));
		} else if (typeFlag == 2) {
			aMap.clear();
			tempLatLng = latLng;
			MarkerOptions otMarkerOptions = new MarkerOptions();
			otMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.yellowpoint));
			otMarkerOptions.position(latLng);
			aMap.addMarker(otMarkerOptions);
			aMap.moveCamera(CameraUpdateFactory.changeLatLng(tempLatLng));
			circle = aMap.addCircle(new CircleOptions().center(tempLatLng).radius(1000)
					.strokeColor(Color.argb(50, 1, 1, 1)).fillColor(Color.argb(50, 1, 1, 1)).strokeWidth(1));
			sbRadius.setProgress(0);
			progressValue = 1000;
		}
	}
	
	/**
	 * 发送AGRQ_P的查询请求
	 */
	private void dealMethod() {
		new Thread() {
			@Override
			public void run() {
				ArrayList<Vertex> queryList = new ArrayList<Vertex>();
				for (int i = latitude.size() - 1; i >= 0; i--) {
					String[] vertexString = coor.latLon2UTM(Double.parseDouble(ChangTwoBit.change(latitude.get(i))), Double.parseDouble(ChangTwoBit.change(longitude.get(i)))).split(" ");
					queryList.add(new Vertex(Integer.parseInt(vertexString[2]), Integer.parseInt(vertexString[3])));
					queryArea = vertexString[0]+" "+vertexString[1]; 
				}
				String queryMessage = ManageStatic.qU_AGRQ_P.QU_AGRQ_P_QDC(queryList);
				TransMessage tm = new TransMessage();
				tm.setMessageType(MessageType.SEND_QUERY);
				tm.setMessageContent(queryMessage);
				tm.setSender(ManageStatic.user.getUserName());
				tm.setQueryType(MessageType.QUERY_P);
				queryTypeTemp = MessageType.QUERY_P;
				try {
					ObjectOutputStream oos = new ObjectOutputStream(ManageStatic
							.getClientConServerThread(ManageStatic.user.getUserName()).getSocket().getOutputStream());
					oos.writeObject(tm);
				} catch (IOException e) {
					handler.post(runRe);
					e.printStackTrace();
				}
			}
		}.start();
		tempTime = System.currentTimeMillis();
		progressDialog.show();
		eventFlag = 1;
	}

	/**
	 * 选取地图上的点
	 * @param arr1
	 * @param arr2
	 * @return
	 */
	public int Dbx(List<Double> arr1, List<Double> arr2) {
		int t = arr1.size();
		int flag = 1;
		double[][] arr = new double[t][2];
		for (int i = 0; i < t; i++) {
			arr[i][0] = (double) arr1.get(i);
			arr[i][1] = (double) arr2.get(i);
		}
		double x1, y1, x2, y2, x3, y3;
		for (int i = 0; i < t; i++) {
			x1 = arr[i][0];
			y1 = arr[i][1];
			x2 = arr[(i + 1) % t][0];
			y2 = arr[(i + 1) % t][1];
			x3 = arr[(i + 2) % t][0];
			y3 = arr[(i + 2) % t][1];
			if ((x1 * y3 + x3 * y2 + x2 * y1 - x2 * y3 - x1 * y2 - x3 * y1) >= 0) {
				flag = 0;
				return 0;
			}
		}
		return 1;
	}

	/**
	 * 定位成功后回调函数
	 */
	@Override
	public void onLocationChanged(AMapLocation amapLocation) {
		if (mListener != null && amapLocation != null) {
			if (amapLocation != null && amapLocation.getErrorCode() == 0) {
				mListener.onLocationChanged(amapLocation);// 显示系统小蓝点
			} else {
				String errText = "定位失败," + amapLocation.getErrorCode() + ": " + amapLocation.getErrorInfo();
				// Log.e("AmapErr",errText);
			}
		}
	}

	/**
	 * 激活定位
	 */
	@Override
	public void activate(OnLocationChangedListener listener) {
		mListener = listener;
		if (mlocationClient == null) {
			mlocationClient = new AMapLocationClient(this);
			mLocationOption = new AMapLocationClientOption();
			mlocationClient.setLocationListener(this);
			mLocationOption.setLocationMode(AMapLocationMode.Hight_Accuracy);
			mlocationClient.setLocationOption(mLocationOption);
			mlocationClient.startLocation();
		}
	}

	/**
	 * 停止定位
	 */
	@Override
	public void deactivate() {
		mListener = null;
		if (mlocationClient != null) {
			mlocationClient.stopLocation();
			mlocationClient.onDestroy();
		}
		mlocationClient = null;
	}

}
