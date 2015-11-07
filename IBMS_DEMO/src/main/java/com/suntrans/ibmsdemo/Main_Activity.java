package com.suntrans.ibmsdemo;

import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Map;

import org.ksoap2.serialization.SoapObject;

import views.ControlFragment;
import views.MainPopview;
import views.SceneFragment;

import convert.ToneLayer;

import database.DbHelper;
import WebServices.jiexi;
import WebServices.soap;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("ResourceAsColor")
public class Main_Activity extends FragmentActivity implements OnClickListener {
	private DisplayMetrics displayMetrics = new DisplayMetrics();
	private MainPopview mainpopview=null;
	private ViewPager vPager;
	private ImageView img_control,img_scene;
	private TextView tx_control,tx_scene;
	private DatagramSocket UDPclient;	
	private String Role="";   //角色号，1是管理员，2是用户
	private String RName="";   //房间名称
	private String RSAddr="9999";   //开关的485地址
	private ControlFragment control;    //控制页
	private SceneFragment scene;        //场景页
	private String[] newmac=null;    //需要添加的新开关的MAC地址数组
	private String[] newip=null;
	private String[] oldmac=null;   //数据库中已存开关的MAC地址数组	
	private long mExitTime;        //用于连按两次返回键退出      中间的时间判断   
	private ArrayList<Map<String,String>> datalist_version=new ArrayList<Map<String,String>>(); //存放最新版本信息
	private ToneLayer tone_normal,tone_selected;   //用于设置头标图片被选中时颜色改变  
	private int versioncode=1000;     //当前版本
	private String versionname="";   //版本号	
	//接收线程发送过来信息    弹出选择开关的弹出框
    public Handler handler1 = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            final AlertDialog.Builder builder = new AlertDialog.Builder(Main_Activity.this);   
		        builder.setTitle("请选择需要添加的开关："); 	
		        if(newmac!=null)
		        {
			        builder.setItems(newmac, new DialogInterface.OnClickListener() {  
			            public void onClick(DialogInterface dialog, int which) {  
			            //点击后弹出窗口选择了第几项  
			            Intent intent=new Intent();
		        		intent.putExtra("MACAddr",newmac[which]);             //点击的区域
		        		intent.putExtra("IPAddr",newip[which]);             //点击的区域
						intent.setClass(Main_Activity.this, Insert_Activity.class);//设置要跳转的activity
						Main_Activity.this.startActivity(intent);//开始跳转
						dialog.dismiss();   //跳转页面后关闭当前对话框
			            }  
			        });  
		        }
		        else
		        {
		        	builder.setMessage("当前没有新的开关可以添加");
		        	builder.setPositiveButton("确定", new DialogInterface.OnClickListener(){

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							
						}
		        		
		        	});
		        }
		        builder.create().show();
           
        }
    };
  //接收线程发送过来信息    弹出选择开关的弹出框
    public Handler handler2 = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            final AlertDialog.Builder builder = new AlertDialog.Builder(Main_Activity.this);   
		        builder.setTitle("请选择需要修改的开关："); 	
		        if(oldmac!=null)
		        {
			        builder.setItems(oldmac, new DialogInterface.OnClickListener() {  
			            public void onClick(DialogInterface dialog, int which) {  
			            //点击后弹出窗口选择了第几项  
			            Intent intent=new Intent();
		        		intent.putExtra("MACAddr",oldmac[which]);             //开关mac地址
		        		//intent.putExtra("IPAddr",newip[which]);             //点击的区域
						intent.setClass(Main_Activity.this, Update_Activity.class);//设置要跳转的activity
						Main_Activity.this.startActivity(intent);//开始跳转
						dialog.dismiss();   //跳转页面后关闭当前对话框
			            }  
			        });  
		        }
		        else
		        {
		        	builder.setMessage("当前没有开关可以修改");
		        	builder.setPositiveButton("确定", new DialogInterface.OnClickListener(){

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							
						}
		        		
		        	});
		        }
		        builder.create().show();
           
        }
    };
    private Handler handler3=new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what==1)         //代表请求数据成功
            {
            	if(datalist_version.size()>0)
            	{
	            	if(Integer.valueOf(datalist_version.get(0).get("VersionCode"))>versioncode)   //如果最新版本号大于现在的版本
					{
						final AlertDialog.Builder builder = new AlertDialog.Builder(Main_Activity.this);
						builder.setTitle("更新"+datalist_version.get(0).get("VersionName")); 
						builder.setMessage(datalist_version.get(0).get("Description"));
						builder.setPositiveButton("更新", new DialogInterface.OnClickListener() {  
						         public void onClick(DialogInterface dialog, int whichButton) {
						        	 Intent intent = new Intent();
						        	 String sUrl=datalist_version.get(0).get("URL");							
						        	 intent.setData(Uri.parse(sUrl));
									 intent.setAction(Intent.ACTION_VIEW);
									 //intent.setClassName("com.android.browser","com.android.browser.BrowserActivity");  //设置打开的浏览器 
									 //注释掉上面一行，则选择系统默认浏览器打开
									 startActivity(intent);									
						         	}
								 });
						  builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {  
						         public void onClick(DialogInterface dialog, int whichButton) {  						        	
						         }  
						     });  
						  builder.create().show();
						  //update.setClickable(true);
					}
					else   //否则，提示当前已经是最新版本，不需要更新
					{
						Toast.makeText(getApplication(), "当前应用已经是最新版本", Toast.LENGTH_SHORT).show();
						//update.setClickable(true);
					}
            	}
            	else   //否则，提示当前已经是最新版本，不需要更新
				{
					Toast.makeText(getApplication(), "当前应用已经是最新版本", Toast.LENGTH_SHORT).show();
					//update.setClickable(true);
				}
            }
            else if(msg.what==0)         //代表请求数据失败
            {
            	
            }           
        }};
	@Override     //向标题栏添加item
    public boolean onCreateOptionsMenu(Menu menu) {  
        getMenuInflater().inflate(R.menu.hourse, menu);  
        return true;  
    }  
    @Override
	protected void onResume()    //重写onResume方法 
	{
		super.onResume();
		newmac=null;    //需要添加的新开关的MAC地址数组
		newip=null;		
		control=new ControlFragment();
		scene=new SceneFragment();
		Log.i("IBM","main--OnResume");
		if(vPager.getAdapter()!=null)
		{			
			vPager.getAdapter().notifyDataSetChanged();      //对ViewPager重新进行初始化
		}
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		Intent intent=getIntent();
		RSAddr=intent.getStringExtra("RSAddr");   //485地址
		RName=intent.getStringExtra("Name");   //房间号
		Role=intent.getStringExtra("Role");  //角色号
		ActionBar actionBar = this.getActionBar();   //设置actionbar标题栏		
		actionBar.setDisplayHomeAsUpEnabled(Role.equals("1")?true:false);
		actionBar.setDisplayShowCustomEnabled(true);
		actionBar.setDisplayShowHomeEnabled(true);
		Drawable d = this.getResources().getDrawable(R.color.bg_action); 
		actionBar.setBackgroundDrawable(d);   //设置actionbar背景
		actionBar.setTitle("6栋-"+RName+"房间");
		//actionBar.setTitle(area);
		actionBar.show();		
		//Toast.makeText(getApplicationContext(), RSAddr, Toast.LENGTH_SHORT).show();
		//UDPInit();    //发送UDP指令  寻找在线开关
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);//获取屏幕大小的信息
		setContentView(R.layout.main);     //设置布局文件
		ViewInit();   //初始化控件以及绑定监听	
		new MainThread().start();   //开始访问版本信息的线程
		PackageManager pm = getApplicationContext().getPackageManager();//context为当前Activity上下文 
		PackageInfo pi;
		try {
			pi = pm.getPackageInfo(getApplicationContext().getPackageName(), 0);
			versionname = pi.versionName;
			versioncode = pi.versionCode;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		//Log.i("IBM","main--OnCreate");
	}
	public class MainThread extends Thread{
		public void run(){    //访问最新版本信息
			try{
				SoapObject result=soap.Inquiry_Version();
				datalist_version=jiexi.inquiry_version(result);				
			}
			catch(Exception e)
			{
				
			}
		}
	}
	@Override
	protected void onPause(){
		super.onPause();
		Log.i("IBM","main--OnPause");
	}
	@Override
	protected void onStop(){
		super.onStop();
		/*try{
			UDPclient.close();
			UDPclient=null;
		}
		catch(Exception e){}
		Log.i("IBM", "main--OnStop");*/
	}
	@Override
	protected void onStart(){
		super.onStart();
		Log.i("IBM", "main--OnStart");
	}
	@Override
	protected void onRestart(){
		super.onRestart();
		Log.i("IBM", "main--OnReStart");
	}
	
	public void ViewInit()
	{
		vPager=(ViewPager)findViewById(R.id.vPager);
		img_control=(ImageView)findViewById(R.id.img_control);
		tx_control=(TextView)findViewById(R.id.tx_control);
		img_scene=(ImageView)findViewById(R.id.img_scene);
		tx_scene=(TextView)findViewById(R.id.tx_scene);
		vPager.setAdapter(new MyPagerAdapter(getSupportFragmentManager()));   //设置viewpager适配器
	    vPager.setOnPageChangeListener(new MyOnPageChangeListener());   //设置页面切换监听
	    
	    img_control.setColorFilter(0xff008000);    //设置滤镜,绿色
	    img_scene.setColorFilter(Color.GRAY);      //设置滤镜，灰色
	    //设置监听
	    img_control.setOnClickListener(new MyOnClickListener(0));
        img_scene.setOnClickListener(new MyOnClickListener(1));
        
        tx_control.setOnClickListener(new MyOnClickListener(0));
        tx_scene.setOnClickListener(new MyOnClickListener(1));
        
	}
	//viewpager适配器
	public class MyPagerAdapter extends FragmentPagerAdapter {     //viewpager适配器

				public MyPagerAdapter(FragmentManager fm) {
					super(fm);
				}

				private final String[] titles = { "控制", "场景"};
				@Override
				public CharSequence getPageTitle(int position) {
					return titles[position];    //获得与标题号对应的标题名
				}

				@Override
				public int getCount() {
					return titles.length;     //一共有几个头标
				}

				@Override
				public Fragment getItem(int position) {
					Bundle bundle = new Bundle();
					bundle.putString("RSAddr", RSAddr);
					switch (position) {
					case 0:          //第一个fragment
						if (control == null) {
							control= new ControlFragment();
						}													
						control.setArguments(bundle);
						return control;
					case 1:              //第二个fragment
						if (scene == null) {
							scene = new SceneFragment();
						}								
						scene.setArguments(bundle);
						
						return scene;
					default:
						return null;
					}
				}
				@Override  
				public int getItemPosition(Object object) {  
				    return POSITION_NONE;  
				}  

			}
	    /**
	     * 头标点击监听
	*/
	    public class MyOnClickListener implements OnClickListener {
	        private int index = 0;

	        public MyOnClickListener(int i) {
	            index = i;
	        }

	        @Override
	        public void onClick(View v) {
	        	switch(v.getId())   //判断按下的按钮id，设置标题两个textview的背景颜色
	        	{
	        		case R.id.tx_control:
	        		case R.id.img_control:
	        		{
	        			tx_control.setTextColor(Color.parseColor("#008000"));  //绿色
	        			img_control.clearColorFilter();    //先清除之前的滤镜效果
	        			img_control.setColorFilter(0xff008000);                //绿色
	        			tx_scene.setTextColor(Color.GRAY);     //灰色
	        			img_scene.clearColorFilter();   //清除之前的滤镜效果
	        			img_scene.setColorFilter(Color.GRAY);  //灰色
	        			Main_Activity.this.getActionBar().setTitle("房间");   
	        			break;
	        		}
	        		case R.id.tx_scene:
	        		case R.id.img_scene:
	        		{
	        			tx_control.setTextColor(Color.GRAY);     //灰色
	        			img_control.clearColorFilter();    //先清除之前的滤镜效果
	        			img_control.setColorFilter(Color.GRAY);  //灰色
	        			tx_scene.setTextColor(Color.parseColor("#008000"));  //绿色
	        			img_scene.clearColorFilter();   //清除之前的滤镜效果
	        			img_scene.setColorFilter(0xff008000);    //绿色
	        			Main_Activity.this.getActionBar().setTitle("场景");   
						break;
	        		}
	        		default:break;
	        	}
	            vPager.setCurrentItem(index);   //根据头标选择的内容  对viewpager进行页面切换
	        }
	    };
	    
	    
	    /**
	     * 页卡切换监听
	*/
	    public class MyOnPageChangeListener implements OnPageChangeListener {
	        @Override
	        public void onPageSelected(int arg0) {
	        	 switch(arg0)   //根据页面滑到哪一页，设置标题两个textview的背景颜色
	        	 {
	        	 	case 0:
	        	 	{
	        	 		tx_control.setTextColor(Color.parseColor("#008000"));  //绿色
	        			img_control.clearColorFilter();    //先清除之前的滤镜效果
	        			img_control.setColorFilter(0xff008000);                //绿色
	        			tx_scene.setTextColor(Color.GRAY);     //灰色
	        			img_scene.clearColorFilter();   //清除之前的滤镜效果
	        			img_scene.setColorFilter(Color.GRAY);  //灰色
	        			Main_Activity.this.getActionBar().setTitle("6栋-"+RName+"房间");   
	        			break;
	        		}
	        	 	case 1:
	        	 	{
	        	 		tx_control.setTextColor(Color.GRAY);     //灰色
	        			img_control.clearColorFilter();    //先清除之前的滤镜效果
	        			img_control.setColorFilter(Color.GRAY);  //灰色
	        			tx_scene.setTextColor(Color.parseColor("#008000"));  //绿色
	        			img_scene.clearColorFilter();   //清除之前的滤镜效果
	        			img_scene.setColorFilter(0xff008000);    //绿色
	        			Main_Activity.this.getActionBar().setTitle("6栋-"+RName+"场景");   
						break;
	        		}
	        	 	default:break;
	        	 }
	        }
	        @Override
	        public void onPageScrolled(int arg0, float arg1, int arg2) {
	        }

	        @Override
	        public void onPageScrollStateChanged(int arg0) {
	        }
	    }
	
		public void Redirect(String area)       //实现页面跳转，跳转到area页面，传入的参数是区域名
		{
			Intent intent=new Intent();
    		intent.putExtra("area",area);             //点击的区域
			intent.setClass(Main_Activity.this, Room_Activity.class);//设置要跳转的activity
			Main_Activity.this.startActivity(intent);//开始跳转
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
					                 String clientip=packet.getAddress().toString().replace("/", "");	//ip地址
					                 String clientmac=new String(packet.getData()).replace("+OK=", "");  //MAC地址
					                 clientmac=clientmac.replaceAll("\r|\n", "");    //去掉换行符
					                 clientmac=clientmac.replace(" ", "");   //去掉空格
					                 clientmac=clientmac.substring(8,12);
					                 //Toast.makeText(getApplicationContext(),"接收到的是"+clientmac+"!",Toast.LENGTH_SHORT).show();
					                 DbHelper dh1=new DbHelper(Main_Activity.this,"IBMS",null,1);
					     			 SQLiteDatabase db = dh1.getWritableDatabase(); 
					     			Cursor cursor = db.query("switchs_tb", new String[]{"MACAddr"}, "MACAddr=?", new String[]{clientmac}, null, null, null);
					     			//String str=String.valueOf(cursor.getCount())+"行数据\n";
					     			/*if(cursor.getCount()<10)     //如果数据库中不存在这个开关，则向新开关数组中添加开关
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
					    			  }
					     			else   //如果数据库中已经存在，则根据MACAddr地址修改开关的IPAddr
					     			{
					     				ContentValues cv=new ContentValues();
										cv.put("IPAddr", clientip);
										db.update("switchs_tb", cv, "MACAddr=?", new String[]{clientmac});   //更新数据库中对应MAC地址的ip地址
					     			}*/
					     			 //直接将数据库中的开关信息改成现在的mac地址和ip地址
					     			ContentValues cv=new ContentValues();
					     			cv.put("MACAddr", clientmac);
					     			cv.put("IPAddr", clientip);
									db.update("switchs_tb", cv, "", null);   //更新数据库中对应MAC地址的ip地址
					     			db.close();
				                 }   
				                 Log.i("IBM", "main--UDP退出1");
			                 } catch (Exception e) {
			                     e.printStackTrace();
			                     Log.i("IBM", "main--UDP退出2");
			                 }
			             }
		            
		      }
		
		 //连按两次返回键退出
		 public boolean onKeyDown(int keyCode, KeyEvent event) {  
				// TODO Auto-generated method stub
				if (keyCode == KeyEvent.KEYCODE_BACK) {    //返回键
					
					if(Role.equals("2"))  //如果是用户，则显示功能菜单
		            {
						if ((System.currentTimeMillis() - mExitTime) > 2000) {// 如果两次按键时间间隔大于2000毫秒，则不退出
						Toast.makeText(this, "再按一次退出智控", Toast.LENGTH_SHORT).show();
						mExitTime = System.currentTimeMillis();// 更新mExitTime
						} else {
						System.exit(0);// 否则退出程序
						}
						return true;
					}
					else
					{
						finish();
						return true;
					}
				}
				else if(keyCode==KeyEvent.KEYCODE_MENU)    //menu键，弹出菜单
				{  
					if(Role.equals("2"))  //如果是用户，则显示功能菜单
		            {
				    //实例化SelectPicPopupWindow  
		            mainpopview = new MainPopview(Main_Activity.this, new OnClickListener(){  
		                	  
		                    public void onClick(View v) {  		 
		                    	
		                    		
			                        mainpopview.dismiss(); 
			                        mainpopview=null;
			                        switch (v.getId()) {  
			                       /* case R.id.tx_tiaozhuan:     //跳转到武大用电app
			                        {
			                        	//ComponentName componentName = new ComponentName("com.suntrans.whu", "Main_Activity");  
			                        	PackageManager packagemanager=getPackageManager();
			                        	Intent intent = packagemanager.getLaunchIntentForPackage("com.suntrans.whu");
			                        	String userName="2014281050791";
			                        	String publickey="0wdjkjkdj"+userName;
			                        	String time=Long.toString(java.lang.System.currentTimeMillis());
			                        	String key=convert.Converts.md5(time+publickey);
			                        	String params="{\"userName\":\""+userName+"\",\"wakeUpFrom\":\"whu\",\"time\":\""+time+"\",\"key\":\""+key+"\"";
			                        	String encryptparams=Base64.encodeToString(params.getBytes(),Base64.DEFAULT);
			                        	Bundle bundle=new Bundle();
			                        	bundle.putString("basecode", encryptparams);
			                        	intent.putExtra("bundle",bundle);
			                        	//intent.setComponent(componentName);  
			                        	Main_Activity.this.startActivity(intent); 	                        	
			                            break;  
			                        }*/
			                        case R.id.version:  //版本更新
			                        {
			                        	v.setClickable(false);	
			            				if(datalist_version.size()>0)   //如果已经获取到了新版本的信息
			            				{
			            					if(Integer.valueOf(datalist_version.get(0).get("VersionCode"))>versioncode)   //如果最新版本号大于现在的版本
			            					{
			            						final AlertDialog.Builder builder = new AlertDialog.Builder(Main_Activity.this);
			            						builder.setTitle("更新"+datalist_version.get(0).get("VersionName")); 
			            						builder.setMessage(datalist_version.get(0).get("Description"));
			            						builder.setPositiveButton("更新", new DialogInterface.OnClickListener() {  
			            						         public void onClick(DialogInterface dialog, int whichButton) {
			            						        	 Intent intent = new Intent();
			            						        	 String sUrl=datalist_version.get(0).get("URL");							
			            						        	 intent.setData(Uri.parse(sUrl));
			            									 intent.setAction(Intent.ACTION_VIEW);
			            									 //intent.setClassName("com.android.browser","com.android.browser.BrowserActivity");  //设置打开的浏览器 
			            									 //注释掉上面一行，则选择系统默认浏览器打开
			            									 startActivity(intent);									
			            						         	}
			            								 });
			            						  builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {  
			            						         public void onClick(DialogInterface dialog, int whichButton) {  						        	
			            						         }  
			            						     });  
			            						  builder.create().show();
			            						  v.setClickable(true);
			            					}
			            					else   //否则，提示当前已经是最新版本，不需要更新
			            					{
			            						Toast.makeText(getApplication(), "当前应用已经是最新版本", Toast.LENGTH_SHORT).show();
			            						v.setClickable(true);
			            					}
			            				}
			            				else   //如果没有，重新获取信息
			            				{
			            					new Thread(){
			            						public void run(){    //访问最新版本信息
			            							try{
			            								SoapObject result=soap.Inquiry_Version();
			            								datalist_version=jiexi.inquiry_version(result);
			            								Message msg=new Message();
			            								msg.what=1;   //成功
			            								handler3.sendMessage(msg);
			            							}
			            							catch(Exception e)
			            							{
			            								Message msg=new Message();
			            								msg.what=0;   //失败
			            								handler3.sendMessage(msg);
			            							}
			            						}
			            					}.start();
			            					v.setClickable(true);
			            				}
			            				break;
			            				
			                        }
			                       /* case R.id.inquery:     //查询在线设备
			                        {
			                        	Intent intent=new Intent();
			    						intent.setClass(Main_Activity.this, Online_Activity.class);//设置要跳转的activity
			    						Main_Activity.this.startActivity(intent);//开始跳转
			                        	//Toast.makeText(Main_Activity.this, "点击了查询命令", Toast.LENGTH_SHORT).show();
			                            break;  
			                        }*/
			                        case R.id.config:   //配置服务器端口
			                        {
			                        	Intent intent=new Intent();
			    						intent.setClass(Main_Activity.this,ServerConfig_Activity.class);//设置要跳转的activity
			    						Main_Activity.this.startActivity(intent);//开始跳转			    						
			                        	break;
			                        }
			                        case R.id.logout:    //注销登录
			                        {
			                        	Intent intent=new Intent();
			    						intent.setClass(Main_Activity.this, LogIn_Activity.class);//设置要跳转的activity
			    						Main_Activity.this.startActivity(intent);//开始跳转
			    						finish();
			                        	break;
			                        }
			                        case R.id.update:    //修改密码
			                        {
			                        	Intent intent=new Intent();
			    						intent.setClass(Main_Activity.this, Password_Activity.class);//设置要跳转的activity
			    						Main_Activity.this.startActivity(intent);//开始跳转
			                        	break;
			                        }
			                        case R.id.backto:    //恢复默认配置
			                        {
			                        	Backto();
			                        	//UDPInit();
			                        	control=new ControlFragment();
			                    		scene=new SceneFragment();
			                    		//Log.i("IBM","main--OnResume");
			                    		if(vPager.getAdapter()!=null)
			                    		{			
			                    			vPager.getAdapter().notifyDataSetChanged();      //对ViewPager重新进行初始化
			                    		}
			                        }
			                        case KeyEvent.KEYCODE_MENU:   //菜单键
			                        {
			                        	try{
			                        	mainpopview.dismiss();
			                        	}
			                        	catch(Exception e){}
			                        	break;
			                        }
			                        default:  
			                            break;  
			                        }   
			                    }}  );  
			                
			                //显示窗口  
			                mainpopview.showAtLocation(Main_Activity.this.findViewById(R.id.main), Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 0); //设置layout在PopupWindow中显示的位置
							
		            }   
		            return true;
					
					
				}
					return super.onKeyDown(keyCode, event);
				}
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			
		}
		
		@Override           //menu选项监听
		public boolean onMenuItemSelected(int featureId, MenuItem item)
		{
			if(item.getItemId() == android.R.id.home)    //如果按下的是返回键
		    {
		        finish();
		        return true;
		    }
			else if(item.getItemId()==R.id.action_update) //按下的是修改键
			{
				oldmac=null;     //每次点击都将数组清空
				/*DbHelper dh1=new DbHelper(Main_Activity.this,"IBMS",null,1);
	 			SQLiteDatabase db = dh1.getWritableDatabase(); 
	 			Cursor cursor = db.query(true, "switchs_tb", new String[]{"MACAddr"}, null, null, null, null, null, null, null);
	 			while (cursor.moveToNext()) 
	 			{ 
	 				if(oldmac==null)   //如果还是空的 ，就添加第一个开关
	 				{
	 					oldmac=new String[1];		     					
	 					oldmac[0]=cursor.getString(0);   //获取第一列的数据
	 					
	 				}
	 				else            //如果不是空的，证明数组中有数据，再加一条即可
	 				{
	 					String[] oldmac1=oldmac.clone();  //一个中间变量用来存储		     					
	 					oldmac=new String[oldmac.length+1];//将数据组的长度加1			     					
	 					for(int j=0;j<oldmac1.length;j++)
	 					{
	 						oldmac[j]=oldmac1[j];		     						
	 					}
	 					oldmac[oldmac.length-1]=cursor.getString(0);	//增加一条记录	     					
	 				}
	 			}
	 			db.close();
	 			Message msg=new Message();	                                        
	            handler2.sendMessage(msg);*/
				Intent intent=new Intent();
        	//	intent.putExtra("MACAddr",oldmac[which]);             //开关mac地址
        		//intent.putExtra("IPAddr",newip[which]);             //点击的区域
				intent.setClass(Main_Activity.this, Update_Activity.class);//设置要跳转的activity
				Main_Activity.this.startActivity(intent);//开始跳转
				return true;
			}
			
			/*else if(item.getItemId()==R.id.action_add) //按下的是添加键
			{
				 Message msg=new Message();	                                        
	             handler1.sendMessage(msg);
				
				return true;
			}*/
			
			else
				return true;
		}
		
		//恢复到默认配置的操作
		private void Backto()
		{
			DbHelper dh1=new DbHelper(Main_Activity.this,"IBMS",null,1);
			SQLiteDatabase db = dh1.getWritableDatabase();
			db.beginTransaction();
			db.delete("room_tb", "", null);
			db.delete("switchs_tb", "", null);
			db.delete("scene_tb", "", null);
			db.delete("scenename_tb", "", null);
			String sql1="DELETE FROM sqlite_sequence WHERE name = 'switchs_tb'";
			db.execSQL(sql1);
			Cursor cursor = db.query(true, "room_tb", new String[]{"RID","Name"}, null, null, null, null, null, null, null);
			//获取房间图标的图片
			Bitmap  bmp_room = BitmapFactory.decodeResource(Main_Activity.this.getResources(), R.drawable.room);   
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
			bmp_room = BitmapFactory.decodeResource(Main_Activity.this.getResources(), R.drawable.switchoff);   
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
				bmp_room = BitmapFactory.decodeResource(Main_Activity.this.getResources(), R.drawable.bulb1); 
				os = new ByteArrayOutputStream();  
				bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
				ContentValues cva = new ContentValues();
				cva.put("IPAddr","192.168.1.1");
				cva.put("State","false");
				cva.put("Type","无");
				cva.put("MACAddr","9999");
				cva.put("IValue","0");
				cva.put("Channel",1);
				cva.put("Area","主房间");
				cva.put("Name","门前灯");
				cva.put("Image", os.toByteArray());
				row = db.insert("switchs_tb", null, cva);  //将数据添加到数据库
				
				//第二个通道
				bmp_room = BitmapFactory.decodeResource(Main_Activity.this.getResources(), R.drawable.bulb1); 
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
				bmp_room = BitmapFactory.decodeResource(Main_Activity.this.getResources(), R.drawable.bulb);  
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
				bmp_room = BitmapFactory.decodeResource(Main_Activity.this.getResources(), R.drawable.bulb1); 
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
				bmp_room = BitmapFactory.decodeResource(Main_Activity.this.getResources(), R.drawable.bulb); 
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
				bmp_room = BitmapFactory.decodeResource(Main_Activity.this.getResources(), R.drawable.bulb1); 
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
				bmp_room = BitmapFactory.decodeResource(Main_Activity.this.getResources(), R.drawable.airconditioner);   
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
				bmp_room = BitmapFactory.decodeResource(Main_Activity.this.getResources(), R.drawable.television);
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
				bmp_room = BitmapFactory.decodeResource(Main_Activity.this.getResources(), R.drawable.heater); 
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
				bmp_room = BitmapFactory.decodeResource(Main_Activity.this.getResources(), R.drawable.fan); 
				os = new ByteArrayOutputStream();  
				bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
				cva = new ContentValues();
				cva.put("IPAddr","192.168.1.1");
				cva.put("State","false");
				cva.put("Type","无");
				cva.put("MACAddr","9999");
				cva.put("IValue","0");
				cva.put("Channel",10);
				cva.put("Area","卫生间");
				cva.put("Name","换气扇");
				cva.put("Image", os.toByteArray());
				row = db.insert("switchs_tb", null, cva);  //将数据添加到数据库			
			}
			
			bmp_room = BitmapFactory.decodeResource(Main_Activity.this.getResources(), R.drawable.ic_scene);   
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
}
