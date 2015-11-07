package com.suntrans.ibmsdemo;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.readystatesoftware.viewbadger.BadgeView;
import convert.Converts;
import database.DbHelper;


import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("ResourceAsColor")
public class Room_Activity extends Activity {
	private DisplayMetrics displayMetrics = new DisplayMetrics();
	private GridView gridview;
	private float UValue=0;   //电压值
	private float IValue=0;   //十个通道总电流值，总是保存最新状态
	private String last_state="";  //保存上一次通道状态和电压电流值
	private byte[] last_byte;   //上一次状态的byte数组
	private float last_IValue=0;     //上一次的电流值
	private BadgeView[] badge=new BadgeView[500];   //开关图片右上角显示开关状态的图标，默认最多为500个
	private int result_code=0;
	private String RSAddr="9999";
	private int error=-1;    ///调试用
	private ProgressDialog progressdialog;      //定义”加载中。。。“的圆形滚动条弹出框
	private String serverip;   //服务器IP
	private int serverport;     //服务器端口
	private String area="";      //存放要显示的区域
	private int which=-1;        //是通道几改变引起的progressdialog的显示，-1代表没有显示，没有命令正在进行
	private long time=0;        //存放progressdialog显示的时间，每次dismiss()后都清零   
    private byte[] bits={(byte)0x01,(byte)0x02,(byte)0x04,(byte)0x08,(byte)0x10,(byte)0x20,(byte)0x40,(byte)0x80};     //从1到8只有一位是1，用于按位与计算，获取某一位的值
	private Map<String,String> mChannel=new HashMap<String,String>(); //存放此区域开关的总开关状态，键是ipaddr，值是true或false
	private Map<String,Socket> client=new HashMap<String,Socket>();   //存放所有tcp连接的socket客户端,键是ipaddr，值是socket
    private ArrayList<Map<String, Object>> data=new ArrayList<Map<String, Object>>();    //列表显示的内容,存放着通道号、ip地址、开关状态等
  //  private BadgeView badgeview=new BadgeView(Room_Activity.this);     //可以在右上角添加文字，数字或图标
    // private String[] ip_channel=null;   //与data一一对应，存放此区域中各个通道的ip+channel，格式为"IPAddr+Channel"
    private Handler handler1=new Handler()   //接收到TCP发回的数据后调用，用于更新列表中的开关状态
	{
		 public void handleMessage(Message msg) 
		 {
	           super.handleMessage(msg);
	           Map<String,Object> map=(Map<String,Object>)msg.obj;
	           byte[] a=(byte[])(map.get("data"));    //byte数组a即为客户端发回的数据，aa68 0006是单个开通道，aa68 0003是所有的通道
	           String ipaddr=(String)(map.get("ipaddr"));    //开关的IP地址
	           String s="";		               //保存命令的十六进制字符串
	           for(int i=0;i<msg.what;i++)
	           {   
	        	   String s1=Integer.toHexString((a[ i ] +256)%256);   //转换成十六进制字符串   
	        	   if(s1.length()==1)
	        		   s1="0"+s1;          		   
	        	   s=s+s1;     	  
	           }
	           
	           String crc=Converts.GetCRC(a, 2, msg.what-2);    //获取返回数据的校验码
	           s=s.replace(" ", ""); //去掉空格
	           Log.i("Order","收到返回数据："+s);
	           if(s.length()>=10)
	           if(s.substring(0,8).equals("aa69"+RSAddr))   //返回数据符合要求才进行解析
	           {
		           if(s.substring(8, 10).equals("06"))    //根据第6,7个字符判断返回的是单个通道还是所有通道
	        	   {     //如果是单个通道
		        	   if(s.length()>=22)
			        	   if(crc.equals(s.substring(18,22)))     //判断crc校验码是否正确
			        	   {
			        		   int k=0;         //k是通道号
			        		   int state=Integer.valueOf(s.substring(17, 18));  //开关状态，1代表打开，0代表关闭
			        		   if(s.substring(13,14).equals("a"))
			        			   k=10;
			        		   else
			        			   k=Integer.valueOf(s.substring(13, 14));
			        		   if(k==0)                                          //如果通道号为0
			        			   mChannel.put(ipaddr,(state==1?"true":"false")); //更新总开关数组中的开关状态
			        		   else     //如果通道号不为0，则判断此区域是否存在开关，若存在，则更新开关状态
			        		   {
			        			   for(int j=0;j<data.size();j++)
			        			   {
			        				   Map<String,Object> map1=data.get(j);    //获取map对象
			        				   if(map1.get("Channel").equals(String.valueOf(k)))
			        				   {       //如果data数组中存在这个通道，则对开关的状态进行更新		        					   
			        					   map1.put("State",(state==1?"true":"false"));		        					  
			        					   if(j==which)     //如果返回的通道正好是进行控制的通道号，则对progressdialog执行dismiss（）方法
			        					   {		        						  
			        						   which=-1;
			        						   if(progressdialog!=null)
			        						   {
			        							   progressdialog.dismiss();  
			        					           progressdialog = null;  
			        						   }
			        					   }
			        					   //Toast.makeText(getApplicationContext(),"接收到"+ipaddr+":"+s,Toast.LENGTH_SHORT).show();
			        				   }
			        				   
			        			   }
			        			   Adapter adapter=(Adapter)gridview.getAdapter();
	        					   adapter.notifyDataSetChanged();   //更新listview
			        		  }
			        		//  Inquiry(serverip);    //查询通道状态改变后，电压电流值
			        	   }		        	   
		        	   
	        	   }
	        	   else if(s.substring(8, 10).equals("03"))
	        	   {    //如果是全部通道
	        		   if(s.length()>=28)
		        		   if(crc.equals(s.substring(24,28)))     //判断crc校验码是否正确
		        		   {        			        			   
		        			   String[] states={"false","false","false","false","false","false","false","false","false","false"};   //十个通道的状态，state[0]对应1通道
			        		   for(int i=0;i<8;i++)   //先获取前八位的开关状态
			        		   {
			        			   states[i]=((a[7]&bits[i])==bits[i])?"true":"false";   //1-8通道
			        		   }
			        		   for(int i=0;i<2;i++)
			        		   {
			        			   states[i+8]=((a[6]&bits[i])==bits[i])?"true":"false";  //9、10通道
			        		   }
			        		   UValue= (float) (((a[8])*256)/10.0+(a[9]&0xff)/10.0);   //电压值
		        			   IValue= (float) ((a[10]*256+a[11])/10.0);    //总电流值
			        		   mChannel.put(ipaddr,(((a[6]&bits[2])==bits[2])?"true":"false"));  //总开关状态，存放在mChannel中
			        		   
			        		   //先判断通道状态是否改变，若没改变，则不计算。若改变了，则计算电流差，并保存到数据库中
			        		 /*  if(last_state.length()>=28)   //上次数据有效
			        			   if(!s.substring(12, 16).equals(last_state.substring(12, 16)))  //并且这次的通道状态与上次相比，发生了改变
			        			   {
			        				   //判断发生改变的通道号
			        				   int states_now =(a[6]&0xff)*256+(a[7]&0xff);  //现在通道打开情况
			        				   int states_last=(last_byte[6]&0xff)*256+(last_byte[7]&0xff);   //上次通道打开情况
			        				   int change_channel=Math.abs(states_now-states_last);   //计算出差值
			        				 
			        				   try{
				        				   DbHelper dh1=new DbHelper(Room_Activity.this,"IBMS",null,1);
				        				   SQLiteDatabase db = dh1.getWritableDatabase(); 			
				        				   ContentValues cv = new ContentValues();    //内容数组												
										   cv.put("IValue", String.valueOf(Math.abs(IValue-last_IValue)));		
				        				   switch(change_channel)  //判断通道号
				        				   {
					        				   case 1:  //通道1发生了变化
					        				   {				        					   
					        					   db.update("switchs_tb", cv, "Channel=?", new String[]{"1"});
					        					   break;
					        				   }
					        				   case 2:
					        				   {				        					   
					        					   db.update("switchs_tb", cv, "Channel=?", new String[]{"2"});
					        					   break;
					        				   }
					        				   case 4:
					        				   {				        					   
					        					   db.update("switchs_tb", cv, "Channel=?", new String[]{"3"});
					        					   break;
					        				   }
					        				   case 8:
					        				   {				        					   
					        					   db.update("switchs_tb", cv, "Channel=?", new String[]{"4"});
					        					   break;
					        				   }
					        				   case 16:
					        				   {				        					   
					        					   db.update("switchs_tb", cv, "Channel=?", new String[]{"5"});
					        					   break;
					        				   }
					        				   case 32:
					        				   {				        					   
					        					   db.update("switchs_tb", cv, "Channel=?", new String[]{"6"});
					        					   break;
					        				   }
					        				   case 64:
					        				   {				        					   
					        					   db.update("switchs_tb", cv, "Channel=?", new String[]{"7"});
					        					   break;
					        				   }
					        				   case 128:
					        				   {				        					   
					        					   db.update("switchs_tb", cv, "Channel=?", new String[]{"8"});
					        					   break;
					        				   }
					        				   case 256:
					        				   {				        					   
					        					   db.update("switchs_tb", cv, "Channel=?", new String[]{"9"});
					        					   break;
					        				   }
					        				   case 512:
					        				   {				        					   
					        					   db.update("switchs_tb", cv, "Channel=?", new String[]{"10"});
					        					   break;
					        				   }
					        				   default:break;			        				   
				        				   }
				        				   db.close();
			        				   }
			        				   catch(Exception e){}
			        				   
			        				//   Toast.makeText(getApplicationContext(), String.valueOf(change_channel)+"+"+String.valueOf(IValue-last_IValue), Toast.LENGTH_LONG).show();
			        				   
			        			   }*/
			        		   
			        		   for(int i=0;i<10;i++)    //依次对十个通道进行判断，若存在于data数组中，则对开关状态进行更新
			        		   {
			        			   for(int j=0;j<data.size();j++)
			        			   {
				        			   Map<String,Object> map1=data.get(j);    //获取map对象
			        				   if(map1.get("Channel").equals(String.valueOf(i+1)))
			        				   {       //如果data数组中存在这个通道，则对开关的状态进行更新
			        					   map1.put("State",states[i]);
			        					//   Adapter adapter=(Adapter)gridview.getAdapter();
			        					//   adapter.notifyDataSetChanged();   //更新gridview
			        					   gridview.setAdapter(new Adapter());
			        					  
			        				   }
			        			   }
			        		   }
			        		   Log.i("States","s:"+s+" \nlast_s:"+last_state); 
			        		   last_state=s;   //将本次的值赋值给last_state，保存下来
			        		   last_byte=new byte[a.length];
			        		   System.arraycopy(a, 0, last_byte,0,a.length);     //数组直接传的话，是传地址。所以用拷贝
			        		   last_IValue=IValue;
		        		   		   
		        		   }	        		  
	        	   }
	           }
	        	   
	           
	     }
	};
	
