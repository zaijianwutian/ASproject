package convert;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

public abstract class Converts {
	//MD5加密  32位，小写
	public static String md5(String string) {
	     byte[] hash;
	     try {
	         hash = MessageDigest.getInstance("MD5").digest(string.getBytes("UTF-8"));
	     } catch (NoSuchAlgorithmException e) {
	         throw new RuntimeException("Huh, MD5 should be supported?", e);
	     } catch (UnsupportedEncodingException e) {
	         throw new RuntimeException("Huh, UTF-8 should be supported?", e);
	     }

	     StringBuilder hex = new StringBuilder(hash.length * 2);
	     for (byte b : hash) {
	         if ((b & 0xFF) < 0x10) hex.append("0");
	         hex.append(Integer.toHexString(b & 0xFF));
	     }
	     return hex.toString();
	 }
	//dip转换成像素px
	public static int dip2px(Context context, float dipValue){ 
        final float scale = context.getResources().getDisplayMetrics().density; 
        return (int)(dipValue * scale + 0.5f); 
	} 
	//像素转换成px
	public static int px2dip(Context context, float pxValue){ 
        final float scale = context.getResources().getDisplayMetrics().density; 
        return (int)(pxValue / scale + 0.5f); 
	} 

	//转化字符串为十六进制编码
	public static String toHexString(String s) {
	   String str = "";
	   for (int i = 0; i < s.length(); i++) {
	    int ch = (int) s.charAt(i);
	    String s4 = Integer.toHexString(ch);
	    str = str + s4;
	   }
	   return str;
	}
	// 转化十六进制编码为字符串
	public static String toStringHex1(String s) {
	   byte[] baKeyword = new byte[s.length() / 2];
	   for (int i = 0; i < baKeyword.length; i++) {
	    try {
	     baKeyword[i] = (byte) (0xff & Integer.parseInt(s.substring(
	       i * 2, i * 2 + 2), 16));
	    } catch (Exception e) {
	     e.printStackTrace();
	    }
	   }
	   try {
	    s = new String(baKeyword, "utf-8");// UTF-16le:Not
	   } catch (Exception e1) {
	    e1.printStackTrace();
	   }
	   return s;
	}
	// 转化十六进制编码为字符串
	public static String toStringHex2(String s) {
	   byte[] baKeyword = new byte[s.length() / 2];
	   for (int i = 0; i < baKeyword.length; i++) {
	    try {
	     baKeyword[i] = (byte) (0xff & Integer.parseInt(s.substring(
	       i * 2, i * 2 + 2), 16));
	    } catch (Exception e) {
	     e.printStackTrace();
	    }
	   }
	   try {
	    s = new String(baKeyword, "utf-8");// UTF-16le:Not
	   } catch (Exception e1) {
	    e1.printStackTrace();
	   }
	   return s;
	}
	public static void main(String[] args) {
	   System.out.println(encode("中文"));
	   System.out.println(decode(encode("中文")));
	}
	/*
	* 16进制数字字符集
	*/
	private static String hexString = "0123456789ABCDEF";
	/*
	* 将字符串编码成16进制数字,适用于所有字符（包括中文）
	*/
	public static String encode(String str) {
	   // 根据默认编码获取字节数组
	   byte[] bytes = str.getBytes();
	   StringBuilder sb = new StringBuilder(bytes.length * 2);
	   // 将字节数组中每个字节拆解成2位16进制整数
	   for (int i = 0; i < bytes.length; i++) {
	    sb.append(hexString.charAt((bytes[i] & 0xf0) >> 4));
	    sb.append(hexString.charAt((bytes[i] & 0x0f) >> 0));
	   }
	   return sb.toString();
	}
	/*
	* 将16进制数字解码成字符串,适用于所有字符（包括中文）
	*/
	public static String decode(String bytes) {
	   ByteArrayOutputStream baos = new ByteArrayOutputStream(
	     bytes.length() / 2);
	   // 将每2位16进制整数组装成一个字节
	   for (int i = 0; i < bytes.length(); i += 2)
	    baos.write((hexString.indexOf(bytes.charAt(i)) << 4 | hexString
	      .indexOf(bytes.charAt(i + 1))));
	   return new String(baos.toByteArray());
	}
	
