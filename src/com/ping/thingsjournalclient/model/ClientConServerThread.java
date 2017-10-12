package com.ping.thingsjournalclient.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.amap.api.location.AMapLocation;
import com.ping.thingsjournalclient.common.Vertex;
import com.ping.thingsjournalclient.util.CoordinateConversion;
import com.ping.thingsjournalclient.util.SMUtils;

import android.content.Context;
import android.content.Intent;

public class ClientConServerThread extends Thread {

	private Context context;
	private Socket socket;
	
	public ClientConServerThread(Context context, Socket socket) {
		super();
		this.context = context;
		this.socket = socket;
	}
	
	//重写线程方法
	public void run() {
		
		/**
		 * 新建一个线程发送心跳包
		 */
		new Thread(){
			public void run() {
				long timeCount = System.currentTimeMillis();
				try {
				while(true){
					if(System.currentTimeMillis() - timeCount > 1000*60){
						timeCount = System.currentTimeMillis();
						TransMessage tmCount =new TransMessage();
						tmCount.setMessageType(MessageType.ON);
						new ObjectOutputStream(socket.getOutputStream()).writeObject(tmCount);
					}
				}
				} catch (IOException e) {
					try {
						if(socket != null){
							socket.close();
						}
						Intent intent = new Intent("org.ping.socketfailed");
						context.sendBroadcast(intent);
					} catch (IOException e1) {
						
					}
				}
			}
			
		}.start();
		
		/**
		 * 接收服务器发送过来的数据处理
		 */
		while(true){
			try{
				TransMessage tm = null;
				ObjectInputStream ois =null;
				ois = new ObjectInputStream(socket.getInputStream());
				tm = (TransMessage) ois.readObject();
				String type = tm.getMessageType();
				if(type.equals(MessageType.RET_FRIENDS)){//更新好友列表，此处应加密传输
					Intent intent = new Intent("org.ping.retfriend");
					String friendNamesStr = SMUtils.decryptBySm4(tm.getMessageContent(), ManageStatic.user.getKey_QU());
					String[] friendNames = friendNamesStr.split(User.separator);
					intent.putExtra("friendNames", friendNames);
					System.out.println("accept friendsNames client");
					context.sendBroadcast(intent);
				}else if(type.equals(MessageType.BACK_QUERY)){//处理服务器对查询返回的请求
					Intent intent = new Intent("org.ping.backquery");
					int allCount = tm.getReceiverCount();
					intent.putExtra("allCount", allCount);
					context.sendBroadcast(intent);
				}else if(type.equals(MessageType.BACK_RESULT)){//处理好友返回的查询结果
					Intent intent = new Intent("org.ping.backresult");
					String responseData = tm.getMessageContent();
					String sender = tm.getSender();
					String resultArea = tm.getArea().trim();
					intent.putExtra("responseData", responseData);
					intent.putExtra("sender", sender);
					intent.putExtra("resultArea", resultArea);
					context.sendBroadcast(intent);
				}else if(type.equals(MessageType.DEAL_MESSAGE)){//处理好友返回的查询结果
					/**
					 * 获取位置
					 */
					AMapLocation location = ManageStatic.locationClient.getLastKnownLocation();
					//errCode等于0代表定位成功，其他的为定位失败，具体的可以参照官网定位错误码说明
					if(location.getErrorCode() == 0){
						double longitude = location.getLongitude();
						double latitude = location.getLatitude();
						CoordinateConversion coor = new CoordinateConversion();
						String coorResult = coor.latLon2UTM(latitude, longitude);
						String[] coorResults = coorResult.split(" ");
						responseData(new Vertex(Long.parseLong(coorResults[2]), Long.parseLong(coorResults[3])), tm, coorResults[0]+" "+coorResults[1] );
					} else {
						//定位失败
						TransMessage tmSender = new TransMessage();
						tmSender.setSender(ManageStatic.user.getUserName());
						tmSender.setMessageType(MessageType.LOCATIONFAILED);
						ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
						oos.writeObject(tmSender);
					}
				}else if(type.equals(MessageType.ADDFRIEND_SUCCESS)||type.equals(MessageType.ADDFRIEND_FAIL)){
					Intent intent = new Intent("org.ping.addfriend");
					String typeAdd = type;
					String friendsName = tm.getMessageContent();
					intent.putExtra("type", typeAdd);
					intent.putExtra("friendsName", friendsName);
					context.sendBroadcast(intent);
				}else if(type.equals(MessageType.LOCATIONFAILED)){
					Intent intent = new Intent("org.ping.backresult");
					String resultFaild = "true";
					intent.putExtra("resultFaild", resultFaild);
					context.sendBroadcast(intent);
				}else if(type.equals(MessageType.ON)){
				}
			}catch(IOException e){//连接断开时，关闭socket，提示用户重新登录
				try {
					if(socket != null){
						socket.close();
					}
					Intent intent = new Intent("org.ping.socketfailed");
					context.sendBroadcast(intent);
					break;
				} catch (IOException e1) {
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	/**
	 * 处理查询数据
	 * @param vertex
	 */
	private void responseData(Vertex vertex, TransMessage tm, String area ) {
		String sender = tm.getSender();
		String receiver = tm.getReceiver();
		String queryType = tm.getQueryType();
		String message = tm.getMessageContent();
		TransMessage tmSender = new TransMessage();
		String responseDataMessage;
		if(queryType.equals(MessageType.QUERY_P)){
			try {
				responseDataMessage = ManageStatic.qU_AGRQ_P.UF_AGRQ_P_RDC(message, vertex);
				tmSender.setMessageContent(responseDataMessage);
			} catch (Exception e) {
				System.out.println("数据处理出错");
				e.printStackTrace();
			}
		}else if(queryType.equals(MessageType.QUERY_C)){
			try {
				responseDataMessage = ManageStatic.qU_AGRQ_C.UF_AGRQ_C_RDC(message, vertex);
				tmSender.setMessageContent(responseDataMessage);
			} catch (Exception e) {
				System.out.println("数据处理出错");
				e.printStackTrace();
			}
		}
		tmSender.setReceiver(sender);
		tmSender.setSender(receiver);
		tmSender.setMessageType(MessageType.SEND_RESULT);
		tmSender.setQueryType(queryType);
		tmSender.setArea(area);
		try {
			ObjectOutputStream oos = new ObjectOutputStream(getSocket().getOutputStream());
			oos.writeObject(tmSender);
		} catch (IOException e) {
			System.out.println("发送处理数据失败");
			e.printStackTrace();
		}
	}


	public Context getContext() {
		return context;
	}
	public void setContext(Context context) {
		this.context = context;
	}
	public Socket getSocket() {
		return socket;
	}
	public void setSocket(Socket socket) {
		this.socket = socket;
	}
	
}