	private Handler handler2=new Handler(){      //用于进行加载中。。。弹出框的show与dismiss
		public void handleMessage(Message msg) 
		 {
			super.handleMessage(msg);
			if(msg.what==1)    //如果值=1，代表需要显示progressdialog。为0代表需要隐藏
			{
				progressdialog = new ProgressDialog(Room_Activity.this);    //初始化progressdialog
				progressdialog.setCancelable(true);// 设置是否可以通过点击Back键取消  
		        progressdialog.setCanceledOnTouchOutside(false);// 设置在点击Dialog外是否取消Dialog进度条
				progressdialog.show();
				progressdialog.setContentView(R.layout.progressdialog);    //设置显示的内容
			}
			 else if(msg.what==2)   //如果是要根据时间判断是否关闭progressdialog的显示，用于通讯条件不好，收不到反馈时
	           {
	        	   if(new Date().getTime()-time>=2600)
	        	   {
	        		   if(progressdialog!= null)
				        {
				        	progressdialog.dismiss();
				        	progressdialog=null;
				        }	
	        		   if(which!=-1)
	        		   {
	        			   which=-1;
	        			   Toast.makeText(getApplication(), "网络错误！", Toast.LENGTH_SHORT).show();
	        		   }
	        	   }
	           }
			else
			{				
				if(progressdialog!= null)
		        {
		        	progressdialog.dismiss();
		        	progressdialog=null;
		        }	
			}
			
		 }		 
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		Intent intent=this.getIntent();    //获取Intent
		area=(String)intent.getStringExtra("area");    //房间名
		RSAddr=(String)intent.getStringExtra("RSAddr");   //开关485地址
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);//获取屏幕大小的信息
		ActionBar actionBar = this.getActionBar();   //设置actionbar标题栏		
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setDisplayShowCustomEnabled(true);
		actionBar.setDisplayShowHomeEnabled(true);
		Drawable d = this.getResources().getDrawable(R.color.bg_action); 
		actionBar.setBackgroundDrawable(d);   //设置actionbar背景
		setContentView(R.layout.room);     //设置布局文件
		actionBar.setTitle(area);
		actionBar.show();	
		if(RSAddr.equals("9999"))
		{
			Toast.makeText(getApplicationContext(),"初始化出错！",Toast.LENGTH_LONG).show();
		}
		
