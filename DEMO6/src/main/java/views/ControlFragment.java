package views;

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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.suntrans.demo6.R;

import convert.Converts;
import android.support.v4.app.Fragment;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.FrameLayout.LayoutParams;
import android.widget.Toast;

public class ControlFragment extends Fragment{
    private ImageView image_bg;   //主背景图
    private GridView gridview;     //灯泡开关布局
    private int Pwidth=0;  //屏幕宽度，单位是pixel
    private int Pheight=0;   //屏幕高度
    private LinearLayout layout;  //大框架
    private LinearLayout layout1;   //主页面
    private FrameLayout layout2;   //图片页面
    private FrameLayout frame1;
    private ObservableScrollView scrollView;   //滚动条
    //	private SwipeRefreshLayout swipe_container;    //下拉刷新控件
    private ImageView[] images=new ImageView[11];    //小窗户图片
    private ArrayList<Map<String, String>> data=new ArrayList<Map<String, String>>();    //列表显示的内容,存放着通道号、ip地址、开关状态等
    private DisplayMetrics displayMetrics = new DisplayMetrics();
    private float LastY=0,CurrentY=0,MoveY=0;  //上一次Y坐标，这一次Y坐标，移动的Y坐标
    private int ItemHeight=0;   //GridView单个项目的高度
    private double XRate=0;    //图片X轴缩放比例，根据背景得出，用图片原始尺寸*XRate即可
    private double YRate=0;   //图片y轴缩放比例，根据屏幕适当地调整，使得一个页面全部展示出来

    private String clientip;    //保存开关的IP地址
    private Socket client;    //保持TCP连接的socket
    private int Mainstate=0;   //总开关的状态，为0表示关
    private byte[] bits={(byte)0x01,(byte)0x02,(byte)0x04,(byte)0x08,(byte)0x10,(byte)0x20,(byte)0x40,(byte)0x80};     //从1到8只有一位是1，用于按位与计算，获取某一位的值
    private ProgressDialog progressdialog;      //定义”加载中。。。“的圆形滚动条弹出框
    private long time;   //触发progressdialog显示的时间
    private String which="100";    //触发progressdialog显示的通道号,默认100为无命令状态
    public Handler handler1=new Handler()   //接收到TCP发回的数据后调用，用于更新列表中的开关状态，即反馈
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

