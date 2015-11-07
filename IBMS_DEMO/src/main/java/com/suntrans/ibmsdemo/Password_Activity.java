package com.suntrans.ibmsdemo;

import views.TouchListener;

import com.readystatesoftware.viewbadger.BadgeView;

import database.DbHelper;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

public class Password_Activity extends Activity {
	private EditText pa1,pa2,pa3;
	private Button confirm,pick,photo;
	private ImageView image; 
	private LinearLayout ly1;
	/** 请求码 */	
	private static final int IMAGE_REQUEST_CODE = 0;   //相册	
	private static final int CAMERA_REQUEST_CODE = 1;  //相机
	private static final int RESULT_REQUEST_CODE = 2;  //裁剪
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
		//actionBar.setTitle(area);
		actionBar.show();
		setContentView(R.layout.password);     //设置布局文件
		pa1=(EditText)findViewById(R.id.pa1);
		pa2=(EditText)findViewById(R.id.pa2);
		pa3=(EditText)findViewById(R.id.pa3);
		image=(ImageView)findViewById(R.id.image);
		//ly1=(LinearLayout)findViewById(R.id.ly1);
		confirm=(Button)findViewById(R.id.confirm);
		/*final BadgeView badgeview=new BadgeView(Password_Activity.this,ly1);     //可以在右上角添加文字，数字或图标
		badgeview.setWidth(convert.Converts.dip2px(getApplicationContext(), 8));  //设置宽度为8dip
		badgeview.setHeight(convert.Converts.dip2px(getApplicationContext(), 8)); //设置高度为8dip
		badgeview.setBackgroundResource(R.drawable.offdot);    //设置图标
		badgeview.setBadgePosition(BadgeView.POSITION_TOP_RIGHT); //设置显示的位置
		badgeview.show();
		image.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				badgeview.setBackgroundResource(R.drawable.ondot);   //更改图标
			}});*/
		confirm.setOnTouchListener(new TouchListener());
		confirm.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String p1=pa1.getText().toString();
				String p2=pa2.getText().toString();
				String p3=pa3.getText().toString();
				if(p1.equals("")||p2.equals("")||p3.equals(""))
				{
					Toast.makeText(getApplicationContext(),"输入不能为空！",Toast.LENGTH_SHORT).show();
				}
				else if(!p2.equals(p3))
				{
					Toast.makeText(getApplicationContext(),"两次输入的新密码不一致！",Toast.LENGTH_SHORT).show();
				}
				else
				{
					DbHelper dh1=new DbHelper(Password_Activity.this,"IBMS",null,1);
					SQLiteDatabase db = dh1.getWritableDatabase(); 
					Cursor cursor = db.query("users_tb", new String[]{"NID","Password"},"IsUsing=?", new String[]{"1"}, null, null, null);
					while(cursor.moveToNext())
					{
						if(p1.equals(cursor.getString(1)))
						{
							ContentValues cv1 = new ContentValues();    //内容数组	
							cv1.put("Password",p2);   //新密码
							db.update("users_tb",cv1,"NID=?",new String[]{cursor.getString(0)});    //更新数据库数据
							Toast.makeText(getApplicationContext(),"修改成功！",Toast.LENGTH_SHORT).show();
						}
						else
							Toast.makeText(getApplicationContext(),"当前密码输入错误！",Toast.LENGTH_SHORT).show();
					}
				}
			}
			
		});
	}
		
	@Override           //menu选项监听
	public boolean onMenuItemSelected(int featureId, MenuItem item)
	{
		if(item.getItemId() == android.R.id.home)    //如果按下的是返回键
	    {
	        finish();
	        return true;
	    }
		return true;
	}
}
