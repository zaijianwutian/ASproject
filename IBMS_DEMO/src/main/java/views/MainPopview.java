package views;

import com.suntrans.ibmsdemo.R;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;

public class MainPopview extends PopupWindow {
	private TextView inquery,update,logout,cancel,tx1,version,backto,config;  
    private View mMenuView;  
  
    public MainPopview(Activity context,OnClickListener itemsOnClick) {  
        super(context);  
        LayoutInflater inflater = (LayoutInflater) context  
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);  
        mMenuView = inflater.inflate(R.layout.mainpopview, null); 
       
   //     inquery = (TextView) mMenuView.findViewById(R.id.inquery);
        config=(TextView)mMenuView.findViewById(R.id.config);    //配置服务器端口
        version = (TextView) mMenuView.findViewById(R.id.version);    //版本更新
        update = (TextView) mMenuView.findViewById(R.id.update);   //修改密码
        logout = (TextView) mMenuView.findViewById(R.id.logout);     //注销登录
        cancel = (TextView) mMenuView.findViewById(R.id.cancel);     //取消
        backto=(TextView)mMenuView.findViewById(R.id.backto);    //恢复默认配置
        //取消按钮  
        cancel.setOnClickListener(new OnClickListener() {  
  
            public void onClick(View v) {  
                //销毁弹出框  
                dismiss();  
            }  
        });  
        //设置按钮监听     
        config.setOnClickListener(itemsOnClick);
        update.setOnClickListener(itemsOnClick);
        version.setOnClickListener(itemsOnClick);
        logout.setOnClickListener(itemsOnClick);
      //  inquery.setOnClickListener(itemsOnClick); 
        backto.setOnClickListener(itemsOnClick);
        //为子view设置此属性，为的是可以在显示时候可以响应menu点击事件
        mMenuView.setFocusableInTouchMode(true);
        //设置SelectPicPopupWindow的View  
        this.setContentView(mMenuView);  
        //设置SelectPicPopupWindow弹出窗体的宽  
        this.setWidth(LayoutParams.FILL_PARENT);  
        //设置SelectPicPopupWindow弹出窗体的高  
        this.setHeight(LayoutParams.WRAP_CONTENT);  
        //设置SelectPicPopupWindow弹出窗体可点击  
        this.setFocusable(true);  
        //设置SelectPicPopupWindow弹出窗体动画效果  
        //this.setAnimationStyle(R.style.AnimBottom);  
        //实例化一个ColorDrawable颜色为半透明  
        ColorDrawable dw = new ColorDrawable(0x00000000);  
        /*设置触摸外面时消失*/  
        this.setOutsideTouchable(true);  
        /*设置系统动画*/  
        this.setAnimationStyle(android.R.style.Animation_Dialog);  
        this.update();  
        this.setTouchable(true);  
        //this.setFocusableInTouchMode(true);  
        //设置SelectPicPopupWindow弹出窗体的背景  
        this.setBackgroundDrawable(dw);  
        //mMenuView添加OnTouchListener监听判断获取触屏位置如果在选择框外面则销毁弹出框  
        mMenuView.setOnKeyListener(new OnKeyListener() {  
              
           	@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if(event.equals(KeyEvent.KEYCODE_MENU))
            	{
            		//Log.i("I","点击了菜单");
            		dismiss();  
            	}
            	/*else
            	{
            		//Log.i("I","点击了菜单以外的地方");
	                int height = mMenuView.findViewById(R.id.pop_layout1).getTop();  
	                int y=(int) event.getY();  
	                if(event.getAction()==MotionEvent.ACTION_UP){  
	                    if(y<height){  
	                        dismiss();  
	                    }  
	                }     
            	}*/
                return true; 
			}  
        });  
  
    }  
}
