package com.suntrans.ibmsdemo;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import views.TouchListener;

import database.DbHelper;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class Insert_Activity extends Activity implements OnClickListener{
	private Button button1;                            //取消按钮
	private Button button2;                            //确定按钮
	private ListView listview1;                        //列表
	private String MACAddr;
	private String IPAddr;
	//定义一个HashMap，用来存放EditText的值，Key是position  
    private HashMap<Integer, String> hashMap = new HashMap<Integer, String>();  
    private HashMap<Integer, String> hashMap1 = new HashMap<Integer, String>();  
    private ArrayList<Map<String, String>> data=new ArrayList<Map<String, String>>();    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		ActionBar actionBar = this.getActionBar();   //设置actionbar标题栏
		//actionBar.setCustomView(R.layout.title);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setDisplayShowCustomEnabled(true);
		actionBar.setDisplayShowHomeEnabled(true);	
		Drawable d = this.getResources().getDrawable(R.color.bg_action); 
		actionBar.setBackgroundDrawable(d);   //设置actionbar背景
		actionBar.show();
		setContentView(R.layout.insert);       //设置activity布局文件
		button1=(Button)findViewById(R.id.button1);    //取消按钮
		button2=(Button)findViewById(R.id.button2);    //确定按钮
		button1.setOnClickListener(this);              //设置监听   在OnClick方法中
		button2.setOnClickListener(this);
		button1.setOnTouchListener(new TouchListener());
		button2.setOnTouchListener(new TouchListener());
		listview1=(ListView)findViewById(R.id.listview1);   //得到listview1
		Intent intent=this.getIntent();    //获取Intent
		//final ArrayList<Map<String, String>> data=(ArrayList<Map<String, String>>)intent.getSerializableExtra("data");
		MACAddr=intent.getStringExtra("MACAddr");
		IPAddr=intent.getStringExtra("IPAddr");
		
		//for(int i=0;i<=9;i++)
		//{
		Map<String, String> map1=new HashMap<String,String>();			
		map1.put("MACAddr", MACAddr);			
		map1.put("Channel", "1");
		map1.put("Area", "主房间");
		map1.put("Name", "门前灯");
		data.add(map1);
		
		Map<String, String> map2=new HashMap<String,String>();			
		map2.put("MACAddr", MACAddr);			
		map2.put("Channel", "2");
		map2.put("Area", "主房间");
		map2.put("Name", "主灯");
		data.add(map2);
		
		Map<String, String> map3=new HashMap<String,String>();			
		map3.put("MACAddr", MACAddr);			
		map3.put("Channel", "3");
		map3.put("Area", "主房间");
		map3.put("Name", "书桌灯");
		data.add(map3);
		
		Map<String, String> map4=new HashMap<String,String>();			
		map4.put("MACAddr", MACAddr);			
		map4.put("Channel", "4");
		map4.put("Area", "卫生间");
		map4.put("Name", "镜前灯");
		data.add(map4);
		
		Map<String, String> map5=new HashMap<String,String>();			
		map5.put("MACAddr", MACAddr);			
		map5.put("Channel", "5");
		map5.put("Area", "厨房");
		map5.put("Name", "顶灯");
		data.add(map5);
		
		Map<String, String> map6=new HashMap<String,String>();			
		map6.put("MACAddr", MACAddr);			
		map6.put("Channel", "6");
		map6.put("Area", "主房间");
		map6.put("Name", "二层顶灯");
		data.add(map6);
		
		Map<String, String> map7=new HashMap<String,String>();			
		map7.put("MACAddr", MACAddr);			
		map7.put("Channel", "7");
		map7.put("Area", "主房间");
		map7.put("Name", "空调");
		data.add(map7);
		
		Map<String, String> map8=new HashMap<String,String>();			
		map8.put("MACAddr", MACAddr);			
		map8.put("Channel", "8");
		map8.put("Area", "客房");
		map8.put("Name", "插座");
		data.add(map8);
		
		Map<String, String> map9=new HashMap<String,String>();			
		map9.put("MACAddr", MACAddr);			
		map9.put("Channel", "9");
		map9.put("Area", "卫生间");
		map9.put("Name", "插座");
		data.add(map9);
		
		Map<String, String> map10=new HashMap<String,String>();			
		map10.put("MACAddr", MACAddr);			
		map10.put("Channel", "10");
		map10.put("Area", "卫生间");
		map10.put("Name", "换气扇");
		data.add(map10);
		//}
		//设置listview适配器
	/*	SimpleAdapter ListAdapter1=new SimpleAdapter(this, data ,R.layout.insertlistview, 
				new String[]{"MACAddr","Channel","Area","Name"},//new String[]{xx,xx,xx}确定里面有几列     名字是map的键值
				new int[]{R.id.macaddr,R.id.channel,R.id.area,R.id.name});//布局文件的控件id
*/		listview1.setAdapter(new Adapter());      //设置适配器
		
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
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId())
		{
			case R.id.button1:    //确定按钮
			{
				long row=0;
				DbHelper dh1=new DbHelper(Insert_Activity.this,"IBMS",null,1);
    			SQLiteDatabase db = dh1.getWritableDatabase(); 
    			db.beginTransaction();       //手动设置开始事务   ，这样只打开一次数据库，一次性将数据写入,防止多次打开和关闭数据库，节省时间 
    			ContentValues[] cv = new ContentValues[10];    //内容数组
    			
    			Bitmap bmp = BitmapFactory.decodeResource(Insert_Activity.this.getResources(), R.drawable.switchoff); 
    			//获取图片输出流
    			ByteArrayOutputStream os = new ByteArrayOutputStream();  
    			bmp.compress(Bitmap.CompressFormat.PNG, 100, os);    //os是输出流，存放图片
    			for(int i=0;i<10;i++)
    			{
    				cv[i] = new ContentValues();
    				cv[i].put("MACAddr", MACAddr);    //MAC地址
    				cv[i].put("IPAddr", IPAddr);       //IP地址
    				cv[i].put("Channel", data.get(i).get("Channel"));    //通道号
    				cv[i].put("Area", data.get(i).get("Area"));    //区域
    				cv[i].put("Name", data.get(i).get("Name"));    	 //开关名
    				cv[i].put("Image", os.toByteArray());
    				row = db.insert("switchs_tb", null, cv[i]);  //将数据添加到数据库    				
    			}
    			db.setTransactionSuccessful();       //设置事务处理成功，不设置会自动回滚不提交
    			db.endTransaction();       //处理完成
				db.close();
    			Toast.makeText(getApplicationContext(),"添加成功！",Toast.LENGTH_SHORT).show();
				finish();
				break;
			}
			case R.id.button2:finish();break;   //取消按钮
			default:break;
		}
	}
	
	 //自定义Adapter  
    class Adapter extends BaseAdapter{  
  
      

		@Override  
        public int getCount() {  
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
            convertView = LayoutInflater.from(getApplication()).inflate(R.layout.insertlistview, null);  
       //     final TextView macaddr = (TextView)convertView.findViewById(R.id.macaddr); 
           // final TextView ipaddr = (TextView)convertView.findViewById(R.id.ipaddr); 
            final TextView channel = (TextView)convertView.findViewById(R.id.channel); 
            final EditText area = (EditText)convertView.findViewById(R.id.area);  
            final EditText name = (EditText)convertView.findViewById(R.id.name); 
            final ImageView img_area=(ImageView)convertView.findViewById(R.id.img_area);   //区域下拉图标
            final ImageView img_name=(ImageView)convertView.findViewById(R.id.img_name);    //名称下拉图标
         //   macaddr.setText(data.get(position).get("MACAddr"));
          //  ipaddr.setText(data.get(position).get("IPAddr"));
            channel.setText(data.get(position).get("Channel"));
            area.setText(data.get(position).get("Area"));
            name.setText(data.get(position).get("Name"));
            img_area.setOnTouchListener(new TouchListener());
            img_name.setOnTouchListener(new TouchListener());
            img_area.setOnClickListener(new OnClickListener(){   //区域下拉图标点击监听
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					DbHelper dh1=new DbHelper(Insert_Activity.this,"IBMS",null,1);
					SQLiteDatabase db = dh1.getWritableDatabase(); 
				 	Cursor cursor = db.query(true, "room_tb", new String[]{"Name"}, null, null, null, null, null, null, null);
				 	final AlertDialog.Builder builder = new AlertDialog.Builder(Insert_Activity.this); 
				 	builder.setTitle("请选择房间");
				 	if(cursor.getCount()<=0)    //如果没有已添加的房间
				 	{
				 		builder.setMessage("请先在主页面“添加房间”");
			        	builder.setPositiveButton("确定", new DialogInterface.OnClickListener(){

							@Override
							public void onClick(DialogInterface dialog, int which) {
								// TODO Auto-generated method stub
								
							}
			        		
			        	});
				 	}
				 	else
				 	{
				 		final String[] list_room=new String[cursor.getCount()];
				 		int k=0;
				 		while(cursor.moveToNext())
				 		{
				 			list_room[k]=cursor.getString(0);
				 			k++;
				 		}
				 		 builder.setItems(list_room, new DialogInterface.OnClickListener() {  
					            public void onClick(DialogInterface dialog, int which) {  
					            //点击后弹出窗口选择了第几项  
					            area.setText(list_room[which]);
					            }  
					        });  
				 	}
				 	db.close();
				 	builder.create().show();
				}});
            img_name.setOnClickListener(new OnClickListener(){   //名称下拉图标点击监听
        				@Override
        				public void onClick(View v) {
        					// TODO Auto-generated method stub        					
        				 	final AlertDialog.Builder builder = new AlertDialog.Builder(Insert_Activity.this); 
        				 	builder.setTitle("请选择开关名称");
        				 	
        				 	final String[] list_name=new String[]{"顶灯","圈灯","边灯","筒灯","壁灯","镜前灯","插座","空调","冰箱","洗衣机","电视机","微波炉","地暖","浴霸"};
        				 	
        				 	builder.setItems(list_name, new DialogInterface.OnClickListener() {  
        					       public void onClick(DialogInterface dialog, int which) {  
        					            //点击后弹出窗口选择了第几项  
        					           name.setText(list_name[which]);
        					        }  
        					        });  
        				 	
        				 	builder.create().show();
        				}});  
            //为editText设置TextChangedListener，每次改变的值设置到hashMap  
            //我们要拿到里面的值根据position拿值  
            area.addTextChangedListener(new TextWatcher() {  
                @Override  
                public void onTextChanged(CharSequence s, int start, int before, int count) {  
                      
                }  
                  
                @Override  
                public void beforeTextChanged(CharSequence s, int start,   
                        int count,int after) {  
                      
                }  
                  
                @Override  
                public void afterTextChanged(Editable s) {  
                    //将editText中改变的值设置的HashMap中  
                    hashMap.put(position, s.toString());  
                    data.get(position).put("Area",s.toString());   
                }  
            });  
            name.addTextChangedListener(new TextWatcher() {  
                @Override  
                public void onTextChanged(CharSequence s, int start, int before, int count) {  
                      
                }  
                  
                @Override  
                public void beforeTextChanged(CharSequence s, int start,   
                        int count,int after) {  
                      
                }  
                  
                @Override  
                public void afterTextChanged(Editable s) {  
                    //将editText中改变的值设置的HashMap中  
                    hashMap1.put(position, s.toString()); 
                    data.get(position).put("Name",s.toString());  
                }  
            });  
              
            //如果hashMap不为空，就设置的editText  
            if(hashMap.get(position) != null){  
            	//data.get(position).put("Area",hashMap.get(position) );  
            	area.setText(hashMap.get(position));            	
            }  
            if(hashMap1.get(position) != null){  
            	//data.get(position).put("Name",hashMap1.get(position) );   
            	name.setText(hashMap1.get(position));            	
            }  
              
              
            return convertView;  
        }

		  
          
    }  
}
