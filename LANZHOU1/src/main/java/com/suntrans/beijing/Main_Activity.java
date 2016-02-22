package com.suntrans.beijing;

import services.MainService;
import views.ControlFragment;
import views.IViewPager;
import views.Line;
import views.ParameterFragment;
import views.RoomFragment;
import views.TouchListener;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;

import android.view.Window;
import android.view.WindowManager;
import android.widget.*;

import java.util.HashMap;
import java.util.Map;

//OnGestureListener   手势接口
public class Main_Activity extends FragmentActivity {
    private LinearLayout layout_room,layout_title;
    public IViewPager vPager;    //自定义ViewPager控件
    private TextView tx_control,tx_parameter,tx_out,tx_in;   //标签栏的两个标题，和标题栏的两个标题
    private ImageView img_control,img_parameter;    //导航栏对应的两个图标
    private TextView tx_edit;
    private ImageView img_info;   //  查看在线参数图标
    private ControlFragment control;
    private ControlFragment control_in;
    private RoomFragment room;
    public String flag_room="外间";     //当前选择的是外间还是里间
    private String flag_state="能源管控";    //当前选择的是能源管控还是室内环境
    private ParameterFragment parameter;    //共2个Fragment页面
    private ParameterFragment parameter_in;
    private long mExitTime;        //用于连按两次返回键退出      中间的时间判断
    public MainService.ibinder binder;  //用于Activity与Service通信
    private ServiceConnection con = new ServiceConnection() {
        @Override   //绑定服务成功后，调用此方法，获取返回的IBinder对象，可以用来调用Service中的方法
        public void onServiceConnected(ComponentName name, IBinder service) {
          //  Toast.makeText(getActivity().getApplication(), "绑定成功！", Toast.LENGTH_SHORT).show();
            binder=(MainService.ibinder)service;   //activity与service通讯的类，调用对象中的方法可以实现通讯
            binder.sendOrder("aa68"+Address.addr_out+"000103 0100 0007",2);
            try {
                Thread.sleep(60);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            binder.sendOrder("aa68"+Address.addr_out+"000203 0100 0007",2);
            try {
                Thread.sleep(60);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            binder.sendOrder("aa68"+Address.addr_in+"000103 0100 0007",2);
            //    Log.v("Time", "绑定后时间：" + String.valueOf(System.currentTimeMillis()));
        }

        @Override   //service因异常而断开的时候调用此方法
        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(getApplication(), "网络错误！", Toast.LENGTH_SHORT).show();

        }
    };;   ///用于绑定activity与service
    //新建广播接收器，接收服务器的数据并解析，根据第六感官的地址和开关的地址将数据转发到相应的Fragment
    private BroadcastReceiver broadcastreceiver=new BroadcastReceiver() {
        @Override
        public void onReceive (Context context, Intent intent){

            int count = intent.getIntExtra("ContentNum", 0);   //byte数组的长度
            byte[] data = intent.getByteArrayExtra("Content");  //内容数组
            String content = "";   //接收的字符串
            for (int i = 0; i < count; i++) {
                String s1 = Integer.toHexString((data[i] + 256) % 256);   //byte转换成十六进制字符串(先把byte转换成0-255之间的非负数，因为java中的数据都是带符号的)
                if (s1.length() == 1)
                    s1 = "0" + s1;
                content = content + s1;
            }
            Map<String, Object> map = new HashMap<String, Object>();   //新建map存放要传递给主线程的数据
            map.put("data", data);    //客户端发回的数据
           // map.put("ipaddr", "a");   //客户端的IP地址
            Message msg = new Message();   //新建Message，用于向handler传递数据
            msg.what = count;   //数组有效数据长度
            msg.obj = map;  //接收到的数据数组
            msg.arg1=0;    //通知Fragment房间有没有发生变化，=0则房间未变化，=1则房间进行了切换
            if(count>10)   //通过Fragment的handler将数据传过去
            {
                if (content.substring(8, 10).equals("f0") || content.substring(8, 10).equals("F0"))
                    parameter.handler1.sendMessage(msg);
                else
                    control.handler1.sendMessage(msg);
            }

        }
    };//广播接收器
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);      //设置activity布局文件
        //绑定MainService
        Intent intent = new Intent(getApplicationContext(), MainService.class);    //指定要绑定的service
        bindService(intent, con, Context.BIND_AUTO_CREATE);   //绑定主service
        // 注册自定义动态广播消息。根据Action识别广播
        IntentFilter filter_dynamic = new IntentFilter();
        filter_dynamic.addAction("com.suntrans.beijing.RECEIVE");  //为IntentFilter添加Action，接收的Action与发送的Action相同时才会出发onReceive
        registerReceiver(broadcastreceiver, filter_dynamic);    //动态注册broadcast receiver
        //实例化控件，并初始化控件
        layout_room = (LinearLayout) findViewById(R.id.layout_room);      //文字标题
        layout_title = (LinearLayout) findViewById(R.id.layout_title);     //房间名称标题
        vPager=(IViewPager)findViewById(R.id.vPager);
        tx_control=(TextView)findViewById(R.id.tx_control);
        tx_parameter=(TextView)findViewById(R.id.tx_parameter);
        tx_out=(TextView)findViewById(R.id.tx_out);
        tx_in=(TextView)findViewById(R.id.tx_in);
        img_control=(ImageView)findViewById(R.id.img_control);
        img_parameter=(ImageView)findViewById(R.id.img_parameter);
        tx_edit = (TextView) findViewById(R.id.tx_edit);

