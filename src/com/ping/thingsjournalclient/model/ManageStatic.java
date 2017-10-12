package com.ping.thingsjournalclient.model;

import java.util.HashMap;

import com.amap.api.location.AMapLocationClient;
import com.ping.thingsjournalclient.common.QU_AGRQ_C;
import com.ping.thingsjournalclient.common.QU_AGRQ_P;


public class ManageStatic {
	public static User user = new User();
	public static String publicKey = "04ED6C7C0BCEBB5F8A26A994176268E3F6ABE0F1BD4E620628DC5CC0D2B06FC7B06CE830D3EC18549A9AADCA0B14B653D44114776A3BED195A1DACB9EF1F634CA3";//与服务器交换数据时的公钥
	private static HashMap<String,ClientConServerThread> hm=new HashMap<String,ClientConServerThread>();//管理客户端新建的线程
	
	public static QU_AGRQ_C	qU_AGRQ_C = new QU_AGRQ_C();
	public static QU_AGRQ_P qU_AGRQ_P = new QU_AGRQ_P();
	public static AMapLocationClient locationClient = null;
	
	//把创建好的ClientConServerThread放入到hm
	public static void addClientConServerThread(String account,ClientConServerThread ccst){
		hm.put(account, ccst);
	}
	
	//可以通过account取得该线程
	public static ClientConServerThread getClientConServerThread(String acount){
		return (ClientConServerThread)hm.get(acount);
	}
}
