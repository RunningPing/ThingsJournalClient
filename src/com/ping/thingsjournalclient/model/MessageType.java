package com.ping.thingsjournalclient.model;

public class MessageType {

	public static final String LOGIN_SUCCESS="1";//表明登录成功
	public static final String LOGIN_FAIL="2";//表明登录失败
    public static final String REGISTER_SUCCESS="3";//表明注册成功
	public static final String REGISTER_FAIL="4";//表明注册失败
    public static final String ADDFRIEND_SUCCESS="5";//表明添加朋友成功
    public static final String ADDFRIEND_FAIL="6";//表明添加朋友失败
	public static final String SEND_QUERY="7";//查询数据
	public static final String BACK_QUERY="8";//返回查询数据
	public static final String SEND_RESULT="9";//查询数据的结果
	public static final String BACK_RESULT="10";//返回的查询结果
	public static final String GET_FRIENDS="11";//要求好友的包
	public static final String RET_FRIENDS="12";//返回好友的包
	public static final String LOGIN="13";//请求验证登陆
    public static final String REGISTER="14";//请求验证注册
    public static final String ADDFRIEND="18";//请求添加朋友
    public static final String DEAL_MESSAGE="15";//请求处理数据
	public static final String symbol1="%%";
	public static final String symbol2="&&";
	public static final String symbol3="@@";
	public static final String QUERY_C = "16";//C方式查询
	public static final String QUERY_P = "17";//P方式查询
	public static final String LOCATIONFAILED = "19";//定位失败
	public static final String ON = "20";//保持连接
	
}