        tx_edit.setOnTouchListener(new TouchListener());
        tx_edit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra("Room", flag_room);
                intent.setClass(Main_Activity.this, Edit_Activity.class);
                startActivity(intent);   //跳转到编辑页面
            }
        });
        img_control.clearColorFilter();    //先清除之前的滤镜效果
        img_control.setColorFilter(0xffEB0000);                //红色
        img_info=(ImageView)findViewById(R.id.img_info);   //详细信息按钮
        img_info.setOnTouchListener(new TouchListener());
        img_info.setOnClickListener(new OnClickListener() {   //设置点击监听
            @Override
            public void onClick(View v) {
//                if (vPager.getCurrentItem() == 0)   //如果现在显示的是能源管控页面，则跳转到开关参数配置页面
//                {
//                    Intent intent = new Intent();
//                    intent.putExtra("room", flag_room);
//                    intent.setClass(Main_Activity.this, SwitchInfo_Activity.class);//设置要跳转的activity
//                    Main_Activity.this.startActivity(intent);//开始跳转
//                } else if (vPager.getCurrentItem() == 1)   //如果现在显示的是室内环境页面，则跳转到参数校正页面
//                {
//                    Intent intent = new Intent();
//                    intent.putExtra("room", flag_room);
//                    intent.setClass(Main_Activity.this, ControlInfo_Activity.class);//设置要跳转的activity
//                    Main_Activity.this.startActivity(intent);//开始跳转
//                }
                Intent intent = new Intent();
                intent.putExtra("room", flag_room);
                intent.setClass(Main_Activity.this, Config_Activity.class);//设置要跳转的activity
                Main_Activity.this.startActivity(intent);//开始跳转
            }
        });
        vPager.setAdapter(new MyPagerAdapter(getSupportFragmentManager()));   //设置viewpager适配器
        vPager.setOnPageChangeListener(new MyOnPageChangeListener());   //设置页面切换监听
        vPager.setPagingEnabled(true);   //false禁止左右滑动,true允许左右滑动
        //设置标签栏监听，根据用户点击显示相应的Fragment
        img_control.setOnClickListener(new MyOnClickListener(0));
        img_parameter.setOnClickListener(new MyOnClickListener(1));
        tx_control.setOnClickListener(new MyOnClickListener(0));
        tx_parameter.setOnClickListener(new MyOnClickListener(1));

        tx_out.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                if(flag_room.equals("里间"))    //如果现在显示的是里间，则切换到外间，并通知正在显示的fragment
                {
                    tx_in.setTextColor(getResources().getColor(R.color.white));
                    tx_in.setBackgroundColor(getResources().getColor(R.color.bg_action));
                    tx_out.setTextColor(getResources().getColor(R.color.bg_action));
                    tx_out.setBackgroundColor(getResources().getColor(R.color.white));
                    flag_room = "外间";

                        Message msg = new Message();   //新建Message，用于向handler传递数据
                        msg.arg1 = 1;    //通知Fragment房间发生了变化，让其更新页面显示
                        control.handler1.sendMessage(msg);

                        Message msg1 = new Message();   //新建Message，用于向handler传递数据
                        msg1.arg1 = 1;    //通知Fragment房间发生了变化，让其更新页面显示
                        parameter.handler1.sendMessage(msg1);

                }
            }
        });
        tx_in.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(flag_room=="外间")
                {
                    tx_out.setTextColor(getResources().getColor(R.color.white));
                    tx_out.setBackgroundColor(getResources().getColor(R.color.bg_action));
                    tx_in.setTextColor(getResources().getColor(R.color.bg_action));
                    tx_in.setBackgroundColor(getResources().getColor(R.color.white));
                    flag_room = "里间";

                        Message msg = new Message();   //新建Message，用于向handler传递数据
                        msg.arg1 = 1;    //通知Fragment房间发生了变化，让其更新页面显示
                        control.handler1.sendMessage(msg);

                        Message msg1 = new Message();   //新建Message，用于向handler传递数据
                        msg1.arg1 = 1;    //通知Fragment房间发生了变化，让其更新页面显示
                        parameter.handler1.sendMessage(msg1);

                }
            }
        });

