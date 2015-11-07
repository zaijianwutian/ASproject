package com.suntrans.ibmsdemo;

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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class Update_Activity extends Activity implements OnClickListener{
	private Button button1;
	private Button button2;
	private ListView listview1;
	private String MACAddr;	
	//定义一个HashMap，用来存放EditText的值，Key是position  
    private HashMap<Integer, String> hashMap = new HashMap<Integer, String>();  
    private HashMap<Integer, String> hashMap1 = new HashMap<Integer, String>();  
    private ArrayList<Map<String, String>> data=new ArrayList<Map<String, String>>();
   /* @Override     //向标题栏添加item
    public boolean onCreateOptionsMenu(Menu menu) {  
        getMenuInflater().inflate(R.menu.update, menu);  
        return true;  
    }  */
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
		setContentView(R.layout.update);       //设置activity布局文件
		button1=(Button)findViewById(R.id.button1);
		button2=(Button)findViewById(R.id.button2);
		button1.setOnClickListener(this);
		button2.setOnClickListener(this);
		button1.setOnTouchListener(new TouchListener());
		button2.setOnTouchListener(new TouchListener());
		listview1=(ListView)findViewById(R.id.listview1);   //得到listview1
		DbHelper dh1=new DbHelper(Update_Activity.this,"IBMS",null,1);
		SQLiteDatabase db = dh1.getWritableDatabase(); 
		Cursor cursor = db.query( "switchs_tb", new String[]{"MACAddr","IPAddr","Channel","Area","Name"},"", null, null, null, null);
		
		while(cursor.moveToNext())   //提取数据库中的数据用于显示
		{
			Map<String, String> map=new HashMap<String,String>();			
			map.put("MACAddr", MACAddr);
			map.put("IPAddr", cursor.getString(1));
			map.put("Channel", cursor.getString(2));
			map.put("Area", cursor.getString(3));
			map.put("Name", cursor.getString(4));
			data.add(map);
		}		
		
		/*//设置listview适配器
		SimpleAdapter ListAdapter1=new SimpleAdapter(this, data ,R.layout.updatelistview, 
				new String[]{"MACAddr","Channel","Area","Name"},//new String[]{xx,xx,xx}确定里面有几列     名字是map的键值
				new int[]{R.id.macaddr,R.id.channel,R.id.area,R.id.name});//布局文件的控件id
*/		db.close();
		listview1.setAdapter(new Adapter());      //设置适配器
		
	}
	@Override           //menu选项监听
	public boolean onMenuItemSelected(int featureId, MenuItem item)
	{
		if(item.getItemId() == android.R.id.home)    //如果按下的是返回键
	    {
	        finish();
	        return true;
	    }
		else if(item.getItemId()==R.id.action_delete)   //如果按下的是标题栏的删除键
		{
			 final AlertDialog.Builder builder = new AlertDialog.Builder(Update_Activity.this);   
		        builder.setTitle("确定要删除开关?"); 	       
		        builder.setMessage("点击确定删除当前开关");
		        builder.setPositiveButton("确定", new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
						DbHelper dh1=new DbHelper(Update_Activity.this,"IBMS",null,1);
						SQLiteDatabase db = dh1.getWritableDatabase(); 
						db.delete("switchs_tb", "MACAddr=?", new String[]{MACAddr});
						Toast.makeText(getApplicationContext(),"开关已成功删除!",Toast.LENGTH_SHORT).show();
						finish();
					}
		        		
		        });
		        builder.setNegativeButton("取消", new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
							
					}
		        		
		        });
		        builder.create().show();
			return true;
		}
		else
			return true;
	}
	@Override
	public void onClick(View v) {
		
		switch(v.getId())
		{
			case R.id.button1:    //确定按钮   将修改结果保存到数据库中
			{
				DbHelper dh1=new DbHelper(Update_Activity.this,"IBMS",null,1);
				SQLiteDatabase db = dh1.getWritableDatabase(); 
				db.beginTransaction();       //手动设置开始事务   ，这样只打开一次数据库，一次性将数据写入,防止多次打开和关闭数据库，节省时间 
				ContentValues[] cv = new ContentValues[10];
				for(int i=0;i<10;i++)     //逐行进行更新
				{
					cv[i]=new ContentValues();
					cv[i].put("Area", data.get(i).get("Area"));
					cv[i].put("Name", data.get(i).get("Name"));
					db.update("switchs_tb", cv[i], "Channel=?", new String[]{data.get(i).get("Channel")});
					
				}
				db.setTransactionSuccessful();       //设置事务处理成功，不设置会自动回滚不提交
    			db.endTransaction();       //处理完成
				db.close();
				Toast.makeText(getApplicationContext(),"修改成功！",Toast.LENGTH_SHORT).show();
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
        //    if(convertView==null)
            convertView = LayoutInflater.from(getApplication()).inflate(R.layout.insertlistview, null);  
          
           // final TextView ipaddr = (TextView)convertView.findViewById(R.id.ipaddr); 
            final TextView channel = (TextView)convertView.findViewById(R.id.channel); 
            final EditText area = (EditText)convertView.findViewById(R.id.area);  
            final EditText name = (EditText)convertView.findViewById(R.id.name);
            final ImageView img_area=(ImageView)convertView.findViewById(R.id.img_area);   //区域下拉图标
            final ImageView img_name=(ImageView)convertView.findViewById(R.id.img_name);    //名称下拉图标
        
           // ipaddr.setText(data.get(position).get("IPAddr"));
            channel.setText(data.get(position).get("Channel"));
            area.setText(data.get(position).get("Area"));
            name.setText(data.get(position).get("Name"));
            img_area.setOnTouchListener(new TouchListener());
            img_name.setOnTouchListener(new TouchListener());
            img_area.setOnClickListener(new OnClickListener(){   //区域下拉图标点击监听
				@Override
				public void onClick(View v) {
					
					DbHelper dh1=new DbHelper(Update_Activity.this,"IBMS",null,1);
					SQLiteDatabase db = dh1.getWritableDatabase(); 
				 	Cursor cursor = db.query(true, "room_tb", new String[]{"Name"}, null, null, null, null, null, null, null);
				 	final AlertDialog.Builder builder = new AlertDialog.Builder(Update_Activity.this); 
				 	builder.setTitle("请选择房间");
				 	if(cursor.getCount()<=0)    //如果没有已添加的房间
				 	{
				 		builder.setMessage("请先在主页面“添加房间”");
			        	builder.setPositiveButton("确定", new DialogInterface.OnClickListener(){

							@Override
							public void onClick(DialogInterface dialog, int which) {
								
								
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
        					        					
        				 	final AlertDialog.Builder builder = new AlertDialog.Builder(Update_Activity.this); 
        				 	builder.setTitle("请选择开关名称");
        				 	
        				 	final String[] list_name=new String[]{"顶灯","圈灯","边灯","筒灯","壁灯","镜前灯","插座","空调","冰箱","洗衣机","电视机","微波炉","地暖","浴霸","换气扇"};
        				 	
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
                    data.get(position).put("Area",s.toString() );
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
            	//listview1.invalidate();
            }  
            if(hashMap1.get(position) != null){  
            //	data.get(position).put("Name",hashMap1.get(position) );   
            	name.setText(hashMap1.get(position));
            	//listview1.invalidate();
            }  
              
              
            return convertView;  
        }

		  
          
    }  
}
