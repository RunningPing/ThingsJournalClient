package com.ping.thingsjournalclient.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import com.ping.thingsjournalclient.util.SMUtils;

import android.content.Context;

public class Client {
	
	private final String address = "139.199.152.152";//��������ַ
//	private final String address = "192.168.56.1";//��������ַ
	private final int port = 30001;//ʹ�ö˿�
	private Context context;
	public Socket s;
	
	
	public Client(Context context){
		this.context = context;
	}
	
	/**
	 * �����¼����
	 * @param user
	 * @return
	 */
	public boolean sendLoginInfo(User user){
		boolean b = false;
		
		s = new Socket();
		try {
			s.connect(new InetSocketAddress(address,port),60000*3);
			s.setSoTimeout(1000*60*60*24*10);
		} catch (UnknownHostException e1) {
			System.out.println("host not find");
			return false;
		} catch (IOException e1) {
			System.out.println("IOException");
			return false;
		}
		
		ObjectOutputStream oos; 
		ObjectInputStream ois;
		try {
			oos = new ObjectOutputStream(s.getOutputStream());
			oos.writeObject(user);
			ois = new ObjectInputStream(s.getInputStream());
			TransMessage tm = (TransMessage)ois.readObject();
			if(tm.getMessageType().equals(MessageType.LOGIN_SUCCESS)){
				String key_QU = SMUtils.decryptBySm2(tm.getMessageContent(), user.getPrivateKey());
				ManageStatic.user.setKey_QU(key_QU);
				ManageStatic.user.setUserName(user.getUserName());
				ManageStatic.qU_AGRQ_C.setKey_QU(key_QU);
				ManageStatic.qU_AGRQ_C.setQUID(user.getUserName());
				ManageStatic.qU_AGRQ_P.setKey_QU(key_QU);
				ManageStatic.qU_AGRQ_P.setQUID(user.getUserName());
				ClientConServerThread ccst = new ClientConServerThread(context,s);
				ccst.start();
				ManageStatic.addClientConServerThread(user.getUserName(), ccst);
				b = true;
			}else if(tm.getMessageType().equals(MessageType.LOGIN_FAIL)){
				b = false;
			}
		} catch (Exception e) {
			try {
				if(s != null){
					s.close();
				}
			} catch (IOException e1) {
				e.printStackTrace();
			}
		}
		return b;
	}
	
	/**
	 * ����ע������
	 * @param user
	 * @return
	 */
	public boolean sendRegisterInfo(User user){
		boolean b = false;
		
		s = new Socket();
		try {
			s.connect(new InetSocketAddress(address,port),2000);
		} catch (UnknownHostException e1) {
			System.out.println("host not find");
			return false;
		} catch (IOException e1) {
			System.out.println("IOException");
			return false;
		}
		
		ObjectOutputStream oos; 
		ObjectInputStream ois;
		try {
			oos = new ObjectOutputStream(s.getOutputStream());
			oos.writeObject(user);
			ois = new ObjectInputStream(s.getInputStream());
			TransMessage tm = (TransMessage)ois.readObject();
			if(tm.getMessageType().equals(MessageType.REGISTER_SUCCESS)){
				b = true;
			}else if(tm.getMessageType().equals(MessageType.REGISTER_FAIL)){
				b = false;
			}
			oos.close();
			ois.close();
			s.close();
		} catch (Exception e) {
			try {
				if(s != null){
					s.close();
				}
			} catch (IOException e1) {
				e.printStackTrace();
			}
		}
		return b;
	}
	
}