//    设置通知栏半透明
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            setTranslucentStatus(true);
        }

//        SystemBarTintManager tintManager = new SystemBarTintManager(this);
//        tintManager.setStatusBarTintEnabled(true);
//        tintManager.setStatusBarTintResource(R.color.bg_action);
    }

    @Override     //activity销毁时解除Service的绑定
    public void onDestroy()
    {
        unbindService(con);   //解除Service的绑定
        unregisterReceiver(broadcastreceiver);  //注销广播接收者
        super.onDestroy();
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
                        Bundle bundle = new Bundle();

                            /*	Bundle bundle = new Bundle();
								bundle.putString("clientip", clientip);   //传入IP地址
								control.setArguments(bundle);		*/
                    }
//                    if(room==null)
//                        room = new RoomFragment();
//                    layout_room.setVisibility(View.GONE);
//                    layout_title.setVisibility(View.VISIBLE);
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
//                    layout_room.setVisibility(View.VISIBLE);
//                    layout_title.setVisibility(View.GONE);
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
//                case R.id.tx_out:
//                {
//                    tx_out.setTextColor(getResources().getColor(R.color.white));
//                    tx_out.setBackgroundColor(getResources().getColor(R.color.bg_action));
//                    tx_in.setTextColor(getResources().getColor(R.color.bg_action));
//                    tx_in.setBackgroundColor(getResources().getColor(R.color.white));
//                    flag_room="外间";
//
//                }
//                case R.id.tx_in:
//                {
//                    tx_in.setTextColor(getResources().getColor(R.color.white));
//                    tx_in.setBackgroundColor(getResources().getColor(R.color.bg_action));
//                    tx_out.setTextColor(getResources().getColor(R.color.bg_action));
//                    tx_out.setBackgroundColor(getResources().getColor(R.color.white));
//                    flag_room="里间";
//                }
                case R.id.tx_control:
                case R.id.img_control:
                {
                    tx_control.setTextColor(Color.parseColor("#EB0000"));  //红色
                    img_control.clearColorFilter();    //先清除之前的滤镜效果
                    img_control.setColorFilter(0xffEB0000);                //红色
                    tx_parameter.setTextColor(Color.GRAY);     //灰色
                    img_parameter.clearColorFilter();   //清除之前的滤镜效果
                    img_parameter.setColorFilter(Color.GRAY);  //灰色
                    flag_state="能源管控";

                    layout_room.setVisibility(View.GONE);
                    layout_title.setVisibility(View.VISIBLE);
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
                    flag_state="室内环境";

                    layout_room.setVisibility(View.VISIBLE);
                    layout_title.setVisibility(View.GONE);
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
                    flag_state="能源管控";

                    layout_room.setVisibility(View.GONE);
                    layout_title.setVisibility(View.VISIBLE);
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
                    flag_state="室内环境";

                    layout_room.setVisibility(View.VISIBLE);
                    layout_title.setVisibility(View.GONE);
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
        if (keyCode == KeyEvent.KEYCODE_BACK) {    //返回键

            if ((System.currentTimeMillis() - mExitTime) > 2000) {// 如果两次按键时间间隔大于2000毫秒，则不退出
                Toast.makeText(this, "再按一次返回键退出", Toast.LENGTH_SHORT).show();
                mExitTime = System.currentTimeMillis();// 更新mExitTime
            } else {
                finish();
                //System.exit(0);// 连续点击两次返回键，退出程序

            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    @TargetApi(19)   //屏幕状态栏进行透明化处理
    private void setTranslucentStatus(boolean on) {
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }

}
