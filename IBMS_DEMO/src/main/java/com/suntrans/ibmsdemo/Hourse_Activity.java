package com.suntrans.ibmsdemo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.ksoap2.serialization.SoapObject;

import views.MainPopview;

import convert.Converts;

import database.DbHelper;
import WebServices.jiexi;
import WebServices.soap;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
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
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("ResourceAsColor")
public class Hourse_Activity extends Activity {
	private DisplayMetrics displayMetrics = new DisplayMetrics();
	private MainPopview mainpopview=null;
	private GridView gridview;
	private DatagramSocket UDPclient;	
	private String[] newmac=null;    //需要添加的新开关的MAC地址数组
	private String[] newip=null;
	private String[] oldmac=null;   //数据库中已存开关的MAC地址数组	
	private int result_code=0;   //确定需要更换图标的是data中的哪一个
	private int IsShowing=0;     //popwindow是否正在显示，1表示正在显示，0表示没有显示
	private ArrayList<Map<String, Object>> data=new ArrayList<Map<String, Object>>();    //列表显示的内容,存放着通道号、ip地址、开关状态等
	private ArrayList<Map<String,String>> datalist_version=new ArrayList<Map<String,String>>(); //存放最新版本信息
	
	private int versioncode=1000;     //当前版本
	private String versionname="";   //版本号	
	 private long mExitTime;        //用于连按两次返回键退出      中间的时间判断   
		//接收线程发送过来信息    弹出选择开关的弹出框
	    public Handler handler1 = new Handler() {
	        public void handleMessage(Message msg) {
	            super.handleMessage(msg);
	            final AlertDialog.Builder builder = new AlertDialog.Builder(Hourse_Activity.this);   
			        builder.setTitle("请选择需要添加的开关："); 	
			        if(newmac!=null)
			        {
				        builder.setItems(newmac, new DialogInterface.OnClickListener() {  
				            public void onClick(DialogInterface dialog, int which) {  
				            //点击后弹出窗口选择了第几项  
				            Intent intent=new Intent();
			        		intent.putExtra("MACAddr",newmac[which]);             //点击的区域
			        		intent.putExtra("IPAddr",newip[which]);             //点击的区域
							intent.setClass(Hourse_Activity.this, Insert_Activity.class);//设置要跳转的activity
							Hourse_Activity.this.startActivity(intent);//开始跳转
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
	            final AlertDialog.Builder builder = new AlertDialog.Builder(Hourse_Activity.this);   
			        builder.setTitle("请选择需要修改的开关："); 	
			        if(oldmac!=null)
			        {
				        builder.setItems(oldmac, new DialogInterface.OnClickListener() {  
				            public void onClick(DialogInterface dialog, int which) {  
				            //点击后弹出窗口选择了第几项  
				            Intent intent=new Intent();
			        		intent.putExtra("MACAddr",oldmac[which]);             //点击的区域
			        		//intent.putExtra("IPAddr",newip[which]);             //点击的区域
							intent.setClass(Hourse_Activity.this, Update_Activity.class);//设置要跳转的activity
							Hourse_Activity.this.startActivity(intent);//开始跳转
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
							final AlertDialog.Builder builder = new AlertDialog.Builder(Hourse_Activity.this);
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
	 /*@Override     //向标题栏添加item
	 public boolean onCreateOptionsMenu(Menu menu) {  
	        getMenuInflater().inflate(R.menu.hourse, menu);  
	        return true;  
	 }  */
	 
	 @Override
	 protected void onResume()    //重写onResume方法 
	{
			super.onResume();
			
	}
	  
	 @Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);//获取屏幕大小的信息
		ActionBar actionBar = this.getActionBar();   //设置actionbar标题栏		
		actionBar.setDisplayHomeAsUpEnabled(false);
		actionBar.setDisplayShowCustomEnabled(true);
		actionBar.setDisplayShowHomeEnabled(true);
		Drawable d = this.getResources().getDrawable(R.color.bg_action); 
		actionBar.setBackgroundDrawable(d);   //设置actionbar背景
		actionBar.setTitle("6栋所有房间");
		actionBar.show();
		setContentView(R.layout.hourse);     //设置布局文件
		DbHelper dh1=new DbHelper(Hourse_Activity.this,"IBMS",null,1);
		SQLiteDatabase db = dh1.getWritableDatabase(); 
	 	Cursor cursor = db.query(true, "users_tb", new String[]{"NID","Name","RSAddr"}, "Name!= ? and Name!=?", new String[]{"admin","1111"}, null, null, null, null, null);
		
		 while(cursor.moveToNext())   //分别添加各个房间
		 {
			 HashMap<String,Object> map = new HashMap<String,Object>();			
			 map.put("name", cursor.getString(1));
			 map.put("rid",cursor.getString(0));
			 map.put("rsaddr",cursor.getString(2));
			 data.add(map);
		 }	   
		 HashMap<String,Object> map1=new HashMap<String,Object>();
		 map1.put("name", "自定义开关");
		 map1.put("rid", "null");
		 map1.put("rsaddr", "9999");
		 data.add(map1);
		gridview=(GridView)findViewById(R.id.gridview);
		gridview.setAdapter(new Adapter());
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
	 
	 //连按两次返回键退出
	 public boolean onKeyDown(int keyCode, KeyEvent event) {  
			// TODO Auto-generated method stub
			if (keyCode == KeyEvent.KEYCODE_BACK) {    //返回键
				
				if ((System.currentTimeMillis() - mExitTime) > 2000) {// 如果两次按键时间间隔大于2000毫秒，则不退出
				Toast.makeText(this, "再按一次退出智控", Toast.LENGTH_SHORT).show();
				mExitTime = System.currentTimeMillis();// 更新mExitTime
				} else {
				System.exit(0);// 否则退出程序
				}
				return true;
				}
			else if(keyCode==KeyEvent.KEYCODE_MENU)    //menu键，弹出菜单
			{  
				
			    //实例化SelectPicPopupWindow  
	            mainpopview = new MainPopview(Hourse_Activity.this, new OnClickListener(){  
	                	  
	                    public void onClick(View v) {  
	                        mainpopview.dismiss(); 
	                        mainpopview=null;
	                        switch (v.getId()) { 
	                        case R.id.version:  //版本更新
	                        {
	                        	v.setClickable(false);	
	            				if(datalist_version.size()>0)   //如果已经获取到了新版本的信息
	            				{
	            					if(Integer.valueOf(datalist_version.get(0).get("VersionCode"))>versioncode)   //如果最新版本号大于现在的版本
	            					{
	            						final AlertDialog.Builder builder = new AlertDialog.Builder(Hourse_Activity.this);
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
	                        case R.id.config:   //配置服务器端口
	                        {
	                        	Intent intent=new Intent();
	    						intent.setClass(Hourse_Activity.this,ServerConfig_Activity.class);//设置要跳转的activity
	    						Hourse_Activity.this.startActivity(intent);//开始跳转			    						
	                        	break;
	                        }
	                        case R.id.logout:    //注销登录
	                        {
	                        	Intent intent=new Intent();
	    						intent.setClass(Hourse_Activity.this, LogIn_Activity.class);//设置要跳转的activity
	    						Hourse_Activity.this.startActivity(intent);//开始跳转
	    						finish();
	                        	break;
	                        }
	                        case R.id.update:    //修改密码
	                        {
	                        	Intent intent=new Intent();
	    						intent.setClass(Hourse_Activity.this, Password_Activity.class);//设置要跳转的activity
	    						Hourse_Activity.this.startActivity(intent);//开始跳转
	                        	break;
	                        }
	                        case R.id.backto:    //恢复默认配置
	                        {
	                        	Backto();                        	
	                    		
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
	                mainpopview.showAtLocation(Hourse_Activity.this.findViewById(R.id.main), Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 0); //设置layout在PopupWindow中显示的位置
					return true;
				
				
			}
				return super.onKeyDown(keyCode, event);
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
            final String Rname=map.get("name").toString();  //房间名
            final String Rid=map.get("rid").toString();     //房间号       
            final String RSAddr=map.get("rsaddr").toString();  //485地址
            convertView = LayoutInflater.from(getApplication()).inflate(R.layout.hoursegridview, null);  
            final TextView name=(TextView)convertView.findViewById(R.id.name);         
            final ImageView image = (ImageView)convertView.findViewById(R.id.image); //开关图片
            if(Rname.equals("自定义开关"))
            	name.setText(Rname);
            else
            	name.setText("6栋-"+Rname);          
            Bitmap bmp_room = BitmapFactory.decodeResource(Hourse_Activity.this.getResources(), R.drawable.room);
            bmp_room=Converts.toRoundCorner(bmp_room, 20);  //图片圆角
            image.setImageBitmap(bmp_room);
            image.setOnClickListener(new OnClickListener(){   //设置点击事件

				@SuppressLint("ResourceAsColor")
				@Override
				public void onClick(View v) {					
					//Toast.makeText(getApplicationContext(),"你要"+Rname,Toast.LENGTH_SHORT).show();
					if(Rname.equals("自定义开关"))
					{
						LayoutInflater factory = LayoutInflater.from(Hourse_Activity.this);  
						final View view = factory.inflate(R.layout.hoursedialog, null); 
						final AlertDialog.Builder builder = new AlertDialog.Builder(Hourse_Activity.this);   
					    builder.setTitle("请输入4位16进制开关地址(小写)："); 	
					    builder.setView(view);
					    builder.setCancelable(true);
					    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {  
					         public void onClick(DialogInterface dialog, int whichButton) { 
					        	 EditText  tx1= (EditText) view.findViewById(R.id.tx1);  
						         String new_resaddr=tx1.getText().toString();
					        	 Intent intent=new Intent();
						    	 intent.putExtra("Name","自定义");             //房间号
						    	 intent.putExtra("RSAddr", new_resaddr);
						    	 intent.putExtra("Role","1");    //角色号=1，代表管理员，=2代表用户
			   					 intent.setClass(Hourse_Activity.this, Main_Activity.class);//设置要跳转的activity
								 Hourse_Activity.this.startActivity(intent);//开始跳转
					         }  
					     });  
					     builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {  
					         public void onClick(DialogInterface dialog, int whichButton) {  
					 
					         }  
					     });  
					    builder.create().show();
					}
					else
					{
						Intent intent=new Intent();
			    		intent.putExtra("Name",Rname);             //房间号
			    		intent.putExtra("RSAddr", RSAddr);
			    		intent.putExtra("Role","1");    //角色号=1，代表管理员，=2代表用户
   						intent.setClass(Hourse_Activity.this, Main_Activity.class);//设置要跳转的activity
						Hourse_Activity.this.startActivity(intent);//开始跳转
					}
				}
 				
            	
            });           
            return convertView;
            }
    	}
	
	@Override  
    protected void onActivityResult(int requestCode, int resultCode, Intent data1) {  
		super.onActivityResult(requestCode, resultCode, data1);
		if (resultCode != RESULT_CANCELED) 
		{ 
			if(requestCode==10000)//如果是刚刚选择完，还未裁剪，则跳转到裁剪的activity
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
		                        	data.get(result_code).put("image",image);
		                        	DbHelper dh1=new DbHelper(Hourse_Activity.this,"IBMS",null,1);
									SQLiteDatabase db = dh1.getWritableDatabase(); 
									ContentValues cv = new ContentValues();    //内容数组
									ByteArrayOutputStream os = new ByteArrayOutputStream();  
									image.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
									cv.put("Image", os.toByteArray());
									db.update("room_tb", cv, "RID=?", new String[]{data.get(result_code).get("rid").toString()});
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
		                        	data.get(result_code).put("image",image);
		                        	DbHelper dh1=new DbHelper(Hourse_Activity.this,"IBMS",null,1);
									SQLiteDatabase db = dh1.getWritableDatabase(); 
									ContentValues cv = new ContentValues();    //内容数组
									ByteArrayOutputStream os = new ByteArrayOutputStream();  
									image.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
									cv.put("Image", os.toByteArray());
									db.update("room_tb", cv, "RID=?", new String[]{data.get(result_code).get("rid").toString()});
									((Adapter)gridview.getAdapter()).notifyDataSetChanged();   //刷新
		                        }  
		                    }  
		                }
	        		}
		        }
	        
		}
		else   //如果点击了取消，则判断是不是裁剪的activity，若是则返回图片选择或拍照的页面，若不是则不进行操作
		{
			if(requestCode==10000)//如果是在选择图片中，点击了取消，不进行操作
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
	intent.putExtra("outputX", convert.Converts.dip2px(getApplicationContext(), 100));
	intent.putExtra("outputY", convert.Converts.dip2px(getApplicationContext(), 100));
	intent.putExtra("return-data", false);
	startActivityForResult(intent, result_code);
	}
	
	//恢复到默认配置的操作
			private void Backto()
			{
				DbHelper dh1=new DbHelper(Hourse_Activity.this,"IBMS",null,1);
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
				Bitmap  bmp_room = BitmapFactory.decodeResource(Hourse_Activity.this.getResources(), R.drawable.room);   
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
				bmp_room = BitmapFactory.decodeResource(Hourse_Activity.this.getResources(), R.drawable.switchoff);   
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
					bmp_room = BitmapFactory.decodeResource(Hourse_Activity.this.getResources(), R.drawable.bulb1); 
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
					bmp_room = BitmapFactory.decodeResource(Hourse_Activity.this.getResources(), R.drawable.bulb1); 
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
					bmp_room = BitmapFactory.decodeResource(Hourse_Activity.this.getResources(), R.drawable.bulb);  
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
					bmp_room = BitmapFactory.decodeResource(Hourse_Activity.this.getResources(), R.drawable.bulb1); 
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
					bmp_room = BitmapFactory.decodeResource(Hourse_Activity.this.getResources(), R.drawable.bulb); 
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
					bmp_room = BitmapFactory.decodeResource(Hourse_Activity.this.getResources(), R.drawable.bulb1); 
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
					bmp_room = BitmapFactory.decodeResource(Hourse_Activity.this.getResources(), R.drawable.airconditioner);   
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
					bmp_room = BitmapFactory.decodeResource(Hourse_Activity.this.getResources(), R.drawable.television);
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
					bmp_room = BitmapFactory.decodeResource(Hourse_Activity.this.getResources(), R.drawable.heater); 
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
					bmp_room = BitmapFactory.decodeResource(Hourse_Activity.this.getResources(), R.drawable.fan); 
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
				
				bmp_room = BitmapFactory.decodeResource(Hourse_Activity.this.getResources(), R.drawable.ic_scene);   
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
