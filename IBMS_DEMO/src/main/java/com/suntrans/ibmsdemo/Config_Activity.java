package com.suntrans.ibmsdemo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import views.TouchListener;

import convert.Converts;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class Config_Activity extends Activity {
	private String ipaddr;
	private Socket client;
	private EditText maxi,ov,uv,irate,urate;
	private Button bt;
	private int UV,OV,URate,IRate,MaxI;
	private int IsFinish=0;
	private Handler handler1=new Handler()   //接收到TCP发回的数据后调用，用于更新列表中的开关状态
	{
		 public void handleMessage(Message msg) 
		 {
	           super.handleMessage(msg);
	           Map<String,Object> map=(Map<String,Object>)msg.obj;
	           byte[] a=(byte[])(map.get("data"));    //byte数组a即为客户端发回的数据，aa68 0006是单个开通道，aa68 0003是所有的通道
	           String s="";		               //保存命令的十六进制字符串
	           for(int i=0;i<msg.what;i++)  //将数组转化成字符串
	           {   
	        	   String s1=Integer.toHexString((a[i] +256)%256);   //转换成十六进制字符串   
	        	   if(s1.length()==1)
	        		   s1="0"+s1;          		   
	        	   s=s+s1;     	  
	           }
	           Log.i("Order","收回:"+s);
	           String crc=Converts.GetCRC(a, 2, msg.what-2);    //获取返回数据的校验码
	           s=s.replace(" ", ""); //去掉空格
	           if(s.length()==42)   //返回的是配置参数信息，则刷新显示
    		   {
    			  MaxI=(int) (((a[5]&0xff)*256+a[6]&0xff));
    			  UV=(int)(((a[7]&0xff)*256+a[8]&0xff));    			  
    			  OV=(int)(((a[9]&0xff)*256+a[10]&0xff));    			  
    			  URate=(int)(((a[15]&0xff)*256)+(a[16]&0xff));
    			  IRate=(int)(((a[17]&0xff)*256)+(a[18]&0xff));
    			  maxi.setText(MaxI+"");
    			  uv.setText(UV+"");
    			  ov.setText(OV+"");
    			  urate.setText(URate+"");
    			  irate.setText(IRate+"");    			  
    		   }	
	          else if(s.length()==20)    //返回的是配置成功的信息，则提示配置成功
	           {
	        	  IsFinish++;
	        	  if(IsFinish>=1)
	        	  {
	        		  bt.setClickable(true);
	        		  Toast.makeText(getApplicationContext(),"配置成功！", Toast.LENGTH_SHORT).show();
	        	  }
	           }
        		   
         }	  
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);				
		ActionBar actionBar = this.getActionBar();   //设置actionbar标题栏		
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setDisplayShowCustomEnabled(true);
		actionBar.setDisplayShowHomeEnabled(true);
		Drawable d = this.getResources().getDrawable(R.color.burlywood); 
		actionBar.setBackgroundDrawable(d);   //设置actionbar背景
		actionBar.show();
		setContentView(R.layout.config);     //设置布局文件
		maxi=(EditText)findViewById(R.id.maxi);
		ov=(EditText)findViewById(R.id.ov);
		uv=(EditText)findViewById(R.id.uv);
	
		urate=(EditText)findViewById(R.id.urate);
		irate=(EditText)findViewById(R.id.irate);
		bt=(Button)findViewById(R.id.bt);
		Intent intent=getIntent();
		ipaddr=intent.getStringExtra("IPAddr");
		bt.setOnTouchListener(new TouchListener());
		bt.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				bt.setClickable(false);
				// TODO Auto-generated method stub
				new Thread(){
					public void run(){ 
					try 
					{						
						DataOutputStream out=new DataOutputStream(client.getOutputStream());  
					//String toServer = "aa68 0010 0201 0007 0e 0060 0000 00fa 0001 0001 0199 0199";    //修改
			          //  toServer.replace(" ","");    //去掉空格           9    11   13   15   17   19   21			    		            
			            IsFinish=0;
			            byte[] data=new byte[14];
			            data[0]=(byte)(Integer.valueOf(maxi.getText().toString())/256);
			            data[1]=(byte)((Integer.valueOf(maxi.getText().toString()))%256);   //最大电流
			            data[2]=(byte)(Integer.valueOf(uv.getText().toString())/256);
			            data[3]=(byte)((Integer.valueOf(uv.getText().toString()))%256);   //欠压
			            data[4]=(byte)(Integer.valueOf(ov.getText().toString())/256);
			            data[5]=(byte)((Integer.valueOf(ov.getText().toString()))%256);   //过压
			            data[6]=0x00;
			            data[7]=0x00;
			            data[8]=0x00;
			            data[9]=0x00;
			            data[10]=(byte)(Integer.valueOf(urate.getText().toString())/256);    //电压系数高八位
			            data[11]=(byte)((Integer.valueOf(urate.getText().toString()))%256);   //电压系数低八位
			            data[12]=(byte)(Integer.valueOf(irate.getText().toString())/256);   //电流系数高八位
			            data[13]=(byte)((Integer.valueOf(irate.getText().toString()))%256);   //电流系数低八位
			            byte[] bt=null;//+
			            String toServer="aa68 0010 0201 0007 0e"+Converts.Bytes2HexString(data);
			            toServer.replace(" ","");    //去掉空格      
			            bt=Converts.HexString2Bytes(toServer);
			            String str=toServer+Converts.GetCRC(bt, 2, bt.length);   //添加校验码			            
			            byte[] bt1=Converts.HexString2Bytes(str);      //将完整的命令转换成十六进制
			            Log.i("Order", Converts.Bytes2HexString(bt1));   //打印命令内容
			            if(client!=null)    //如果数组中存在此socket，则检测是否关闭
						 {
							if(!client.isOutputShutdown()&&!client.isClosed()&&client.isConnected())     //如果输出通道没有关，且正在连接中
							{
				                out.write(bt1);  
					            out.flush();
				            //out.close();   //关闭输出流
							}
			            }				           
			            /*Thread.sleep(200);
			            data=new byte[4];
			            data[0]=(byte)(Float.valueOf(urate.getText().toString())/256);
			            data[1]=(byte)((Float.valueOf(urate.getText().toString()))%256);   //电压系数
			            data[2]=(byte)(Float.valueOf(irate.getText().toString())/256);
			            data[3]=(byte)((Float.valueOf(irate.getText().toString()))%256);   //电流系数
			            bt=null;//+
			            toServer="aa68 0010 0206 0002 04"+Converts.Bytes2HexString(data);
			            toServer.replace(" ","");    //去掉空格      
			            bt=Converts.HexString2Bytes(toServer);
			            str=toServer+Converts.GetCRC(bt, 2, bt.length);   //添加校验码		
			            Log.i("Or",str);
			            bt1=Converts.HexString2Bytes(str);      //将完整的命令转换成十六进制
			            if(!client.isClosed())
			            {
			                out.write(bt1);  
				            out.flush();
				            //out.close();   //关闭输出流
			            }	*/
					}
					catch(Exception e){
						Log.i("Order","发送出错！"+e.toString());
					}}
				}.start();
			}});
		try {
			TCPInit();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}   //tcp初始化
	}
	private void TCPInit() throws UnknownHostException{
		//获取服务器ip
		final InetAddress serverAddr = InetAddress.getByName(ipaddr);// TCPServer.SERVERIP
		//获取服务器端口
		final int port=8000;
		//定义socketaddress
		final SocketAddress my_sockaddr = new InetSocketAddress(serverAddr, port);
		new Thread(){      //不能在主线程中访问网络，所以要新建线程
			public void run(){ 
				try 
				{
					client = new Socket(serverAddr, port);   //新建TCP连接	
					//client.connect(my_sockaddr,5000);	  //第二个参数是timeout	
				    DataOutputStream out=new DataOutputStream(client.getOutputStream());   
		            // 把内容发送给server  
		            String toServer = "aa68 0003 0201 0007";    //查询参数，共七个寄存器
		            toServer.replace(" ","");    //去掉空格			    		            
		            byte[] bt=null;
		            bt=Converts.HexString2Bytes(toServer);
		            String str=toServer+Converts.GetCRC(bt, 2, bt.length);   //添加校验码
		            byte[] bt1=Converts.HexString2Bytes(str);      //将完整的命令转换成十六进制
		            if(!client.isClosed())
		            {
		                out.write(bt1);  
			            out.flush();
			            //out.close();   //关闭输出流
		            }	
		            new TCPServerThread().start();    //开启新的线程接收数据			         	
	                
				} 
				catch (IOException e) {
						// TODO Auto-generated catch block	
						e.printStackTrace();
					}    
					
					
				}
			}.start();
		
}

