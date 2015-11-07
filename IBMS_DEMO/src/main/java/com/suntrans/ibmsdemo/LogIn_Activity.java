package com.suntrans.ibmsdemo;

import database.DbHelper;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class LogIn_Activity extends Activity {
	private Button login;              //登录按钮
	private EditText name;             //账号
	private EditText password;         //密码
	private CheckBox remember;         //记住密码  选择框
	private CheckBox auto;             //自动登录选择框
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);     //设置布局文件
		login=(Button)findViewById(R.id.login);
		name=(EditText)findViewById(R.id.name);
		password=(EditText)findViewById(R.id.password);
		remember=(CheckBox)findViewById(R.id.remember);
		auto=(CheckBox)findViewById(R.id.auto);
		remember.setChecked(true);
		auto.setChecked(true);
		DbHelper dh1=new DbHelper(LogIn_Activity.this,"IBMS",null,1);
		SQLiteDatabase db = dh1.getWritableDatabase(); 
		Cursor cursor = db.query(true, "users_tb", new String[]{"NID","Name","Password","IsUsing","Remember","Auto"}, null, null, null, null, null, null, null);
		while(cursor.moveToNext())
		{
			if(cursor.getString(3).equals("1"))
			{
				if(cursor.getString(4).equals("1"))   //如果之前选择记住密码，则账号和密码都显示
				{
					name.setText(cursor.getString(1));					
					password.setText(cursor.getString(2));
					
				} 
				else                //如果没有记住密码，只显示账号，用户去输入密码
				{
					name.setText(cursor.getString(1));
					password.requestFocus();    //输入密码获取焦点
				}
				remember.setChecked(cursor.getString(4).equals("1")?true:false);
				auto.setChecked(cursor.getString(5).equals("1")?true:false);
			}
			
		}
		//为登录按钮绑定点击事件，执行登录操作
		login.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				
				// TODO Auto-generated method stub
				if(name.getText().toString().equals("")||name.getText()==null)
				{
					Toast.makeText(getApplicationContext(),"账号不能为空！",Toast.LENGTH_SHORT).show();
				}
				else if(password.getText().toString().equals("")||password.getText()==null)
				{
					Toast.makeText(getApplicationContext(),"密码不能为空！",Toast.LENGTH_SHORT).show();
				}
				else
				{
					//Toast.makeText(getApplicationContext(),"正在登录中。。。",300).show();
					DbHelper dh1=new DbHelper(LogIn_Activity.this,"IBMS",null,1);
					SQLiteDatabase db = dh1.getWritableDatabase(); 
					db.beginTransaction();
					Cursor cursor = db.query("users_tb", new String[]{"Password","RSAddr"},"Name=?", new String[]{name.getText().toString()}, null, null, null);
					if(cursor.getCount()<1)
					{
						db.setTransactionSuccessful();
						db.endTransaction();
						db.close();
						Toast.makeText(getApplicationContext(),"用户名不存在！",Toast.LENGTH_SHORT).show();
					}
					else
					{
						String pass="";   //密码
						String rsaddr="";   //485地址
						while(cursor.moveToNext())
						{	
							pass=cursor.getString(0);
							rsaddr=cursor.getString(1);
						}
						if(password.getText().toString().equals(pass))
						{
							Toast.makeText(getApplicationContext(),"登录成功！",Toast.LENGTH_SHORT).show();
							ContentValues cv = new ContentValues();    //内容数组	
							cv.put("IsUsing","1");   //是否正在使用
							cv.put("Auto",auto.isChecked()?"1":"0");   //是否自动登录 
							cv.put("Remember",remember.isChecked()?"1":"0");   //是否记住密码
							ContentValues cv1 = new ContentValues();    //内容数组	
							cv1.put("IsUsing","0");   //是否正在使用
							db.update("users_tb",cv1,null,null);    //更新数据库数据，先将所有的账号设为未使用
							db.update("users_tb",cv,"Name=?",new String[]{name.getText().toString()});   //更新用户表数据
							db.setTransactionSuccessful();
							db.endTransaction();
							db.close();
							if(name.getText().toString().equals("admin"))   //如果是管理员
							{
								Intent intent = new Intent();
								intent.putExtra("Name", name.getText().toString());   //传递用户名								
								intent.putExtra("Role","1");   //角色号
							    intent.setClass(LogIn_Activity.this, Hourse_Activity.class); 
							    LogIn_Activity.this.startActivity(intent);
							    LogIn_Activity.this.finish();
							}
							else   //如果是用户
							{
								Intent intent = new Intent();
								intent.putExtra("Name", name.getText().toString());   //传递用户名
								intent.putExtra("RSAddr",rsaddr);   //传递开关485地址
								intent.putExtra("Role","2");   //角色号
							    intent.setClass(LogIn_Activity.this, Main_Activity.class); 
							    LogIn_Activity.this.startActivity(intent);
							    LogIn_Activity.this.finish();
							}
						}
						else
						{	
							db.setTransactionSuccessful();
							db.endTransaction();
							db.close();
							Toast.makeText(getApplicationContext(),"密码错误！",Toast.LENGTH_SHORT).show();
						}									
												
					}
					
				}
			}});
	}
}