		SharedPreferences sharedPreferences= getSharedPreferences("data", Activity.MODE_PRIVATE);       
	    serverip =sharedPreferences.getString("serverip", "-1");   //读取服务器ip，若没有则是-1
	    serverport=Integer.valueOf(sharedPreferences.getString("port", "8028"));
		//Toast.makeText(getApplicationContext(),RSAddr,Toast.LENGTH_LONG).show();
	/*	HashMap<String,String> map1 = new HashMap<String,String>();
	    map1.put("state","false");
		data.add(map1);*/
		if(area.equals("所有房间"))
		{
			DbHelper dh1=new DbHelper(Room_Activity.this,"IBMS",null,1);
			SQLiteDatabase db = dh1.getWritableDatabase(); 			
			Cursor cursor = db.query(true, "switchs_tb", new String[]{"MACAddr"}, "", null, null, null, null, null);
			String mac1="";			
			cursor = db.query(true, "switchs_tb", new String[]{"IPAddr","Name","Channel","Type","Area","CID","Image"}, "Area!=? and Name!=?", new String[]{"无","无"}, null, null, null, null);
			while (cursor.moveToNext())    //将所有的开关ip添加到ipaddr数组中 
			{
				String ipaddr=cursor.getString(0);  //获取开关ip地址
				String Name=cursor.getString(1);    //获取通道名称
				String Channel=cursor.getString(2); //获取通道号
				String Type=cursor.getString(3);  //获取通道类型
				String Area=cursor.getString(4);  //获取区域名称
				String CID=cursor.getString(5);  //获取ID
				byte[] in = cursor.getBlob(6);     //获取图片
				Bitmap bitmap = BitmapFactory.decodeByteArray(in, 0, in.length);   //转化成bitmap格式
				Map<String, Object> map=new HashMap<String,Object>();
				map.put("IPAddr", ipaddr);
				map.put("Name", Name);
				map.put("State", "false");
				map.put("Channel", Channel);
				map.put("Type",Type);
				map.put("Area",Area);
				map.put("CID",CID);
				map.put("Image", bitmap);
				data.add(map);
			}
			db.close();
		}
		else
		{
			DbHelper dh1=new DbHelper(Room_Activity.this,"IBMS",null,1);
			SQLiteDatabase db = dh1.getWritableDatabase(); 		
			Cursor cursor = db.query(true, "switchs_tb", new String[]{"MACAddr"}, "", null, null, null, null, null);
			String mac1="";
			
			cursor = db.query(true, "switchs_tb", new String[]{"IPAddr","Name","Channel","Type","Area","CID","Image"}, "Area=?", new String[]{area}, null, null, null, null);
			while (cursor.moveToNext())    //将所有的开关ip添加到ipaddr数组中 
			{
				String ipaddr=cursor.getString(0);  //获取开关ip地址
				String Name=cursor.getString(1);    //获取通道名称
				String Channel=cursor.getString(2); //获取通道号
				String Type=cursor.getString(3);  //获取通道类型
				String Area=cursor.getString(4);  //获取区域名称
				String CID=cursor.getString(5);   //获取ID号
				byte[] in = cursor.getBlob(6);     //获取图片
				Bitmap bitmap = BitmapFactory.decodeByteArray(in, 0, in.length);   //转化成bitmap格式
				Map<String, Object> map=new HashMap<String,Object>();
				map.put("IPAddr", ipaddr); //IP地址
				map.put("Name", Name);    //名称
				map.put("State", "false");//开关状态
				map.put("Channel", Channel);//通道号
				map.put("Type",Type);     //通道类型
				map.put("Area",Area);   //区域名
				map.put("CID",CID);     //ID号
				map.put("Image", bitmap);
				data.add(map);
			}
			db.close();
		}
		try {
			TCPInit();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			 Toast.makeText(getApplicationContext(),"初始化错误！",Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
		gridview=(GridView)findViewById(R.id.gridview);
		gridview.setAdapter(new Adapter());
		
	}
	private void TCPInit() throws UnknownHostException{
		 /*DbHelper dh1=new DbHelper(Room_Activity.this,"IBMS",null,1);
		 SQLiteDatabase db = dh1.getWritableDatabase(); 
		 Cursor cursor;
		 if(area.equals("所有房间"))
			cursor = db.query(true, "switchs_tb", new String[]{"IPAddr"}, "Area!=? and Name!=?", new String[]{"无","无"}, null, null, null, null);
		 else
			 cursor = db.query(true, "switchs_tb", new String[]{"IPAddr"}, "Area=?", new String[]{area}, null, null, null, null);
		 while (cursor.moveToNext())    //将所有的开关ip添加到ipaddr数组中 
		 { */
			//final String IPAddr=cursor.getString(0);    //获取IP地址
			final String IPAddr=serverip;
			mChannel.put(IPAddr,"false");
			//获取服务器ip
			final InetAddress serverAddr = InetAddress.getByName(IPAddr);// TCPServer.SERVERIP
			//获取服务器端口
			final int port=serverport;
			new Thread(){      //不能在主线程中访问网络，所以要新建线程
				public void run(){ 
					try 
					{
						Socket client1 = new Socket(serverAddr, port);   //新建TCP连接						
						client.put(IPAddr, client1);     //将<ip,socket>对象添加到client数组中,如果已存在，会自动覆盖掉
						   
						//client.connect(my_sockaddr,5000);	  //第二个参数是timeout	
					    DataOutputStream out=new DataOutputStream(client1.getOutputStream());   
					    
			            // 把用户输入的内容发送给server  
			            String toServer = "aa68"+ RSAddr +"03 0100 0003";    //查询开关所有通道的状态
			            toServer.replace(" ","");    //去掉空格			    		            
			            byte[] bt=null;
			            bt=Converts.HexString2Bytes(toServer);
			            String str=toServer+Converts.GetCRC(bt, 2, bt.length);   //添加校验码
			            byte[] bt1=Converts.HexString2Bytes(str);      //将完整的命令转换成十六进制
			            if(!client1.isClosed())
			            {
			                out.write(bt1);  
				            out.flush();
				            //out.close();   //关闭输出流
			            }	
			            Log.i("Order","发送命令"+toServer);
		                new TCPServerThread(IPAddr).start();    //开启新的线程接收数据	
					} 
					catch (Exception e) {
							// TODO Auto-generated catch block	
							e.printStackTrace();
						}    
						
						
					}
				}.start();
		//}	
		 //db.close();
	}
	
	public class TCPServerThread extends Thread 
	{  
	    String ipaddr="";
	    public TCPServerThread(String ip) 
	    {  	
	    	ipaddr=ip;    //传入ip
	    }
	    public void run() 
	    {  
	    	//tvRecv.setText("start");
	            byte[] buffer = new byte[1024];  
				final StringBuilder sb = new StringBuilder();
	             try {
	            	 Socket client1=client.get(ipaddr);      //获取到socket对象
	            	 // 接收服务器信息       定义输入流                
	                 InputStream in=client1.getInputStream(); 
	                 DataInputStream ins = new DataInputStream(in);
	                 while (client1!=null) {
	                	 //content=new byte[1024];
	                         if (!client1.isClosed()) {
	                             if (client1.isConnected()) {
	                                 if (!client1.isInputShutdown()) {
	                                	 byte[] content=new byte[50];
	                                	 int count=0;   //记录接收数据数组的长度	                                	 
	                                	 while((count=ins.read(content)) !=-1) {     //读取数据 ，存放到缓存区content中
	                                                   		 					
	                                         Map<String,Object> map=new HashMap<String,Object>();   //新建map存放要传递给主线程的数据
	                                         map.put("data",content);    //客户端发回的数据
	                                         map.put("ipaddr",ipaddr);   //客户端的IP地址
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
	    	for (Socket v : client.values())   //遍历socket客户端数组，依次进行关闭
	    	{
	    		if(!v.isClosed())
	    		{
	    			if(v.isConnected())
	    			{
	    				try {
							v.close();
							v=null;
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	    			}
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
		
		else
			return true;
	}
	
	/****
	 * 查询开关的实时状态，包括通道开关状态和电压、电流
	 * @param ipaddr
	 */
	private void Inquiry(final String ipaddr)
	{
		new Thread()   //新建子线程，发送命令
		{
			public void run(){
			DataOutputStream out;
			try 
			{				
				Socket client1=client.get(ipaddr);
				out = new DataOutputStream(client1.getOutputStream());				
				String toServer ="aa68 "+RSAddr+"03 0100 0003";    //指令
			    toServer.replace(" ","");    //去掉空格			    		            
			    byte[] bt=null;
			    bt=Converts.HexString2Bytes(toServer);
			    String str=toServer+Converts.GetCRC(bt, 2, bt.length);   //添加校验码
			    Log.i("Order","发送的命令+"+str);
			    byte[] bt1=Converts.HexString2Bytes(str);      //将完整的命令转换成十六进制
			    if(!client1.isClosed())
			    {
			        out.write(bt1);  
				    out.flush();
				   // out.close();   //关闭输出流
			    }	
			}
			 catch (Exception e) {			// 发送出错，证明TCP断开了连接，重新建立连接
				 try 
					{					
						InetAddress serverAddr = InetAddress.getByName(ipaddr);// TCPServer.SERVERIP
						Socket client1 = new Socket(serverAddr,8028);   //新建TCP连接
						client.put(ipaddr, client1);
						out=new DataOutputStream(client1.getOutputStream());  
						new TCPServerThread(ipaddr).start();						
						
						String toServer = "aa68 "+RSAddr+"03 0100 0003";    //指令
					    toServer.replace(" ","");    //去掉空格			    		            
					    byte[] bt=null;
					    bt=Converts.HexString2Bytes(toServer);
					    String str=toServer+Converts.GetCRC(bt, 2, bt.length);   //添加校验码
					    Log.i("Order","发送的命令+"+str);
					    byte[] bt1=Converts.HexString2Bytes(str);      //将完整的命令转换成十六进制
					    if(!client1.isClosed())
					    {
					        out.write(bt1);  
						    out.flush();
						   // out.close();   //关闭输出流
					    }	
					}
					catch (Exception ee){}			
						
			}
		}
	}.start();
	}
	
	/****
	 * 向开关发送命令
	 * @param ipaddr  目标开关的IP地址
	 * @param channel  目标开关的通道号
	 * @param state     打开或关闭，1代表打开，0代表关闭
	 * @param IsShow     是否显示加载条，true表示需要显示，false表示不显示
	 * @param position   data的第position条数据需要发送命令，赋值给which
	 */
	private void SendOrder(final String ipaddr,final String channel,final String state,boolean IsShow,int position)
	{
		if(which==-1)   //首先判断是否有开关命令正在执行，如果没有则向开关发送命令
		{
			    time=new Date().getTime();    //获取现在的时间，单位是ms
				if(IsShow)      //0.25s后判断是否有反馈，若没有则显示progressdialog，使页面不能点，然后在2.5秒后判断此次触发的progressdialog的显示是否已关闭，如果没有，则进行关闭，并将which置"100"
				{					
					which=position;
					Timer timer = new Timer(true);     //定义定时器，定时执行关闭progressdialog命令，定时时长为2秒
				    timer.schedule(new TimerTask(){  
				   	        public void run() {     //在新线程中执行
				   	        	if(which!=-1)
				   	        	{
				       		        Message message = new Message();      
				       		        message.what = 1;       //1表示要显示        		       
				       		        handler2.sendMessage(message);  
				           		    Timer timer1 = new Timer(true);     //定义定时器，定时执行关闭progressdialog命令，定时时长为2秒
				 		            timer1.schedule(new TimerTask(){  
				 		           	        public void run() {     //在新线程中执行				 		           	        	
				 			           		        Message message = new Message();      
				 			           		        message.what = 2;       //2表示要隐藏             		       
				 			           		        handler2.sendMessage(message);  				 		           	        	
				 		           		     }  
				 		           		 } ,2700); //2.7s后判断是否关闭progressdialog，若没关闭，则进行关闭
				   	        	}
				   		     }  
				   		 } ,300); //0.30s后判断是否关闭progressdialog，若没关闭，则进行关闭
				   
				}
				else   //如果选择不显示progressdialog				
					which=-1;   //直接允许下一条指令发送
				
				new Thread()   //新建子线程，发送命令
				{
					public void run(){
					DataOutputStream out;
					try 
					{				
						Socket client1=client.get(ipaddr);
						out = new DataOutputStream(client1.getOutputStream());
						if(mChannel.get(ipaddr).equals("false")&&state.equals("1"))   //如果主开关是关的，则先打开总开关
						{
							String toServer = "aa68 "+RSAddr+" 06 0300 0001";    //指令
						    toServer.replace(" ","");    //去掉空格			    		            
						    byte[] bt=null;
						    bt=Converts.HexString2Bytes(toServer);
						    String str=toServer+Converts.GetCRC(bt, 2, bt.length);   //添加校验码
						    Log.i("Order","发送的命令+"+str);
						    byte[] bt1=Converts.HexString2Bytes(str);      //将完整的命令转换成十六进制
						    if(!client1.isClosed())
						    {
						        out.write(bt1);  
							    out.flush();
							   // out.close();
						    }	
						    try {
								Thread.sleep(200);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						String toServer = "aa68 "+RSAddr+" 06 030"+channel+" 000"+state;    //指令
					    toServer.replace(" ","");    //去掉空格			    		            
					    byte[] bt=null;
					    bt=Converts.HexString2Bytes(toServer);
					    String str=toServer+Converts.GetCRC(bt, 2, bt.length);   //添加校验码
					    Log.i("Order","发送的命令+"+str);
					    byte[] bt1=Converts.HexString2Bytes(str);      //将完整的命令转换成十六进制
					    if(!client1.isClosed())
					    {
					        out.write(bt1);  
						    out.flush();
						   // out.close();   //关闭输出流
					    }	
					}
					 catch (Exception e) {			// 发送出错，证明TCP断开了连接，重新建立连接
						 try 
							{					
								InetAddress serverAddr = InetAddress.getByName(ipaddr);// TCPServer.SERVERIP
								Socket client1 = new Socket(serverAddr,serverport);   //新建TCP连接
								client.put(ipaddr, client1);
								out=new DataOutputStream(client1.getOutputStream());  
								new TCPServerThread(ipaddr).start();
								
								if(mChannel.get(ipaddr).equals("false")&&state.equals("1"))   //如果主开关是关的，则先打开总开关
								{
									String toServer = "aa68 "+RSAddr+" 06 0300 0001";    //指令
								    toServer.replace(" ","");    //去掉空格			    		            
								    byte[] bt=null;
								    bt=Converts.HexString2Bytes(toServer);
								    String str=toServer+Converts.GetCRC(bt, 2, bt.length);   //添加校验码
								    Log.i("Order","发送的命令+"+str);
								    byte[] bt1=Converts.HexString2Bytes(str);      //将完整的命令转换成十六进制
								    if(!client1.isClosed())
								    {
								        out.write(bt1);  
									    out.flush();
									   // out.close();
								    }	
								    try {
										Thread.sleep(200);
									} catch (InterruptedException ex) {
										// TODO Auto-generated catch block
										ex.printStackTrace();
									}
								}
								String toServer = "aa68 "+RSAddr+" 06 030"+channel+" 000"+state;    //指令
							    toServer.replace(" ","");    //去掉空格			    		            
							    byte[] bt=null;
							    bt=Converts.HexString2Bytes(toServer);
							    String str=toServer+Converts.GetCRC(bt, 2, bt.length);   //添加校验码
							    Log.i("Order","发送的命令+"+str);
							    byte[] bt1=Converts.HexString2Bytes(str);      //将完整的命令转换成十六进制
							    if(!client1.isClosed())
							    {
							        out.write(bt1);  
								    out.flush();
								   // out.close();   //关闭输出流
							    }	
							}
							catch (Exception ee){Log.i("Order","发送出错"+ee.toString());}			
								
					}
				}
			}.start();
		}
				//Log.i("Time","发送命令+"+String.valueOf(System.currentTimeMillis()));		
	}
	
	@SuppressLint("ResourceAsColor")
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
  
        @SuppressLint("ResourceAsColor")
		@Override  
        public View getView(final int position, View convertView, ViewGroup parent) {  
            final Map<String,Object> map = data.get(position);  
            final String state=map.get("State").toString();
            final String ipaddr=map.get("IPAddr").toString();
            final String name=map.get("Name").toString();
            final String channel=map.get("Channel").toString();
            final String Area=map.get("Area").toString();
            final String CID=map.get("CID").toString();
            Bitmap bitmap=(Bitmap)map.get("Image");
           // final String type=map.get("Type").toString();
           // if(convertView==null)
           	convertView = LayoutInflater.from(getApplication()).inflate(R.layout.roomgridview, null);  
            final TextView tx_name=(TextView)convertView.findViewById(R.id.name);         
            final ImageView image = (ImageView)convertView.findViewById(R.id.image); //开关图片
            final LinearLayout layout1=(LinearLayout)convertView.findViewById(R.id.layout1);  //图片的layout
            bitmap=convert.Converts.toRoundCorner(bitmap, 20);  //实现图片的圆角
            image.setImageBitmap(bitmap);
            if(state.equals("false"))     //如果开关状态为关
            {
            	
            	badge[position]=new BadgeView(Room_Activity.this.getApplicationContext(),layout1);
            	badge[position].setWidth(convert.Converts.dip2px(getApplicationContext(), 7));  //设置宽度为7dip
            	badge[position].setHeight(convert.Converts.dip2px(getApplicationContext(), 7)); //设置高度为7dip
            	badge[position].setBackgroundResource(R.drawable.offdot);    //设置图标
            	badge[position].setBadgePosition(BadgeView.POSITION_TOP_RIGHT); //设置显示的位置，右上角
            	badge[position].show();     	    
            	
            	tx_name.setText(Area+"-"+name);
            }
            else            //如果开关状态为开
            {                	
            	badge[position]=new BadgeView(Room_Activity.this.getApplicationContext(),layout1);
            	badge[position].setWidth(convert.Converts.dip2px(getApplicationContext(), 7));  //设置宽度为7dip
            	badge[position].setHeight(convert.Converts.dip2px(getApplicationContext(), 7)); //设置高度为7dip
            	badge[position].setBackgroundResource(R.drawable.ondot);    //设置图标
            	badge[position].setBadgePosition(BadgeView.POSITION_TOP_RIGHT); //设置显示的位置，右上角
            	badge[position].show(); 		  
            	
            	tx_name.setText(Area+"-"+name);
            }
            convertView.setOnClickListener(new OnClickListener(){   //设置点击事件,发送控制开关的命令

				@SuppressLint("ResourceAsColor")
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					//Toast.makeText(getApplicationContext(),"你点击了第"+position+"个图标",Toast.LENGTH_SHORT).show();
					//which=position;    //设置which的值，表明是第position行的开关状态发生了改变
					//先发送查询命令，若对通道进行了操作，则操作完成后再执行一次查询，两次查询的电流之差就是该通道的电流值
		/*			Inquiry(serverip);					
					
					final AlertDialog.Builder builder = new AlertDialog.Builder(Room_Activity.this); 
					View view=LayoutInflater.from(getApplication()).inflate(R.layout.roomdialog, null); 
					TextView tx_title=(TextView)view.findViewById(R.id.tx_title);    //标题
					TextView tx_u=(TextView)view.findViewById(R.id.tx_u);    //电压值
					TextView tx_i=(TextView)view.findViewById(R.id.tx_i);    //电流值
					TextView tx_p=(TextView)view.findViewById(R.id.tx_p);    //功率
					TextView tx_channel=(TextView)view.findViewById(R.id.tx_channel);   //通道号
					Button bt_send=(Button)view.findViewById(R.id.bt_send);    //发送命令按钮
					Button bt_cancel=(Button)view.findViewById(R.id.bt_cancel);    //取消按钮
					builder.setCancelable(true);
					
					tx_channel.setText("通道"+channel);
					tx_title.setText(map.get("Area").toString()+"-"+map.get("Name").toString());
					
					if(state.equals("true"))   //如果状态是打开的，从数据库读取电流
					{
						bt_send.setText("关闭开关");
						tx_u.setText(String.valueOf(UValue));
						float ivalue=0;
						try
						{
							DbHelper dh1=new DbHelper(Room_Activity.this,"IBMS",null,1);
	      				    SQLiteDatabase db = dh1.getWritableDatabase(); 	
	      				    Cursor cursor = db.query(true, "switchs_tb", new String[]{"IValue"}, "IPAddr=? and Channel=?", new String[]{ipaddr,channel}, null, null, null, null);
	      				    while(cursor.moveToNext())
	      				    {
	      				    	ivalue=Float.valueOf(cursor.getString(0));
	      				    }	      				    
						}
						catch(Exception e){}
						DecimalFormat decimalFormat=new DecimalFormat("##0.00");//构造方法的字符格式这里如果小数不足2位,会以0补足.
						tx_i.setText(decimalFormat.format(ivalue));
						tx_p.setText(decimalFormat.format(UValue*ivalue));
					}
					else    //如果状态是关闭，则电压、电流和功率显示0
					{
						bt_send.setText("打开开关");
						tx_u.setText("0.0");
						tx_i.setText("0.0");
						tx_p.setText("0.00");
					}
					builder.setView(view);
					final AlertDialog alertdialog =builder.create();
					bt_send.setOnClickListener(new OnClickListener(){

						@Override
						public void onClick(View v) {
							// TODO Auto-generated method stub
							if(state.equals("true"))   //执行关闭命令
							{
								SendOrder("61.235.65.160",channel.equals("10")?"a":channel,"0",true,position);
							
							}
							else                   //执行打开命令
							{
								SendOrder("61.235.65.160",channel.equals("10")?"a":channel,"1",true,position);
							}
							alertdialog.cancel();
						}});
					bt_cancel.setOnClickListener(new OnClickListener(){

						@Override
						public void onClick(View v) {
							// TODO Auto-generated method stub
							alertdialog.cancel();
						}});
					
					
					//alertdialog=builder.create();
					alertdialog.show();*/
					if(state.equals("true"))   //执行关闭命令
					{
						SendOrder(serverip,channel.equals("10")?"a":channel,"0",true,position);
					
					}
					else                   //执行打开命令
					{
						SendOrder(serverip,channel.equals("10")?"a":channel,"1",true,position);
					}
			//		Adapter.this.notifyDataSetChanged();   //刷新显示
 				}
            	
            });
            convertView.setOnLongClickListener(new OnLongClickListener(){    //设置长点击事件

				@Override
				public boolean onLongClick(View v) {
					// TODO Auto-generated method stub
					final AlertDialog.Builder builder = new AlertDialog.Builder(Room_Activity.this);   
				    builder.setTitle("编辑开关信息："); 	
				    builder.setItems(new String[]{"修改名称","更换图标","删除开关"}, new DialogInterface.OnClickListener() {  
			            public void onClick(DialogInterface dialog, int which) {  
			            //点击后弹出窗口选择了第几项  
			            	//Toast.makeText(getApplicationContext(),"你点击了第"+which+"个图标",Toast.LENGTH_SHORT).show();
			            	switch(which)
			            	{
				            	case 0:  //修改名称
				            	{
				            		LayoutInflater factory = LayoutInflater.from(Room_Activity.this);  
									final View view = factory.inflate(R.layout.hoursedialog, null); 
									final AlertDialog.Builder builder = new AlertDialog.Builder(Room_Activity.this);   
								    builder.setTitle("请输入开关名称："); 	
								    final EditText  tx1= (EditText) view.findViewById(R.id.tx1); 
								    tx1.setText(name);
								    builder.setView(view);
								    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {  
								         public void onClick(DialogInterface dialog, int whichButton) {
								        	
								        	 String New_Name=tx1.getText().toString();
									         DbHelper dh1=new DbHelper(Room_Activity.this,"IBMS",null,1);
											 SQLiteDatabase db = dh1.getWritableDatabase(); 
											 ContentValues cv = new ContentValues();    //内容数组
											 Cursor cursor = db.query(true, "switchs_tb", new String[]{"IPAddr","Name","Channel","Type","Area","CID"}, "Area=?", new String[]{area}, null, null, null, null);
											 cv.put("Name", New_Name);
											 db.update("switchs_tb", cv, "CID=?", new String[]{CID});
											 cursor = db.query(true, "room_tb", new String[]{"RID","Name"}, null, null, null, null, null, null, null);
											 data=new ArrayList<Map<String, Object>>();
											 if(area.equals("所有房间"))
												{
												 	cursor = db.query(true, "switchs_tb", new String[]{"IPAddr","Name","Channel","Type","Area","CID","Image"}, "Area!=? and Name!=?", new String[]{"无","无"}, null, null, null, null);
													while (cursor.moveToNext())    //将所有的开关ip添加到ipaddr数组中 
													{
														String ipaddr=cursor.getString(0);  //获取开关ip地址
														String Name=cursor.getString(1);    //获取通道名称
														String Channel=cursor.getString(2); //获取通道号
														String Type=cursor.getString(3);  //获取通道类型
														String Area=cursor.getString(4);  //获取区域名称
														String CID=cursor.getString(5);  //获取ID
														byte[] in = cursor.getBlob(6);     //获取图片
														Bitmap bitmap = BitmapFactory.decodeByteArray(in, 0, in.length);   //转化成bitmap格式
														Map<String, Object> map=new HashMap<String,Object>();
														map.put("IPAddr", ipaddr);
														map.put("Name", Name);
														map.put("State", "false");
														map.put("Channel", Channel);
														map.put("Type",Type);
														map.put("Area",Area);
														map.put("CID",CID);
														map.put("Image", bitmap);
														data.add(map);
													}
														
												}
												else
												{
														cursor = db.query(true, "switchs_tb", new String[]{"IPAddr","Name","Channel","Type","Area","CID","Image"}, "Area=?", new String[]{area}, null, null, null, null);
														while (cursor.moveToNext())    //将所有的开关ip添加到ipaddr数组中 
														{
															String ipaddr=cursor.getString(0);  //获取开关ip地址
															String Name=cursor.getString(1);    //获取通道名称
															String Channel=cursor.getString(2); //获取通道号
															String Type=cursor.getString(3);  //获取通道类型
															String Area=cursor.getString(4);  //获取区域名称
															String CID=cursor.getString(5);   //获取ID号
															byte[] in = cursor.getBlob(6);     //获取图片
															Bitmap bitmap = BitmapFactory.decodeByteArray(in, 0, in.length);   //转化成bitmap格式
															Map<String, Object> map=new HashMap<String,Object>();
															map.put("IPAddr", ipaddr); //IP地址
															map.put("Name", Name);    //名称
															map.put("State", "false");//开关状态
															map.put("Channel", Channel);//通道号
															map.put("Type",Type);     //通道类型
															map.put("Area",Area);   //区域名
															map.put("CID",CID);     //ID号
															map.put("Image",bitmap);
															data.add(map);
														}
												}
											    Adapter.this.notifyDataSetChanged();   //刷新
												Toast.makeText(getApplicationContext(),"修改成功！",Toast.LENGTH_SHORT).show();
											 
										 
											 }
										 });
								    builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {  
								         public void onClick(DialogInterface dialog, int whichButton) {  
								 
								         }  
								     });  
								    builder.create().show();
										 
				            		break;
				            	}
				            	case 1:  //  更换图标
				            	{
				            		builder.setTitle("更换图标："); 	
								    builder.setItems(new String[]{"本地图库","拍照","取消"}, new DialogInterface.OnClickListener() {  
							            public void onClick(DialogInterface dialog, int which) {  
							            //点击后弹出窗口选择了第几项  
							            	//Toast.makeText(getApplicationContext(),"你点击了第"+which+"个图标",Toast.LENGTH_SHORT).show();
							            	switch(which)
							            	{
								            	case 0:    //选择本地图库
								            		{
								            			result_code=position;
								            			//打开图库
								      				    Intent i = new Intent(
								      					Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
								      					startActivityForResult(i,10000); //请求码为10000，  用来区分图片是裁剪前的还是裁剪后的
								            			break;
								            		}
								            	case 1:    //选择拍照
								            		{
								            			result_code=position;
								            			//拍照
								        				Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);  
								                        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);  
								                        startActivityForResult(intent, 10001);  //请求码为10001， 用来区分图片是裁剪前的还是裁剪后的
								            			break;
								            		}
								            	case 2://点击取消
								            	{
								            		break;
								            	}
								            	default:break;
							            	}}});
								    builder.create().show();
				            		break;
				            	}
				            	case 2:    //删除开关
				            		{
				            			DbHelper dh1=new DbHelper(Room_Activity.this,"IBMS",null,1);
										SQLiteDatabase db = dh1.getWritableDatabase(); 
										ContentValues cv = new ContentValues();    //内容数组
										cv.put("Area","无");
										cv.put("Name","无");
										db.update("switchs_tb", cv, "CID=?", new String[]{CID});
										Cursor cursor = db.query(true, "switchs_tb", new String[]{"IPAddr","Name","Channel","Type","Area","CID"}, "Area=?", new String[]{area}, null, null, null, null);
										data=new ArrayList<Map<String, Object>>();
										if(area.equals("所有房间"))
										{
										 	cursor = db.query(true, "switchs_tb", new String[]{"IPAddr","Name","Channel","Type","Area","CID","Image"}, "Area!=? and Name!=?", new String[]{"无","无"}, null, null, null, null);
											while (cursor.moveToNext())    //将所有的开关ip添加到ipaddr数组中 
											{
												String ipaddr=cursor.getString(0);  //获取开关ip地址
												String Name=cursor.getString(1);    //获取通道名称
												String Channel=cursor.getString(2); //获取通道号
												String Type=cursor.getString(3);  //获取通道类型
												String Area=cursor.getString(4);  //获取区域名称
												String CID=cursor.getString(5);  //获取ID
												byte[] in = cursor.getBlob(6);     //获取图片
												Bitmap bitmap = BitmapFactory.decodeByteArray(in, 0, in.length);   //转化成bitmap格式
												Map<String, Object> map=new HashMap<String,Object>();
												map.put("IPAddr", ipaddr);
												map.put("Name", Name);
												map.put("State", "false");
												map.put("Channel", Channel);
												map.put("Type",Type);
												map.put("Area",Area);
												map.put("CID",CID);
												map.put("Image", bitmap);
												data.add(map);
											}
												
										}
										else
										{
												cursor = db.query(true, "switchs_tb", new String[]{"IPAddr","Name","Channel","Type","Area","CID","Image"}, "Area=?", new String[]{area}, null, null, null, null);
												while (cursor.moveToNext())    //将所有的开关ip添加到ipaddr数组中 
												{
													String ipaddr=cursor.getString(0);  //获取开关ip地址
													String Name=cursor.getString(1);    //获取通道名称
													String Channel=cursor.getString(2); //获取通道号
													String Type=cursor.getString(3);  //获取通道类型
													String Area=cursor.getString(4);  //获取区域名称
													String CID=cursor.getString(5);   //获取ID号
													byte[] in = cursor.getBlob(6);     //获取图片
													Bitmap bitmap = BitmapFactory.decodeByteArray(in, 0, in.length);   //转化成bitmap格式
													Map<String, Object> map=new HashMap<String,Object>();
													map.put("IPAddr", ipaddr); //IP地址
													map.put("Name", Name);    //名称
													map.put("State", "false");//开关状态
													map.put("Channel", Channel);//通道号
													map.put("Type",Type);     //通道类型
													map.put("Area",Area);   //区域名
													map.put("CID",CID);     //ID号
													map.put("Image",bitmap);
													data.add(map);
												}
										}
										 Adapter.this.notifyDataSetChanged();   //刷新
										 Toast.makeText(getApplicationContext(),"删除成功！",Toast.LENGTH_SHORT).show();
				            			break;
				            		}
				            	default:break;
			            	}
			            }
			            }); 
				    builder.show();
				    return false;
				}});
            return convertView;
            }
    	}
	@Override  
    protected void onActivityResult(int requestCode, int resultCode, Intent data1) {  
		super.onActivityResult(requestCode, resultCode, data1);
		if (resultCode != RESULT_CANCELED) 
		{ 
			if(requestCode==10000||requestCode==10001)//如果是刚刚选择完，还未裁剪，则跳转到裁剪的activity
			{
	            if (data1 != null) {  
	                //取得返回的Uri,基本上选择照片的时候返回的是以Uri形式，但是在拍照中有得机子呢Uri是空的，所以要特别注意  
	            	Uri mImageCaptureUri = data1.getData();  
	                //返回的Uri不为空时，那么图片信息数据都会在Uri中获得。如果为空，那么我们就进行下面的方式获取  
	                if (mImageCaptureUri != null) {  
	                    Bitmap image;  
	                    try {  
	                        //这个方法是根据Uri获取Bitmap图片的静态方法  
	                        image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), mImageCaptureUri);  
	                        if (image != null) {  
	                        	startPhotoZoom(mImageCaptureUri);    //打开裁剪activity
	                        }  
	                    } catch (Exception e) {  
	                    	Log.i("IBM","URI出错"+e.toString());
	                    }  
	                } 
	                else {  
	                    Bundle extras = data1.getExtras();
	                    Bitmap image=null;
	                    if (extras != null) {  
	                        //这里是有些拍照后的图片是直接存放到Bundle中的所以我们可以从这里面获取Bitmap图片  
	                        image = extras.getParcelable("data");  
	                    }  
	                	// 判断存储卡是否可以用，可用进行存储
	                	String state = Environment.getExternalStorageState();
	                	if (state.equals(Environment.MEDIA_MOUNTED)) {
		                	File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
		                	File tempFile = new File(path, "image.jpg");
		                	FileOutputStream b = null;
		                	try {
		                        b = new FileOutputStream(tempFile);
		                        image.compress(Bitmap.CompressFormat.JPEG, 100, b);// 把数据写入文件
		                    } catch (FileNotFoundException e) {
		                        e.printStackTrace();
		                    } finally {
		                        try {
		                            b.flush();
		                            b.close();
		                        } catch (IOException e) {
		                            e.printStackTrace();
		                        }
		                    }

		                	startPhotoZoom(Uri.fromFile(tempFile));
	                	} 
	                	else {
	                		Toast.makeText(getApplicationContext(), "未找到存储卡，无法存储照片！", Toast.LENGTH_SHORT).show();
	                	}
	                }  
	  
	            }  
			}
			else
	        	{
	        		if (data1 != null) {  
		                //取得返回的Uri,基本上选择照片的时候返回的是以Uri形式，但是在拍照中有得机子呢Uri是空的，所以要特别注意  
		            	Uri mImageCaptureUri = data1.getData();  
		                //返回的Uri不为空时，那么图片信息数据都会在Uri中获得。如果为空，那么我们就进行下面的方式获取  
		                if (mImageCaptureUri != null) {  
		                	Log.i("IBM","URI不为空"+mImageCaptureUri.toString());
		                    Bitmap image;  
		                    try {  
		                        //这个方法是根据Uri获取Bitmap图片的静态方法  
		                        image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), mImageCaptureUri);  
		                        if (image != null) {  
		                        	data.get(result_code).put("Image",image);
		                        	DbHelper dh1=new DbHelper(Room_Activity.this,"IBMS",null,1);
									SQLiteDatabase db = dh1.getWritableDatabase(); 
									ContentValues cv = new ContentValues();    //内容数组
									ByteArrayOutputStream os = new ByteArrayOutputStream();  
									image.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
									cv.put("Image", os.toByteArray());
									db.update("switchs_tb", cv, "CID=?", new String[]{data.get(result_code).get("CID").toString()});
									((Adapter)gridview.getAdapter()).notifyDataSetChanged();   //刷新
		                        }  
		                    } catch (Exception e) {  
		                    	Log.i("IBM","URI出错"+e.toString());
		                    }  
		                } 
		                else {  
		                    Bundle extras = data1.getExtras();
		                    Log.i("IBM",extras.toString()); 
		                    if (extras != null) {  
		                        //这里是有些拍照后的图片是直接存放到Bundle中的所以我们可以从这里面获取Bitmap图片  
		                        Bitmap image = extras.getParcelable("data");  
		                        if (image != null) {  
		                        	data.get(result_code).put("Image",image);
		                        	DbHelper dh1=new DbHelper(Room_Activity.this,"IBMS",null,1);
									SQLiteDatabase db = dh1.getWritableDatabase(); 
									ContentValues cv = new ContentValues();    //内容数组
									ByteArrayOutputStream os = new ByteArrayOutputStream();  
									image.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
									cv.put("Image", os.toByteArray());
									db.update("switchs_tb", cv, "CID=?", new String[]{data.get(result_code).get("CID").toString()});
									((Adapter)gridview.getAdapter()).notifyDataSetChanged();   //刷新
		                        }  
		                    }  
		                }
	        		}
		        }
	        
		}
		else   //如果点击了取消，则判断是不是裁剪的activity，若是则返回图片选择或拍照的页面，若不是则不进行操作
		{
			if((requestCode==10000||requestCode==10001))//如果是在选择图片中，点击了取消，不进行操作
			{}
			else
			{
				//打开图库
				    Intent i = new Intent(
					Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
					startActivityForResult(i,10000); //请求码为10000，  用来区分图片是裁剪前的还是裁剪后的	            			
	            		
	            		
			}
		}
    }  
	/**
	* 裁剪图片方法实现
	* @param uri
	*/
	public void startPhotoZoom(Uri uri) {
	Intent intent = new Intent("com.android.camera.action.CROP");
	intent.setDataAndType(uri, "image/*");
	// 设置裁剪
	intent.putExtra("crop", "true");
	intent.putExtra("scale", true);// 去黑边
    intent.putExtra("scaleUpIfNeeded", true);// 去黑边
	// aspectX aspectY 是宽高的比例
	intent.putExtra("aspectX", 1);
	intent.putExtra("aspectY", 1);
	// outputX outputY 是裁剪图片宽高
	intent.putExtra("outputX", convert.Converts.dip2px(getApplicationContext(), 90));
	intent.putExtra("outputY", convert.Converts.dip2px(getApplicationContext(), 90));
	intent.putExtra("return-data", false);
	startActivityForResult(intent, result_code);
	}
	
}
