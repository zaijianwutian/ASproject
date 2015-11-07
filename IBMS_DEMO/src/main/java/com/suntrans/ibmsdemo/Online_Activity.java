package com.suntrans.ibmsdemo;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


import pulltofresh.PullDownScrollView;
import pulltofresh.PullDownScrollView.RefreshListener;


import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class Online_Activity extends Activity implements RefreshListener{
	private PullDownScrollView mPullDownScrollView;  
	private ProgressDialog progressdialog;      //定义”加载中。。。“的圆形滚动条弹出框
	private ListView listview1;	
	private ArrayList<Map<String, String>> data=new ArrayList<Map<String, String>>();    //列表显示的内容,存放着开关的mac地址和IP地址
	private DatagramSocket UDPclient;	 //UDP客户端
	//接收线程发送过来信息    更新列表
    public Handler handler1 = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Map<String,String> map=(Map<String, String>) msg.obj;
            int j=0;
            for(int i=0;i<data.size();i++)
            {
            	if(data.get(i).get("MACAddr").equals(map.get("MACAddr")))
            	     j=1;
            }
            if(j==0)
            	data.add(map);
            Adapter adapter=(Adapter)listview1.getAdapter();
			adapter.notifyDataSetChanged();   //更新listview
     }
    };
    
  //接收线程发送过来信息    更新列表
    public Handler handler2 = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(progressdialog!=null)
		        {
		        	progressdialog.dismiss();
		        	progressdialog=null;
		        }
            Adapter adapter=(Adapter)listview1.getAdapter();
			adapter.notifyDataSetChanged();   //更新listview
     }
    };
    @Override     //向标题栏添加item
	 public boolean onCreateOptionsMenu(Menu menu) {  
		  getMenuInflater().inflate(R.menu.online, menu);  
		  return true;  
} 
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
		setContentView(R.layout.online);     //设置布局文件
		listview1=(ListView)findViewById(R.id.listview1);   //得到listview1	
		listview1.setAdapter(new Adapter());      //设置listview适配器，为自定义适配器
		UDPInit();    //发送UDP指令  寻找在线开关
		/*mPullDownScrollView = (PullDownScrollView) findViewById(R.id.refresh_root);  
	    mPullDownScrollView.setRefreshListener(this);  
	    mPullDownScrollView.setPullDownElastic(new PullDownElasticImp(this)); */ 
	}
	@Override
	protected void onStop(){
		super.onStop();
		try{
			UDPclient.close();
			UDPclient=null;
		}
		catch(Exception e){}
		
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
			            	 byte bt [] = new byte[1024];
			                 //创建一个空的DatagramPacket对象
			                 DatagramPacket packet = new DatagramPacket(bt,bt.length);
			                 //使用receive方法接收客户端所发送的数据
			                 UDPclient.receive(packet);
			                 String clientip=packet.getAddress().toString().replace("/", "");	//ip地址
			                 String clientmac=new String(packet.getData()).replace("+OK=", "");  //MAC地址
			                 clientmac=clientmac.replaceAll("\r|\n", "");    //去掉换行符
			                 Map<String,String> map=new HashMap<String,String>();
			                 map.put("IPAddr", clientip);
			                 map.put("MACAddr",clientmac);
			                 //data.add(map);
                             Message msg=new Message();	
                             msg.obj=map;
                             handler1.sendMessage(msg);	
			                 
		                 }    
	                 } catch (Exception e) {
	                     e.printStackTrace();
	                 }
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
		else if(item.getItemId()==R.id.action_refresh)   //如果按下的是标题栏的刷新键
		{
			progressdialog = new ProgressDialog(Online_Activity.this);    //初始化progressdialog
			progressdialog.setCancelable(false);// 设置是否可以通过点击Back键取消  
	        progressdialog.setCanceledOnTouchOutside(false);// 设置在点击Dialog外是否取消Dialog进度条
			progressdialog.show();
			progressdialog.setContentView(R.layout.progressdialog);    //设置显示的内容
			data=new ArrayList<Map<String, String>>(); 
			UDPInit();
			Timer timer = new Timer(true);     //定义定时器，定时执行关闭progressdialog命令，定时时长为4秒
            timer.schedule(new TimerTask(){  
           	        public void run() {     //在新线程中执行
           	         Message msg=new Message();	
                    
                     handler2.sendMessage(msg);	
           		      
           		     }  
           		 } ,1500); //1.5s后判断是否关闭，若没关闭，则进行关闭
            return true;
			
		}      
		     
		else
			return true;
	}
	//自定义Adapter  
    class Adapter extends BaseAdapter{  
  
      

		@Override  
        public int getCount() {       //列表的条目
            return data.size();  
        }  
  
        @Override  
        public Object getItem(int position) {  
            return data.get(position);  
        }  
  
        @Override  
        public long getItemId(int position) {  
            return position;  
        }  
  
        @Override  
        public View getView(final int position, View convertView, ViewGroup parent) {  
            Map<String,String> map = data.get(position);  
            convertView = LayoutInflater.from(getApplication()).inflate(R.layout.onlinelistview, null);  
            final TextView name = (TextView)convertView.findViewById(R.id.name); 
            name.setText(data.get(position).get("MACAddr"));
            convertView.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					Intent intent=new Intent();
		            intent.putExtra("IPAddr",data.get(position).get("IPAddr"));      //开关ip
		         	intent.setClass(Online_Activity.this, Mode_Activity.class);//设置要跳转的activity
					Online_Activity.this.startActivity(intent);//开始跳转
					 //Toast.makeText(getApplicationContext(),"IP是"+data.get(position).get("IPAddr")+"；通道号是："+data.get(position).get("Channel"),Toast.LENGTH_SHORT).show();
				}});
            return convertView;
        }
    }
    
    @Override  
    public void onRefresh(PullDownScrollView view) { 
  	  new Handler().postDelayed(new Runnable() {               //延时0.8s  
            @Override  
            public void run() {          	  
                // TODO Auto-generated method stub  
          	  SimpleDateFormat sdf=new SimpleDateFormat("HH:mm:ss");    //获取当前时间
          	  String date=sdf.format(new java.util.Date()); 
              mPullDownScrollView.finishRefresh("上次刷新时间:"+date);
              data=null;    //刷新前先清空数据
              UDPInit(); 
                           
            }  
        }, 1000);      //延迟1s执行
  	 
    }  
    
}
