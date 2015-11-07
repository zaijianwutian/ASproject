package WebServices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.ksoap2.serialization.SoapObject;

public class jiexi {


	//22.用于解析Inqurey_Version返回数据的静态方法  		
	public static ArrayList<Map<String, String>> inquiry_version(SoapObject result) {
		// TODO Auto-generated constructor stub
		ArrayList<Map<String, String>> data = new ArrayList<Map<String, String>>();
		try{
			result = (SoapObject)result.getProperty(1); 
			if(result.getPropertyCount()>=1)
				result = (SoapObject)result.getProperty(0);  //有数据  则继续
			else 
				return data;     //无数据直接返回空
			
			for(int i=0;  i< result.getPropertyCount(); i++ ){
				SoapObject soap = (SoapObject) result.getProperty(i);
			    Map<String, String> map=new HashMap<String,String>();
			    try{map.put("Type", soap.getProperty("Type").toString());}catch(Exception e){}  //类型
			    try{map.put("VersionCode", soap.getProperty("VersionCode").toString());}catch(Exception e){}  //版本更新次数，用来检查是否需要更新
			    try{map.put("VersionName", soap.getProperty("VersionName").toString());}catch(Exception e){}  //版本号
			    try{map.put("Description", soap.getProperty("Description").toString());}catch(Exception e){}  //版本更新内容说明
			    try{map.put("GetTime", soap.getProperty("GetTime").toString());}catch(Exception e){}  //更新时间
			    try{map.put("URL", soap.getProperty("URL").toString());}catch(Exception e){}      //apk下载地址
			   
				data.add(map);
			
			}
		}
		catch(Exception e){}
		return data;
	}						
				
}
