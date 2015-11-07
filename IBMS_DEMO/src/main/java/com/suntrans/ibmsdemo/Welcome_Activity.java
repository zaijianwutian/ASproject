package com.suntrans.ibmsdemo;

import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import database.DbHelper;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;

public class Welcome_Activity extends Activity {
	private ImageView image;
	private DatagramSocket UDPclient;	
	private String[] newmac=null;    //需要添加的新开关的MAC地址数组
	private String[] newip=null;
	private String[] oldmac=null;   //数据库中已存开关的MAC地址数组	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.welcome);     //设置布局文件
		image=(ImageView)findViewById(R.id.image);      
		AnimationSet animationSet=new AnimationSet(true);
		//创建一个AlphaAnimation对象，参数从完全的透明度，到完全的不透明度
		AlphaAnimation alphaAnimation=new AlphaAnimation(0,1);
		//设置动画执行时间
		alphaAnimation.setDuration(1500);
		//将alphaAnimation对象添加到AnimationSet中
		animationSet.addAnimation(alphaAnimation);
		
		//创建一个ScaleAnimation对象
		ScaleAnimation scaleAnimation=new ScaleAnimation(0,1,0,1,Animation.RELATIVE_TO_SELF ,0.5f,Animation.RELATIVE_TO_SELF ,0.5f);
		//设置动画执行时间
		scaleAnimation.setDuration(1500);
		animationSet.addAnimation(scaleAnimation);
		//设置图片控件开始执行此动画Set
		image.startAnimation(animationSet);
		new Thread(){
			@Override
			public void run(){
			DbInit();   //初始化数据库数据
			}
		}.start();
		
