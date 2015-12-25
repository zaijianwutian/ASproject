package com.suntrans.beijing;

import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import com.suntrans.beijing.R;
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

import database.DbHelper;

public class Welcome_Activity extends Activity {
	private String clientip;    //开关的ip地址
	private DatagramSocket UDPclient;	   //UDP客户端
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
		setContentView(R.layout.welcome);      //设置activity布局文件
		//UDPInit();   //发送UDP命令，获取开关ip地址
		DbInit();       //初始化数据库

		new Handler().postDelayed(new Runnable(){

            public void run() {
                // TODO Auto-generated method stub
            	Intent intent=new Intent();
        		intent.putExtra("clientip",clientip);             //点击的区域
        		intent.setClass(Welcome_Activity.this, Main_Activity.class);//设置要跳转的activity
        		startActivity(intent);//开始跳转
        		finish();
            }
            
        }, 2000);   //延时2秒打开主页面
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
			            		                 
				                 while (UDPclient!=null) 
				                 {
				                	 // 接收服务器信息       定义输入流
					            	 byte data [] = new byte[1024];
					                 //创建一个空的DatagramPacket对象
					                 DatagramPacket packet = new DatagramPacket(data,data.length);
					                 //使用receive方法接收客户端所发送的数据
					                 UDPclient.receive(packet);
					                 clientip=packet.getAddress().toString().replace("/", "");	//ip地址
					                 String clientmac=new String(packet.getData()).replace("+OK=", "");  //MAC地址
					                 clientmac=clientmac.replaceAll("\r|\n", "");    //去掉换行符
					                 clientmac=clientmac.replace(" ", "");   //去掉空格
					                 clientmac=clientmac.substring(8,12);   //取出mac地址的最后四位	
					                 //将ip地址保存到文件中
					               //实例化SharedPreferences对象（第一步） 
					                 SharedPreferences mySharedPreferences= getSharedPreferences("data", Activity.MODE_PRIVATE); 
					                 //实例化SharedPreferences.Editor对象（第二步） 
					                 SharedPreferences.Editor editor = mySharedPreferences.edit(); 
					                 //用putString的方法保存数据 
					                 editor.putString("clientip", clientip); 
					                 //提交当前数据 
					                 editor.commit(); 
				                 }   				                
				                 
			                 } catch (Exception e) {
			                     e.printStackTrace();
			                   
			                 }
			             }			            
		      }
	private void DbInit()   //初始化数据库
	{
		DbHelper dh1 = new DbHelper(Welcome_Activity.this, "IBMS", null, 1);
		SQLiteDatabase db = dh1.getWritableDatabase();
		db.beginTransaction();
		//获取房间图标的图片
		Bitmap bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.room);

		bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.bulb);
		//获取图片输出流
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
		Cursor cursor = db.query(true, "switchs_tb", new String[]{"CID", "Name"}, null, null, null, null, null, null, null);
		if (cursor.getCount() < 1)  //如果开关表中没有数据，则添加
		{
			long row = 0;
			//ContentValues[] cv = new ContentValues[10];    //内容数组
			///外间开关

			//第一个通道
			bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.bulb1);
			os = new ByteArrayOutputStream();
			bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
			ContentValues cva = new ContentValues();
			cva.put("Room", "外间");
			cva.put("State", "0");
			cva.put("MainAddr", "0002");   //第六感官地址
			cva.put("RSAddr","01");		    //开关地址
			cva.put("Channel", 1);
			cva.put("Area", "主房间");
			cva.put("Name", "会议室");
			cva.put("VoiceName", "未配置");    //语音名称
			cva.put("Image", os.toByteArray());
			row = db.insert("switchs_tb", null, cva);  //将数据添加到数据库

			//第二个通道
			bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.bulb1);
			os = new ByteArrayOutputStream();
			bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
			cva = new ContentValues();
			cva.put("Room", "外间");
			cva.put("State", "0");
			cva.put("MainAddr", "0002");   //第六感官地址
			cva.put("RSAddr","01");		    //开关地址
			cva.put("Channel", 2);
			cva.put("Area", "主房间");
			cva.put("Name", "门灯");
			cva.put("VoiceName", "未配置");
			cva.put("Image", os.toByteArray());
			row = db.insert("switchs_tb", null, cva);  //将数据添加到数据库

			//第三个通道
			bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.bulb1);
			os = new ByteArrayOutputStream();
			bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
			cva = new ContentValues();
			cva.put("Room", "外间");
			cva.put("State", "0");
			cva.put("MainAddr", "0002");   //第六感官地址
			cva.put("RSAddr","01");		    //开关地址
			cva.put("Channel", 3);
			cva.put("Area", "主房间");
			cva.put("Name", "射灯");
			cva.put("VoiceName", "未配置");
			cva.put("Image", os.toByteArray());
			row = db.insert("switchs_tb", null, cva);  //将数据添加到数据库

			//第四个通道
			bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.bulb1);
			os = new ByteArrayOutputStream();
			bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
			cva = new ContentValues();
			cva.put("Room", "外间");
			cva.put("State", "0");
			cva.put("MainAddr", "0002");   //第六感官地址
			cva.put("RSAddr","01");		    //开关地址
			cva.put("Channel", 4);
			cva.put("Area", "卫生间");
			cva.put("Name", "筒灯");
			cva.put("VoiceName", "未配置");
			cva.put("Image", os.toByteArray());
			row = db.insert("switchs_tb", null, cva);  //将数据添加到数据库

			//第五个通道
			bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.bulb1);
			os = new ByteArrayOutputStream();
			bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
			cva = new ContentValues();
			cva.put("Room", "外间");
			cva.put("State", "0");
			cva.put("MainAddr", "0002");   //第六感官地址
			cva.put("RSAddr","01");		    //开关地址
			cva.put("Channel", 5);
			cva.put("Area", "厨房");
			cva.put("Name", "客厅");
			cva.put("VoiceName", "未配置");
			cva.put("Image", os.toByteArray());
			row = db.insert("switchs_tb", null, cva);  //将数据添加到数据库

			//第六个通道
			bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.bulb1);
			os = new ByteArrayOutputStream();
			bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
			cva = new ContentValues();
			cva.put("Room", "外间");
			cva.put("State", "0");
			cva.put("MainAddr", "0002");   //第六感官地址
			cva.put("RSAddr","01");		    //开关地址
			cva.put("Channel", 6);
			cva.put("Area", "主房间");
			cva.put("Name", "灯带");
			cva.put("VoiceName", "未配置");
			cva.put("Image", os.toByteArray());
			row = db.insert("switchs_tb", null, cva);  //将数据添加到数据库

			//第七个通道
			bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.bulb1);
			os = new ByteArrayOutputStream();
			bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
			cva = new ContentValues();
			cva.put("Room", "外间");
			cva.put("State", "0");
			cva.put("MainAddr", "0002");   //第六感官地址
			cva.put("RSAddr","01");		    //开关地址
			cva.put("Channel", 7);
			cva.put("Area", "主房间");
			cva.put("Name", "画面灯");
			cva.put("VoiceName", "未配置");
			cva.put("Image", os.toByteArray());
			row = db.insert("switchs_tb", null, cva);  //将数据添加到数据库

			//第八个通道
			bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.socket);
			os = new ByteArrayOutputStream();
			bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
			cva = new ContentValues();
			cva.put("Room", "外间");
			cva.put("State", "0");
			cva.put("MainAddr", "0002");   //第六感官地址
			cva.put("RSAddr","01");		    //开关地址
			cva.put("Channel", 8);
			cva.put("Area", "主房间");
			cva.put("Name", "无");
			cva.put("VoiceName", "未配置");
			cva.put("Image", os.toByteArray());
			row = db.insert("switchs_tb", null, cva);  //将数据添加到数据库

			//第九个通道
			bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.socket);
			os = new ByteArrayOutputStream();
			bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
			cva = new ContentValues();
			cva.put("Room", "外间");
			cva.put("State", "0");
			cva.put("MainAddr", "0002");   //第六感官地址
			cva.put("RSAddr","01");		    //开关地址
			cva.put("Channel", 9);
			cva.put("Area", "卫生间");
			cva.put("Name", "无");
			cva.put("VoiceName", "未配置");
			cva.put("Image", os.toByteArray());
			row = db.insert("switchs_tb", null, cva);  //将数据添加到数据库

			//第十个通道
			bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.socket);
			os = new ByteArrayOutputStream();
			bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
			cva = new ContentValues();
			cva.put("Room", "外间");
			cva.put("State", "0");
			cva.put("MainAddr", "0002");   //第六感官地址
			cva.put("RSAddr","01");		    //开关地址
			cva.put("Channel", 10);
			cva.put("Area", "卫生间");
			cva.put("Name", "无");
			cva.put("VoiceName", "未配置");
			cva.put("Image", os.toByteArray());
			row = db.insert("switchs_tb", null, cva);  //将数据添加到数据库

			///里间的开关
			//第一个通道
			bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.socket);
			os = new ByteArrayOutputStream();
			bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
			cva = new ContentValues();
			cva.put("Room", "里间");
			cva.put("State", "0");
			cva.put("MainAddr", "0003");   //第六感官地址
			cva.put("RSAddr","01");		    //开关地址
			cva.put("Channel", 1);
			cva.put("Area", "主房间");
			cva.put("Name", "无");
			cva.put("VoiceName", "未配置");
			cva.put("Image", os.toByteArray());
			row = db.insert("switchs_tb", null, cva);  //将数据添加到数据库

			//第二个通道
			bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.socket);
			os = new ByteArrayOutputStream();
			bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
			cva = new ContentValues();
			cva.put("Room", "里间");
			cva.put("State", "0");
			cva.put("MainAddr", "0003");   //第六感官地址
			cva.put("RSAddr","01");		    //开关地址
			cva.put("Channel", 2);
			cva.put("Area", "主房间");
			cva.put("Name", "无");
			cva.put("VoiceName", "未配置");
			cva.put("Image", os.toByteArray());
			row = db.insert("switchs_tb", null, cva);  //将数据添加到数据库

			//第三个通道
			bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.socket);
			os = new ByteArrayOutputStream();
			bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
			cva = new ContentValues();
			cva.put("Room", "里间");
			cva.put("State", "0");
			cva.put("MainAddr", "0003");   //第六感官地址
			cva.put("RSAddr","01");		    //开关地址
			cva.put("Channel", 3);
			cva.put("Area", "主房间");
			cva.put("Name", "无");
			cva.put("VoiceName", "未配置");
			cva.put("Image", os.toByteArray());
			row = db.insert("switchs_tb", null, cva);  //将数据添加到数据库

			//第四个通道
			bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.socket);
			os = new ByteArrayOutputStream();
			bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
			cva = new ContentValues();
			cva.put("Room", "里间");
			cva.put("State", "0");
			cva.put("MainAddr", "0003");   //第六感官地址
			cva.put("RSAddr","01");		    //开关地址
			cva.put("Channel", 4);
			cva.put("Area", "卫生间");
			cva.put("Name", "无");
			cva.put("VoiceName", "未配置");
			cva.put("Image", os.toByteArray());
			row = db.insert("switchs_tb", null, cva);  //将数据添加到数据库

			//第五个通道
			bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.socket);
			os = new ByteArrayOutputStream();
			bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
			cva = new ContentValues();
			cva.put("Room", "里间");
			cva.put("State", "0");
			cva.put("MainAddr", "0003");   //第六感官地址
			cva.put("RSAddr","01");		    //开关地址
			cva.put("Channel", 5);
			cva.put("Area", "厨房");
			cva.put("Name", "无");
			cva.put("VoiceName", "未配置");
			cva.put("Image", os.toByteArray());
			row = db.insert("switchs_tb", null, cva);  //将数据添加到数据库

			//第六个通道
			bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.bulb1);
			os = new ByteArrayOutputStream();
			bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
			cva = new ContentValues();
			cva.put("Room", "里间");
			cva.put("State", "0");
			cva.put("MainAddr", "0003");   //第六感官地址
			cva.put("RSAddr","01");		    //开关地址
			cva.put("Channel", 6);
			cva.put("Area", "主房间");
			cva.put("Name", "灯带");
			cva.put("VoiceName", "未配置");
			cva.put("Image", os.toByteArray());
			row = db.insert("switchs_tb", null, cva);  //将数据添加到数据库

			//第七个通道
			bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.bulb1);
			os = new ByteArrayOutputStream();
			bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
			cva = new ContentValues();
			cva.put("Room", "里间");
			cva.put("State", "0");
			cva.put("MainAddr", "0003");   //第六感官地址
			cva.put("RSAddr","01");		    //开关地址
			cva.put("Channel", 7);
			cva.put("Area", "主房间");
			cva.put("Name", "餐厅");
			cva.put("VoiceName", "未配置");
			cva.put("Image", os.toByteArray());
			row = db.insert("switchs_tb", null, cva);  //将数据添加到数据库

			//第八个通道
			bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.bulb1);
			os = new ByteArrayOutputStream();
			bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
			cva = new ContentValues();
			cva.put("Room", "里间");
			cva.put("State", "0");
			cva.put("MainAddr", "0003");   //第六感官地址
			cva.put("RSAddr","01");		    //开关地址
			cva.put("Channel", 8);
			cva.put("Area", "主房间");
			cva.put("Name", "射灯");
			cva.put("VoiceName", "未配置");
			cva.put("Image", os.toByteArray());
			row = db.insert("switchs_tb", null, cva);  //将数据添加到数据库

			//第九个通道
			bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.bulb1);
			os = new ByteArrayOutputStream();
			bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
			cva = new ContentValues();
			cva.put("Room", "里间");
			cva.put("State", "0");
			cva.put("MainAddr", "0003");   //第六感官地址
			cva.put("RSAddr","01");		    //开关地址
			cva.put("Channel", 9);
			cva.put("Area", "卫生间");
			cva.put("Name", "办公室右");
			cva.put("VoiceName", "未配置");
			cva.put("Image", os.toByteArray());
			row = db.insert("switchs_tb", null, cva);  //将数据添加到数据库

			//第十个通道
			bmp_room = BitmapFactory.decodeResource(Welcome_Activity.this.getResources(), R.drawable.bulb1);
			os = new ByteArrayOutputStream();
			bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
			cva = new ContentValues();
			cva.put("Room", "里间");
			cva.put("State", "0");
			cva.put("MainAddr", "0003");   //第六感官地址
			cva.put("RSAddr","01");		    //开关地址
			cva.put("Channel", 10);
			cva.put("Area", "卫生间");
			cva.put("Name", "办公室左");
			cva.put("VoiceName", "未配置");
			cva.put("Image", os.toByteArray());
			row = db.insert("switchs_tb", null, cva);  //将数据添加到数据库


		}
		db.setTransactionSuccessful();
		db.endTransaction();
		db.close();
	}
}
