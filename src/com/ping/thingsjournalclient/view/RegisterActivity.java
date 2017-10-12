package com.ping.thingsjournalclient.view;

import com.example.thingsjournalclient.R;
import com.ping.thingsjournalclient.model.Client;
import com.ping.thingsjournalclient.model.ManageStatic;
import com.ping.thingsjournalclient.model.MessageType;
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

public class RegisterActivity extends Activity {//注册界面
	
	EditText accountEt = null;
	EditText passwordEt = null;
	EditText passwordSecEt = null;
	Button registerBtn = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_register);
		ManageActivity.allActiviy.add(this);
		accountEt = (EditText) findViewById(R.id.register_account);
		passwordEt = (EditText) findViewById(R.id.register_password);
		passwordSecEt = (EditText) findViewById(R.id.register_password_sec);
		registerBtn = (Button) findViewById(R.id.register_btn);
		registerBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (android.os.Build.VERSION.SDK_INT > 9) {
				    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
				    StrictMode.setThreadPolicy(policy);
				}
				if(accountEt.getText().toString().equals("")||passwordEt.getText().toString().equals("")){
					Toast.makeText(RegisterActivity.this, "用户名或密码不能为空！", Toast.LENGTH_SHORT).show();
				}else if(!passwordEt.getText().toString().equals(passwordSecEt.getText().toString())){
					Toast.makeText(RegisterActivity.this, "两次密码输入不一致", Toast.LENGTH_SHORT).show();
				}else{
					User user = new User();
					user.setType(MessageType.REGISTER);
					user.setUserName(accountEt.getText().toString());
					try {
						user.setPassword(SMUtils.encryptBySm2(passwordEt.getText().toString(), ManageStatic.publicKey));
						boolean b = new Client(RegisterActivity.this).sendRegisterInfo(user);
						if(b){
							Toast.makeText(RegisterActivity.this, "恭喜您注册成功！", Toast.LENGTH_SHORT).show();
							startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
						}else{
							Toast.makeText(RegisterActivity.this, "注册失败，用户名已被使用，请更换后重试！", Toast.LENGTH_SHORT).show();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					
				}
			}
		});
		findViewById(R.id.register_back_btn).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
			}
		});
	}


	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, 1, 0, "退出");
		return true;
	}
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()){
		case 1:
			for(int i = 0; i<ManageActivity.allActiviy.size();i++){
				if(ManageActivity.allActiviy.get(i) != this){
					ManageActivity.allActiviy.get(i).finish();
				}
		    }
			this.finish();
			System.exit(1);
			break;
    	}
        return true;
    }
}
