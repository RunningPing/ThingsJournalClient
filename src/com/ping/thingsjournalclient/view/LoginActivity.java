package com.ping.thingsjournalclient.view;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import com.amap.api.location.AMapLocationClient;
import com.example.thingsjournalclient.R;
import com.ping.thingsjournalclient.model.Client;
import com.ping.thingsjournalclient.model.ManageStatic;
import com.ping.thingsjournalclient.model.MessageType;
import com.ping.thingsjournalclient.model.TransMessage;
import com.ping.thingsjournalclient.model.User;
import com.ping.thingsjournalclient.util.SMUtils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity {

	private EditText accountEt,passwordEt;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_login);
		ManageActivity.allActiviy.add(this);
		accountEt = (EditText) findViewById(R.id.et_account);
		passwordEt = (EditText) findViewById(R.id.et_password);
		Button btnLogin = (Button) findViewById(R.id.btn_login);
		btnLogin.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if(accountEt.getText().toString().trim().equals("") || passwordEt.getText().toString().trim().equals("")){
					Toast.makeText(LoginActivity.this, "账号或密码不能为空！", Toast.LENGTH_SHORT).show();
				}else{
					login(accountEt.getText().toString(), passwordEt.getText().toString());
				}
			}

		});
		Button btnRegister = (Button) findViewById(R.id.btn_register);
		btnRegister.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
			}
		});
	}
	
	/**
	 * 处理登录请求
	 * @param userName
	 * @param string
	 */
	protected void login(String userName, String string) {
		
		if (android.os.Build.VERSION.SDK_INT > 9) {
		    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		    StrictMode.setThreadPolicy(policy);
		}
		
		User user = new User();
		user.setUserName(userName);
		user.setPassword(SMUtils.encryptBySm3(string));
		user.setType(MessageType.LOGIN);
		HashMap<String, String> keypair = SMUtils.sm2generateKeyPair();
		user.setPrivateKey(keypair.get("privateKey"));
		user.setPublicKey(keypair.get("publicKey"));
		System.out.println("1");
		boolean b = new Client(this).sendLoginInfo(user);
		System.out.println(b);
		if(b){
			try {
				ObjectOutputStream oos = new ObjectOutputStream(ManageStatic.getClientConServerThread(ManageStatic.user.getUserName()).getSocket().getOutputStream());
				TransMessage tm = new TransMessage();
				tm.setMessageType(MessageType.GET_FRIENDS);
				tm.setSender(user.getUserName());
				oos.writeObject(tm);
			} catch (IOException e) {
				e.printStackTrace();
			}
			ManageStatic.locationClient = new AMapLocationClient(this.getApplicationContext());
			startActivity(new Intent(this, MainActivity.class));
		}else{
			Toast.makeText(this, "登录失败，请重新登录", Toast.LENGTH_SHORT).show();
		}
		
	}

	/**
	 * 添加menu
	 */
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, 1, 0, "退出");
		return true;
	}
	
	/*
	 * 添加menu事件
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()){
		case 1:
			for(int i = 0; i<ManageActivity.allActiviy.size();i++){
				if(ManageActivity.allActiviy.get(i) != this){
					ManageActivity.allActiviy.get(i).finish();
				}
		    }
			this.finish();
			System.exit(0);
			break;
    	}
        return true;
    }
}
