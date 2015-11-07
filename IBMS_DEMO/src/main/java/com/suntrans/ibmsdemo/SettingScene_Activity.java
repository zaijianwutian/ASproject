package com.suntrans.ibmsdemo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import wheel.TosGallery;
import wheel.TosGallery.OnEndFlingListener;
import wheel.WheelView;

import database.DbHelper;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class SettingScene_Activity extends Activity {
	private String SName="";      //场景名称
	private EditText name;    //名称EditText
	private ListView list;
	private ImageView image,addimage;   //场景图标、“添加”图标
	private WheelView wheel_operation,wheel_area,wheel_name;
	private EditText delay;
	private String[] Operation={"关闭","打开"};   //三个String数组  是添加命令时弹出的dialog显示的内容
	private String[] Area=null;
	private String[] RName=null;
	private ArrayList<Map<String,Object>> data=new ArrayList<Map<String,Object>>();   //数组集合
	 @Override
		protected void onCreate(Bundle savedInstanceState) {
			// TODO Auto-generated method stub
			super.onCreate(savedInstanceState);
			Intent intent=getIntent();
			SName=intent.getStringExtra("Name");    //获取场景名称
		    ActionInit();    //初始化标题栏，并设置监听
			setContentView(R.layout.setting_scene);     //设置布局文件
			
			DbHelper dh1=new DbHelper(this,"IBMS",null,1);
			SQLiteDatabase db = dh1.getWritableDatabase(); 
		 	Cursor cursor = db.query("scene_tb", new String[]{"Name","Image","SID","CID","Delay","Operation"},  "Name=?", new String[]{SName}, null, null, null);
			name=(EditText)findViewById(R.id.name);
			list=(ListView)findViewById(R.id.list);
			image=(ImageView)findViewById(R.id.image);
			addimage=(ImageView)findViewById(R.id.addimage);
			name.setText(SName);
			Bitmap bitmap=null;
			while(cursor.moveToNext())
			{
				Map<String,Object> map=new HashMap<String,Object>();
				map.put("Name",SName);
				map.put("SID", cursor.getString(2));
				map.put("CID", cursor.getString(3));
				map.put("Delay", cursor.getString(4));
				map.put("Operation",cursor.getString(5));
				data.add(map);
			}
			cursor = db.query("scenename_tb", new String[]{"Name","Image"},  "Name=?", new String[]{SName}, null, null, null);
			while(cursor.moveToNext())
			{
				byte[] in = cursor.getBlob(1);
				bitmap = BitmapFactory.decodeByteArray(in, 0, in.length); 
				
			}
			cursor=db.query(true,"room_tb", new String[]{"Name"},null,null, null, null, null, null);
			Area=new String[cursor.getCount()];
			int j=0;
			while(cursor.moveToNext())
			{
				Area[j]=cursor.getString(0);
				j++;
			}			
			if(Area.length>=1)
			{
				cursor=db.query(true,"switchs_tb", new String[]{"Name"},"Area=?",new String[]{Area[0]}, null, null, null, null);
				if(cursor.getCount()>0)
				{
					RName=new String[cursor.getCount()];
					j=0;
					while(cursor.moveToNext())
					{
						RName[j]=cursor.getString(0);
						j++;
					}
				}
				else
					RName=new String[]{"无"};
			}
			else
			{
				Area=new String[]{"无"};
				RName=new String[]{"无"};
			}
			db.close();
			image.setImageBitmap(bitmap);
			image.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					 final AlertDialog.Builder builder = new AlertDialog.Builder(SettingScene_Activity.this);   
					builder.setTitle("更换图标："); 	
				    builder.setItems(new String[]{"本地图库","拍照","取消"}, new DialogInterface.OnClickListener() {  
			            public void onClick(DialogInterface dialog, int which) {  
			            //点击后弹出窗口选择了第几项  
			            	//Toast.makeText(getApplicationContext(),"你点击了第"+which+"个图标",Toast.LENGTH_SHORT).show();
			            	switch(which)
			            	{
				            	case 0:    //选择本地图库
				            		{
				            			//打开图库
				      				    Intent i = new Intent(
				      					Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
				      					startActivityForResult(i,10000); //请求码为10000，  用来区分图片是裁剪前的还是裁剪后的
				            			break;
				            		}
				            	case 1:    //选择拍照
				            		{
				            			//拍照
				        				Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);  
				                        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);  
				                        startActivityForResult(intent, 10000);  //请求码为10000， 用来区分图片是裁剪前的还是裁剪后的
				            			break;
				            		}
				            	case 2://点击取消
				            	{
				            		break;
				            	}
				            	default:break;
			            	}}});
				    builder.create().show();
				}
				
			});
			list.setAdapter(new Adapter());   //为listview设置适配器
			addimage.setOnClickListener(new OnClickListener(){
                   //添加命令
				@Override
				public void onClick(View v) {
					
					//先挑选出房间和开关数组，
					DbHelper dh1=new DbHelper(SettingScene_Activity.this,"IBMS",null,1);
					final SQLiteDatabase db1 = dh1.getWritableDatabase(); 
					//db1.beginTransaction();
					Cursor cursor=db1.query(true,"room_tb", new String[]{"Name"},null,null, null, null, null, null);
					Area=new String[cursor.getCount()+1];
					Area[0]="所有房间";
					int j=0;
					while(cursor.moveToNext())
					{
						Area[j+1]=cursor.getString(0);
						j++;
					}			
					//if(Area.length>=1)
				//{
					//选出所有的通道名称
						cursor=db1.query(true,"switchs_tb", new String[]{"Name"},"Name!=?",new String[]{"无"}, null, null, null, null);
						if(cursor.getCount()>0)
						{
							RName=new String[cursor.getCount()];
							j=0;
							while(cursor.moveToNext())
							{
								RName[j]=cursor.getString(0);
								j++;
							}
						}
						else
							RName=new String[]{"无"};
				//	}
					/*else
					{
						Area=new String[]{"无"};
						RName=new String[]{"无"};
					}*/
					
					//设置滚动显示的布局，然后打开dialog，让用户选择命令
					// TODO Auto-generated method stub
					LayoutInflater factory = LayoutInflater.from(SettingScene_Activity.this);  
					final View view = factory.inflate(R.layout.setting_scenedialog, null); 
					delay=(EditText)view.findViewById(R.id.delay);
					wheel_operation=(WheelView)view.findViewById(R.id.wheel_operation);
					wheel_area=(WheelView)view.findViewById(R.id.wheel_area);
					wheel_name=(WheelView)view.findViewById(R.id.wheel_name);
					wheel_operation.setAdapter(new WheelAdapter(Operation));
					wheel_area.setAdapter(new WheelAdapter(Area));
					wheel_name.setAdapter(new WheelAdapter(RName));
					//为区域  设置选中监听
					wheel_area.setOnEndFlingListener(new OnEndFlingListener(){

						
						@Override
						public void onEndFling(TosGallery v) {     //结束滚动
							// TODO Auto-generated method stub
							if(Area.length>0)
							{
								try{
									Cursor cursor;
									if(v.getSelectedItemPosition()==0)
										cursor=db1.query(true,"switchs_tb", new String[]{"Name"},"Name!=?",new String[]{"无"}, null, null, null, null);
	
									else
										cursor=db1.query(true,"switchs_tb", new String[]{"Name"},"Area=?",new String[]{Area[v.getSelectedItemPosition()]}, null, null, null, null);
									if(cursor.getCount()>0)
									{
										RName=new String[cursor.getCount()];
										int k=0;
										while(cursor.moveToNext())
										{
											RName[k]=cursor.getString(0);
											k++;
										}
									}
									else
										RName=new String[]{"无"};
									wheel_name.setAdapter(new WheelAdapter(RName));
									
									wheel_name.setSelection(0);
								}
								catch(Exception e){}
							}
							//Toast.makeText(getApplicationContext(),"滚动结束!",Toast.LENGTH_SHORT).show();
							}});
					final AlertDialog.Builder builder = new AlertDialog.Builder(SettingScene_Activity.this);   
				    builder.setTitle("添加控制命令："); 	
				    builder.setView(view);
				    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {  
				         public void onClick(DialogInterface dialog, int whichButton) 
				         { 
				        	 if(RName.length>0&&!RName[0].equals("无"))
				        	 {
				        		 Cursor cursor;
					        	 if(wheel_area.getSelectedItemPosition()==0)   //所有房间
					        		 cursor=db1.query(false,"switchs_tb", new String[]{"CID"}, "Name=?", new String[]{RName[wheel_name.getSelectedItemPosition()]}, null, null,null,null);
					        	 else
					        		 cursor=db1.query(false,"switchs_tb", new String[]{"CID"}, "Area=? and Name=?", new String[]{Area[wheel_area.getSelectedItemPosition()],RName[wheel_name.getSelectedItemPosition()]}, null,null, null,null);
					             while(cursor.moveToNext())
					             {
						        	 Map<String,Object> map=new HashMap<String,Object>();
						        	 map.put("Name", SName);
						        	 map.put("Delay", delay.getText().toString());
						        	 map.put("Operation", wheel_operation.getSelectedItemPosition());
						        	 map.put("CID", cursor.getString(0));
						        	 data.add(map);
					             }
					             ((Adapter)list.getAdapter()).notifyDataSetChanged();
				        	 }
				        	 else
				        		 Toast.makeText(getApplicationContext(), "添加命令失败", Toast.LENGTH_SHORT).show();
				        	 //db1.setTransactionSuccessful();       //设置事务处理成功，不设置会自动回滚不提交
				        	 //db1.endTransaction();
				        	// db1.close();
				       }
						 
				        // showDialog("姓名 ："  + userName.getText().toString()  + "密码：" + password.getText().toString() );  
				         
				     });  
				     builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {  
				         public void onClick(DialogInterface dialog, int whichButton) {  
				        	// db1.setTransactionSuccessful();       //设置事务处理成功，不设置会自动回滚不提交
				        	// db1.endTransaction();
				        	// db1.close();
				         }  
				     });  
				    builder.create().show();
				}});
	 }
	 private void ActionInit(){
			ActionBar actionBar = SettingScene_Activity.this.getActionBar();   //设置actionbar标题栏		
			//自定义标题栏
			actionBar.setDisplayShowHomeEnabled( false );
			actionBar.setDisplayShowCustomEnabled(true);

			LayoutInflater inflator = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View v = inflator.inflate(R.layout.setting_title, null);
			TextView cancel,title,save,delete;
			cancel=(TextView)v.findViewById(R.id.cancel);
			title=(TextView)v.findViewById(R.id.title);
			save=(TextView)v.findViewById(R.id.save);
			delete=(TextView)v.findViewById(R.id.delete);
			
			title.setText(SName);
		    //取消按钮，设置监听
			cancel.setOnClickListener( new OnClickListener(){

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					finish();
				}
				
			});
			
			//保存按钮，设置监听
			save.setOnClickListener( new OnClickListener(){

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					BitmapDrawable bd = (BitmapDrawable) (image.getDrawable());
					Bitmap bitmap = bd.getBitmap();
					//获取图片输出流
					ByteArrayOutputStream os = new ByteArrayOutputStream();  
					bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
					DbHelper dh1=new DbHelper(SettingScene_Activity.this,"IBMS",null,1);
					SQLiteDatabase db = dh1.getWritableDatabase(); 
					db.beginTransaction();       //手动设置开始事务   ，这样只打开一次数据库，一次性将数据写入,防止多次打开和关闭数据库，节省时间 
	        		
					ContentValues cv=new ContentValues();
					cv.put("Name",name.getText().toString());
					cv.put("Image", os.toByteArray());
					db.update("scenename_tb", cv, "Name=?", new String[]{SName});
					
					db.delete("scene_tb", "Name=?", new String[]{SName});
					ContentValues[] cvs=new ContentValues[data.size()];
					for(int i=0;i<data.size();i++)
					{
						cvs[i]=new ContentValues();
						Map<String,Object> map=data.get(i);
						cvs[i].put("Name", name.getText().toString());
						cvs[i].put("Delay", map.get("Delay").toString());
						cvs[i].put("Operation", map.get("Operation").toString());
						cvs[i].put("CID", map.get("CID").toString());
						db.insert("scene_tb",null, cvs[i]);
					}
					db.setTransactionSuccessful();       //设置事务处理成功，不设置会自动回滚不提交
	    			db.endTransaction();       //处理完成
					db.close();
					Toast.makeText(getApplicationContext(),"保存成功!",Toast.LENGTH_SHORT).show();
					
				}
				
			});
			//删除按钮，设置监听
			delete.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					 final AlertDialog.Builder builder = new AlertDialog.Builder(SettingScene_Activity.this);   
				        builder.setTitle("确定要删除场景:"+SName+"?"); 	       
				        builder.setMessage("点击确定删除当前场景");
				        builder.setPositiveButton("确定", new DialogInterface.OnClickListener(){
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub
								DbHelper dh1=new DbHelper(SettingScene_Activity.this,"IBMS",null,1);
								SQLiteDatabase db = dh1.getWritableDatabase(); 
								db.delete("scene_tb", "Name=?", new String[]{SName});
								db.delete("scenename_tb", "Name=?", new String[]{SName});
								db.close();
								Toast.makeText(getApplicationContext(),"场景"+SName+"已成功删除!",Toast.LENGTH_SHORT).show();
								finish();
							}
				        		
				        });
				        builder.setNegativeButton("取消", new DialogInterface.OnClickListener(){
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub
									
							}
				        		
				        });
				        builder.create().show();
				}
				
			});
			
			
			ActionBar.LayoutParams layout_actionbar = new ActionBar.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
			actionBar.setCustomView(v,layout_actionbar);	
		    actionBar.show();
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
	 
	 private class Adapter extends BaseAdapter {   
		    private Context context;                        //运行上下文   
		    private LayoutInflater listContainer;           //视图容器   
		    public int getCount() {   
		        // TODO Auto-generated method stub   
		        return data.size();   
		    }   
		  
		    public Object getItem(int arg0) {   
		        // TODO Auto-generated method stub   
		        return null;   
		    }   
		  
		    public long getItemId(int arg0) {   
		        // TODO Auto-generated method stub   
		        return 0;   
		    }   
		          
		    /**  
		     * ListView Item设置  
		     */  
		    public View getView(final int position, View convertView, ViewGroup parent) {   
		        // TODO Auto-generated method stub   
		    	//获取list_item布局文件的视图   
		    	if(convertView==null)
		    		convertView = LayoutInflater.from(SettingScene_Activity.this).inflate(R.layout.scenelistview, null);  
	           //获取控件对象   
	            TextView delay= (TextView)convertView.findViewById(R.id.delay);   
	            TextView operation = (TextView)convertView.findViewById(R.id.operation);   
	            TextView name = (TextView)convertView.findViewById(R.id.name);   
	            ImageView clear = (ImageView)convertView.findViewById(R.id.clear);   
	           	Map<String,Object> map=data.get(position);
	           	delay.setText(map.get("Delay").toString()+"秒后");
	           	operation.setText(map.get("Operation").toString().equals("1")?"打开":"关闭");
	           	String cid=map.get("CID").toString();
	           	//final String sid=map.get("SID").toString();
	           	DbHelper dh1=new DbHelper(SettingScene_Activity.this,"IBMS",null,1);
				final SQLiteDatabase db = dh1.getWritableDatabase(); 
			 	Cursor cursor = db.query("switchs_tb", new String[]{"Area","Name","CID"},  "CID=?", new String[]{cid}, null, null, null);
		       	String Area="无";
				while(cursor.moveToNext())
			    {
			        Area=cursor.getString(0)+cursor.getString(1);
			    }
				db.close();
				name.setText(Area);
				//为删除按钮设置监听函数
				clear.setOnClickListener(new OnClickListener(){

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						data.remove(position);
						
						Adapter.this.notifyDataSetChanged();  //刷新
					}
					
				});
		        
		         
		        return convertView;   
		    }   
		    
	 }
		    @Override
		    public void onActivityResult(int requestCode, int resultCode, Intent data1) {  
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
			                        image = MediaStore.Images.Media.getBitmap(getContentResolver(), mImageCaptureUri);  
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
				                    Bitmap btm;  
				                    try {  
				                        //这个方法是根据Uri获取Bitmap图片的静态方法  
				                        btm = MediaStore.Images.Media.getBitmap(getContentResolver(), mImageCaptureUri);  
				                        if (btm != null) {  
				                        	image.setImageBitmap(btm);
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
				                        Bitmap btm = extras.getParcelable("data");  
				                        if (btm != null) {  
				                        	image.setImageBitmap(btm);
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
			intent.putExtra("aspectX", 3);
			intent.putExtra("aspectY", 2);
			// outputX outputY 是裁剪图片宽高
			intent.putExtra("outputX", convert.Converts.dip2px(SettingScene_Activity.this.getApplicationContext(), 150));   //宽
			intent.putExtra("outputY", convert.Converts.dip2px(SettingScene_Activity.this.getApplicationContext(), 100));   //高
			intent.putExtra("return-data", false);
			startActivityForResult(intent, 1);
			}
		 
			//滚动选择条的适配器
			 private class WheelAdapter extends BaseAdapter {
			        int mHeight = 50;
			        private String[] mData=null;     //要显示的数组内容

			        public WheelAdapter(String[] mdata) {
			           // mHeight = (int) Utils.px2dip(SettingScene_Activity.this, mHeight);
			            mData=new String[mdata.length];
			            for(int i=0;i<mdata.length;i++)
			            {
			            	mData[i]=mdata[i];
			            }
			        }

			        @Override
			        public int getCount() {
			            return (null != mData) ? mData.length : 0;
			        }

			        @Override
			        public Object getItem(int arg0) {
			            return null;
			        }

			        @Override
			        public long getItemId(int arg0) {
			            return 0;
			        }

			        @Override
			        public View getView(int position, View convertView, ViewGroup parent) {
			            TextView txtView = null;

			            if (null == convertView) {
			                convertView = new TextView(SettingScene_Activity.this);
			                convertView.setLayoutParams(new TosGallery.LayoutParams(-1, mHeight));
			               
			                txtView = (TextView) convertView;
			                txtView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
			                
			                txtView.setTextColor(Color.WHITE);
			                txtView.setGravity(Gravity.CENTER);
			            }

			            String text = String.valueOf(mData[position]);
			            if (null == txtView) {
			                txtView = (TextView) convertView;
			            }

			            txtView.setText(text);

			            return convertView;
			        }
			    }

		 

}
