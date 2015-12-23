package com.suntrans.demo6;

import views.ControlFragment;
import views.IViewPager;
import views.ParameterFragment;
import views.TouchListener;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;

import android.widget.*;

//OnGestureListener   手势接口
public class Main_Activity extends FragmentActivity{
    private IViewPager vPager;    //自定义ViewPager控件
    private TextView tx_control,tx_parameter;   //导航栏的两个标题
    private ImageView img_control,img_parameter;    //导航栏对应的两个图标
    private ImageView img_info;   //  查看在线参数图标
    private ControlFragment control;
    private ParameterFragment parameter;
    private long mExitTime;        //用于连按两次返回键退出      中间的时间判断
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);      //设置activity布局文件
        vPager=(IViewPager)findViewById(R.id.vPager);
        tx_control=(TextView)findViewById(R.id.tx_control);
        tx_parameter=(TextView)findViewById(R.id.tx_parameter);
        img_control=(ImageView)findViewById(R.id.img_control);
        img_parameter=(ImageView)findViewById(R.id.img_parameter);
        img_info=(ImageView)findViewById(R.id.img_info);
        img_info.setOnTouchListener(new TouchListener());
        img_info.setOnClickListener(new OnClickListener(){   //设置点击监听
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if(vPager.getCurrentItem()==0)   //如果现在显示的是能源管控页面，则跳转到开关参数配置页面
                {
                    Intent intent=new Intent();
                    intent.setClass(Main_Activity.this, SwitchInfo_Activity.class);//设置要跳转的activity
                    Main_Activity.this.startActivity(intent);//开始跳转
                }
                else if(vPager.getCurrentItem()==1)   //如果现在显示的是室内环境页面，则跳转到参数校正页面
                {
                    Intent intent=new Intent();
                    intent.setClass(Main_Activity.this, ControlInfo_Activity.class);//设置要跳转的activity
                    Main_Activity.this.startActivity(intent);//开始跳转
                }
            }});
        vPager.setAdapter(new MyPagerAdapter(getSupportFragmentManager()));   //设置viewpager适配器
        vPager.setOnPageChangeListener(new MyOnPageChangeListener());   //设置页面切换监听
        vPager.setPagingEnabled(true);   //false禁止左右滑动,true允许左右滑动
        //设置监听
        img_control.setOnClickListener(new MyOnClickListener(0));
        img_parameter.setOnClickListener(new MyOnClickListener(1));

        tx_control.setOnClickListener(new MyOnClickListener(0));
        tx_parameter.setOnClickListener(new MyOnClickListener(1));

    }
    //viewpager适配器
    public class MyPagerAdapter extends FragmentPagerAdapter {     //viewpager适配器

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        private final String[] titles = { "能源管控", "室内环境"};
        //	private final String[] titles = { "能源管控"};
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
            switch (position) {
                case 0:          //第一个fragment
                    if (control == null) {
                        control= new ControlFragment();
							/*	Bundle bundle = new Bundle();
								bundle.putString("clientip", clientip);   //传入IP地址								
								control.setArguments(bundle);		*/
                    }
                    return control;
                case 1:              //第二个fragment
                    if (parameter == null) {
                        parameter = new ParameterFragment();
                    }
						/*	Bundle bundle = new Bundle();
							bundle.putSerializable("data1", data1);
							bundle.putSerializable("data2", data2);
							hourFragment.setArguments(bundle);
							*/
                    return parameter;
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
    public class MyOnClickListener implements View.OnClickListener {
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
                    tx_control.setTextColor(Color.parseColor("#EB0000"));  //红色
                    img_control.clearColorFilter();    //先清除之前的滤镜效果
                    img_control.setColorFilter(0xffEB0000);                //红色
                    tx_parameter.setTextColor(Color.GRAY);     //灰色
                    img_parameter.clearColorFilter();   //清除之前的滤镜效果
                    img_parameter.setColorFilter(Color.GRAY);  //灰色
                    break;
                }
                case R.id.tx_parameter:
                case R.id.img_parameter:
                {
                    tx_control.setTextColor(Color.GRAY);     //灰色
                    img_control.clearColorFilter();    //先清除之前的滤镜效果
                    img_control.setColorFilter(Color.GRAY);  //灰色
                    tx_parameter.setTextColor(Color.parseColor("#EB0000"));  //红色
                    img_parameter.clearColorFilter();   //清除之前的滤镜效果
                    img_parameter.setColorFilter(0xffEB0000);    //红色
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
                    tx_control.setTextColor(Color.parseColor("#EB0000"));  //红色
                    img_control.clearColorFilter();    //先清除之前的滤镜效果
                    img_control.setColorFilter(0xffEB0000);                //红色
                    tx_parameter.setTextColor(Color.GRAY);     //灰色
                    img_parameter.clearColorFilter();   //清除之前的滤镜效果
                    img_parameter.setColorFilter(Color.GRAY);  //灰色
                    break;
                }
                case 1:
                {
                    tx_control.setTextColor(Color.GRAY);     //灰色
                    img_control.clearColorFilter();    //先清除之前的滤镜效果
                    img_control.setColorFilter(Color.GRAY);  //灰色
                    tx_parameter.setTextColor(Color.parseColor("#EB0000"));  //红色
                    img_parameter.clearColorFilter();   //清除之前的滤镜效果
                    img_parameter.setColorFilter(0xffEB0000);    //红色
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

    //连按两次返回键退出
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // TODO Auto-generated method stub
        if (keyCode == KeyEvent.KEYCODE_BACK) {    //返回键

            if ((System.currentTimeMillis() - mExitTime) > 2000) {// 如果两次按键时间间隔大于2000毫秒，则不退出
                Toast.makeText(this, "再按一次返回键退出", Toast.LENGTH_SHORT).show();
                mExitTime = System.currentTimeMillis();// 更新mExitTime
            } else {
                System.exit(0);// 否则退出程序
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
	
	/*	@Override
		public boolean onTouch(View v, MotionEvent event) 
		{
			//如果滚动到了顶部或者滚动到了底部，通知父控件勿拦截touch事件
			if((scrollView.getScrollY()==0&&MoveY>0)||(scrollView.getScrollY()>=(layout1.getHeight()-scrollView.getHeight())&&MoveY<0))
				v.getParent().requestDisallowInterceptTouchEvent(true);    //通知父控件勿拦截本控件的touch事件
			// TODO Auto-generated method stub
			switch (event.getAction()) 
			{
				case MotionEvent.ACTION_DOWN:
				{
					LastY = event.getRawY();    //获取点击位置Y轴绝对坐标
					break;
				}
				case MotionEvent.ACTION_MOVE:
				{
					CurrentY=event.getRawY();   //获取点击位置Y轴绝对坐标
					MoveY=CurrentY-LastY;									
					if((scrollView.getScrollY()==0&&MoveY>0)||(scrollView.getScrollY()>=(layout1.getHeight()-scrollView.getHeight())&&MoveY<0))
					{
						LayoutParams para=new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT);
						para.setMargins(0, (int)MoveY/2, 0, 0);
						layout1.setLayoutParams(para);
					}
					//Toast.makeText(getApplicationContext(), String.valueOf(MoveY), Toast.LENGTH_SHORT).show();
					//Toast.makeText(getApplicationContext(),String.valueOf(layout1.getHeight()) +";;"+String.valueOf(scrollView.getHeight())+";;"+ String.valueOf( scrollView.getScrollY()), Toast.LENGTH_SHORT).show();
					break;
				}
				case MotionEvent.ACTION_UP:
				{
					LayoutParams para=new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT);
					para.setMargins(0, 0, 0, 0);
					layout1.setLayoutParams(para);
					
					break;
				}
				default:break;
				
			}
			return true;
		
	}*/

}
