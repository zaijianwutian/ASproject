package views;


import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import views.ControlFragment.Adapter;

import com.suntrans.ibmsdemo.R;

import convert.Converts;

import database.DbHelper;

import com.suntrans.ibmsdemo.Hourse_Activity;
import  com.suntrans.ibmsdemo.Main_Activity;
import  com.suntrans.ibmsdemo.Room_Activity;
import  com.suntrans.ibmsdemo.SettingScene_Activity;
import  com.suntrans.ibmsdemo.Update_Activity;
import  com.suntrans.ibmsdemo.Room_Activity.TCPServerThread;
import android.support.v4.app.Fragment;
import android.annotation.SuppressLint;
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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.View.OnLongClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class SceneFragment extends Fragment {
	private int result_code=0;   //确定需要更换图标的是data中的哪一个
	private GridView gridview;
	private int IsFinish=1;
	private String serverip;
	private int serverport;	
	private String RSAddr="";   //开关485地址
	private int error=-1;    ///调试用
	private String which_ip="";   //正在控制的ip
	private int which=-1;        //是第几条开关状态改变引起的progressdialog的显示，-1代表没有显示
	private ProgressDialog progressdialog;      //定义”加载中。。。“的圆形滚动条弹出框
	private long time=0;        //存放progressdialog显示的时间，每次dismiss()后都清零   
    private byte[] bits={(byte)0x01,(byte)0x02,(byte)0x04,(byte)0x08,(byte)0x10,(byte)0x20,(byte)0x40,(byte)0x80};     //从1到8只有一位是1，用于按位与计算，获取某一位的值
	private Map<String,String> mChannel=new HashMap<String,String>(); //存放此区域开关的总开关状态，键是ipaddr，值是true或false
	private Map<String,Socket> client=new HashMap<String,Socket>();   //存放所有tcp连接的socket客户端,键是ipaddr，值是socket
	private ArrayList<Map<String, Object>> data=new ArrayList<Map<String, Object>>();    //列表显示的内容,存放着通道号、ip地址、开关状态等
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
		           Log.i("Order","收到数据："+s);
		           if(s.length()>=12)
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
				        		   else     //如果通道号不为0，则判断返回的命令是要进行控制的通道，如果是，则继续下一个命令
				        		   {
				        			   IsFinish=1;   //表明这条命令完成了
				        		   }
				        		   
				        	   }
			        	   
		        	   }
		        	   else if(s.substring(8, 10).equals("03"))
		        	   {    //如果是全部通道
			        		   if(s.length()>=20)
		        		       if(crc.equals(s.substring(16,20)))     //判断crc校验码是否正确
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
				        		   mChannel.put(ipaddr,(((a[6]&bits[2])==bits[2])?"true":"false"));  //总开关状态，存放在mChannel中			        		  
				        		  /* for(int i=0;i<10;i++)    //依次对十个通道进行判断，若存在于data数组中，则对开关状态进行更新
				        		   {
				        			   for(int j=0;j<data.size();j++)
				        			   {
					        			   Map<String,Object> map1=data.get(j);    //获取map对象
				        				   if(map1.get("IPAddr").equals(ipaddr)&&map1.get("Channel").equals(String.valueOf(i+1)))
				        				   {       //如果data数组中存在这个通道，则对开关的状态进行更新
				        					   map1.put("State",states[i]);
				        					   Adapter adapter=(Adapter)gridview.getAdapter();
				        					   adapter.notifyDataSetChanged();   //更新listview
				        					  
				        				   }
				        			   }
				        		   }*/
				        		   
			        		   }
			        		   /*else{
			        			  // Toast.makeText(getActivity().getApplicationContext(),s+"\ncrc:"+crc+"\n真实的:"+s.substring(14),Toast.LENGTH_SHORT).show();
			        		   }*/
		        		  
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
					progressdialog = new ProgressDialog(getActivity());    //初始化progressdialog
					progressdialog.setCancelable(true);// 设置是否可以通过点击Back键取消  
			        progressdialog.setCanceledOnTouchOutside(false);// 设置在点击Dialog外是否取消Dialog进度条
					progressdialog.show();
					progressdialog.setContentView(R.layout.progressdialog);    //设置显示的内容
					
				}
				else
				{	
					if (progressdialog != null)
					{
						
			            progressdialog.dismiss();  
			            progressdialog = null;  			          
			        }
					IsFinish=1;     
					
				}
				
			 }
			 
		};
		@Override  
		   public void setUserVisibleHint(boolean isVisibleToUser) {  
		       super.setUserVisibleHint(isVisibleToUser);  
		       if (isVisibleToUser) { 
		    	   Log.i("IBM", "scene--visible");
		    	   try {
		    		   TCPInit();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						 Toast.makeText(getActivity().getApplicationContext(),"初始化错误！",Toast.LENGTH_SHORT).show();
						e.printStackTrace();
					}
		           //相当于Fragment的onResume  
		       } else {  
		           //相当于Fragment的onPause    ,关闭所有socket连接
		    	   Log.i("IBM", "scene--disvisible");
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
		   }  
	
	@Override  
	    public View onCreateView(LayoutInflater inflater, ViewGroup container,  
	            Bundle savedInstanceState) {
		 	Bundle bundle=getArguments();
	    	RSAddr=bundle.getString("RSAddr");
	    	SharedPreferences sharedPreferences= getActivity().getSharedPreferences("data", Activity.MODE_PRIVATE);       
		    serverip =sharedPreferences.getString("serverip", "-1");   //读取服务器ip，若没有则是-1
		    serverport=Integer.valueOf(sharedPreferences.getString("port", "8028"));
	    	View view = inflater.inflate(R.layout.scene, null);
	    	DbHelper dh1=new DbHelper(this.getActivity(),"IBMS",null,1);
			SQLiteDatabase db = dh1.getWritableDatabase(); 
			data=new ArrayList<Map<String,Object>>();
		 	Cursor cursor = db.query(true, "scenename_tb", new String[]{"Name","Image"}, null, null, null, null, null, null, null);
		    while(cursor.moveToNext())   //分别添加各个房间
			 {
				 HashMap<String,Object> map = new HashMap<String,Object>();
				 /**得到Bitmap字节数据**/  
				 byte[] in = cursor.getBlob(1);  
				 /** 根据Bitmap字节数据转换成 Bitmap对象 
				 * BitmapFactory.decodeByteArray() 方法对字节数据，从0到字节的长进行解码，生成Bitmap对像。 
				 **/  
				 Bitmap bitmap = BitmapFactory.decodeByteArray(in, 0, in.length);  
				 map.put("Image", bitmap);
				 map.put("Name",cursor.getString(0));
				 data.add(map);
			 }
		    db.close();
		    Bitmap bitmap1=BitmapFactory.decodeResource(this.getActivity().getResources(), R.drawable.add);
		    HashMap<String,Object> map1=new HashMap<String,Object>();
		    map1.put("Name", "添加场景");
		    map1.put("Image", bitmap1);
		    data.add(map1);
			gridview=(GridView)view.findViewById(R.id.gridview);
			gridview.setAdapter(new Adapter());    //设置适配器
			
	    	Log.i("IBM", "scene--onCreateView");
	    	return view;
	    }
	 
	private void TCPInit() throws UnknownHostException{
		/* DbHelper dh1=new DbHelper(getActivity(),"IBMS",null,1);
		 SQLiteDatabase db = dh1.getWritableDatabase(); 
		 //先获取所有开关的ip，查询开关状态，主要目的是获取主开关的状态，方便控制
		 Cursor cursor= db.query(true, "switchs_tb", new String[]{"IPAddr"}, null, null, null, null, null, null);
		 while (cursor.moveToNext())    //将所有的开关ip添加到ipaddr数组中 
		 { */
			
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
		            String toServer = "aa68"+ RSAddr +"03 0100 0001";    //查询开关所有通道的状态
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
	                 	Log.i("IBM", "scene--TCP退出1");
	                 } catch (Exception e) {
	                     e.printStackTrace();
	                     Log.i("IBM", "scene--TCP退出2");
	                 }
	    }   
	}
	
	/***
	 * 向开关发送命令
	 * @param ipaddr   IP地址
	 * @param channel    通道号
	 * @param state     打开或关闭，1打开，0关闭
	 */
	private void SendOrder(final String ipaddr,final String channel,final String state)
	{
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
					catch (Exception ee){}			
						
			}
		}
	}.start();
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
	            final String Name=map.get("Name").toString();  //场景名
	            Bitmap bitmap=(Bitmap) map.get("Image");           //房间图标
	          //  if(convertView==null)
	            convertView = LayoutInflater.from(getActivity().getApplication()).inflate(R.layout.scenegridview, null);  
	            final TextView name=(TextView)convertView.findViewById(R.id.name);         
	            final ImageView image = (ImageView)convertView.findViewById(R.id.image); //开关图片
	            name.setText(Name);
	            bitmap= Converts.toRoundCorner(bitmap, 20);  //实现图片的圆角
	            image.setImageBitmap(bitmap);
	            image.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						if(Name.equals("添加场景"))   //如果点击的是添加场景，则添加
						{
							LayoutInflater factory = LayoutInflater.from(getActivity());  
							final View view = factory.inflate(R.layout.hoursedialog, null); 
							final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());   
						    builder.setTitle("请输入场景名称："); 	
						    builder.setView(view);
						    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {  
						         public void onClick(DialogInterface dialog, int whichButton) { 
						         EditText  tx1= (EditText) view.findViewById(R.id.tx1);  
						         String New_Name=tx1.getText().toString();
						         DbHelper dh1=new DbHelper(getActivity(),"IBMS",null,1);
								 SQLiteDatabase db = dh1.getWritableDatabase(); 
								 ContentValues cv = new ContentValues();    //内容数组
								 Cursor cursor = db.query(true, "scenename_tb", new String[]{"SID","Name"}, null, null, null, null, null, null, null);
								 int exits=0;    //数据库中是否已存在
								 if(cursor.getCount()>=1)
								 {
									 while(cursor.moveToNext())
									 {
										 if(cursor.getString(1).equals(New_Name))
											 exits=1;
									 }
								 }
								 if(exits==0&&(!New_Name.equals("添加场景")))
								 {
									 long row=-1;
									//获取房间图标的图片
									 Bitmap  bmp_room = BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.ic_scene);   
									 //获取图片输出流
									 ByteArrayOutputStream os = new ByteArrayOutputStream();  
									 bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
									 cv = new ContentValues();
									 cv.put("Name",New_Name);
									 cv.put("Image", os.toByteArray());
									 row = db.insert("scenename_tb", null, cv);  //将数据添加到数据库
									 
									 if(row>=1)
									 {
										
										 data=new ArrayList<Map<String,Object>>(); 
										 cursor = db.query(true, "scenename_tb", new String[]{"Name","Image"}, null, null, null, null, null, null, null);
										    while(cursor.moveToNext())   //分别添加各个房间
											 {
												 HashMap<String,Object> map = new HashMap<String,Object>();
												 /**得到Bitmap字节数据**/  
												 byte[] in = cursor.getBlob(1);  
												 /** 根据Bitmap字节数据转换成 Bitmap对象 
												 * BitmapFactory.decodeByteArray() 方法对字节数据，从0到字节的长进行解码，生成Bitmap对像。 
												 **/  
												 Bitmap bitmap = BitmapFactory.decodeByteArray(in, 0, in.length);  
												 map.put("Image", bitmap);
												 map.put("Name",cursor.getString(0));
												 data.add(map);
											 }
										    Bitmap bitmap1=BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.add);
										    HashMap<String,Object> map1=new HashMap<String,Object>();
										    map1.put("Name", "添加场景");
										    map1.put("Image", bitmap1);
										    data.add(map1);
										   
											Adapter.this.notifyDataSetChanged();   //刷新
										 Toast.makeText(getActivity().getApplicationContext(),"添加成功！",Toast.LENGTH_SHORT).show();
									 }
								 }
								 else
								 {
									 Toast.makeText(getActivity().getApplicationContext(),"该房间已存在，添加失败！",Toast.LENGTH_SHORT).show();
								 }
									 
								 db.close();
						        // showDialog("姓名 ："  + userName.getText().toString()  + "密码：" + password.getText().toString() );  
						         }  
						     });  
						     builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {  
						         public void onClick(DialogInterface dialog, int whichButton) {  
						 
						         }  
						     });  
						    builder.create().show();
						}
						else     //否则，执行场景中的所有命令
						{
							new Thread(){
								public void run()
								{									
									String IPAddr="";     //IP地址
									String Channel="";     //通道号
									DbHelper dh1=new DbHelper(getActivity(),"IBMS",null,1);
									SQLiteDatabase db = dh1.getWritableDatabase();     //查询该场景的所有命令，不是distinct
									Cursor cursor= db.query(false, "scene_tb", new String[]{"Name","Delay","Operation","CID"}, "Name=?", new String[]{Name}, null, null, null, null);
									if(cursor.getCount()>0)   //如果命令条数大于0，则显示加载对话框，不响应点击事件
									{
										/*Looper.prepare();
										Toast.makeText(getActivity().getApplicationContext(),cursor.getCount()+"条命令",Toast.LENGTH_SHORT).show();
										Looper.loop();*/
										Message msg=new Message();	                                        
		                                msg.what=1;   //1表示要进行显示
		                                handler2.sendMessage(msg);	       //因为要进行控制，通知主线程显示progressdialog
									}
	                                while(cursor.moveToNext())    //挑选每条命令  进行控制，直至结束
									{	
	                                	try {
											int delay=Integer.valueOf(cursor.getString(1));      //延时时间
											Thread.sleep(delay*1000);   //这里的延时是命令的要求
										} catch (Exception e) {
											// TODO Auto-generated catch block
											e.printStackTrace();											
										}
										IsFinish=0;   //标志位置为0
										/*Thread timethread=new TimeThread();
										timethread.start();*/
										time=new Date().getTime();    //获取现在的时间，单位是ms										
										
										//根据CID查询出通道的IP地址和通道号，配合scene_tb表中的延时时间和操作，对开关进行控制
										Cursor cursor1=db.query(true, "switchs_tb", new String[]{"IPAddr","Channel"},"CID=?", new String[]{cursor.getString(3)}, null, null, null, null, null);
										while(cursor1.moveToNext())    //其实此处只有一条命令，也就是说while循环只循环一次
										{
											IPAddr=cursor1.getString(0);   //ip地址
											Channel=cursor1.getString(1);    //通道号
											String state=cursor.getString(2);    //命令类型，打开或关闭
											which_ip=IPAddr;
											which=Integer.valueOf(Channel);
											//下面是指令的发送程序	       
											SendOrder(serverip,Channel.equals("10")?"a":Channel,state);
											
											while(IsFinish==0){
												if(new Date().getTime()-time>=500)
												{
													IsFinish=1;
												}
												// now=new Date().getTime();    //获取现在的时间，单位是ms
											}    //只要没完成就一直循环
											try {
												Thread.sleep(60);   //为了避免数据错乱
											} catch (InterruptedException e) {
												// TODO Auto-generated catch block
												e.printStackTrace();
											}
										}    //此条命令完成
										
										
									}//所有命令轮询完成
	                                db.close();
									Message msg=new Message();	                                        
		                            msg.what=0;   //0表示要进行关闭
		                            handler2.sendMessage(msg);	    
									
								}
							}.start();
						}
					}
	            });
	            
	            
	            image.setOnLongClickListener(new OnLongClickListener(){    //设置长点击事件

					@Override
					public boolean onLongClick(View v) {
						if(Name.equals("添加场景"))//长点击此按钮  不进行任何操作
						{
							
						}
						else
						{
							Intent intent=new Intent();
			        		intent.putExtra("Name",Name);             //场景名称
							intent.setClass(getActivity(), SettingScene_Activity.class);//设置要跳转的activity
							getActivity().startActivity(intent);//开始跳转
						}
						// TODO Auto-generated method stub
						return true;
					     }
	            	}
					);
	        
	        return convertView;
	     }
	 	
	 }
	 
	 class TimeThread extends Thread{
		 public void run(){
		 	try {
				Thread.sleep(1000);   //1s后判断
				long now=new Date().getTime();
				if(now-time>=900)
				{					
					IsFinish=1;
					/*if (progressdialog != null)
					{
			            progressdialog.dismiss();  
			            progressdialog = null;  
		            } */
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}   
		 	
		 }
		}
}
