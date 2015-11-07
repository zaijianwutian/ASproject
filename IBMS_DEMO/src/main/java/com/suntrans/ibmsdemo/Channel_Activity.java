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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import views.Switch;
import views.Switch.OnSwitchChangedListener;

import convert.Converts;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class Channel_Activity extends Activity {
	private String ipaddr;
	private ListView listview1;	
	private Socket client;
	private Switch s1;   //一键控制开关
	private byte[] bits={(byte)0x01,(byte)0x02,(byte)0x04,(byte)0x08,(byte)0x10,(byte)0x20,(byte)0x40,(byte)0x80};     //从1到8只有一位是1，用于按位与计算，获取某一位的值
	private ArrayList<Map<String, String>> data=new ArrayList<Map<String, String>>();    //列表显示的内容
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
		         Log.i("Order","收到数据"+s);
		           if(s.substring(6, 8).equals("06"))    //根据第6,7个字符判断返回的是单个通道还是所有通道
	        	   {     //如果是单个通道
		        	   if(crc.equals(s.substring(16,20)))     //判断crc校验码是否正确
		        	   {
		        		   int k=0;         //k是通道号
		        		   int state=Integer.valueOf(s.substring(15, 16));  //开关状态，1代表打开，0代表关闭
		        		   if(s.substring(11,12).equals("a"))
		        			   k=10;
		        		   else
		        			   k=Integer.valueOf(s.substring(11, 12));
		        		  
		        			   if(k==0)   
		        			   {
		        				   s1.setState(state==1?true:false);
		        				   if(state==0) //如果是主开关关闭，则关闭所有通道
		        				   {
			        				   for(int i=0;i<=10;i++)
			        				   {
				        				   Map<String,String> map1=data.get(i);    //获取map对象
				        				   map1.put("State","false");
			        				   }
			        				   Adapter adapter=(Adapter)listview1.getAdapter();
			        				   adapter.notifyDataSetChanged();   //更新listview
		        				   }
		        				   else
		        				   {
		        					   Map<String,String> map1=data.get(0);    //获取map对象
			        				   map1.put("State","true");
			        				   Adapter adapter=(Adapter)listview1.getAdapter();
			        				   adapter.notifyDataSetChanged();   //更新listview
		        				   }
		        				   
		        			   }
		        			   else
		        			   {
		        				   Map<String,String> map1=data.get(k);    //获取map对象
		        				   map1.put("State",(state==1?"true":"false"));
		        				   Adapter adapter=(Adapter)listview1.getAdapter();
		        				   adapter.notifyDataSetChanged();   //更新listview
		        			   }
		        				   //Toast.makeText(getApplicationContext(),"接收到"+ipaddr+":"+s,Toast.LENGTH_SHORT).show();
		        				   
		   
		        		   
		        	   }
	        	   }
	        	   else if(s.substring(6, 8).equals("03"))
	        	   {    //如果是全部通道
	        		   if(crc.equals(s.substring(14,18)))     //判断crc校验码是否正确
	        		   {        			   
		        		   String[] states={"false","false","false","false","false","false","false","false","false","false","false"};   //十个通道的状态，state[0]对应1通道
		        		   for(int i=0;i<8;i++)   //先获取前八位的开关状态
		        		   {
		        			   states[i+1]=((a[6]&bits[i])==bits[i])?"true":"false";   //1-8通道
		        		   }
		        		   for(int i=0;i<2;i++)
		        		   {
		        			   states[i+9]=((a[5]&bits[i])==bits[i])?"true":"false";  //9、10通道
		        		   }
		        		   states[0]=(((a[5]&bits[2])==bits[2])?"true":"false");  //总开关状态，存放在mChannel中
		          		   s1.setState(((a[5]&bits[2])==bits[2])?true:false);
		        		   for(int i=0;i<10;i++)    //依次对十个通道进行更新
		        		   {		        			   
			        			   Map<String,String> map1=data.get(i);    //获取map对象
		        				   map1.put("State",states[i]);
		        				   Adapter adapter=(Adapter)listview1.getAdapter();
		        				   adapter.notifyDataSetChanged();   //更新listview
		        		   }
		        		   Map<String,String> map2=data.get(10);    //获取map对象
        				   map2.put("State",states[10]);
        				   Adapter adapter=(Adapter)listview1.getAdapter();
        				   adapter.notifyDataSetChanged();   //更新listview
	        		   }	
	        	   }
	        	   else if(s.substring(6, 8).equals("10"))   //如果是控制多个寄存器的指令，则代表全部打开。因为全部关闭发送的是关闭主开关的命令
	        	   {    //如果是多个寄存器
	        		   //Toast.makeText(getApplicationContext(),crc+"\n返回的是:"+s.substring(14),Toast.LENGTH_SHORT).show();
	        		   if(crc.equals(s.substring(16,20)))     //判断crc校验码是否正确
	        		   {
	        			   
		          		   for(int i=0;i<11;i++)    //依次对十个通道进行判断，若存在于data数组中，则对开关状态进行更新
		        		   {		        			   
			        			   Map<String,String> map1=data.get(i);    //获取map对象
		        				   map1.put("State","true");
		        		   }
		        		   
        				   Adapter adapter=(Adapter)listview1.getAdapter();
        				   adapter.notifyDataSetChanged();   //更新listview
	        		   }	
	        	   }
		        	   
		           
		     }
		};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		Intent intent=this.getIntent();    //获取Intent
		ipaddr=(String)intent.getStringExtra("IPAddr");   //获取IP地址
		Log.i("Order",ipaddr);
		ActionBar actionBar = this.getActionBar();   //设置actionbar标题栏
		//actionBar.setCustomView(R.layout.title);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setDisplayShowCustomEnabled(true);
		actionBar.setDisplayShowHomeEnabled(true);
		actionBar.setTitle("开关控制");
		Drawable d = this.getResources().getDrawable(R.color.bg_action); 
		actionBar.setBackgroundDrawable(d);   //设置actionbar背景
		actionBar.show();
		setContentView(R.layout.channel);       //设置activity布局文件
		Map<String,String> map=new HashMap<String,String>();
		map.put("Name", "总开关");
		map.put("State", "false");
		data.add(map);
		Map[] maps=new Map[11];
		for(int i=1;i<=10;i++)
		{
			maps[i]=new HashMap<String,String>();
			maps[i].put("Name", "通道"+String.valueOf(i));
			maps[i].put("State", "false");
			data.add(maps[i]);
		}
		listview1=(ListView)findViewById(R.id.listview1);   //得到listview1	
		listview1.setAdapter(new Adapter());      //设置listview适配器，为自定义适配器
		s1=(Switch)findViewById(R.id.s1);
		s1.setOnChangeListener(new OnSwitchChangedListener(){   //监听一键控制开关

				@Override                //开关状态监听
				public void onSwitchChange(Switch switchView,final boolean isChecked) {
					new Thread(){      //不能在主线程中访问网络，所以要新建线程
						public void run(){ 
							try 
							{	
								if(isChecked)   //打开指令
								{
									DataOutputStream out=new DataOutputStream(client.getOutputStream()); 
								    String channel="0";
								    String toServer = "aa68 0010 0300 000b 16 0001 0001 0001 0001 0001 0001 0001 0001 0001 0001 0001 ";    //打开所有通道，11个
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
								}
								else           //关闭指令
								{
								    DataOutputStream out=new DataOutputStream(client.getOutputStream()); 
								    String channel="0";
								    String toServer = "aa68 0006 0300 0000";    //关闭指令
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
								}
							}
							catch(Exception e){
								
							}
						}}.start();
				}
            });
		try {
			TCPInit();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
			            // 把用户输入的内容发送给server  
			            String toServer = "aa68 0003 0100 0001";    //查询开关所有通道的状态
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
	    String ipaddr="";
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
    	
    				try {
						client.close();
						client=null;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
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
            convertView = LayoutInflater.from(getApplication()).inflate(R.layout.arealistview, null);  
            final TextView name = (TextView)convertView.findViewById(R.id.name); 
            final Switch switch1 = (Switch)convertView.findViewById(R.id.switch1); 
            name.setText(data.get(position).get("Name"));    //通道号
            switch1.setState(Boolean.parseBoolean(data.get(position).get("State")));    //设置开关状态
            convertView.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					
					 //Toast.makeText(getApplicationContext(),"IP是"+data.get(position).get("IPAddr")+"；通道号是："+data.get(position).get("Channel"),Toast.LENGTH_SHORT).show();
				}});
            switch1.setOnChangeListener(new OnSwitchChangedListener(){

				@Override                //开关状态监听
				public void onSwitchChange(Switch switchView,final boolean isChecked) {
					// TODO Auto-generated method stub
					//Toast.makeText(getApplicationContext(),"你刚刚"+(isChecked?"打开":"关闭")+"了开关",Toast.LENGTH_SHORT).show();
					new Thread(){      //不能在主线程中访问网络，所以要新建线程
						public void run(){ 
							try 
							{
							    DataOutputStream out=new DataOutputStream(client.getOutputStream()); 
							    String channel="0";
							    if(position==10)
							    	channel="a";
							    else
							    	channel=String.valueOf(position);
					            // 把用户输入的内容发送给server  
					            String toServer = "aa68 0006 030"+channel+" 000"+(isChecked?"1":"0");    //指令
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
							}
							catch(Exception e){
								
							}
						}}.start();
				}
            });
            return convertView;
        }
    }
						
}
