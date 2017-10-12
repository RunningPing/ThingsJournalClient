package com.ping.thingsjournalclient.model;

import java.io.Serializable;

public class TransMessage implements Serializable{
	
	private String messageType = null;
	private String messageContent = null;
	private String sender = null;
	private String receiver = null;
	private int receiverCount = 0;
	private String queryType = null;
	private String area = null;
	
	public String getArea() {
		return area;
	}
	public void setArea(String area) {
		this.area = area;
	}
	public String getQueryType() {
		return queryType;
	}
	public void setQueryType(String queryType) {
		this.queryType = queryType;
	}
	public String getMessageType() {
		return messageType;
	}
	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}
	public String getMessageContent() {
		return messageContent;
	}
	public void setMessageContent(String messageContent) {
		this.messageContent = messageContent;
	}
	public String getSender() {
		return sender;
	}
	public void setSender(String sender) {
		this.sender = sender;
	}
	public String getReceiver() {
		return receiver;
	}
	public void setReceiver(String receiver) {
		this.receiver = receiver;
	}
	public int getReceiverCount() {
		return receiverCount;
	}
	public void setReceiverCount(int receiverCount) {
		this.receiverCount = receiverCount;
	}
	
	
}
