package com.ping.thingsjournalclient.util;

import java.text.DecimalFormat;

public class ChangTwoBit {

	/**
	 * 保存小数点后两位的小数
	 * @param num
	 * @return
	 */
	public static String change(double num){
		String result = null;
		DecimalFormat df = new DecimalFormat("#.00");
		result = df.format(num);
		return result;
	}
}