public class TCPServerThread extends Thread 
{  
   // String ipaddr="";
    public void run() 
    {  
    	//tvRecv.setText("start");
            byte[] buffer = new byte[1024];  
			final StringBuilder sb = new StringBuilder();
             try {
            	 // 接收服务器信息       定义输入流                
                 InputStream in=client.getInputStream(); 
                 DataInputStream ins = new DataInputStream(in);
                 while (client!=null) {
                	 //content=new byte[1024];
                         if (!client.isClosed()) {
                             if (client.isConnected()) {
                                 if (!client.isInputShutdown()) {
                                	 byte[] content=new byte[50];
                                	 int count=0;   //记录接收数据数组的长度	                                	 
                                	 while((count=ins.read(content)) !=-1) {     //读取数据 ，存放到缓存区content中
                                                 		 					
                                         Map<String,Object> map=new HashMap<String,Object>();   //新建map存放要传递给主线程的数据
                                         map.put("data",content);    //客户端发回的数据                                   
                                         Message msg=new Message();	                                        
                                         msg.what=count;   //数组有效数据长度
                                         msg.obj=map;  //接收到的数据数组
                                         handler1.sendMessage(msg);	                                       
            		 					
                                     }	                                         
                                 }
                             }
                         }
                     }
                 } catch (Exception e) {
                     e.printStackTrace();
                 }
    }   
}
	@Override      //关闭时调用,将所有的socket连接关闭
	protected void onDestroy()
	{
		super.onDestroy();		
		try {
			client.close();
			client=null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			
		}			
				
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