		//UDPInit();  //UDP初始化
		new Handler().postDelayed(new Runnable(){

            public void run() {
                // TODO Auto-generated method stub
            	DbHelper dh1=new DbHelper(Welcome_Activity.this,"IBMS",null,1);
        		SQLiteDatabase db = dh1.getWritableDatabase(); 
        		Cursor cursor = db.query("users_tb", new String[]{"NID","Name","RSAddr"}, "IsUsing=? and Auto=?", new String[]{"1","1"}, null, null, null);
            	if(cursor.getCount()<1)
            	{
	        		Intent intent = new Intent();
	                intent.setClass(Welcome_Activity.this, LogIn_Activity.class); 
	                Welcome_Activity.this.startActivity(intent);
	                Welcome_Activity.this.finish();
	                db.close();
            	}
            	else
            	{
            		String name="";
            		String rsaddr="";
            		while(cursor.moveToNext())
            		{
            			name=cursor.getString(1);
            			rsaddr=cursor.getString(2);
            		
            		}
            		db.close();
            		if(name.equals("admin"))
            		{
            			Intent intent = new Intent();            		
	            		intent.putExtra("Name", name);
	            		intent.putExtra("RSAddr", rsaddr);
	            		intent.putExtra("Role", "1");   //角色号            		
	            		intent.setClass(Welcome_Activity.this, Hourse_Activity.class); 
		                Welcome_Activity.this.startActivity(intent);
		                Welcome_Activity.this.finish();
            		}
            		else
            		{
	            		Intent intent = new Intent();            		
	            		intent.putExtra("Name", name);
	            		intent.putExtra("RSAddr", rsaddr);
	            		intent.putExtra("Role", "2");   //角色号            		
	            		intent.setClass(Welcome_Activity.this, Main_Activity.class); 
		                Welcome_Activity.this.startActivity(intent);
		                Welcome_Activity.this.finish();
            		}
            	}
                
            }
            
        }, 3000);   //延时3秒打开主页面
	}
	
	 //初始化数据库中的内容
	 public void DbInit(){
		 
	     //将服务器ip地址和端口保存到文件中
        //实例化SharedPreferences对象（第一步） 
        SharedPreferences sharedPreferences= getSharedPreferences("data", Activity.MODE_PRIVATE);       
        String serverip =sharedPreferences.getString("serverip", "-1");   //读取服务器ip，若没有则是-1
        if(serverip.equals("-1"))   //如果没有保存服务器ip，则写入服务器ip和端口
        {
	        //实例化SharedPreferences.Editor对象（第二步） 
	        SharedPreferences.Editor editor = sharedPreferences.edit(); 
	        //用putString的方法保存数据 
	        editor.putString("serverip", "61.235.65.160");    //服务器IP
	        editor.putString("port", "8028");    //端口
	        //提交当前数据 
	        editor.commit(); 
        }
		 
		DbHelper dh1=new DbHelper(Welcome_Activity.this,"IBMS",null,1);
		SQLiteDatabase db = dh1.getWritableDatabase();
		db.beginTransaction();
		Cursor cursor = db.query(true, "room_tb", new String[]{"RID","Name"}, null, null, null, null, null, null, null);
		//获取房间图标的图片
		Bitmap  bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.room);   
		//获取图片输出流
		ByteArrayOutputStream os = new ByteArrayOutputStream();  
		bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
		if(cursor.getCount()<1)   //如果房间表没有数据，则添加
		{
			 long row=0;
			 String[] room=new String[]{"主房间","厨房","卫生间"};
			 ContentValues[] cv = new ContentValues[13];    //内容数组	
			 for(int i=0;i<3;i++)
				{
					cv[i] = new ContentValues();
					cv[i].put("Name",room[i]);
					cv[i].put("Image",os.toByteArray());
					row = db.insert("room_tb", null, cv[i]);  //将数据添加到数据库
				}
		}
		
		cursor = db.query(true, "users_tb", new String[]{"NID","Name"}, null, null, null, null, null, null, null);
		if(cursor.getCount()<1)   //如果用户表没有数据，则添加一组默认用户名
		{
			 long row=0;
			 
			 ContentValues cv = new ContentValues();    //内容数组	
			 cv.put("Name","admin");   //用户名			
		     cv.put("Password","password");	//密码
		     cv.put("RSAddr","9999");   //开关485地址
			 cv.put("IsUsing","0");   //是否正在使用
			 cv.put("Auto","0");   //是否自动登录
			 cv.put("Remember","0");//是否记住密码
			 row = db.insert("users_tb", null, cv);  //将数据添加到数据库
			 
			 cv = new ContentValues();    //内容数组	
			 cv.put("Name","6101");   //用户名			
		     cv.put("Password","qpf9jx");	//密码
		     cv.put("RSAddr","0001");   //开关485地址
			 cv.put("IsUsing","0");   //是否正在使用
			 cv.put("Auto","0");   //是否自动登录
			 cv.put("Remember","0");//是否记住密码
			 row = db.insert("users_tb", null, cv);  //将数据添加到数据库
			 
			 cv = new ContentValues();    //内容数组	
			 cv.put("Name","6102");   //用户名			
		     cv.put("Password","kniif2");	//密码
		     cv.put("RSAddr","0002");   //开关485地址
			 cv.put("IsUsing","0");   //是否正在使用
			 cv.put("Auto","0");   //是否自动登录
			 cv.put("Remember","0");//是否记住密码
			 row = db.insert("users_tb", null, cv);  //将数据添加到数据库
			 
			 cv = new ContentValues();    //内容数组	
			 cv.put("Name","6103");   //用户名			
		     cv.put("Password","kv9bxb");	//密码
		     cv.put("RSAddr","0003");   //开关485地址
			 cv.put("IsUsing","0");   //是否正在使用
			 cv.put("Auto","0");   //是否自动登录
			 cv.put("Remember","0");//是否记住密码
			 row = db.insert("users_tb", null, cv);  //将数据添加到数据库
			 
			 cv = new ContentValues();    //内容数组	
			 cv.put("Name","6105");   //用户名			
		     cv.put("Password","dh49rb");	//密码
		     cv.put("RSAddr","0004");   //开关485地址
			 cv.put("IsUsing","0");   //是否正在使用
			 cv.put("Auto","0");   //是否自动登录
			 cv.put("Remember","0");//是否记住密码
			 row = db.insert("users_tb", null, cv);  //将数据添加到数据库
			 
			 cv = new ContentValues();    //内容数组	
			 cv.put("Name","6106");   //用户名			
		     cv.put("Password","oku06w");	//密码
		     cv.put("RSAddr","0005");   //开关485地址
			 cv.put("IsUsing","0");   //是否正在使用
			 cv.put("Auto","0");   //是否自动登录
			 cv.put("Remember","0");//是否记住密码
			 row = db.insert("users_tb", null, cv);  //将数据添加到数据库
			 
			 cv = new ContentValues();    //内容数组	
			 cv.put("Name","6201");   //用户名			
		     cv.put("Password","hb7b2k");	//密码
		     cv.put("RSAddr","0006");   //开关485地址
			 cv.put("IsUsing","0");   //是否正在使用
			 cv.put("Auto","0");   //是否自动登录
			 cv.put("Remember","0");//是否记住密码
			 row = db.insert("users_tb", null, cv);  //将数据添加到数据库
			 
			 cv = new ContentValues();    //内容数组	
			 cv.put("Name","6202");   //用户名			
		     cv.put("Password","vh2rxz");	//密码
		     cv.put("RSAddr","0007");   //开关485地址
			 cv.put("IsUsing","0");   //是否正在使用
			 cv.put("Auto","0");   //是否自动登录
			 cv.put("Remember","0");//是否记住密码
			 row = db.insert("users_tb", null, cv);  //将数据添加到数据库
			 
			 cv = new ContentValues();    //内容数组	
			 cv.put("Name","6203");   //用户名			
		     cv.put("Password","9nrf7f");	//密码
		     cv.put("RSAddr","0008");   //开关485地址
			 cv.put("IsUsing","0");   //是否正在使用
			 cv.put("Auto","0");   //是否自动登录
			 cv.put("Remember","0");//是否记住密码
			 row = db.insert("users_tb", null, cv);  //将数据添加到数据库
			 
			 cv = new ContentValues();    //内容数组	
			 cv.put("Name","6205");   //用户名			
		     cv.put("Password","3fkere");	//密码
		     cv.put("RSAddr","0009");   //开关485地址
			 cv.put("IsUsing","0");   //是否正在使用
			 cv.put("Auto","0");   //是否自动登录
			 cv.put("Remember","0");//是否记住密码
			 row = db.insert("users_tb", null, cv);  //将数据添加到数据库
			 
			 cv = new ContentValues();    //内容数组	
			 cv.put("Name","6206");   //用户名			
		     cv.put("Password","53yu8h");	//密码
		     cv.put("RSAddr","000a");   //开关485地址
			 cv.put("IsUsing","0");   //是否正在使用
			 cv.put("Auto","0");   //是否自动登录
			 cv.put("Remember","0");//是否记住密码
			 row = db.insert("users_tb", null, cv);  //将数据添加到数据库
			 
			 cv = new ContentValues();    //内容数组	
			 cv.put("Name","6301");   //用户名			
		     cv.put("Password","mn62ny");	//密码
		     cv.put("RSAddr","000b");   //开关485地址
			 cv.put("IsUsing","0");   //是否正在使用
			 cv.put("Auto","0");   //是否自动登录
			 cv.put("Remember","0");//是否记住密码
			 row = db.insert("users_tb", null, cv);  //将数据添加到数据库
			 
			 cv = new ContentValues();    //内容数组	
			 cv.put("Name","6302");   //用户名			
		     cv.put("Password","i2064w");	//密码
		     cv.put("RSAddr","000c");   //开关485地址
			 cv.put("IsUsing","0");   //是否正在使用
			 cv.put("Auto","0");   //是否自动登录
			 cv.put("Remember","0");//是否记住密码
			 row = db.insert("users_tb", null, cv);  //将数据添加到数据库
			 
			 cv = new ContentValues();    //内容数组	
			 cv.put("Name","6303");   //用户名			
		     cv.put("Password","mf3jae");	//密码
		     cv.put("RSAddr","000d");   //开关485地址
			 cv.put("IsUsing","0");   //是否正在使用
			 cv.put("Auto","0");   //是否自动登录
			 cv.put("Remember","0");//是否记住密码
			 row = db.insert("users_tb", null, cv);  //将数据添加到数据库
			 
			 cv = new ContentValues();    //内容数组	
			 cv.put("Name","6305");   //用户名			
		     cv.put("Password","hv43t6");	//密码
		     cv.put("RSAddr","000e");   //开关485地址
			 cv.put("IsUsing","0");   //是否正在使用
			 cv.put("Auto","0");   //是否自动登录
			 cv.put("Remember","0");//是否记住密码
			 row = db.insert("users_tb", null, cv);  //将数据添加到数据库
			 
			 cv = new ContentValues();    //内容数组	
			 cv.put("Name","6306");   //用户名			
		     cv.put("Password","txmf6w");	//密码
		     cv.put("RSAddr","000f");   //开关485地址
			 cv.put("IsUsing","0");   //是否正在使用
			 cv.put("Auto","0");   //是否自动登录
			 cv.put("Remember","0");//是否记住密码
			 row = db.insert("users_tb", null, cv);  //将数据添加到数据库
			 
			 cv = new ContentValues();    //内容数组	
			 cv.put("Name","6401");   //用户名			
		     cv.put("Password","569ygw");	//密码
		     cv.put("RSAddr","0010");   //开关485地址
			 cv.put("IsUsing","0");   //是否正在使用
			 cv.put("Auto","0");   //是否自动登录
			 cv.put("Remember","0");//是否记住密码
			 row = db.insert("users_tb", null, cv);  //将数据添加到数据库
			 
			 cv = new ContentValues();    //内容数组	
			 cv.put("Name","6402");   //用户名			
		     cv.put("Password","p3pzer");	//密码
		     cv.put("RSAddr","0011");   //开关485地址
			 cv.put("IsUsing","0");   //是否正在使用
			 cv.put("Auto","0");   //是否自动登录
			 cv.put("Remember","0");//是否记住密码
			 row = db.insert("users_tb", null, cv);  //将数据添加到数据库
			 
			 cv = new ContentValues();    //内容数组	
			 cv.put("Name","6403");   //用户名			
		     cv.put("Password","stq04k");	//密码
		     cv.put("RSAddr","0012");   //开关485地址
			 cv.put("IsUsing","0");   //是否正在使用
			 cv.put("Auto","0");   //是否自动登录
			 cv.put("Remember","0");//是否记住密码
			 row = db.insert("users_tb", null, cv);  //将数据添加到数据库
			 
			 cv = new ContentValues();    //内容数组	
			 cv.put("Name","6405");   //用户名			
		     cv.put("Password","c682yo");	//密码
		     cv.put("RSAddr","0013");   //开关485地址
			 cv.put("IsUsing","0");   //是否正在使用
			 cv.put("Auto","0");   //是否自动登录
			 cv.put("Remember","0");//是否记住密码
			 row = db.insert("users_tb", null, cv);  //将数据添加到数据库
			 
			 cv = new ContentValues();    //内容数组	
			 cv.put("Name","6406");   //用户名			
		     cv.put("Password","etbsim");	//密码
		     cv.put("RSAddr","0014");   //开关485地址
			 cv.put("IsUsing","0");   //是否正在使用
			 cv.put("Auto","0");   //是否自动登录
			 cv.put("Remember","0");//是否记住密码
			 row = db.insert("users_tb", null, cv);  //将数据添加到数据库
			 
			 cv = new ContentValues();    //内容数组	
			 cv.put("Name","1111");   //用户名			
		     cv.put("Password","123456");	//密码
		     cv.put("RSAddr","0015");   //开关485地址
			 cv.put("IsUsing","0");   //是否正在使用
			 cv.put("Auto","0");   //是否自动登录
			 cv.put("Remember","0");//是否记住密码
			 row = db.insert("users_tb", null, cv);  //将数据添加到数据库	
			
		}
		
		bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.switchoff);   
		//获取图片输出流
		os = new ByteArrayOutputStream();  
		bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
		cursor = db.query(true, "switchs_tb", new String[]{"CID","Name"}, null, null, null, null, null, null, null);
		if(cursor.getCount()<1)  //如果开关表中没有数据，则添加
		{
			long row=0;
			ContentValues[] cv = new ContentValues[10];    //内容数组
			//第一个开关
			//String[] area=new String[]{"主房间","主房间","主房间","卫生间","厨房","主房间","主房间","客房","卫生间","卫生间"};
			//String[] name=new String[]{"门前灯","主灯","书桌灯","镜前灯","顶灯","二层顶灯","空调","插座","插座","换气扇"};
			
			//第一个通道
			bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.bulb1); 
			os = new ByteArrayOutputStream();  
			bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
			ContentValues cva = new ContentValues();
			cva.put("IPAddr","192.168.1.1");
			cva.put("State","false");
			cva.put("Type","无");
			cva.put("MACAddr","9999");			
			cva.put("IValue","0");   //电流
			cva.put("Channel",1);
			cva.put("Area","主房间");
			cva.put("Name","门前灯");
			cva.put("Image", os.toByteArray());
			row = db.insert("switchs_tb", null, cva);  //将数据添加到数据库
			
			//第二个通道
			bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.bulb1); 
			os = new ByteArrayOutputStream();  
			bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
			cva = new ContentValues();
			cva.put("IPAddr","192.168.1.1");
			cva.put("State","false");
			cva.put("Type","无");
			cva.put("MACAddr","9999");
			cva.put("IValue","0");
			cva.put("Channel",2);
			cva.put("Area","主房间");
			cva.put("Name","主灯");
			cva.put("Image", os.toByteArray());
			row = db.insert("switchs_tb", null, cva);  //将数据添加到数据库
			
			//第三个通道
			bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.bulb);  
			os = new ByteArrayOutputStream();  
			bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
			cva = new ContentValues();
			cva.put("IPAddr","192.168.1.1");
			cva.put("State","false");
			cva.put("Type","无");
			cva.put("MACAddr","9999");
			cva.put("IValue","0");
			cva.put("Channel",3);
			cva.put("Area","主房间");
			cva.put("Name","书桌灯");
			cva.put("Image", os.toByteArray());
			row = db.insert("switchs_tb", null, cva);  //将数据添加到数据库
			
			//第四个通道
			bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.bulb1); 
			os = new ByteArrayOutputStream();  
			bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
			cva = new ContentValues();
			cva.put("IPAddr","192.168.1.1");
			cva.put("State","false");
			cva.put("Type","无");
			cva.put("MACAddr","9999");
			cva.put("IValue","0");
			cva.put("Channel",4);
			cva.put("Area","卫生间");
			cva.put("Name","镜前灯");
			cva.put("Image", os.toByteArray());
			row = db.insert("switchs_tb", null, cva);  //将数据添加到数据库
			
			//第五个通道
			bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.bulb); 
			os = new ByteArrayOutputStream();  
			bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
			cva = new ContentValues();
			cva.put("IPAddr","192.168.1.1");
			cva.put("State","false");
			cva.put("Type","无");
			cva.put("MACAddr","9999");
			cva.put("IValue","0");
			cva.put("Channel",5);
			cva.put("Area","厨房");
			cva.put("Name","顶灯");
			cva.put("Image", os.toByteArray());
			row = db.insert("switchs_tb", null, cva);  //将数据添加到数据库
			
			//第六个通道
			bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.bulb1); 
			os = new ByteArrayOutputStream();  
			bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
			cva = new ContentValues();
			cva.put("IPAddr","192.168.1.1");
			cva.put("State","false");
			cva.put("Type","无");
			cva.put("MACAddr","9999");
			cva.put("IValue","0");
			cva.put("Channel",6);
			cva.put("Area","主房间");
			cva.put("Name","二层顶灯");
			cva.put("Image", os.toByteArray());
			row = db.insert("switchs_tb", null, cva);  //将数据添加到数据库
			
			//第七个通道
			bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.airconditioner);   
			os = new ByteArrayOutputStream();  
			bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
			cva = new ContentValues();
			cva.put("IPAddr","192.168.1.1");
			cva.put("State","false");
			cva.put("Type","无");
			cva.put("MACAddr","9999");
			cva.put("IValue","0");
			cva.put("Channel",7);
			cva.put("Area","主房间");
			cva.put("Name","空调");
			cva.put("Image", os.toByteArray());
			row = db.insert("switchs_tb", null, cva);  //将数据添加到数据库
			
			//第八个通道
			bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.television);
			os = new ByteArrayOutputStream();  
			bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
			cva = new ContentValues();
			cva.put("IPAddr","192.168.1.1");
			cva.put("State","false");
			cva.put("Type","无");
			cva.put("MACAddr","9999");
			cva.put("IValue","0");
			cva.put("Channel",8);
			cva.put("Area","主房间");
			cva.put("Name","电视机");
			cva.put("Image", os.toByteArray());
			row = db.insert("switchs_tb", null, cva);  //将数据添加到数据库
			
			//第九个通道
			bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.heater); 
			os = new ByteArrayOutputStream();  
			bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
			cva = new ContentValues();
			cva.put("IPAddr","192.168.1.1");
			cva.put("State","false");
			cva.put("Type","无");
			cva.put("MACAddr","9999");
			cva.put("IValue","0");
			cva.put("Channel",9);
			cva.put("Area","卫生间");
			cva.put("Name","热水器");
			cva.put("Image", os.toByteArray());
			row = db.insert("switchs_tb", null, cva);  //将数据添加到数据库
			
			//第十个通道
			bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.fan); 
			os = new ByteArrayOutputStream();  
			bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
			cva = new ContentValues();
			cva.put("IPAddr","192.168.1.1");
			cva.put("State","false");
			cva.put("Type","无");
			cva.put("MACAddr","9999");
			cva.put("Channel",10);
			cva.put("IValue","0");
			cva.put("Area","卫生间");
			cva.put("Name","换气扇");
			cva.put("Image", os.toByteArray());
			row = db.insert("switchs_tb", null, cva);  //将数据添加到数据库
			/*
			//第二个开关
			area=new String[]{"娱乐室","门厅","过道","客卫","客卫","客卫","客房","娱乐室","无","无"};
			name=new String[]{"顶灯","筒灯","筒灯","干区顶灯","干区镜前灯","浴霸","空调","通用插座","无","无"};
			for(int i=0;i<10;i++)
			{
				cv[i] = new ContentValues();
				cv[i].put("IPAddr","192.168.1.12");
				cv[i].put("State","false");
				cv[i].put("Type","无");
				cv[i].put("MACAddr","56d3");
				cv[i].put("Channel",i+1);
				cv[i].put("Area",area[i]);
				cv[i].put("Name",name[i]);
				cv[i].put("Image", os.toByteArray());
				row = db.insert("switchs_tb", null, cv[i]);  //将数据添加到数据库
			} 
			//第三个开关
			area=new String[]{"客厅","客厅","客厅","客厅阳台","观景区","客厅","客厅","客厅","无","无"};
			name=new String[]{"顶灯","圈灯","边灯","顶灯","圈灯","电视机","空调柜机","空调柜机","无","无"};
			for(int i=0;i<10;i++)
			{
				cv[i] = new ContentValues();
				cv[i].put("IPAddr","192.168.1.12");
				cv[i].put("State","false");
				cv[i].put("Type","无");
				cv[i].put("MACAddr","57de");
				cv[i].put("Channel",i+1);
				cv[i].put("Area",area[i]);
				cv[i].put("Name",name[i]);
				cv[i].put("Image", os.toByteArray());
				row = db.insert("switchs_tb", null, cv[i]);  //将数据添加到数据库
			} 
			//第四个开关
			area=new String[]{"女儿房","女儿房","女儿房","女儿房","次卧室","女儿房","次卧室","女儿房","次卧室","无"};
			name=new String[]{"顶灯","筒灯","壁灯左","壁灯右","顶灯","通用插座","通用插座","空调","空调","无"};
			for(int i=0;i<10;i++)
			{
				cv[i] = new ContentValues();
				cv[i].put("IPAddr","192.168.1.12");
				cv[i].put("State","false");
				cv[i].put("Type","无");
				cv[i].put("MACAddr","56f4");
				cv[i].put("Channel",i+1);
				cv[i].put("Area",area[i]);
				cv[i].put("Name",name[i]);
				cv[i].put("Image", os.toByteArray());
				row = db.insert("switchs_tb", null, cv[i]);  //将数据添加到数据库
			} 
			//第五个开关
			area=new String[]{"主卧室","主卧室","主卧室","主卧室","主卫生间","主卫生间","主卧室","主卧室","主卧室","无"};
			name=new String[]{"顶灯","壁灯左","壁灯右","圈灯","镜前灯","浴霸","电视机","通用插座","空调","无"};
			for(int i=0;i<10;i++)
			{
				cv[i] = new ContentValues();
				cv[i].put("IPAddr","192.168.1.12");
				cv[i].put("State","false");
				cv[i].put("Type","无");
				cv[i].put("MACAddr","57e1");
				cv[i].put("Channel",i+1);
				cv[i].put("Area",area[i]);
				cv[i].put("Name",name[i]);
				cv[i].put("Image", os.toByteArray());
				row = db.insert("switchs_tb", null, cv[i]);  //将数据添加到数据库
			} */
		}
		
		bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.ic_scene);   
		//获取图片输出流
		os = new ByteArrayOutputStream();  
		bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
		cursor = db.query(true, "scene_tb", new String[]{"SID","Name"}, null, null, null, null, null, null, null);
		if(cursor.getCount()<1)  //如果场景命令表中没有数据，则添加
		{
			long row=0;
			ContentValues[] cv = new ContentValues[6];    //内容数组
			for(int i=0;i<=5;i++)
			{
				cv[i] = new ContentValues();
				cv[i].put("Name","休息");        //场景名	
				cv[i].put("Operation","0");          //操作：关
				cv[i].put("Delay","0");              //延时：0s
				cv[i].put("CID",String.valueOf(1+i));               //开关ID：1-10
			//	cv[i].put("Image",os.toByteArray()); //场景图标
				row = db.insert("scene_tb", null, cv[i]);  //将数据添加到数据库
			}
			
			for(int i=0;i<=5;i++)
			{
				cv[i] = new ContentValues();
				cv[i].put("Name","明亮");        //场景名	
				cv[i].put("Operation","1");          //操作：关
				cv[i].put("Delay","0");              //延时：0s
				cv[i].put("CID",String.valueOf(1+i));               //开关ID：1-10
			//	cv[i].put("Image",os.toByteArray()); //场景图标
				row = db.insert("scene_tb", null, cv[i]);  //将数据添加到数据库
			}
			
		}
		cursor = db.query(true, "scenename_tb", new String[]{"SID","Name"}, null, null, null, null, null, null, null);
		if(cursor.getCount()<1)  //如果场景名称表中没有数据，则添加
		{
			long row=-1;
			ContentValues cv1=new ContentValues();
			cv1.put("Name", "休息");
			cv1.put("Image", os.toByteArray());
			row=db.insert("scenename_tb", null, cv1);
			
			cv1=new ContentValues();
			cv1.put("Name", "明亮");
			cv1.put("Image", os.toByteArray());
			row=db.insert("scenename_tb", null, cv1);
		}
		db.setTransactionSuccessful();       //设置事务处理成功，不设置会自动回滚不提交
		db.endTransaction();
		db.close();
	 } 
	 
	 public void UDPInit()   //UDP初始化
		{
			new Thread(){
				 public void run(){
					 try {
				 
					      //首先创建一个DatagramSocket对象
					      UDPclient = new DatagramSocket();
					      //创建一个InetAddree
					      InetAddress serverAddress = InetAddress.getByName("255.255.255.255");
					      String str = "123456AT+QMAC";
					      byte data [] = str.getBytes();
					      //创建一个DatagramPacket对象，并指定要讲这个数据包发送到网络当中的哪个地址，以及端口号
					      DatagramPacket packet = new DatagramPacket(data,data.length,serverAddress,988);
					      //调用socket对象的send方法，发送数据
					      UDPclient.send(packet);
					      new UDPServerThread().start();	  //创建新的线程监听UDP客户端返回的数据				    
					 	} catch (Exception e) 
					 		{
					 		    e.printStackTrace();
					 		}
				 	}
				 	}.start();
		}
		
		public class UDPServerThread extends Thread    //新建线程接收UDP回应
		{  
		  
			 public UDPServerThread() 
			    {  	
			    }
			    public void run() 
			    {  
			    	//tvRecv.setText("start");
			            byte[] buffer = new byte[1024];  
						final StringBuilder sb = new StringBuilder();
			             try {
			            		                 
				                 while (true) 
				                 {
				                	 // 接收服务器信息       定义输入流
					            	 byte data [] = new byte[1024];
					                 //创建一个空的DatagramPacket对象
					                 DatagramPacket packet = new DatagramPacket(data,data.length);
					                 //使用receive方法接收客户端所发送的数据
					                 UDPclient.receive(packet);
					                 String clientip=packet.getAddress().toString().replace("/", "");	//ip地址
					                 String clientmac=new String(packet.getData()).replace("+OK=", "");  //MAC地址
					                 clientmac=clientmac.replaceAll("\r|\n", "");    //去掉换行符
					                 clientmac=clientmac.replace(" ", "");   //去掉空格
					                 clientmac=clientmac.substring(8,12);
					                 //Toast.makeText(getApplicationContext(),"接收到的是"+clientmac+"!",Toast.LENGTH_SHORT).show();
					                 DbHelper dh1=new DbHelper(Welcome_Activity.this,"IBMS",null,1);
					     			 SQLiteDatabase db = dh1.getWritableDatabase(); 
					     			/*Cursor cursor = db.query("switchs_tb", new String[]{"MACAddr"}, "MACAddr=?", new String[]{clientmac}, null, null, null);
					     			//String str=String.valueOf(cursor.getCount())+"行数据\n";
					     			if(cursor.getCount()<10)     //如果数据库中不存在这个开关，则向新开关数组中添加开关
					     			{
					     				   
					     				if(newmac==null)   //如果还是空的 ，就添加第一个开关
					     				{
					     					newmac=new String[1];
					     					newip=new String[1];
					     					newmac[0]=clientmac;
					     					newip[0]=clientip;
					     				}
					     				else            //如果不是空的，证明数组中有数据，再加一条即可
					     				{
					     					String[] newmac1=newmac.clone();  //一个中间变量用来存储
					     					String[] newip1=newip.clone();    //中间变量
					     					newmac=new String[newmac.length+1];
					     					newip=new String[newip.length+1];   //将两个数据组的长度分别加1
					     					for(int j=0;j<newmac1.length;j++)
					     					{
					     						newmac[j]=newmac1[j];
					     						newip[j]=newip1[j];
					     					}
					     					newmac[newmac.length-1]=clientmac;
					     					newip[newip.length-1]=clientip;
					     				}
					    			  }*/
					     			//else   //如果数据库中已经存在，则根据MACAddr地址修改开关的IPAddr
					     			//{
					     			 //直接将数据库中的开关信息改成现在的mac地址和ip地址
					     			ContentValues cv=new ContentValues();
					     			cv.put("MACAddr", clientmac);
					     			cv.put("IPAddr", clientip);
									db.update("switchs_tb", cv, "", null);   //更新数据库中对应MAC地址的ip地址
					     			//}
					     			db.close();
				                 }    
			                 } catch (Exception e) {
			                     e.printStackTrace();
			                 }
			             }
		            
		      }
		
}
