package views;

import com.suntrans.ibmsdemo.Room_Activity;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.suntrans.ibmsdemo.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import database.DbHelper;

public class ControlFragment extends Fragment {
	private int result_code=0;   //确定需要更换图标的是data中的哪一个
	private GridView gridview;
	private String RSAddr="9999";
	private ArrayList<Map<String, Object>> data=new ArrayList<Map<String, Object>>();    //列表显示的内容,存放着通道号、ip地址、开关状态等
	 @Override  
	    public View onCreateView(LayoutInflater inflater, ViewGroup container,  
	            Bundle savedInstanceState) {
		 	Bundle bundle=getArguments();
	    	RSAddr=bundle.getString("RSAddr");
	    	View view = inflater.inflate(R.layout.hourse, null);
	    	data=new ArrayList<Map<String,Object>>();
	    	DbHelper dh1=new DbHelper(this.getActivity(),"IBMS",null,1);
			SQLiteDatabase db = dh1.getWritableDatabase(); 
		 	Cursor cursor = db.query(true, "room_tb", new String[]{"RID","Name","Image"}, null, null, null, null, null, null, null);
			 HashMap<String,Object> map1 = new HashMap<String,Object>();
			 Bitmap  bitmap = BitmapFactory.decodeResource(this.getActivity().getResources(), R.drawable.room);
			 map1.put("image",bitmap);
			 map1.put("name", "所有房间");
			 map1.put("rid","all");
			 data.add(map1);  //所有房间
			 while(cursor.moveToNext())   //分别添加各个房间
			 {
				 HashMap<String,Object> map = new HashMap<String,Object>();
				 /**得到Bitmap字节数据**/  
				 byte[] in = cursor.getBlob(2);  
				 /** 根据Bitmap字节数据转换成 Bitmap对象 
				 * BitmapFactory.decodeByteArray() 方法对字节数据，从0到字节的长进行解码，生成Bitmap对像。 
				 **/  
				 bitmap = BitmapFactory.decodeByteArray(in, 0, in.length);  
				 map.put("image", bitmap);
				 map.put("name", cursor.getString(1));
				 map.put("rid",cursor.getString(0));
				 data.add(map);
			 }
			 db.close();
		    HashMap<String,Object> map2 = new HashMap<String,Object>();
			bitmap = BitmapFactory.decodeResource(this.getActivity().getResources(), R.drawable.add);
			map2.put("image",bitmap);
			map2.put("name", "添加房间");
			map2.put("rid","null");
			data.add(map2);     //“添加房间”
			gridview=(GridView)view.findViewById(R.id.gridview);
			gridview.setAdapter(new Adapter());    //设置适配器
	    	 
	    	
	    	return view;
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
	            Bitmap bitmap=(Bitmap) map.get("image");           //房间图标
	            if(convertView==null)
	            	convertView = LayoutInflater.from(getActivity().getApplication()).inflate(R.layout.hoursegridview, null);  
	            final TextView name=(TextView)convertView.findViewById(R.id.name);         
	            final ImageView image = (ImageView)convertView.findViewById(R.id.image); //开关图片
	            name.setText(Rname);
	            bitmap=convert.Converts.toRoundCorner(bitmap, 20);  //实现图片的圆角
	            image.setImageBitmap(bitmap);
	            image.setOnClickListener(new OnClickListener(){   //设置点击事件

					@SuppressLint("ResourceAsColor")
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						//Toast.makeText(getApplicationContext(),"你要"+Rname,Toast.LENGTH_SHORT).show();
						if(Rname.equals("添加房间"))
						{
							LayoutInflater factory = LayoutInflater.from(getActivity());  
							final View view = factory.inflate(R.layout.hoursedialog, null); 
							final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());   
						    builder.setTitle("请输入房间名称："); 	
						    builder.setView(view);
						    builder.setCancelable(true);
						    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {  
						         public void onClick(DialogInterface dialog, int whichButton) { 
						         EditText  tx1= (EditText) view.findViewById(R.id.tx1);  
						         String New_Name=tx1.getText().toString();
						         DbHelper dh1=new DbHelper(getActivity(),"IBMS",null,1);
								 SQLiteDatabase db = dh1.getWritableDatabase(); 
								 ContentValues cv = new ContentValues();    //内容数组
								 Cursor cursor = db.query(true, "room_tb", new String[]{"RID","Name"}, null, null, null, null, null, null, null);
								 int exits=0;    //数据库中是否已存在
								 while(cursor.moveToNext())
								 {
									 if(cursor.getString(1).equals(New_Name))
										 exits=1;
								 }
								 if(exits==0)
								 {
									 long row=-1;
									//获取房间图标的图片
									 Bitmap  bmp_room = BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.room);   
									 //获取图片输出流
									 ByteArrayOutputStream os = new ByteArrayOutputStream();  
									 bmp_room.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
									 cv = new ContentValues();
									 cv.put("Name",New_Name);
									 cv.put("Image", os.toByteArray());
									 row = db.insert("room_tb", null, cv);  //将数据添加到数据库
									 
									 if(row>=1)
									 {
										 cursor = db.query(true, "room_tb", new String[]{"RID","Name"}, null, null, null, null, null, null, null);
										 data=new ArrayList<Map<String,Object>>(); 
											cursor = db.query(true, "room_tb", new String[]{"RID","Name","Image"}, null, null, null, null, null, null, null);
											 HashMap<String,Object> map1 = new HashMap<String,Object>();
											 Bitmap  bitmap = BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.room);
											 map1.put("image",bitmap);
											 map1.put("name", "所有房间");
											 map1.put("rid","all");
											 data.add(map1);  //所有房间
											 while(cursor.moveToNext())   //分别添加各个房间
											 {
												 HashMap<String,Object> map = new HashMap<String,Object>();
												 /**得到Bitmap字节数据**/  
												 byte[] in = cursor.getBlob(2);  
												 /** 根据Bitmap字节数据转换成 Bitmap对象 
												 * BitmapFactory.decodeByteArray() 方法对字节数据，从0到字节的长进行解码，生成Bitmap对像。 
												 **/  
												 bitmap = BitmapFactory.decodeByteArray(in, 0, in.length);  
												 map.put("image", bitmap);
												 map.put("name", cursor.getString(1));
												 map.put("rid",cursor.getString(0));
												 data.add(map);
											 }
										    HashMap<String,Object> map2 = new HashMap<String,Object>();
											bitmap = BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.add);
											map2.put("image",bitmap);
											map2.put("name", "添加房间");
											map2.put("rid","null");
											data.add(map2);     //“添加房间”
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
						else
						{
							Intent intent=new Intent();
				    		intent.putExtra("area",Rname);             //点击的区域
				    		intent.putExtra("RSAddr", RSAddr);   //开关485地址
							intent.setClass(getActivity(), Room_Activity.class);//设置要跳转的activity
							getActivity().startActivity(intent);//开始跳转
						}
	 				}
	            	
	            });
	            image.setOnLongClickListener(new OnLongClickListener(){    //设置长点击事件
					@Override
					public boolean onLongClick(View v) {
						if(Rname.equals("所有房间")||Rname.equals("添加房间"))
						{
							return true;
						}
						else
						{
							final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());   
						    builder.setTitle("编辑房间信息："); 	
						    builder.setItems(new String[]{"修改名称","更换图标","删除房间"}, new DialogInterface.OnClickListener() {  
					            public void onClick(DialogInterface dialog, int which) {  
					            //点击后弹出窗口选择了第几项  
					            	//Toast.makeText(getApplicationContext(),"你点击了第"+which+"个图标",Toast.LENGTH_SHORT).show();
					            	switch(which)
					            	{
						            	case 0:  //修改名称
						            	{
						            		LayoutInflater factory = LayoutInflater.from(getActivity());  
											final View view = factory.inflate(R.layout.hoursedialog, null); 
											final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());   
										    builder.setTitle("请输入房间名称："); 	
										    builder.setView(view);
										    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {  
										         public void onClick(DialogInterface dialog, int whichButton) {
										        	 EditText  tx1= (EditText) view.findViewById(R.id.tx1); 
										        	 String New_Name=tx1.getText().toString();
											         DbHelper dh1=new DbHelper(getActivity(),"IBMS",null,1);
													 SQLiteDatabase db = dh1.getWritableDatabase(); 
													 ContentValues cv = new ContentValues();    //内容数组
													 Cursor cursor = db.query(true, "room_tb", new String[]{"RID","Name"}, null, null, null, null, null, null, null);
													 int exits=0;    //数据库中是否已存在
													 while(cursor.moveToNext())
													 {
														 if(cursor.getString(1).equals(New_Name))
															 exits=1;
													 }
													 if(exits==0&&(!New_Name.equals("所有房间"))&&(!New_Name.equals("添加房间")))   //如果不存在，则进行修改
													 {
														cv.put("Name", New_Name);
														db.update("room_tb", cv, "RID=?", new String[]{Rid});
														cv=new ContentValues();
														cv.put("Area",New_Name);
														db.update("switchs_tb",cv,"Area=?",new String[]{Rname});
														data=new ArrayList<Map<String,Object>>(); 
														cursor = db.query(true, "room_tb", new String[]{"RID","Name","Image"}, null, null, null, null, null, null, null);
														 HashMap<String,Object> map1 = new HashMap<String,Object>();
														 Bitmap  bitmap = BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.room);
														 map1.put("image",bitmap);
														 map1.put("name", "所有房间");
														 map1.put("rid","all");
														 data.add(map1);  //所有房间
														 while(cursor.moveToNext())   //分别添加各个房间
														 {
															 HashMap<String,Object> map = new HashMap<String,Object>();
															 /**得到Bitmap字节数据**/  
															 byte[] in = cursor.getBlob(2);  
															 /** 根据Bitmap字节数据转换成 Bitmap对象 
															 * BitmapFactory.decodeByteArray() 方法对字节数据，从0到字节的长进行解码，生成Bitmap对像。 
															 **/  
															 bitmap = BitmapFactory.decodeByteArray(in, 0, in.length);  
															 map.put("image", bitmap);
															 map.put("name", cursor.getString(1));
															 map.put("rid",cursor.getString(0));
															 data.add(map);
														 }
													    HashMap<String,Object> map2 = new HashMap<String,Object>();
														bitmap = BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.add);
														map2.put("image",bitmap);
														map2.put("name", "添加房间");
														map2.put("rid","null");
														data.add(map2);     //“添加房间”
														 Adapter.this.notifyDataSetChanged();   //刷新
														Toast.makeText(getActivity().getApplicationContext(),"修改成功！",Toast.LENGTH_SHORT).show();
													 }
													 else
													 {
														 Toast.makeText(getActivity().getApplicationContext(),"房间名已存在，修改失败！",Toast.LENGTH_SHORT).show();
														
													 }
													 db.close();												
													 }
												 });
										    builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {  
										         public void onClick(DialogInterface dialog, int whichButton) {  
										 
										         }  
										     });  
										    builder.create().show();
												 
						            		break;
						            	}
						            	case 1:    //更换图标 ，弹出对话框，用户选择本地图库或拍照上传 
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
						            		break;
						            	}
						            	case 2:    //删除开关
						            		{
						            			DbHelper dh1=new DbHelper(getActivity(),"IBMS",null,1);
												SQLiteDatabase db = dh1.getWritableDatabase(); 
												db.delete("room_tb", "RID=?", new String[]{Rid});
												data=new ArrayList<Map<String,Object>>();
												Cursor cursor = db.query(true, "room_tb", new String[]{"RID","Name","Image"}, null, null, null, null, null, null, null);
												 HashMap<String,Object> map1 = new HashMap<String,Object>();
												 Bitmap  bitmap = BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.room);
												 map1.put("image",bitmap);
												 map1.put("name", "所有房间");
												 map1.put("rid","all");
												 data.add(map1);  //所有房间
												 while(cursor.moveToNext())   //分别添加各个房间
												 {
													 HashMap<String,Object> map = new HashMap<String,Object>();
													 /**得到Bitmap字节数据**/  
													 byte[] in = cursor.getBlob(2);  
													 /** 根据Bitmap字节数据转换成 Bitmap对象 
													 * BitmapFactory.decodeByteArray() 方法对字节数据，从0到字节的长进行解码，生成Bitmap对像。 
													 **/  
													 bitmap = BitmapFactory.decodeByteArray(in, 0, in.length);  
													 map.put("image", bitmap);
													 map.put("name", cursor.getString(1));
													 map.put("rid",cursor.getString(0));
													 data.add(map);
												 }
												 db.close();
											    HashMap<String,Object> map2 = new HashMap<String,Object>();
												bitmap = BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.add);
												map2.put("image",bitmap);
												map2.put("name", "添加房间");
												map2.put("rid","null");
												data.add(map2);     //“添加房间”
												 Adapter.this.notifyDataSetChanged();   //刷新
												 Toast.makeText(getActivity().getApplicationContext(),"删除成功！",Toast.LENGTH_SHORT).show();
						            			break;
						            		}
						            	default:break;
					            	}
					            }
					           }
							); 
						    builder.show();
						    return false;
						}
						}
					}
					);
	            return convertView;
	            }
	    	}
		
		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent data1) {  
			super.onActivityResult(requestCode, resultCode, data1);
			if (resultCode != getActivity().RESULT_CANCELED) 
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
		                        image = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), mImageCaptureUri);  
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
		                		Toast.makeText(getActivity().getApplicationContext(), "未找到存储卡，无法存储照片！", Toast.LENGTH_SHORT).show();
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
			                        image = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), mImageCaptureUri);  
			                        if (image != null) {  
			                        	data.get(result_code).put("image",image);
			                        	DbHelper dh1=new DbHelper(getActivity(),"IBMS",null,1);
										SQLiteDatabase db = dh1.getWritableDatabase(); 
										ContentValues cv = new ContentValues();    //内容数组
										ByteArrayOutputStream os = new ByteArrayOutputStream();  
										image.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
										cv.put("Image", os.toByteArray());
										db.update("room_tb", cv, "RID=?", new String[]{data.get(result_code).get("rid").toString()});
										db.close();
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
			                        	DbHelper dh1=new DbHelper(getActivity(),"IBMS",null,1);
										SQLiteDatabase db = dh1.getWritableDatabase(); 
										ContentValues cv = new ContentValues();    //内容数组
										ByteArrayOutputStream os = new ByteArrayOutputStream();  
										image.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
										cv.put("Image", os.toByteArray());
										db.update("room_tb", cv, "RID=?", new String[]{data.get(result_code).get("rid").toString()});
										db.close();
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
		intent.putExtra("outputX", convert.Converts.dip2px(this.getActivity().getApplicationContext(), 100));
		intent.putExtra("outputY", convert.Converts.dip2px(this.getActivity().getApplicationContext(), 100));
		intent.putExtra("return-data", false);
		startActivityForResult(intent, result_code);
		}
}
