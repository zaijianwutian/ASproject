package com.suntrans.demo6;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

public class Welcome_Activity extends Activity {
	private String clientip;    //开关的ip地址
	private DatagramSocket UDPclient;	   //UDP客户端
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
		setContentView(R.layout.welcome);      //设置activity布局文件
		UDPInit();   //发送UDP命令，获取开关ip地址
		
		new Handler().postDelayed(new Runnable(){

            public void run() {
                // TODO Auto-generated method stub
            	Intent intent=new Intent();
        		intent.putExtra("clientip",clientip);             //点击的区域
        		intent.setClass(Welcome_Activity.this, Main_Activity.class);//设置要跳转的activity
        		startActivity(intent);//开始跳转
        		finish();
            }
            
        }, 2500);   //延时2.5秒打开主页面
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
}