            s=s.replace(" ", ""); //去掉空格
            // Toast.makeText(getActivity(), s, Toast.LENGTH_LONG).show();
            Log.i("Order", "收到的数据："+s);
            int IsEffective=0;    //指令是否有效，0表示无效，1表示有效；对于和第六感官通讯而言，包头为ab68的数据才有效
            if(msg.what>5)
                if(s.substring(0, 5).equals("ab680"))
                    IsEffective=1;
            if(IsEffective==1)    //命令有效才对命令进行解析
            {
                String crc=Converts.GetCRC(a, 2, msg.what-2-2);    //获取返回数据的校验码，倒数第3、4位是验证码
                if(s.substring(6, 8).equals("06")&&s.length()>=24)    //根据第6,7个字符判断返回的是单个通道还是所有通道
                {     //如果是单个通道

                    if(crc.equals(s.substring(16,20)))     //判断crc校验码是否正确
                    {
                        int k=0;         //k是通道号
                        int state=Integer.valueOf(s.substring(15, 16));  //开关状态，1代表打开，0代表关闭
                        if(s.substring(11,12).equals("a"))
                            k=10;
                        else
                            k=Integer.valueOf(s.substring(11, 12));   //通道号,int型
                        String k_s=s.substring(11,12);   //通道号 ，String型
                        if(k_s.equals(which))  //如果返回的通道号是当前正在发送指令的通道号，则关闭progressdialog的显示
                        {
                            which="100";
                            Message message = new Message();
                            message.what =0;       //0表示要隐藏
                            handler2.sendMessage(message);
                        }
                        if(k==0)                                          //如果通道号为0，则是总开关
                        {
                            Mainstate=state; //更新总开关数组中的开关状态
                            if(state==0)   //如果总开关关了，那肯定所有通道都关了
                            {
                                for(int i=0;i<10;i++)
                                    data.get(i).put("State", "0");
                            }
                        }
                        else     //如果通道号不为0，则更改data中的状态，并更新
                        {
                            for(int i=0;i<10;i++)
                            {
                                if(data.get(i).get("Channel").equals(k_s))
                                    data.get(i).put("State", state==1?"1":"0");
                            }
                        }
                        //   Log.i("Time","解析完数据1+"+String.valueOf(System.currentTimeMillis()));
                        //以下更新图片中的显示
                        if(state==0)    //如果返回的状态是0
                            switch(k)
                            {
                                case 0:
                                {
                                    images[0].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image1_off));
                                    images[1].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image2_off));
                                    images[2].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image3_off));
                                    images[3].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image4_off));
                                    images[4].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image5_off));
                                    images[5].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image6_off));
                                    images[6].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image7_off));
                                    images[7].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image8_off));
                                    images[8].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image9_off));
                                    images[9].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image10_off));
                                    images[10].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image11_off));
                                    break;
                                }
                                case 1:
                                {
                                    images[1].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image2_off));
                                    break;
                                }
                                case 2:
                                {
                                    images[0].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image1_off));
                                    break;
                                }

                                case 3:
                                {
                                    images[2].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image3_off));
                                    images[10].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image11_off));
                                    break;
                                }
                                case 4:
                                {
                                    images[7].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image8_off));
                                    break;
                                }
                                case 5:
                                {
                                    images[4].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image5_off));
                                    break;
                                }
                                case 6:
                                {
                                    images[3].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image4_off));
                                    break;
                                }
                                case 7:
                                {
                                    images[6].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image7_off));
                                    break;
                                }
                                case 8:
                                {
                                    images[5].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image6_off));
                                    break;
                                }


                                case 9:
                                {
                                    images[8].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image9_off));
                                    break;
                                }
                                case 10:
                                {
                                    images[9].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image10_off));
                                    break;
                                }
                                default:break;
                            }
                        else      //如果返回的状态是1
                            switch(k)
                            {
                                case 0:break;
                                case 1:
                                {
                                    images[1].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image2_on));
                                    break;
                                }
                                case 2:
                                {
                                    images[0].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image1_on));
                                    break;
                                }

                                case 3:
                                {
                                    images[2].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image3_on));
                                    images[10].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image11_on));
                                    break;
                                }
                                case 4:
                                {
                                    images[7].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image8_on));
                                    break;
                                }
                                case 5:
                                {
                                    images[4].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image5_on));
                                    break;
                                }
                                case 6:
                                {
                                    images[3].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image4_on));
                                    break;
                                }
                                case 7:
                                {
                                    images[6].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image7_on));
                                    break;
                                }
                                case 8:
                                {
                                    images[5].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image6_on));
                                    break;
                                }

                                case 9:
                                {
                                    images[8].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image9_on));
                                    break;
                                }
                                case 10:
                                {
                                    images[9].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image10_on));
                                    break;
                                }
                                default:break;
                            }
                        //ImageInit();
                        ((Adapter)gridview.getAdapter()).notifyDataSetChanged();
                    }
                }
                else if(s.substring(6, 8).equals("03")&&s.length()>=22)
                {    //如果是全部通道
                    //Toast.makeText(getActivity(), "校验码正确", Toast.LENGTH_SHORT).show();
                    if(crc.equals(s.substring(14,18)))     //判断crc校验码是否正确
                    {
                        String[] states={"0","0","0","0","0","0","0","0","0","0"};   //十个通道的状态，state[0]对应1通道
                        for(int i=0;i<8;i++)   //先获取前八位的开关状态
                        {
                            states[i]=((a[6]&bits[i])==bits[i])?"1":"0";   //1-8通道
                        }
                        for(int i=0;i<2;i++)
                        {
                            states[i+8]=((a[5]&bits[i])==bits[i])?"1":"0";  //9、10通道
                        }
                        Mainstate=((a[5]&bits[2])==bits[2])?1:0;
                        for(int i=0;i<10;i++)
                            data.get(i).put("State", states[Integer.valueOf(data.get(i).get("Channel").equals("a")?"10":data.get(i).get("Channel"))-1]);
                        //   Log.i("Time","解析完数据2+"+String.valueOf(System.currentTimeMillis()));
                        ImageInit();
                        ((Adapter)gridview.getAdapter()).notifyDataSetChanged();
                    }

                }

            }
            Log.i("Order", "是否有效："+IsEffective+"   ==>总开关："+String.valueOf(Mainstate));
        }
    };
    private Handler handler2=new Handler()   //用来控制progressdialog的显示和销毁
    {
        public void handleMessage(Message msg)
        {
            super.handleMessage(msg);
            if(msg.what==0)   //如果是要关闭progresedialog的显示（收到相应通道的反馈，则进行此操作）
            {
                if(progressdialog!= null)
                {
                    progressdialog.dismiss();
                    progressdialog=null;
                }
                //which="100";
            }
            else if(msg.what==1)   //是要显示progressdialog
            {
                progressdialog = new ProgressDialog(getActivity());    //初始化progressdialog
                progressdialog.setCancelable(true);// 设置是否可以通过点击Back键取消
                progressdialog.setCanceledOnTouchOutside(false);// 设置在点击Dialog外是否取消Dialog进度条
                progressdialog.show();
                progressdialog.setContentView(R.layout.progressdialog);    //设置显示的内容
            }
            else if(msg.what==2)   //如果是要根据时间判断是否关闭progressdialog的显示，用于通讯条件不好，收不到反馈时
            {
                if(new Date().getTime()-time>=1900)
                {
                    if(progressdialog!= null)
                    {
                        progressdialog.dismiss();
                        progressdialog=null;
                    }
                    if(!which.equals("100"))
                    {
                        which="100";
                        Toast.makeText(getActivity(), "网络错误！", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            else   //如果是要显示progressdialog
            {
                progressdialog = new ProgressDialog(getActivity());    //初始化progressdialog
                progressdialog.setCancelable(true);// 设置是否可以通过点击Back键取消
                progressdialog.setCanceledOnTouchOutside(false);// 设置在点击Dialog外是否取消Dialog进度条
                progressdialog.show();
                progressdialog.setContentView(R.layout.progressdialog);    //设置显示的内容
            }
        }
    };
    @Override     //当前页面可见与不可见的状态
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser)
        {    //可见时
            try
            {
                //Log.i("Info", "可见");
                //先读取出开关的IP地址
                //在读取SharedPreferences数据前要实例化出一个SharedPreferences对象
                SharedPreferences sharedPreferences= getActivity().getSharedPreferences("data", Activity.MODE_PRIVATE);
                // 使用getString方法获得value，注意第2个参数是value的默认值
                //clientip =sharedPreferences.getString("clientip", "192.168.1.235");
                clientip="192.168.1.235";
                //Toast.makeText(getActivity(), clientip, Toast.LENGTH_LONG).show();
                TCPInit();
            }
            catch (UnknownHostException e)
            {
                // TODO Auto-generated catch block
                Toast.makeText(getActivity().getApplicationContext(),"tcp初始化出错"+e.toString(),Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
            //相当于Fragment的onResume
        }
        else     //不可见时
        {
            //相当于Fragment的onPause    ,关闭socket连接
            try
            {
                if(client!=null)
                    client.close();   //关闭socket连接
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroyView()
    {
        try
        {
            if(client!=null)
                client.close();   //关闭socket连接
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.control, null);
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);//获取屏幕大小的信息
        Pwidth=displayMetrics.widthPixels;   //屏幕宽度
        XRate=Pwidth/849.0;             //X轴缩放比例，Y轴按相同比例缩放
        YRate=XRate*72/100;    //Y轴缩放比例
        //实例化控件
        layout=(LinearLayout)view.findViewById(R.id.layout);		//大框架
        //	swipe_container=(SwipeRefreshLayout)view.findViewById(R.id.swipe_container);   //下拉控件
        scrollView=(ObservableScrollView)view.findViewById(R.id.scrollView);   //滚动条
        layout1=(LinearLayout)view.findViewById(R.id.layout1);		//整体画面
        layout2=(FrameLayout)view.findViewById(R.id.layout2);   //图片部分
        image_bg=(ImageView)view.findViewById(R.id.image_bg);     //房间背景
        gridview=(GridView)view.findViewById(R.id.gridview);   //按钮部分，GridView布局
        images[0]=(ImageView)view.findViewById(R.id.image1);   //左上
        images[1]=(ImageView)view.findViewById(R.id.image2);   //左下
        images[2]=(ImageView)view.findViewById(R.id.image3);   //中间
        images[3]=(ImageView)view.findViewById(R.id.image4);   //右上
        images[4]=(ImageView)view.findViewById(R.id.image5);   //右下
        images[5]=(ImageView)view.findViewById(R.id.image6);   //最右上
        images[6]=(ImageView)view.findViewById(R.id.image7);   //最右下
        images[7]=(ImageView)view.findViewById(R.id.image8);   //大门
        images[8]=(ImageView)view.findViewById(R.id.image9);   //水栈
        images[9]=(ImageView)view.findViewById(R.id.image10);   //水池
        images[10]=(ImageView)view.findViewById(R.id.image11);	//左中窗户
        //UDPInit();
        //Log.i("Info","oncreateview");
        DataInit();    //数据初始化函数，主要是开关状态
        WidgtInit();   //控件初始化函数	，主要包括图片的初始化，监听和gridview的初始化
        return view;
    }
    private void DataInit()    //数据初始化，十个开关的名称和通道号
    {
        Map<String,String> map1=new HashMap<String,String>();
        map1.put("Name", "房间");     //名称
        map1.put("State", "1");         //状态
        map1.put("Channel","2");      //通道号
        data.add(map1);

        Map<String,String> map2=new HashMap<String,String>();
        map2.put("Name", "餐厅");
        map2.put("State", "1");
        map2.put("Channel","1");      //通道号
        data.add(map2);

        Map<String,String> map3=new HashMap<String,String>();
        map3.put("Name", "客厅");
        map3.put("State", "1");
        map3.put("Channel","3");      //通道号
        data.add(map3);

        Map<String,String> map4=new HashMap<String,String>();
        map4.put("Name", "客房");
        map4.put("State", "1");
        map4.put("Channel","6");      //通道号
        data.add(map4);

        Map<String,String> map5=new HashMap<String,String>();
        map5.put("Name", "书房");
        map5.put("State", "1");
        map5.put("Channel","5");      //通道号
        data.add(map5);

        Map<String,String> map6=new HashMap<String,String>();
        map6.put("Name", "观景灯");
        map6.put("State", "1");
        map6.put("Channel","8");      //通道号
        data.add(map6);

        Map<String,String> map7=new HashMap<String,String>();
        map7.put("Name", "阳台");
        map7.put("State", "1");
        map7.put("Channel","7");      //通道号
        data.add(map7);

        Map<String,String> map8=new HashMap<String,String>();
        map8.put("Name", "门灯");
        map8.put("State", "1");
        map8.put("Channel","4");      //通道号
        data.add(map8);

        Map<String,String> map9=new HashMap<String,String>();
        map9.put("Name", "路灯");
        map9.put("State", "1");
        map9.put("Channel","9");      //通道号
        data.add(map9);

        Map<String,String> map10=new HashMap<String,String>();
        map10.put("Name", "泳池");
        map10.put("State", "1");
        map10.put("Channel","a");      //通道号
        data.add(map10);
    }  //数据初始化结束

    /* @Override
     public void onRefresh()
     {
         //Toast.makeText(getActivity().getApplicationContext(), "刷新动作！",Toast.LENGTH_SHORT).show();
         new Handler().postDelayed(new Runnable() {
             @Override public void run() {
                 swipe_container.setRefreshing(false);
             }
         }, 3000);
     }*/
    public void WidgtInit()   //控件初始化
    {
		/*	swipe_container.setOnRefreshListener(this);
		    swipe_container.setColorScheme(android.R.color.holo_blue_bright, 
		            android.R.color.holo_green_light, 
		            android.R.color.holo_orange_light, 
		            android.R.color.holo_red_light);*/

        ImageInit();
        LinearLayout.LayoutParams pa=(LinearLayout.LayoutParams)layout2.getLayoutParams();
        pa.width=Pwidth;
        pa.height=(int)(872*YRate);
        layout2.setLayoutParams(pa);
        //tx_bottom=(TextView)findViewById(R.id.tx_bottom);
        //分别设置各个图片的大小和位置
        FrameLayout.LayoutParams para=new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT);
        para.width=Pwidth;   //背景图宽度=屏幕宽度
        para.height=(int) (872*YRate);
        image_bg.setLayoutParams(para);
        //Toast.makeText(getApplicationContext(), String.valueOf(XRate)+";"+String.valueOf(972*XRate), Toast.LENGTH_SHORT).show();
        FrameLayout.LayoutParams para0=new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT);
        para0.width=(int)(XRate*130);
        para0.height=(int)(140*YRate);
        para0.setMargins((int)(60*XRate), (int)(160*YRate), 0, 0);
        images[0].setLayoutParams(para0);

        FrameLayout.LayoutParams para1=new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT);
        para1.width=(int)(XRate*130);
        para1.height=(int)(130*YRate);
        para1.setMargins((int)(60*XRate), (int)(310*YRate), 0, 0);
        images[1].setLayoutParams(para1);

        FrameLayout.LayoutParams para2=new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT);
        para2.width=(int)(XRate*170);
        para2.height=(int)(425*YRate);
        para2.setMargins((int)(235*XRate), (int)(145*YRate), 0, 0);
        images[2].setLayoutParams(para2);

        FrameLayout.LayoutParams para3=new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT);
        para3.width=(int)(XRate*100);
        para3.height=(int)(125*YRate);
        para3.setMargins((int)(490*XRate), (int)(165*YRate), 0, 0);
        images[3].setLayoutParams(para3);

        FrameLayout.LayoutParams para4=new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT);
        para4.width=(int)(XRate*100);
        para4.height=(int)(120*YRate);
        para4.setMargins((int)(490*XRate), (int)(305*YRate), 0, 0);
        images[4].setLayoutParams(para4);

        FrameLayout.LayoutParams para5=new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT);
        para5.width=(int)(XRate*100);
        para5.height=(int)(115*YRate);
        para5.setMargins((int)(620*XRate), (int)(190*YRate), 0, 0);
        images[5].setLayoutParams(para5);

        FrameLayout.LayoutParams para6=new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT);
        para6.width=(int)(XRate*100);
        para6.height=(int)(120*YRate);
        para6.setMargins((int)(620*XRate), (int)(305*YRate), 0, 0);
        images[6].setLayoutParams(para6);

        FrameLayout.LayoutParams para7=new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT);
        para7.width=(int)(XRate*175);
        para7.height=(int)(165*YRate);
        para7.setMargins((int)(520*XRate), (int)(430*YRate), 0, 0);
        images[7].setLayoutParams(para7);

        FrameLayout.LayoutParams para8=new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT);
        para8.width=(int)(XRate*380);
        para8.height=(int)(130*YRate);
        para8.setMargins(0, (int)(575*YRate), 0, 0);
        images[8].setLayoutParams(para8);

        FrameLayout.LayoutParams para9=new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT);
        para9.width=(int)(XRate*248);
        para9.height=(int)(200*YRate);
        para9.setMargins((int)(600*XRate), (int)(640*YRate), 0, 0);
        images[9].setLayoutParams(para9);

        FrameLayout.LayoutParams para10=new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT);
        para10.width=(int)(XRate*45);
        para10.height=(int)(385*YRate);
        para10.setMargins((int)(95*XRate), (int)(175*YRate), 0, 0);
        images[10].setLayoutParams(para10);

        //分别为ImageView设置点击监听
        images[0].setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Bitmap bmp = null;
                String state=data.get(0).get("State");   //点击之前的通道状态,1表示开，0表示关

                if(state.equals("0"))    //如果之前是关的，则换成打开的
                {
                    SendOrder(data.get(0).get("Channel"),"1");
                }
                else    //如果之前是开的，则换成关闭的
                {
                    SendOrder(data.get(0).get("Channel"),"0");
                }


            }});

        images[1].setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Bitmap bmp = null;
                String state=data.get(1).get("State");   //点击之前的通道状态,1表示开，0表示关

                if(state.equals("0"))    //如果之前是关的，则换成打开的
                {
                    SendOrder(data.get(1).get("Channel"),"1");
                }
                else    //如果之前是开的，则换成关闭的
                {
                    SendOrder(data.get(1).get("Channel"),"0");
                }


            }});

        images[2].setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Bitmap bmp = null;
                String state=data.get(2).get("State");   //点击之前的通道状态,1表示开，0表示关

                if(state.equals("0"))    //如果之前是关的，则换成打开的
                {
                    SendOrder(data.get(2).get("Channel"),"1");
                }
                else    //如果之前是开的，则换成关闭的
                {
                    SendOrder(data.get(2).get("Channel"),"0");
                }

            }});

        images[3].setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Bitmap bmp = null;
                String state=data.get(3).get("State");   //点击之前的通道状态,1表示开，0表示关

                if(state.equals("0"))    //如果之前是关的，则换成打开的
                {
                    SendOrder(data.get(3).get("Channel"),"1");
                }
                else    //如果之前是开的，则换成关闭的
                {
                    SendOrder(data.get(3).get("Channel"),"0");
                }

            }});

        images[4].setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Bitmap bmp = null;
                String state=data.get(4).get("State");   //点击之前的通道状态,1表示开，0表示关

                if(state.equals("0"))    //如果之前是关的，则换成打开的
                {
                    SendOrder(data.get(4).get("Channel"),"1");
                }
                else    //如果之前是开的，则换成关闭的
                {
                    SendOrder(data.get(4).get("Channel"),"0");
                }

            }});

        images[5].setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Bitmap bmp = null;
                String state=data.get(5).get("State");   //点击之前的通道状态,1表示开，0表示关

                if(state.equals("0"))    //如果之前是关的，则换成打开的
                {
                    SendOrder(data.get(5).get("Channel"),"1");
                }
                else    //如果之前是开的，则换成关闭的
                {
                    SendOrder(data.get(5).get("Channel"),"0");
                }

            }});

        images[6].setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Bitmap bmp = null;
                String state=data.get(6).get("State");   //点击之前的通道状态,1表示开，0表示关

                if(state.equals("0"))    //如果之前是关的，则换成打开的
                {
                    SendOrder(data.get(6).get("Channel"),"1");
                }
                else    //如果之前是开的，则换成关闭的
                {
                    SendOrder(data.get(6).get("Channel"),"0");
                }

            }});

        images[7].setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Bitmap bmp = null;
                String state=data.get(7).get("State");   //点击之前的通道状态,1表示开，0表示关

                if(state.equals("0"))    //如果之前是关的，则换成打开的
                {
                    SendOrder(data.get(7).get("Channel"),"1");
                }
                else    //如果之前是开的，则换成关闭的
                {
                    SendOrder(data.get(7).get("Channel"),"0");
                }

            }});

        images[8].setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Bitmap bmp = null;
                String state=data.get(8).get("State");   //点击之前的通道状态,1表示开，0表示关

                if(state.equals("0"))    //如果之前是关的，则换成打开的
                {
                    SendOrder(data.get(8).get("Channel"),"1");
                }
                else    //如果之前是开的，则换成关闭的
                {
                    SendOrder(data.get(8).get("Channel"),"0");
                }

            }});

        images[9].setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Bitmap bmp = null;
                String state=data.get(9).get("State");   //点击之前的通道状态,1表示开，0表示关

                if(state.equals("0"))    //如果之前是关的，则换成打开的
                {
                    SendOrder(data.get(9).get("Channel"),"1");
                }
                else    //如果之前是开的，则换成关闭的
                {
                    SendOrder(data.get(9).get("Channel"),"0");
                }

            }});

        int columnwidth=displayMetrics.widthPixels*2/12;   //单个项目宽度
        gridview.setColumnWidth(columnwidth);
        LinearLayout.LayoutParams linearParams = (LinearLayout.LayoutParams) gridview.getLayoutParams(); // 取控件mGrid当前的布局参数
        linearParams.height = (columnwidth+Converts.dip2px(getActivity().getApplication(), 52))*2;     // 设置高度
        gridview.setLayoutParams(linearParams);    // 给GridView赋值，根据每个item的宽度设置整个gridview的高度
        gridview.setAdapter(new Adapter()); //为gridview设置适配器

        scrollView.smoothScrollTo(0, 0);

    }   //控件初始化结束

    //开关图片状态初始化，根据开关状态为图片设定相应图片
    public void ImageInit()
    {
        //图1
        if(data.get(0).get("State").equals("1"))  //如果图1,状态是开
            images[0].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image1_on));
        else
            images[0].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image1_off));
        //图2
        if(data.get(1).get("State").equals("1"))  //如果图1,状态是开
            images[1].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image2_on));
        else
            images[1].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image2_off));
        //图3和图11
        if(data.get(2).get("State").equals("1"))  //如果图1,状态是开
        {
            images[2].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image3_on));
            images[10].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image11_on));
        }
        else
        {
            images[2].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image3_off));
            images[10].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image11_off));
        }
        //图4
        if(data.get(3).get("State").equals("1"))  //如果图1,状态是开
            images[3].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image4_on));
        else
            images[3].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image4_off));
        //图5
        if(data.get(4).get("State").equals("1"))  //如果图1,状态是开
            images[4].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image5_on));
        else
            images[4].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image5_off));
        //图6
        if(data.get(5).get("State").equals("1"))  //如果图1,状态是开
            images[5].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image6_on));
        else
            images[5].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image6_off));
        //图7
        if(data.get(6).get("State").equals("1"))  //如果图1,状态是开
            images[6].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image7_on));
        else
            images[6].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image7_off));
        //图8
        if(data.get(7).get("State").equals("1"))  //如果图1,状态是开
            images[7].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image8_on));
        else
            images[7].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image8_off));
        //图9
        if(data.get(8).get("State").equals("1"))  //如果图1,状态是开
            images[8].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image9_on));
        else
            images[8].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image9_off));
        //图10
        if(data.get(9).get("State").equals("1"))  //如果图1,状态是开
            images[9].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image10_on));
        else
            images[9].setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image10_off));


    }

    private void TCPInit() throws UnknownHostException{
        //获取服务器ip
        final InetAddress serverAddr = InetAddress.getByName(clientip);// TCPServer.SERVERIP
        //获取服务器端口
        final int port=2000;
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
                    String toServer = "ab68 0003 0100 0001";    //查询开关所有通道的状态
                    //  String toServer="ab68 0006 0200 0000";
                    toServer.replace(" ","");    //去掉空格
                    byte[] bt=null;
                    bt=Converts.HexString2Bytes(toServer);
                    String str=toServer+Converts.GetCRC(bt, 2, bt.length)+"0d0a";   //添加校验码
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
                Log.i("Info", "TCP接收监听退出");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private void SendOrder(final String channel,final String state)  //向开关发送命令
    {
        if(which.equals("100"))   //首先判断是否有开关命令正在执行，如果没有则向开关发送命令
        {
            //Log.i("Time","点击按钮+"+String.valueOf(System.currentTimeMillis()));
					/*
*/					time=new Date().getTime();    //获取现在的时间，单位是ms
            which=channel;    //设置which的值，表明是第position行的开关状态发生了改变
            Timer timer = new Timer(true);     //定义定时器，定时执行关闭progressdialog命令，定时时长为2秒
            timer.schedule(new TimerTask(){
                public void run() {     //在新线程中执行
                    if(!which.equals("100"))
                    {
                        Message message = new Message();
                        message.what = 1;       //1表示要显示
                        handler2.sendMessage(message);
                    }
                }
            } ,200); //0.2s后判断是否关闭progressdialog，若没关闭，则进行关闭
            Timer timer1 = new Timer(true);     //定义定时器，定时执行关闭progressdialog命令，定时时长为2秒
            timer1.schedule(new TimerTask(){
                public void run() {     //在新线程中执行
                    if(!which.equals(100))
                    {
                        Message message = new Message();
                        message.what = 2;       //2表示要隐藏
                        handler2.sendMessage(message);
                    }
                }
            } ,2500); //2.5s后判断是否关闭progressdialog，若没关闭，则进行关闭
            new Thread()   //新建子线程，发送命令
            {
                public void run(){
                    DataOutputStream out;
                    try
                    {
                        out = new DataOutputStream(client.getOutputStream());
                        if(Mainstate==0&&state.equals("1"))   //如果主开关是关的，则先打开总开关
                        {
                            String toServer = "ab68 0006 0300 0001";    //指令
                            toServer.replace(" ","");    //去掉空格
                            byte[] bt=null;
                            bt=Converts.HexString2Bytes(toServer);
                            String str=toServer+Converts.GetCRC(bt, 2, bt.length)+"0d0a";   //添加校验码
                            Log.i("Order","发送的命令+"+str);
                            byte[] bt1=Converts.HexString2Bytes(str);      //将完整的命令转换成十六进制
                            if(!client.isClosed())
                            {
                                out.write(bt1);
                                out.flush();
                                // out.close();
                            }
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                        String toServer = "ab68 0006 030"+channel+" 000"+state;    //指令
                        toServer.replace(" ","");    //去掉空格
                        byte[] bt=null;
                        bt=Converts.HexString2Bytes(toServer);
                        String str=toServer+Converts.GetCRC(bt, 2, bt.length)+"0d0a";   //添加校验码
                        Log.i("Order","发送的命令+"+str);
                        byte[] bt1=Converts.HexString2Bytes(str);      //将完整的命令转换成十六进制
                        if(!client.isClosed())
                        {
                            out.write(bt1);
                            out.flush();
                            // out.close();   //关闭输出流
                        }
                    }
                    catch (Exception e) {			// 发送出错，证明TCP断开了连接，重新建立连接
                        try
                        {
                            InetAddress serverAddr = InetAddress.getByName(clientip);// TCPServer.SERVERIP
                            client = new Socket(serverAddr,8000);   //新建TCP连接
                            out=new DataOutputStream(client.getOutputStream());
                            new TCPServerThread().start();

                            if(Mainstate==0&&state.equals("1"))   //如果主开关是关的，则先打开总开关
                            {
                                String toServer = "ab68 0006 0300 0001";    //指令
                                toServer.replace(" ","");    //去掉空格
                                byte[] bt=null;
                                bt=Converts.HexString2Bytes(toServer);
                                String str=toServer+Converts.GetCRC(bt, 2, bt.length)+"0d0a";   //添加校验码
                                Log.i("Order","发送的命令+"+str);
                                byte[] bt1=Converts.HexString2Bytes(str);      //将完整的命令转换成十六进制
                                if(!client.isClosed())
                                {
                                    out.write(bt1);
                                    out.flush();
                                    // out.close();
                                }
                                try {
                                    Thread.sleep(200);
                                } catch (InterruptedException ex) {
                                    // TODO Auto-generated catch block
                                    ex.printStackTrace();
                                }
                            }
                            String toServer = "ab68 0006 030"+channel+" 000"+state;    //指令
                            toServer.replace(" ","");    //去掉空格
                            byte[] bt=null;
                            bt=Converts.HexString2Bytes(toServer);
                            String str=toServer+Converts.GetCRC(bt, 2, bt.length)+"0d0a";   //添加校验码
                            Log.i("Order","发送的命令+"+str);
                            byte[] bt1=Converts.HexString2Bytes(str);      //将完整的命令转换成十六进制
                            if(!client.isClosed())
                            {
                                out.write(bt1);
                                out.flush();
                                // out.close();   //关闭输出流
                            }
                        }
                        catch (Exception ee){}

                    }
                }
            }.start();
            //Log.i("Time","发送命令+"+String.valueOf(System.currentTimeMillis()));
        }
    }

    public class ViewHolder{
        private TextView tx_name;
        private ImageView image;
    }
    //自定义GridView的Adapter
    class Adapter extends BaseAdapter
    {
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
            final String name=data.get(position).get("Name");
            final String state=data.get(position).get("State");
            ViewHolder vh;
            if(convertView==null) {
                convertView = LayoutInflater.from(getActivity().getApplication()).inflate(R.layout.control_gridview, null);
                vh=new ViewHolder();
                vh.tx_name = (TextView) convertView.findViewById(R.id.name);    //开关名称
                vh.image = (ImageView) convertView.findViewById(R.id.image); //开关图片
                convertView.setTag(vh);   //为这个convertView添加标签，通过它可以找到viewholder
            }
            vh = (ViewHolder) convertView.getTag();
            ViewGroup.LayoutParams para=(ViewGroup.LayoutParams)vh.image.getLayoutParams();
            //	para.height=para.width;
            para.width=Pwidth*2/13;
            para.height=para.width;
            vh.image.setLayoutParams(para);    //设置高度=宽度,宽度是根据屏幕设置的
            vh.tx_name.setText(name);
            Bitmap bmp;
            if(state.equals("1"))
                bmp= BitmapFactory.decodeResource(getResources(), R.drawable.light_on);
            else
                bmp= BitmapFactory.decodeResource(getResources(), R.drawable.light_off);
            bmp=convert.Converts.toRoundCorner(bmp, 40);  //实现图片的圆角
            vh.image.setImageBitmap(bmp);
            convertView.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    if(data.get(position).get("State").equals("1"))   //如果点击前，开关是开的，则关闭
                    {
							/*Bitmap bm= BitmapFactory.decodeResource(getResources(), R.drawable.light_off);
							bm=convert.Converts.toRoundCorner(bm, 40);  //实现图片的圆角
				            image.setImageBitmap(bm);
				            data.get(position).put("State", "0");*/
                        SendOrder(data.get(position).get("Channel"),"0");
                    }
                    else   //如果点击前开关是关的,则打开
                    {
							/*Bitmap bm= BitmapFactory.decodeResource(getResources(), R.drawable.light_on);
							bm=convert.Converts.toRoundCorner(bm, 40);  //实现图片的圆角
				            image.setImageBitmap(bm);
				            data.get(position).put("State", "1");*/
                        SendOrder(data.get(position).get("Channel"),"1");
                    }
                    //ImageInit();  //重新刷新开关图片显示
                }});
            return convertView;
        }
    }


}
