package pulltofresh;

import java.text.DecimalFormat;

import android.content.Context;

public class Utils {

	public Utils() {
		// TODO Auto-generated constructor stub
	}
	//dip转换为px
	public static int dip2px(Context context, float dpValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f);
		}
	//px转为dip
	public static int px2dip(Context context, float pxValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (pxValue / scale + 0.5f);
		}
	//double2String保留两位小数
	public static String double2String(double d){
		DecimalFormat df=new DecimalFormat("######0.00");
		return (df.format(d));
	}
}
