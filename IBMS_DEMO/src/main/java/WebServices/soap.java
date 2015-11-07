package WebServices;

import java.io.IOException;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.SoapFault;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

public class soap {
				
		
		//22.调用Inquiry_Version方法(查看最新版本号)    的soap通讯静态方法   返回一个soapobject对象，存放信息
		public static SoapObject Inquiry_Version()
		{ 
			//命名空间
			final String NAMESPACE="http://www.suntrans.net/";  
			// WebService地址  
			final String URL ="http://210.42.122.127:8080";
			//方法名
			final String METHOD_NAME ="Inquiry_Version";  
			final String SOAP_ACTION ="http://www.suntrans.net/Inquiry_Version";
			//SoapObject detail; 
			SoapObject rpc =new SoapObject(NAMESPACE, METHOD_NAME);  			
			  HttpTransportSE ht =new HttpTransportSE(URL,5000);    //设置访问地址，第二个参数是设置超时的毫秒数
			 ht.debug =true;  
			 SoapSerializationEnvelope envelope =new SoapSerializationEnvelope(SoapEnvelope.VER11);  
			 rpc.addProperty("Type", "Android_IBMS_DEMO");    //类型，Android_IBMS
			 envelope.bodyOut = rpc;  
			 envelope.dotNet =true;  
			 envelope.setOutputSoapObject(rpc);  
			  try {
				ht.call(SOAP_ACTION, envelope);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (XmlPullParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}  
			  SoapObject result = null;
				 
				try {
					result = (SoapObject)envelope.getResponse();
				} catch (SoapFault e) {
					// TODO Auto-generated catch block
					
				}      //result是服务器返回的数据  形式为SoapObject对象
			return result;   //返回result 
		}								
}
	 
