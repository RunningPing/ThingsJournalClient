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
	
	//��д�̷߳���
	public void run() {
		
		/**
		 * �½�һ���̷߳���������
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
		 * ���շ��������͹��������ݴ���
		 */
		while(true){
			try{
				TransMessage tm = null;
				ObjectInputStream ois =null;
				ois = new ObjectInputStream(socket.getInputStream());
				tm = (TransMessage) ois.readObject();
				String type = tm.getMessageType();
				if(type.equals(MessageType.RET_FRIENDS)){//���º����б��˴�Ӧ���ܴ���
					Intent intent = new Intent("org.ping.retfriend");
					String friendNamesStr = SMUtils.decryptBySm4(tm.getMessageContent(), ManageStatic.user.getKey_QU());
					String[] friendNames = friendNamesStr.split(User.separator);
					intent.putExtra("friendNames", friendNames);
					System.out.println("accept friendsNames client");
					context.sendBroadcast(intent);
				}else if(type.equals(MessageType.BACK_QUERY)){//����������Բ�ѯ���ص�����
					Intent intent = new Intent("org.ping.backquery");
					int allCount = tm.getReceiverCount();
					intent.putExtra("allCount", allCount);
					context.sendBroadcast(intent);
				}else if(type.equals(MessageType.BACK_RESULT)){//������ѷ��صĲ�ѯ���
					Intent intent = new Intent("org.ping.backresult");
					String responseData = tm.getMessageContent();
					String sender = tm.getSender();
					String resultArea = tm.getArea().trim();
					intent.putExtra("responseData", responseData);
					intent.putExtra("sender", sender);
					intent.putExtra("resultArea", resultArea);
					context.sendBroadcast(intent);
				}else if(type.equals(MessageType.DEAL_MESSAGE)){//������ѷ��صĲ�ѯ���
					/**
					 * ��ȡλ��
					 */
					AMapLocation location = ManageStatic.locationClient.getLastKnownLocation();
					//errCode����0����λ�ɹ���������Ϊ��λʧ�ܣ�����Ŀ��Բ��չ�����λ������˵��
					if(location.getErrorCode() == 0){
						double longitude = location.getLongitude();
						double latitude = location.getLatitude();
						CoordinateConversion coor = new CoordinateConversion();
						String coorResult = coor.latLon2UTM(latitude, longitude);
						String[] coorResults = coorResult.split(" ");
						responseData(new Vertex(Long.parseLong(coorResults[2]), Long.parseLong(coorResults[3])), tm, coorResults[0]+" "+coorResults[1] );
					} else {
						//��λʧ��
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
			}catch(IOException e){//���ӶϿ�ʱ���ر�socket����ʾ�û����µ�¼
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
	 * �����ѯ����
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
				System.out.println("���ݴ������");
				e.printStackTrace();
			}
		}else if(queryType.equals(MessageType.QUERY_C)){
			try {
				responseDataMessage = ManageStatic.qU_AGRQ_C.UF_AGRQ_C_RDC(message, vertex);
				tmSender.setMessageContent(responseDataMessage);
			} catch (Exception e) {
				System.out.println("���ݴ������");
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
			System.out.println("���ʹ�������ʧ��");
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