	public static String Bytes2HexString(byte[] b) {  
	    String ret = "";  
	    for (int i = 0; i < b.length; i++) {  
	     String hex = Integer.toHexString(b[i] & 0xFF);  
	     if (hex.length() == 1) {  
	      hex = '0' + hex;  
	     }  
	     ret += hex.toUpperCase();  
	    }  
	    return ret;  
	   }  
	   /** 
	   * 将两个ASCII字符合成一个字节； 如："EF"--> 0xEF 
	   * @param src0 byte 
	   * @param src1 byte 
	   * @return byte 
	   */  
	   public static byte uniteBytes(byte src0, byte src1) {  
	    byte _b0 = Byte.decode("0x" + new String(new byte[] { src0 }))  
	      .byteValue();  
	    _b0 = (byte) (_b0 << 4);  
	    byte _b1 = Byte.decode("0x" + new String(new byte[] { src1 }))  
	      .byteValue();  
	    byte ret = (byte) (_b0 ^ _b1);  
	    return ret;  
	   }  
	   /** 
	   * 将指定字符串src，以每两个字符分割转换为16进制形式 如："2B44EFD9" --> byte[]{0x2B, 0x44, 0xEF, 0xD9} 
	   * @param src String 
	   * @return byte[] 
	   */  
	   public static byte[] HexString2Bytes(String s) {  
		   s=s.replace(" ","");
			byte[] ret = new byte[s.length()/2];  
		    byte[] tmp = s.getBytes();  
		    for (int i = 0; i < tmp.length/2; i++) 
		    {  
		     ret[i] = uniteBytes(tmp[i * 2], tmp[i * 2 + 1]);  
		    }  
		    return ret;
	   }
	// CRC校验计算    返回两个字节的数据，字符串形式， 高字节在前。   后两个参数分别是开始，结束的位置
	   public static String GetCRC(byte[] data,int start, int recv)     
	    {
	        int CRC_SEED = (int) 0XFFFF;
	        int CRC16Poly = (int) 0XA001;
	        int CRCReg = CRC_SEED;       //int型数据32位
	        String CRC="";
	        for (int i = start; i < recv; i++)
	        {
	            CRCReg ^= (data[i]&0x000000ff);    //此处如果不将data[i]和0x000000ff按位与，会出错，
	                                                //因为java中数据类型都是是带符号的,负数以补码形式计算，data[i]如果是负的，那直接转换成int型也是负的
	            for (int j = 0; j < 8; j++)
	            {
	                if ((CRCReg & 0x0001) != 0)
	                {
	                    CRCReg = ((CRCReg >> 1) ^ CRC16Poly)&0xffffffff;	                	
	                }
	                else
	                {
	                    CRCReg = (CRCReg >> 1)&0xffffffff;	                	
	                }
	            }
	        }
	        int uper = 0xffffffff&(CRCReg % 256);   //原高八位变为低八位  存在lower中             
	        int lower = 0xffffffff&(CRCReg / 256);   //原低八位变为高八位  存在uper中	       
	      //  CRCReg = (int)(uper * 256 + lower);            //得到将高八位与低八位互换的新的CRCReg
	        String bh=Integer.toHexString(uper);         //获取高八位
	        String bl=Integer.toHexString(lower);	     //获取低八位
	        bh=bh.length()==2?bh:(bh.length()>2?bh.substring(bh.length()-2,bh.length()):("0"+bh));//将字符串长度调为2
	        bl=bl.length()==2?bl:(bl.length()>2?bl.substring(bl.length()-2,bl.length()):("0"+bl));//将字符串长度调为2
	        CRC+=bh+bl;   //得到校验码的最终四位字符串
	        return CRC;    //返回字符串形式的校验码
  
		  
		   
	    }
	   

	   //设置图片圆角
public static Bitmap toRoundCorner(Bitmap bitmap, int pixels) {  
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);  
        Canvas canvas = new Canvas(output);  
        final int color = 0xff424242;  
        final Paint paint = new Paint(); 
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());  
        final RectF rectF = new RectF(rect);  
        final float roundPx = pixels;  
        paint.setAntiAlias(true);  
        canvas.drawARGB(0, 0, 0, 0);  
        paint.setColor(color);  
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);  
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));  
        canvas.drawBitmap(bitmap, rect, rect, paint); 
        return output; 
    }
}
