package com.suntrans.ibmsdemo;

import views.TouchListener;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class Mode_Activity extends Activity {
	private String ipaddr;
	private TextView tx1,tx2,tx3;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);				
		ActionBar actionBar = this.getActionBar();   //设置actionbar标题栏		
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setDisplayShowCustomEnabled(true);
		actionBar.setDisplayShowHomeEnabled(true);
		Drawable d = this.getResources().getDrawable(R.color.bg_action); 
		actionBar.setBackgroundDrawable(d);   //设置actionbar背景
		actionBar.show();
		setContentView(R.layout.mode);     //设置布局文件	
		Intent intent=getIntent();
		ipaddr=intent.getStringExtra("IPAddr");
		tx1=(TextView)findViewById(R.id.tx1);
		tx2=(TextView)findViewById(R.id.tx2);
		tx3=(TextView)findViewById(R.id.tx3);
		tx1.setOnTouchListener(new TouchListener());
		tx2.setOnTouchListener(new TouchListener());
		tx3.setOnTouchListener(new TouchListener());
		tx1.setOnClickListener(new OnClickListener(){   //开关控制
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent=new Intent();
	            intent.putExtra("IPAddr",ipaddr);      //开关ip
	         	intent.setClass(Mode_Activity.this, Channel_Activity.class);//设置要跳转的activity
				Mode_Activity.this.startActivity(intent);//开始跳转
			}			
		});
		tx2.setOnClickListener(new OnClickListener(){    //在线参数
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent=new Intent();
	            intent.putExtra("IPAddr",ipaddr);      //开关ip
	         	intent.setClass(Mode_Activity.this, Perameter_Activity.class);//设置要跳转的activity
				Mode_Activity.this.startActivity(intent);//开始跳转
			}			
		});
		tx3.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent=new Intent();
	            intent.putExtra("IPAddr",ipaddr);      //开关ip
	         	intent.setClass(Mode_Activity.this, Config_Activity.class);//设置要跳转的activity
				Mode_Activity.this.startActivity(intent);//开始跳转
			}			
		});
	}
	@Override           //menu选项监听
	public boolean onMenuItemSelected(int featureId, MenuItem item)
	{
		if(item.getItemId() == android.R.id.home)    //如果按下的是返回键
	    {
	        finish();
	        return true;
	    }
		
		else
			return true;
	}
}
