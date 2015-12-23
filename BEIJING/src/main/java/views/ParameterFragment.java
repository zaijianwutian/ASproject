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

import services.MainService;
import views.Switch.OnSwitchChangedListener;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.suntrans.beijing.Address;
import com.suntrans.beijing.Main_Activity;
import com.suntrans.beijing.R;
import convert.Converts;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class ParameterFragment extends Fragment {
    private PullToRefreshListView mPullRefreshListView;    //下拉列表控件
    private ListView list;   //列表
    private SeekBar seekbar_r,seekbar_g,seekbar_b;   //三个滚动条，红绿蓝
    private views.Switch switch_r,switch_g,switch_b;     //对应的三个开关，红绿蓝
    private String clientip;    //保存开关的IP地址
    private Socket client;    //保持TCP连接的socket
    private DisplayMetrics displayMetrics = new DisplayMetrics();
    private int Pwidth=0;  //屏幕宽度，单位是pixel
    private String device="手机";   //是手机还是平板，根据屏幕宽度判断
    private String addr= Address.addr_out;   //外间地址，默认0004
    private ProgressDialog progressdialog;
    private ArrayList<Map<String, String>> data_room=new ArrayList<Map<String, String>>();    //室内环境    外间
    private ArrayList<Map<String, String>> data_air=new ArrayList<Map<String, String>>();    //空气质量     外间
    private ArrayList<Map<String, String>> data_room1 = new ArrayList<Map<String,String>>();   ///室内环境  里间
    private ArrayList<Map<String, String>> data_air1=new ArrayList<Map<String, String>>();    //空气质量     里间
    private MainService.ibinder binder=null;    //用于activity与service通讯的接口类
    private String which="100";    //用来标示是否有命令正在发送还没有返回，100表示没有正在发送的数据,2表示刷新所有参数，
    private long time;   //触发progressdialog显示的时间
    public Handler handler1=new Handler()   //接收到TCP发回的数据后调用，用于更新列表中的开关状态，即反馈
    {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.arg1 == 1) {    //房间进行了切换，刷新List的显示
                ListInit();
                if(((Main_Activity)getActivity()).flag_room.equals("外间"))
                    addr=Address.addr_out;
                else
                    addr=Address.addr_in;
            }
            else   //收到新的数据，更新数据
            {
                Map<String, Object> map = (Map<String, Object>) msg.obj;
                byte[] a = (byte[]) (map.get("data"));    //byte数组a即为客户端发回的数据，aa68 0006是单个开通道，aa68 0003是所有的通道
               // String ipaddr = (String) (map.get("ipaddr"));    //开关的IP地址
                String s = "";                       //保存命令的十六进制字符串
                for (int i = 0; i < msg.what; i++) {
                    String s1 = Integer.toHexString((a[i] + 256) % 256);   //byte转换成十六进制字符串(先把byte转换成0-255之间的非负数，因为java中的数据都是带符号的)
                    if (s1.length() == 1)
                        s1 = "0" + s1;
                    s = s + s1;
                }
                //   String crc=Converts.GetCRC(a, 2, msg.what-2-2);    //获取返回数据的校验码，倒数第3、4位是验证码，倒数第1、2位是包尾0d0a
                s = s.replace(" ", ""); //去掉空格
             //   Log.i("Order", "收到数据：" + s);
                int IsEffective = 0;    //指令是否有效，0表示无效，1表示有效；对于和第六感官通讯而言，包头为ab68的数据才有效
                if (msg.what > 44) {
                    if (s.substring(0, 10).equals("ab68"+Address.addr_out+"f0"))
                        IsEffective = 1;    //外间
                    else if(s.substring(0, 10).equals("ab68"+Address.addr_in+"f0"))
                        IsEffective=2;   //里间
                }
                if (IsEffective == 1)   //如果数据有效，则进行解析，并更新页面，外间
                {
                    //if (s.substring(10, 12).equals("04"))   //如果是读寄存器状态，则判断是读寄存器1（室内参数），还是寄存器2（灯光信息）的状态
                  //  {
                        if (s.substring(12, 14).equals("22"))  //寄存器1，长度34个字节
                        {
                            //计算得到各个参数的值，顺序是按寄存器顺序来的
                            double tmp = (((a[7] + 256) % 256) * 256 + (a[8] + 256) % 256) / 100.0;   //温度
                            double humidity = (((a[13] + 256) % 256) * 256 + (a[14] + 256) % 256) + 20;   //湿度
                            double atm = (((a[15] + 256) % 256) * 256 + (a[16] + 256) % 256) / 100.0;       //大气压
                            double arofene = (((a[17] + 256) % 256) * 256 + (a[18] + 256) % 256) / 1000.0;    //甲醛
                            double smoke = (((a[19] + 256) % 256) * 256 + (a[20] + 256) % 256);       //烟雾
                            double staff = (((a[21] + 256) % 256) * 256 + (a[22] + 256) % 256);     //人员信息
                            double light = (((a[23] + 256) % 256) * 256 + (a[24] + 256) % 256);  //光感
                            double pm1 = (((a[25] + 256) % 256) * 256 + (a[26] + 256) % 256);     //PM1
                            double pm25 = (((a[27] + 256) % 256) * 256 + (a[28] + 256) % 256);     //PM2.5
                            double pm10 = (((a[29] + 256) % 256) * 256 + (a[30] + 256) % 256);     //PM10

                            data_room.get(0).put("Value", String.valueOf(tmp) + " ℃");  //温度值
                            data_room.get(1).put("Value", String.valueOf(humidity) + " %RH");  //湿度
                            data_room.get(2).put("Value", String.valueOf(atm) + " kPa");   //大气压
                            data_room.get(3).put("Value", String.valueOf(staff) + " ");     //人员信息
                            data_room.get(4).put("Value", String.valueOf(light) + " ");    //光感

                            data_air.get(0).put("Value", String.valueOf(smoke) + " ppm");    //烟雾
                            data_air.get(1).put("Value", String.valueOf(arofene) + " ppm");   //甲醛
                            data_air.get(2).put("Value", String.valueOf(pm1) + " ");     //PM1
                            data_air.get(3).put("Value", String.valueOf(pm25) + " ");   //PM2.5
                            data_air.get(4).put("Value", String.valueOf(pm10) + " ");   //PM10

                            //评估，空气质量部分
                            String eva = "null";//评估，优、良、轻度污染、中度污染、重度污染、严重污染
                            int progress = 0;//进度
                            if (smoke <= 750) {
                                eva = "清洁";
                                progress = (int) (smoke / 750 * 100 / 6);
                            } else {
                                eva = "污染";
                                progress = (int) (100 / 6 + (smoke - 750) * 500 / 9250 / 6);
                            }
                            data_air.get(0).put("Evaluate", eva);     //烟雾0-10000
                            data_air.get(0).put("Progress", String.valueOf(progress));

                            eva = "null";   //评估甲醛，0-1000
                            progress = 0;
                            if (arofene <= 0.1) {
                                eva = "清洁";
                                progress = (int) (arofene / 0.1 * 100 / 6);
                            } else {
                                eva = "超标";
                                progress = (int) (100 / 6 + (arofene - 0.1) * 500 / 6);
                                if (progress >= 80)
                                    progress = 80;
                            }
                            data_air.get(1).put("Evaluate", eva);     //甲醛，假设是0-1
                            data_air.get(1).put("Progress", String.valueOf(progress));

                            eva = "null";
                            progress = 0;
                            //评估,PM1
                            if (pm1 <= 35) {
                                eva = "优";
                                progress = (int) (pm1 / 35 / 6 * 100);
                            } else if (pm1 <= 75) {
                                eva = "良";
                                progress = (int) ((pm1 - 35) / 240 * 100 + 100 / 6);
                            } else if (pm1 <= 115) {
                                eva = "轻度污染";
                                progress = (int) ((pm1 - 75) / 240 * 100 + 200 / 6);
                            } else if (pm1 <= 150) {
                                eva = "中度污染";
                                progress = (int) ((pm1 - 115) / 35 / 6 * 100 + 300 / 6);
                            } else if (pm1 <= 250) {
                                eva = "重度污染";
                                progress = (int) ((pm1 - 150) / 6 + 400 / 6);
                            } else {
                                eva = "严重污染";
                                progress = 90;
                            }
                            data_air.get(2).put("Evaluate", eva);     //PM1
                            data_air.get(2).put("Progress", String.valueOf(progress));


                            eva = "null";  //评估,PM2.5
                            progress = 0;
                            if (pm25 <= 35) {
                                eva = "优";
                                progress = (int) (pm25 / 35 / 6 * 100);
                            } else if (pm25 <= 75) {
                                eva = "良";
                                progress = (int) ((pm25 - 35) / 240 * 100 + 100 / 6);
                            } else if (pm25 <= 115) {
                                eva = "轻度污染";
                                progress = (int) ((pm25 - 75) / 240 * 100 + 200 / 6);
                            } else if (pm25 <= 150) {
                                eva = "中度污染";
                                progress = (int) ((pm25 - 115) / 35 / 6 * 100 + 300 / 6);
                            } else if (pm25 <= 250) {
                                eva = "重度污染";
                                progress = (int) ((pm25 - 150) / 6 + 400 / 6);
                            } else {
                                eva = "严重污染";
                                progress = 90;
                            }
                            data_air.get(3).put("Evaluate", eva);     //PM2.5
                            data_air.get(3).put("Progress", String.valueOf(progress));
                            // Log.i("Order","计算得到："+String.valueOf(progress));

                            eva = "null";  //评估,PM10
                            progress = 0;
                            if (pm10 <= 50) {
                                eva = "优";
                                progress = (int) (pm10 / 50 * 100 / 6);
                            } else if (pm10 <= 150) {
                                eva = "良";
                                progress = (int) ((pm10 - 50) / 6 + 100 / 6);
                            } else if (pm10 <= 250) {
                                eva = "轻度污染";
                                progress = (int) ((pm10 - 150) / 6 + 200 / 6);
                            } else if (pm10 <= 350) {
                                eva = "中度污染";
                                progress = (int) ((pm10 - 250) / 6 + 300 / 6);
                            } else if (pm10 <= 420) {
                                eva = "重度污染";
                                progress = (int) ((pm10 - 350) / 420 + 400 / 6);
                            } else {
                                eva = "严重污染";
                                progress = 90;
                            }
                            data_air.get(4).put("Evaluate", eva);     //PM10
                            data_air.get(4).put("Progress", String.valueOf(progress));


                            //评估，室内信息部分

                            eva = "null";    //评估温度
                            progress = 0;
                            if (tmp <= 10) {
                                eva = "极寒";
                                progress = 50 / 6;
                            } else if (tmp <= 15) {
                                eva = "寒冷";
                                progress = (int) ((tmp - 10) / 5 * 100 / 6 + 100 / 6);
                            } else if (tmp <= 20) {
                                eva = "凉爽";
                                progress = (int) ((tmp - 15) / 5 * 100 / 6 + 200 / 6);
                            } else if (tmp <= 28) {
                                eva = "舒适";
                                progress = (int) ((tmp - 20) / 8 * 100 / 6 + 300 / 6);
                            } else if (tmp <= 34) {
                                eva = "闷热";
                                progress = (int) ((tmp - 28) / 6 * 100 / 6 + 400 / 6);
                            } else {
                                eva = "极热";
                                progress = 550 / 6;
                            }
                            data_room.get(0).put("Evaluate", eva);     //温度
                            data_room.get(0).put("Progress", String.valueOf(progress));

                            eva = "null";   //评估湿度
                            progress = 0;
                            if (humidity <= 40) {
                                eva = "干燥";
                                progress = (int) (humidity / 40.0 * 100 / 3.0);
                            } else if (humidity <= 70) {
                                eva = "舒适";
                                progress = (int) ((humidity - 40) / 30.0 * 100 / 3.0 + 100 / 3.0);
                            } else {
                                eva = "潮湿";
                                progress = (int) ((humidity - 70) / 30.0 * 100 / 3.0 + 200 / 3.0);
                            }
                            data_room.get(1).put("Evaluate", eva);     //湿度
                            data_room.get(1).put("Progress", String.valueOf(progress));

                            eva = "null";  //评估气压
                            progress = 0;
                            if (atm >= 110) {
                                eva = "气压高";
                                progress = 80;
                            } else if (atm <= 90) {
                                eva = "气压低";
                                progress = 20;
                            } else {
                                eva = "正常";
                                progress = 50;
                            }
                            data_room.get(2).put("Evaluate", eva);     //大气压
                            data_room.get(2).put("Progress", String.valueOf(progress));

                            data_room.get(3).put("Evaluate", staff == 1 ? "有人" : "无人");     //评估人员信息

                            eva = "null";   //评估光感
                            progress = 0;
                            if (light == 0) {
                                eva = "极弱";
                                progress = 10;
                            } else if (light == 1) {
                                eva = "适中";
                                progress = 30;
                            } else if (light == 2) {
                                eva = "强";
                                progress = 50;
                            } else if (light == 3) {
                                eva = "很强";
                                progress = 70;
                            } else {
                                eva = "极强";
                                progress = 90;
                            }
                            data_room.get(4).put("Evaluate", eva);     //光感
                            data_room.get(4).put("Progress", String.valueOf(progress));
                            int x=mPullRefreshListView.getScrollX();
                            int y=mPullRefreshListView.getScrollY();
                            ListInit();
                            mPullRefreshListView.scrollTo(x, y);
                            //   list.setAdapter(new Adapter());    //为listview设置适配器
                            //((SectionListAdapter)list.getAdapter()).notifyDataSetChanged();

                        }
                  //  }
                }
                else if (IsEffective == 2)   //如果数据有效，则进行解析，并更新页面.里间
                {
                   // if (s.substring(10, 12).equals("04"))   //如果是读寄存器状态，则判断是读寄存器1（室内参数），还是寄存器2（灯光信息）的状态
                   // {
                        if (s.substring(12, 14).equals("22"))  //寄存器1，长度34个字节
                        {
                            //计算得到各个参数的值，顺序是按寄存器顺序来的
                            double tmp = (((a[7] + 256) % 256) * 256 + (a[8] + 256) % 256) / 100.0;   //温度
                            double humidity = (((a[13] + 256) % 256) * 256 + (a[14] + 256) % 256) + 20;   //湿度
                            double atm = (((a[15] + 256) % 256) * 256 + (a[16] + 256) % 256) / 100.0;       //大气压
                            double arofene = (((a[17] + 256) % 256) * 256 + (a[18] + 256) % 256) / 1000.0;    //甲醛
                            double smoke = (((a[19] + 256) % 256) * 256 + (a[20] + 256) % 256);       //烟雾
                            double staff = (((a[21] + 256) % 256) * 256 + (a[22] + 256) % 256);     //人员信息
                            double light = (((a[23] + 256) % 256) * 256 + (a[24] + 256) % 256);  //光感
                            double pm1 = (((a[25] + 256) % 256) * 256 + (a[26] + 256) % 256);     //PM1
                            double pm25 = (((a[27] + 256) % 256) * 256 + (a[28] + 256) % 256);     //PM2.5
                            double pm10 = (((a[29] + 256) % 256) * 256 + (a[30] + 256) % 256);     //PM10

                            data_room1.get(0).put("Value", String.valueOf(tmp) + " ℃");  //温度值
                            data_room1.get(1).put("Value", String.valueOf(humidity) + " %RH");  //湿度
                            data_room1.get(2).put("Value", String.valueOf(atm) + " kPa");   //大气压
                            data_room1.get(3).put("Value", String.valueOf(staff) + " ");     //人员信息
                            data_room1.get(4).put("Value", String.valueOf(light) + " ");    //光感

                            data_air1.get(0).put("Value", String.valueOf(smoke) + " ppm");    //烟雾
                            data_air1.get(1).put("Value", String.valueOf(arofene) + " ppm");   //甲醛
                            data_air1.get(2).put("Value", String.valueOf(pm1) + " ");     //PM1
                            data_air1.get(3).put("Value", String.valueOf(pm25) + " ");   //PM2.5
                            data_air1.get(4).put("Value", String.valueOf(pm10) + " ");   //PM10

                            //评估，空气质量部分
                            String eva = "null";//评估，优、良、轻度污染、中度污染、重度污染、严重污染
                            int progress = 0;//进度
                            if (smoke <= 750) {
                                eva = "清洁";
                                progress = (int) (smoke / 750 * 100 / 6);
                            } else {
                                eva = "污染";
                                progress = (int) (100 / 6 + (smoke - 750) * 500 / 9250 / 6);
                            }
                            data_air1.get(0).put("Evaluate", eva);     //烟雾0-10000
                            data_air1.get(0).put("Progress", String.valueOf(progress));

                            eva = "null";   //评估甲醛，0-1000
                            progress = 0;
                            if (arofene <= 0.1) {
                                eva = "清洁";
                                progress = (int) (arofene / 0.1 * 100 / 6);
                            } else {
                                eva = "超标";
                                progress = (int) (100 / 6 + (arofene - 0.1) * 500 / 6);
                                if (progress >= 80)
                                    progress = 80;
                            }
                            data_air1.get(1).put("Evaluate", eva);     //甲醛，假设是0-1
                            data_air1.get(1).put("Progress", String.valueOf(progress));

                            eva = "null";
                            progress = 0;
                            //评估,PM1
                            if (pm1 <= 35) {
                                eva = "优";
                                progress = (int) (pm1 / 35 / 6 * 100);
                            } else if (pm1 <= 75) {
                                eva = "良";
                                progress = (int) ((pm1 - 35) / 240 * 100 + 100 / 6);
                            } else if (pm1 <= 115) {
                                eva = "轻度污染";
                                progress = (int) ((pm1 - 75) / 240 * 100 + 200 / 6);
                            } else if (pm1 <= 150) {
                                eva = "中度污染";
                                progress = (int) ((pm1 - 115) / 35 / 6 * 100 + 300 / 6);
                            } else if (pm1 <= 250) {
                                eva = "重度污染";
                                progress = (int) ((pm1 - 150) / 6 + 400 / 6);
                            } else {
                                eva = "严重污染";
                                progress = 90;
                            }
                            data_air1.get(2).put("Evaluate", eva);     //PM1
                            data_air1.get(2).put("Progress", String.valueOf(progress));


                            eva = "null";  //评估,PM2.5
                            progress = 0;
                            if (pm25 <= 35) {
                                eva = "优";
                                progress = (int) (pm25 / 35 / 6 * 100);
                            } else if (pm25 <= 75) {
                                eva = "良";
                                progress = (int) ((pm25 - 35) / 240 * 100 + 100 / 6);
                            } else if (pm25 <= 115) {
                                eva = "轻度污染";
                                progress = (int) ((pm25 - 75) / 240 * 100 + 200 / 6);
                            } else if (pm25 <= 150) {
                                eva = "中度污染";
                                progress = (int) ((pm25 - 115) / 35 / 6 * 100 + 300 / 6);
                            } else if (pm25 <= 250) {
                                eva = "重度污染";
                                progress = (int) ((pm25 - 150) / 6 + 400 / 6);
                            } else {
                                eva = "严重污染";
                                progress = 90;
                            }
                            data_air1.get(3).put("Evaluate", eva);     //PM2.5
                            data_air1.get(3).put("Progress", String.valueOf(progress));
                            // Log.i("Order","计算得到："+String.valueOf(progress));

                            eva = "null";  //评估,PM10
                            progress = 0;
                            if (pm10 <= 50) {
                                eva = "优";
                                progress = (int) (pm10 / 50 * 100 / 6);
                            } else if (pm10 <= 150) {
                                eva = "良";
                                progress = (int) ((pm10 - 50) / 6 + 100 / 6);
                            } else if (pm10 <= 250) {
                                eva = "轻度污染";
                                progress = (int) ((pm10 - 150) / 6 + 200 / 6);
                            } else if (pm10 <= 350) {
                                eva = "中度污染";
                                progress = (int) ((pm10 - 250) / 6 + 300 / 6);
                            } else if (pm10 <= 420) {
                                eva = "重度污染";
                                progress = (int) ((pm10 - 350) / 420 + 400 / 6);
                            } else {
                                eva = "严重污染";
                                progress = 90;
                            }
                            data_air1.get(4).put("Evaluate", eva);     //PM10
                            data_air1.get(4).put("Progress", String.valueOf(progress));


                            //评估，室内信息部分

                            eva = "null";    //评估温度
                            progress = 0;
                            if (tmp <= 10) {
                                eva = "极寒";
                                progress = 50 / 6;
                            } else if (tmp <= 15) {
                                eva = "寒冷";
                                progress = (int) ((tmp - 10) / 5 * 100 / 6 + 100 / 6);
                            } else if (tmp <= 20) {
                                eva = "凉爽";
                                progress = (int) ((tmp - 15) / 5 * 100 / 6 + 200 / 6);
                            } else if (tmp <= 28) {
                                eva = "舒适";
                                progress = (int) ((tmp - 20) / 8 * 100 / 6 + 300 / 6);
                            } else if (tmp <= 34) {
                                eva = "闷热";
                                progress = (int) ((tmp - 28) / 6 * 100 / 6 + 400 / 6);
                            } else {
                                eva = "极热";
                                progress = 550 / 6;
                            }
                            data_room1.get(0).put("Evaluate", eva);     //温度
                            data_room1.get(0).put("Progress", String.valueOf(progress));

                            eva = "null";   //评估湿度
                            progress = 0;
                            if (humidity <= 40) {
                                eva = "干燥";
                                progress = (int) (humidity / 40.0 * 100 / 3.0);
                            } else if (humidity <= 70) {
                                eva = "舒适";
                                progress = (int) ((humidity - 40) / 30.0 * 100 / 3.0 + 100 / 3.0);
                            } else {
                                eva = "潮湿";
                                progress = (int) ((humidity - 70) / 30.0 * 100 / 3.0 + 200 / 3.0);
                            }
                            data_room1.get(1).put("Evaluate", eva);     //湿度
                            data_room1.get(1).put("Progress", String.valueOf(progress));

                            eva = "null";  //评估气压
                            progress = 0;
                            if (atm >= 110) {
                                eva = "气压高";
                                progress = 80;
                            } else if (atm <= 90) {
                                eva = "气压低";
                                progress = 20;
                            } else {
                                eva = "正常";
                                progress = 50;
                            }
                            data_room1.get(2).put("Evaluate", eva);     //大气压
                            data_room1.get(2).put("Progress", String.valueOf(progress));

                            data_room1.get(3).put("Evaluate", staff == 1 ? "有人" : "无人");     //评估人员信息

                            eva = "null";   //评估光感
                            progress = 0;
                            if (light == 0) {
                                eva = "极弱";
                                progress = 10;
                            } else if (light == 1) {
                                eva = "适中";
                                progress = 30;
                            } else if (light == 2) {
                                eva = "强";
                                progress = 50;
                            } else if (light == 3) {
                                eva = "很强";
                                progress = 70;
                            } else {
                                eva = "极强";
                                progress = 90;
                            }
                            data_room1.get(4).put("Evaluate", eva);     //光感
                            data_room1.get(4).put("Progress", String.valueOf(progress));
                            int x=mPullRefreshListView.getScrollX();
                            int y=mPullRefreshListView.getScrollY();
                            ListInit();
                            mPullRefreshListView.scrollTo(x,y);
                            //   list.setAdapter(new Adapter());    //为listview设置适配器
                            //((SectionListAdapter)list.getAdapter()).notifyDataSetChanged();

                        }
                  //  }
                }
                //Toast.makeText(getActivity(), s, Toast.LENGTH_LONG).show();
            }
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
                    //	ListInit();
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
    public void setUserVisibleHint(boolean isVisibleToUser) {   //这里的可见与不可见只在fragment切换时发生变化。
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser)
        {    //可见时
            try
            {   Log.i("Order", "parameter可见");
                mPullRefreshListView.setRefreshing();
            }
            catch (Exception e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            //相当于Fragment的onResume
        }
        else     //不可见时
        {
            //相当于Fragment的onPause    ,关闭socket连接
            try
            {
                Log.i("Order", "parameter不可见");

            }
            catch (Exception e)
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
            Log.i("Order","parameter销毁");
          //  getActivity().unbindService(con);   //解除Service的绑定
        }
        catch (Exception e)
        {
            Log.i("Order","parameter销毁出错");
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        super.onDestroyView();
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        Log.i("Order", "Parameter==>onCreateView");
        DataInit();    //数据初始化
        View view = inflater.inflate(R.layout.parameter, null);
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);//获取屏幕大小的信息
        Pwidth=displayMetrics.widthPixels;   //屏幕宽度,先锋的宽度是800px，小米2a的宽度是720px
       // Toast.makeText(getActivity().getApplication(), String.valueOf(Converts.px2dip(getActivity().getApplicationContext(),(int)Pwidth)), Toast.LENGTH_SHORT).show();
        int pwidth = Converts.px2dip(getActivity().getApplicationContext(), (int) Pwidth);
        if(pwidth>450)
            device="平板";
        else
            device="手机";
        mPullRefreshListView = (PullToRefreshListView)view.findViewById(R.id.list);   //下拉列表控件
        list=mPullRefreshListView.getRefreshableView();   //从下拉列表控件中获取
        mPullRefreshListView.setMode(Mode.PULL_FROM_START);//只有下拉刷新

        WidgetInit();   //控件初始化
        Log.v("Time","Parameter初始化完成");
        return view;
    }

    //数据初始化，初始化各个参数名称
    private void DataInit()
    {
        //首先是外间的
        //室内环境，5个参数：温度、湿度、大气压、人员信息、光线强度
        Map<String,String> map1=new HashMap<String,String>();
        map1.put("Name", "温度");     //参数名称
        map1.put("Value", "null ℃");         //值
        map1.put("Evaluate", "null");    //评估，冷、热等
        map1.put("Progress", "0");
        data_room.add(map1);

        Map<String,String> map2=new HashMap<String,String>();
        map2.put("Name", "湿度");
        map2.put("Value", "null %RH");
        map2.put("Evaluate", "null");
        map2.put("Progress", "0");
        data_room.add(map2);

        Map<String,String> map3=new HashMap<String,String>();
        map3.put("Name", "大气压");
        map3.put("Value", "null kPa");
        map3.put("Evaluate", "null");
        map3.put("Progress", "0");
        data_room.add(map3);


        Map<String,String> map10=new HashMap<String,String>();
        map10.put("Name", "人员信息");
        map10.put("Value", "0");
        map10.put("Evaluate", "null");
        map10.put("Progress", "0");
        data_room.add(map10);

        Map<String,String> map6=new HashMap<String,String>();
        map6.put("Name", "光线强度");
        map6.put("Value", "null");
        map6.put("Evaluate", "null");
        map6.put("Progress", "0");
        data_room.add(map6);


        //环境质量，5个参数：甲醛、烟雾、PM1，PM2.5，PM10
        Map<String,String> map5=new HashMap<String,String>();
        map5.put("Name", "烟雾");
        map5.put("Value", "null ppm");
        map5.put("Evaluate", "null");
        map5.put("Progress", "0");
        data_air.add(map5);

        Map<String,String> map4=new HashMap<String,String>();
        map4.put("Name", "甲醛");
        map4.put("Value", "null ppm");
        map4.put("Evaluate", "null");
        map4.put("Progress", "0");    //占颜色标准条的比例
        data_air.add(map4);

        Map<String,String> map7=new HashMap<String,String>();
        map7.put("Name", "PM1");
        map7.put("Value", "null");
        map7.put("Evaluate", "null");
        map7.put("Progress", "0");
        data_air.add(map7);

        Map<String,String> map8=new HashMap<String,String>();
        map8.put("Name", "PM2.5");
        map8.put("Value", "null");
        map8.put("Evaluate", "null");
        map8.put("Progress", "0");
        data_air.add(map8);

        Map<String,String> map9=new HashMap<String,String>();
        map9.put("Name", "PM10");
        map9.put("Value", "null");
        map9.put("Evaluate", "null");
        map9.put("Progress", "0");
        data_air.add(map9);

        //然后是里间的
        //室内环境，5个参数：温度、湿度、大气压、人员信息、光线强度
        Map<String,String> map11=new HashMap<String,String>();
        map11.put("Name", "温度");     //参数名称
        map11.put("Value", "null ℃");         //值
        map11.put("Evaluate", "null");    //评估，冷、热等
        map11.put("Progress", "0");
        data_room1.add(map11);

        Map<String,String> map12=new HashMap<String,String>();
        map12.put("Name", "湿度");
        map12.put("Value", "null %RH");
        map12.put("Evaluate", "null");
        map12.put("Progress", "0");
        data_room1.add(map12);

        Map<String,String> map13=new HashMap<String,String>();
        map13.put("Name", "大气压");
        map13.put("Value", "null kPa");
        map13.put("Evaluate", "null");
        map13.put("Progress", "0");
        data_room1.add(map13);


        Map<String,String> map21=new HashMap<String,String>();
        map21.put("Name", "人员信息");
        map21.put("Value", "0");
        map21.put("Evaluate", "null");
        map21.put("Progress", "0");
        data_room1.add(map21);

        Map<String,String> map16=new HashMap<String,String>();
        map16.put("Name", "光线强度");
        map16.put("Value", "null");
        map16.put("Evaluate", "null");
        map16.put("Progress", "0");
        data_room1.add(map16);


        //环境质量，5个参数：甲醛、烟雾、PM1，PM2.5，PM10
        Map<String,String> map15=new HashMap<String,String>();
        map15.put("Name", "烟雾");
        map15.put("Value", "null ppm");
        map15.put("Evaluate", "null");
        map15.put("Progress", "0");
        data_air1.add(map15);

        Map<String,String> map14=new HashMap<String,String>();
        map14.put("Name", "甲醛");
        map14.put("Value", "null ppm");
        map14.put("Evaluate", "null");
        map14.put("Progress", "0");    //占颜色标准条的比例
        data_air1.add(map14);

        Map<String,String> map17=new HashMap<String,String>();
        map17.put("Name", "PM1");
        map17.put("Value", "null");
        map17.put("Evaluate", "null");
        map17.put("Progress", "0");
        data_air1.add(map17);

        Map<String,String> map18=new HashMap<String,String>();
        map18.put("Name", "PM2.5");
        map18.put("Value", "null");
        map18.put("Evaluate", "null");
        map18.put("Progress", "0");
        data_air1.add(map18);

        Map<String,String> map19=new HashMap<String,String>();
        map19.put("Name", "PM10");
        map19.put("Value", "null");
        map19.put("Evaluate", "null");
        map19.put("Progress", "0");
        data_air1.add(map19);
    }
    //控件初始化，为控件绑定监听函数，以及为listview设置适配器
    private void WidgetInit()
    {
        // 列表下拉监听
        mPullRefreshListView.setOnRefreshListener(new OnRefreshListener<ListView>() {
            @Override
            public void onRefresh(PullToRefreshBase<ListView> refreshView)
            {
                String label = DateUtils.formatDateTime(getActivity().getApplicationContext(), System.currentTimeMillis(),
                        DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_ALL);

                // Update the LastUpdatedLabel
                refreshView.getLoadingLayoutProxy().setLastUpdatedLabel("上次刷新："+label);
                // Do work to refresh the list here.
                new GetDataTask().execute();   //执行任务
            }
        });
        //	 list.setAdapter(new Adapter());    //为listview设置适配器
        ListInit();

    }

    ///下拉刷新处理的函数。
    private class GetDataTask extends AsyncTask<Void, Void, String>
    {
        // 后台处理部分
        @Override
        protected String doInBackground(Void... params)
        {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
           // SendOrder("f003 0100 0010",false);
            which="2";   //2表示查询所有参数状态
//            String addr;
//            if(((Main_Activity)getActivity()).flag_room.equals("外间"))
//                addr="0002";
//            else
//                addr="0003";
            String order=addr+"f003 0100 0011";
            ((Main_Activity) getActivity()).binder.sendOrder(order);
         //   SendOrder("f003 0200 0006",true);
            return "1";
        }

        //这里是对刷新的响应，可以利用addFirst（）和addLast()函数将新加的内容加到LISTView中
        //根据AsyncTask的原理，onPostExecute里的result的值就是doInBackground()的返回值
        @Override
        protected void onPostExecute(String result)
        {
            if(result.equals("1"))  //请求数据成功，根据显示的页面重新初始化listview
            {

            }
            else            //请求数据失败
            {
                Toast.makeText(getActivity().getApplicationContext(), "加载失败！", Toast.LENGTH_SHORT).show();
            }
            // Call onRefreshComplete when the list has been refreshed.
            mPullRefreshListView.onRefreshComplete();   //表示刷新完成

            super.onPostExecute(result);//这句是必有的，AsyncTask规定的格式
        }
    }

    //自定义ListView的Adapter
	   /* class Adapter extends BaseAdapter
	    {

			@Override
			public int getCount() {
				// TODO Auto-generated method stub
				return data.size();
			}

			@Override
			public Object getItem(int position) {
				// TODO Auto-generated method stub
				return data.get(position);
			}

			@Override
			public long getItemId(int position) {
				// TODO Auto-generated method stub
				return position;
			}

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				// TODO Auto-generated method stub
				if(convertView==null)
					convertView = LayoutInflater.from(getActivity().getApplication()).inflate(R.layout.parameter_listview, null); 
				
				TextView name=(TextView)convertView.findViewById(R.id.name);
				TextView value=(TextView)convertView.findViewById(R.id.value);
				TextView evaluate=(TextView)convertView.findViewById(R.id.evaluate);
				Map<String,String> map=data.get(position);
				name.setText(map.get("Name"));
				value.setText(map.get("Value"));
				evaluate.setText(map.get("Evaluate"));
				return convertView;
			}
	    	
	    }
	    */

    /***
     * 向第六感官发送数据的函数，输入参数为要发送的字符串（未加校验、包头、包尾的），函数中会添加校验和包头包尾
     * @param order     要发送的原始命令
     * @param IsShow     是否要显示progressdialog，调节灯光亮度时不显示，刷新页面和打开、关闭灯光时显示
     */
    //
    private void SendOrder(final String order,boolean IsShow)
    {
        //	if(which.equals("100"))   //首先判断是否有开关命令正在执行，如果没有则向开关发送命令
        //	{
        //Log.i("Time","点击按钮+"+String.valueOf(System.currentTimeMillis()));
				/*
*/				time=new Date().getTime();    //获取现在的时间，单位是ms
        //which="2";   //2表示查询所有参数状态
        //which="1";    //设置which的值，表明是第position行的开关状态发生了改变
        if(IsShow)      //0.2s后判断是否有反馈，若没有则显示progressdialog，使页面不能点，然后在2.5秒后判断此次触发的progressdialog的显示是否已关闭，如果没有，则进行关闭，并将which置"100"
        {
            Timer timer = new Timer(true);     //定义定时器，定时执行关闭progressdialog命令，定时时长为2秒
            timer.schedule(new TimerTask(){
                public void run() {     //在新线程中执行
                    if(!which.equals("100"))
                    {
                        Message message = new Message();
                        message.what = 1;       //1表示要显示
                        handler2.sendMessage(message);
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
                        } ,2000); //2.0s后判断是否关闭progressdialog，若没关闭，则进行关闭
                    }
                }
            } ,250); //0.25s后判断是否关闭progressdialog，若没关闭，则进行关闭

        }
        else   //如果选择不显示progressdialog
            which="100";   //直接允许下一条指令发送


        new Thread()   //新建子线程，发送命令
        {
            public void run(){
                DataOutputStream out;
                try
                {
                    out = new DataOutputStream(client.getOutputStream());
                    String toServer = "ab68"+order;    //指令，添加包头
                    toServer.replace(" ","");    //去掉空格
                    byte[] bt=null;
                    bt=Converts.HexString2Bytes(toServer);
                    String str=toServer+Converts.GetCRC(bt, 2, bt.length)+"0d0a";   //添加校验码和包尾
                    Log.i("Order","发送数据："+str);
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
                        client = new Socket(serverAddr,2000);   //新建TCP连接
                        out=new DataOutputStream(client.getOutputStream());
                        String toServer = "ab68"+order;    //指令，添加包头
                        toServer.replace(" ","");    //去掉空格
                        byte[] bt=null;
                        bt=Converts.HexString2Bytes(toServer);
                        String str=toServer+Converts.GetCRC(bt, 2, bt.length)+"0d0a";   //添加校验码，和包尾
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
        //}
    }

    //为LishView设置Adapter，用的是SectionListAdapter，分区。参数分为一个区，灯光是一个区
    private void ListInit()
    {
        SectionListAdapter adapter = new SectionListAdapter(getActivity());  //实例化一个SectionListAdapter
        if(((Main_Activity)getActivity()).flag_room.equals("外间")) {
            adapter.addSection("室内环境", new BaseAdapter() {
                @Override
                public int getCount() {
                    // TODO Auto-generated method stub
                    return data_room.size();
                }

                @Override
                public Object getItem(int position) {
                    // TODO Auto-generated method stub
                    return data_room.get(position);
                }

                @Override
                public long getItemId(int position) {
                    // TODO Auto-generated method stub
                    return position;
                }

                @Override
                public View getView(final int position, View convertView, ViewGroup parent) {
                    if (convertView == null)
                        convertView = LayoutInflater.from(getActivity().getApplication()).inflate(R.layout.parameter_room, null);
                    TextView name = (TextView) convertView.findViewById(R.id.name);
                    TextView value = (TextView) convertView.findViewById(R.id.value);
                    TextView evaluate = (TextView) convertView.findViewById(R.id.evaluate);
                    ImageView standard = (ImageView) convertView.findViewById(R.id.standard);   //等级划分条
                    ImageView arrow = (ImageView) convertView.findViewById(R.id.arrow);   //箭头
                    ImageView img_person = (ImageView) convertView.findViewById(R.id.img_person);   //箭头
                    LinearLayout layout_number = (LinearLayout) convertView.findViewById((R.id.layout_number));   //显示数字的布局
                    LinearLayout layout_arrow = (LinearLayout) convertView.findViewById(R.id.layout_arrow);   //游标的布局
                    LinearLayout layout_all = (LinearLayout) convertView.findViewById(R.id.layout_all);   //中间条的整个布局
                    LinearLayout layout_person = (LinearLayout) convertView.findViewById(R.id.layout_person);   //显示小人图标的布局
                    Map<String, String> map = data_room.get(position);
                    int progress = Integer.valueOf(map.get("Progress"));
                    name.setText(map.get("Name"));
                    value.setText(map.get("Value"));
                    //value.setText(map.get("Value")+"+"+ String.valueOf(progress));
                    evaluate.setText(map.get("Evaluate"));
                    if (map.get("Name").equals("湿度"))  //3个等级
                    {
                        standard.setImageResource(R.drawable.standard_humidity);
                        layout_all.setPadding(Pwidth / 10, 0, Pwidth / 10, 0);   //设置颜色条为原长度的3/5
                        layout_arrow.setPadding(Pwidth * progress * 3 / 1010, 0, 0, 0);
                        if(device.equals("平板"))   //如果是平板，则数字可以显示开
                            layout_number.setPadding(Pwidth * (progress) * 3 / 1010, 0, 0, 0);
                        else     //如果是手机，则数字到右边可能显示不开
                            layout_number.setPadding(Pwidth * Math.min(progress, 50) * 3 / 1010, 0, 0, 0);
                    } else if (map.get("Name").equals("光线强度"))  //5个等级
                    {
                        standard.setImageResource(R.drawable.standard_light);
                        layout_all.setPadding(Pwidth / 20, 0, Pwidth / 20, 0);   //设置颜色条为原长度的4/5
                        layout_arrow.setPadding(Pwidth * progress / 255, 0, 0, 0);
                        layout_number.setPadding(Pwidth * progress / 255, 0, 0, 0);
                    } else if (map.get("Name").equals("大气压"))  //3个等级
                    {
                        standard.setImageResource(R.drawable.standard_humidity);
                        layout_all.setPadding(Pwidth / 10, 0, Pwidth / 10, 0);   //设置颜色条为原长度的3/5
                        //layout_arrow.setPadding(Pwidth*progress*3/1010, 0, 0, 0);
                        LayoutParams pm = (LayoutParams) arrow.getLayoutParams();
                        pm.setMargins(Pwidth * progress * 3 / 1010, 0, 0, 0);
                        arrow.setLayoutParams(pm);
                        if(device.equals("平板"))
                            value.setPadding(Pwidth * progress * 3 / 2010, 0, 0, 0);
                        else
                            value.setPadding(Pwidth * Math.min(40,progress) * 3 / 2010, 0, 0, 0);
                    } else if (map.get("Name").equals("温度"))  //6个等级
                    {
                        standard.setImageResource(R.drawable.standard_tem);
                        layout_all.setPadding(0, 0, 0, 0);
                        layout_arrow.setPadding(Pwidth * progress / 205, 0, 0, 0);
                        layout_number.setPadding(Pwidth * progress / 205, 0, 0, 0);
                    } else if (map.get("Name").equals("人员信息"))  //只显示一个小人的图标
                    {
                        if (Double.valueOf(map.get("Value")) == 0)   //无人
                        {
                            img_person.setImageResource(R.drawable.person0);
                            //layout_arrow.setVisibility(View.GONE);
                        } else    //有人
                        {
                            img_person.setImageResource(R.drawable.person1);
                            //layout_arrow.setVisibility(View.GONE);
                        }
                        layout_all.setVisibility(View.GONE);
                        layout_person.setVisibility(View.VISIBLE);

                    } else   //剩余的大气压，x轴夹角，y轴夹角，水平夹角
                    {
                        arrow.setVisibility(View.GONE);
                        standard.setVisibility(View.GONE);
                    }
                    return convertView;
                }
            });
            adapter.addSection("空气质量", new BaseAdapter() {
                @Override
                public int getCount() {
                    // TODO Auto-generated method stub
                    return data_air.size();
                }

                @Override
                public Object getItem(int position) {
                    // TODO Auto-generated method stub
                    return data_air.get(position);
                }

                @Override
                public long getItemId(int position) {
                    // TODO Auto-generated method stub
                    return position;
                }

                @Override
                public View getView(final int position, View convertView, ViewGroup parent) {
                    if (convertView == null)
                        convertView = LayoutInflater.from(getActivity().getApplication()).inflate(R.layout.parameter_listview, null);
                    TextView name = (TextView) convertView.findViewById(R.id.name);
                    TextView value = (TextView) convertView.findViewById(R.id.value);
                    TextView evaluate = (TextView) convertView.findViewById(R.id.evaluate);
                    ImageView standard = (ImageView) convertView.findViewById(R.id.standard);   //等级划分条
                    LinearLayout layout_number = (LinearLayout) convertView.findViewById(R.id.layout_number);// 值
                    LinearLayout layout_arrow = (LinearLayout) convertView.findViewById(R.id.layout_arrow);   //游标的布局
                    LinearLayout layout_all = (LinearLayout) convertView.findViewById(R.id.layout_all);   //中间条的
                    Map<String, String> map = data_air.get(position);
				/*int width=standard.getWidth();     //标准条的总长度
				int layout_width=layout_all.getWidth();     //区域总长度
*/
                    int progress = Integer.valueOf(map.get("Progress"));
                    name.setText(map.get("Name"));
                    value.setText(map.get("Value"));
                    //value.setText(map.get("Value")+"+"+ String.valueOf(progress));
                    evaluate.setText(map.get("Evaluate"));
                    if (map.get("Name").equals("烟雾"))   //两个等级
                    {
                        standard.setImageResource(R.drawable.standard_smoke);
                        layout_all.setPadding(Pwidth / 10, 0, Pwidth / 10, 0);   //设置颜色条为原长度的3/5
                        layout_arrow.setPadding(Pwidth * progress * 3 / 1020, 0, 0, 0);
                        layout_number.setPadding(Pwidth * progress * 3 / 1020, 0, 0, 0);

                    } else if (map.get("Name").equals("甲醛"))  //4个等级
                    {
                        standard.setImageResource(R.drawable.standard_smoke);
                        layout_all.setPadding(Pwidth / 10, 0, Pwidth / 10, 0);   //设置颜色条为原长度的3/5
                        layout_arrow.setPadding(Pwidth * progress * 3 / 1020, 0, 0, 0);
                        layout_number.setPadding(Pwidth * progress * 3 / 1020, 0, 0, 0);
                    }//其他都是6个等级
                    else {
                        layout_arrow.setPadding(Pwidth * progress / 205, 0, 0, 0);
                        if(device.equals("平板"))
                            layout_number.setPadding(Pwidth * progress / 205, 0, 0, 0);
                        else
                            layout_number.setPadding(Pwidth * Math.min(progress,78) / 205, 0, 0, 0);
                    }

                    return convertView;
                }
            });
        }
        else   //里间
        {
            adapter.addSection("室内环境", new BaseAdapter() {
                @Override
                public int getCount() {
                    // TODO Auto-generated method stub
                    return data_room1.size();
                }

                @Override
                public Object getItem(int position) {
                    // TODO Auto-generated method stub
                    return data_room1.get(position);
                }

                @Override
                public long getItemId(int position) {
                    // TODO Auto-generated method stub
                    return position;
                }

                @Override
                public View getView(final int position, View convertView, ViewGroup parent) {
                    if (convertView == null)
                        convertView = LayoutInflater.from(getActivity().getApplication()).inflate(R.layout.parameter_room, null);
                    TextView name = (TextView) convertView.findViewById(R.id.name);
                    TextView value = (TextView) convertView.findViewById(R.id.value);
                    TextView evaluate = (TextView) convertView.findViewById(R.id.evaluate);
                    ImageView standard = (ImageView) convertView.findViewById(R.id.standard);   //等级划分条
                    ImageView arrow = (ImageView) convertView.findViewById(R.id.arrow);   //箭头
                    ImageView img_person = (ImageView) convertView.findViewById(R.id.img_person);   //箭头
                    LinearLayout layout_number = (LinearLayout) convertView.findViewById(R.id.layout_number);
                    LinearLayout layout_arrow = (LinearLayout) convertView.findViewById(R.id.layout_arrow);   //游标的布局
                    LinearLayout layout_all = (LinearLayout) convertView.findViewById(R.id.layout_all);   //中间条的整个布局
                    LinearLayout layout_person = (LinearLayout) convertView.findViewById(R.id.layout_person);   //显示小人图标的布局
                    Map<String, String> map = data_room1.get(position);
                    int progress = Integer.valueOf(map.get("Progress"));
                    name.setText(map.get("Name"));
                    value.setText(map.get("Value"));
                    //value.setText(map.get("Value")+"+"+ String.valueOf(progress));
                    evaluate.setText(map.get("Evaluate"));
                    if (map.get("Name").equals("湿度"))  //3个等级
                    {
                        standard.setImageResource(R.drawable.standard_humidity);
                        layout_all.setPadding(Pwidth / 10, 0, Pwidth / 10, 0);   //设置颜色条为原长度的3/5
                        layout_arrow.setPadding(Pwidth * progress * 3 / 1010, 0, 0, 0);
                        if(device.equals("平板"))   //如果是平板，则数字可以显示开
                            layout_number.setPadding(Pwidth * (progress) * 3 / 1010, 0, 0, 0);
                        else     //如果是手机，则数字到右边可能显示不开
                            layout_number.setPadding(Pwidth * Math.min(progress, 50) * 3 / 1010, 0, 0, 0);
                    } else if (map.get("Name").equals("光线强度"))  //5个等级
                    {
                        standard.setImageResource(R.drawable.standard_light);
                        layout_all.setPadding(Pwidth / 20, 0, Pwidth / 20, 0);   //设置颜色条为原长度的4/5
                        layout_arrow.setPadding(Pwidth * progress / 255, 0, 0, 0);
                        layout_number.setPadding(Pwidth * progress / 255, 0, 0, 0);
                    } else if (map.get("Name").equals("大气压"))  //3个等级
                    {
                        standard.setImageResource(R.drawable.standard_humidity);
                        layout_all.setPadding(Pwidth / 10, 0, Pwidth / 10, 0);   //设置颜色条为原长度的3/5
                        //layout_arrow.setPadding(Pwidth*progress*3/1010, 0, 0, 0);
                        LayoutParams pm = (LayoutParams) arrow.getLayoutParams();
                        pm.setMargins(Pwidth * progress * 3 / 1010, 0, 0, 0);
                        arrow.setLayoutParams(pm);
                        if(device.equals("平板"))
                            value.setPadding(Pwidth * progress * 3 / 2010, 0, 0, 0);
                        else
                            value.setPadding(Pwidth * Math.min(40,progress) * 3 / 2010, 0, 0, 0);
                    } else if (map.get("Name").equals("温度"))  //6个等级
                    {
                        standard.setImageResource(R.drawable.standard_tem);
                        layout_all.setPadding(0, 0, 0, 0);
                        layout_arrow.setPadding(Pwidth * progress / 205, 0, 0, 0);
                        layout_number.setPadding(Pwidth * progress / 205, 0, 0, 0);
                    } else if (map.get("Name").equals("人员信息"))  //只显示一个小人的图标
                    {
                        if (Double.valueOf(map.get("Value")) == 0)   //无人
                        {
                            img_person.setImageResource(R.drawable.person0);
                            //layout_arrow.setVisibility(View.GONE);
                        } else    //有人
                        {
                            img_person.setImageResource(R.drawable.person1);
                            //layout_arrow.setVisibility(View.GONE);
                        }
                        layout_all.setVisibility(View.GONE);
                        layout_person.setVisibility(View.VISIBLE);

                    } else   //剩余的大气压，x轴夹角，y轴夹角，水平夹角
                    {
                        arrow.setVisibility(View.GONE);
                        standard.setVisibility(View.GONE);
                    }
                    return convertView;
                }
            });
            adapter.addSection("空气质量", new BaseAdapter() {
                @Override
                public int getCount() {
                    // TODO Auto-generated method stub
                    return data_air1.size();
                }

                @Override
                public Object getItem(int position) {
                    // TODO Auto-generated method stub
                    return data_air1.get(position);
                }

                @Override
                public long getItemId(int position) {
                    // TODO Auto-generated method stub
                    return position;
                }

                @Override
                public View getView(final int position, View convertView, ViewGroup parent) {
                    if (convertView == null)
                        convertView = LayoutInflater.from(getActivity().getApplication()).inflate(R.layout.parameter_listview, null);
                    TextView name = (TextView) convertView.findViewById(R.id.name);
                    TextView value = (TextView) convertView.findViewById(R.id.value);
                    TextView evaluate = (TextView) convertView.findViewById(R.id.evaluate);
                    ImageView standard = (ImageView) convertView.findViewById(R.id.standard);   //等级划分条
                    LinearLayout layout_number = (LinearLayout) convertView.findViewById(R.id.layout_number);
                    LinearLayout layout_arrow = (LinearLayout) convertView.findViewById(R.id.layout_arrow);   //游标的布局
                    LinearLayout layout_all = (LinearLayout) convertView.findViewById(R.id.layout_all);   //中间条的
                    Map<String, String> map = data_air1.get(position);
				/*int width=standard.getWidth();     //标准条的总长度
				int layout_width=layout_all.getWidth();     //区域总长度
*/
                    int progress = Integer.valueOf(map.get("Progress"));
                    name.setText(map.get("Name"));
                    value.setText(map.get("Value"));
                    //value.setText(map.get("Value")+"+"+ String.valueOf(progress));
                    evaluate.setText(map.get("Evaluate"));
                    if (map.get("Name").equals("烟雾"))   //两个等级
                    {
                        standard.setImageResource(R.drawable.standard_smoke);
                        layout_all.setPadding(Pwidth / 10, 0, Pwidth / 10, 0);   //设置颜色条为原长度的3/5
                        layout_arrow.setPadding(Pwidth * progress * 3 / 1020, 0, 0, 0);
                        layout_number.setPadding(Pwidth * progress * 3 / 1020, 0, 0, 0);

                    } else if (map.get("Name").equals("甲醛"))  //4个等级
                    {
                        standard.setImageResource(R.drawable.standard_smoke);
                        layout_all.setPadding(Pwidth / 10, 0, Pwidth / 10, 0);   //设置颜色条为原长度的3/5
                        layout_arrow.setPadding(Pwidth * progress * 3 / 1020, 0, 0, 0);
                        layout_number.setPadding(Pwidth * progress * 3 / 1020, 0, 0, 0);
                    }//其他都是6个等级
                    else {
                        layout_arrow.setPadding(Pwidth * progress / 205, 0, 0, 0);
                        if(device.equals("平板"))
                            layout_number.setPadding(Pwidth * progress / 205, 0, 0, 0);
                        else
                            layout_number.setPadding(Pwidth * Math.min(progress,78) / 205, 0, 0, 0);
                    }

                    return convertView;
                }
            });
        }

        list.setAdapter(adapter);
    }

}
