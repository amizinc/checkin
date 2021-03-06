/**
 * 
 */
package com.xiaoyazi.checkin;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.net.ssl.SSLContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import static com.xiaoyazi.pools.ConstantsPool.*;
/**
 * @author zhangjie
 * @createDate 2014-7-18 下午11:27:39
 */
public class LookInsider {
	
	private Log log=LogFactory.getLog(LookInsider.class);

	//51cto
	private String loginUrl="http://home.51cto.com/index.php?s=/Index/doLogin";//登录地址
	private String homeUrl="http://home.51cto.com/index.php?s=/Home/index";//主页地址
	private String singInUrl="http://home.51cto.com/index.php?s=/Home/toSign";//签到地址
	private String cookieInfo;
	//三茅打卡网
	private String hrLoginUrl="https://passport.hrloo.com/user/login";
	//private String hrHomeUrl="http://www.hrloo.com/";
	private String hrDataUrl="http://www.hrloo.com/hrloo.php?m=home&c=index&a=login&zwid=198&dktime=1405612800&t=";
	private String hrDaKaUrl="http://www.hrloo.com/dk/mydk/record";
	
	public static void main(String[] args) {
		LookInsider li=new LookInsider();
		li.lookFor();
		li.sanMaoSignIn();
	}
	/***
	 * 网站登录成功后，页面返回的信息是一堆javascript信息，需要提取javascript中的请求地址，逐个请求一次，获取各个请求地址内容
	 * 
	 * */
	public void lookFor(){
		CloseableHttpClient client=HttpClients.createDefault();
		List<NameValuePair> nvps=new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("email",USERNAME));
		nvps.add(new BasicNameValuePair("passwd",PWD));
		nvps.add(new BasicNameValuePair("reback", ""));
		try {
			HttpPost postLogin=new HttpPost(loginUrl);
			postLogin.setEntity(new UrlEncodedFormEntity(nvps,"utf-8"));
			postLogin.setHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			postLogin.setHeader("User-Agent","Mozilla/5.0 (Windows NT 6.1; WOW64; rv:30.0) Gecko/20100101 Firefox/30.0");
			postLogin.setHeader("Referer","http://home.51cto.com/index.php?s=/Index/index/t/5/");
			postLogin.setHeader("Host","home.51cto.com");
			CloseableHttpResponse response=client.execute(postLogin);
			if(response.getStatusLine().getStatusCode()==HttpStatus.SC_OK){
				System.out.println("==登录成功==");
				String loginContent=EntityUtils.toString(response.getEntity());
				//获取登录成功后的页面信息，有9个javascript文件，解析javascript地址，并进行请求获取内容
				Document doc=Jsoup.parse(loginContent);
				Elements items=doc.getElementsByTag("script");
				for(Element item:items){
					String url=item.attr("src");
					HttpGet getBody=new HttpGet(url);
					client.execute(getBody);
				}
				HeaderElementIterator iet = new BasicHeaderElementIterator(response.headerIterator("Set-Cookie"));  
                StringBuilder sb=new StringBuilder();
                while (iet.hasNext()) {
                    HeaderElement elem = iet.nextElement();  
                    sb.append(elem.getName() + "=" + elem.getValue()+";");
                 } 
                cookieInfo=sb.toString();
				HttpGet getHome=new HttpGet(homeUrl);
				getHome.setHeader("User-Agent","Mozilla/5.0 (Windows NT 6.1; WOW64; rv:30.0) Gecko/20100101 Firefox/30.0");
				getHome.setHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
				getHome.setHeader("Host","home.51cto.com");
				getHome.setHeader("Cookie",cookieInfo);
				//javascript9个地址请求完毕后，请求主页地址
				response=client.execute(getHome);
				HttpGet getSingIn=new HttpGet(singInUrl);
				getSingIn.setHeader("User-Agent","Mozilla/5.0 (Windows NT 6.1; WOW64; rv:30.0) Gecko/20100101 Firefox/30.0");
				getSingIn.setHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
				getSingIn.setHeader("Host","home.51cto.com");
				//签到领取无忧币
				response=client.execute(getSingIn);
				String result=EntityUtils.toString(response.getEntity());
				JSONObject obj =  JSON.parseObject(result);
				if(obj.get("success").toString().equals("true")){
					System.out.println("签到成功！");
				}else{
					System.out.println("签到失败！");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			try {
				client.close();
			} catch (IOException e) {
				log.info("【client关闭异常】:"+e.getMessage());
				 
			}
		}
	}
	//三茅打卡
	public void sanMaoSignIn(){
		List<NameValuePair> nvpList=new ArrayList<NameValuePair>();
		nvpList.add(new BasicNameValuePair("hold", "1"));
		nvpList.add(new BasicNameValuePair("ajax", "1"));
		nvpList.add(new BasicNameValuePair("username", "*********"));
		nvpList.add(new BasicNameValuePair("password", "********"));
		System.setProperty ("jsse.enableSNIExtension", "false");
		 //启动https请求操作
		SSLContext sslContext=null;
		try {
			sslContext = new SSLContextBuilder()
			                    .loadTrustMaterial(null, new TrustStrategy() {
				//信任所有
				public boolean isTrusted(X509Certificate[] chain,
								String authType) throws CertificateException {
					return true;
				}
					}).build();
		} catch (Exception e) {
			log.info("【创建SSL异常】："+e.getMessage());
		} 
		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
				sslContext);
        CloseableHttpClient client = HttpClients.custom()
                .setSSLSocketFactory(sslsf)
                .build();
		try {
			HttpPost post=new HttpPost(hrLoginUrl);
			post.setEntity(new UrlEncodedFormEntity(nvpList));
			post.setHeader("Accept",ACCEPT);
			post.setHeader("User-Agent",USER_AGENT);
			post.setHeader("Referer","https://passport.hrloo.com/user/login");
			post.setHeader("Host","passport.hrloo.com");
			CloseableHttpResponse response=client.execute(post);
			HeaderElementIterator iet = new BasicHeaderElementIterator(response.headerIterator("Set-Cookie"));  
            StringBuilder sb=new StringBuilder();
            while (iet.hasNext()) {
                HeaderElement elem = iet.nextElement();  
                sb.append(elem.getName() + "=" + elem.getValue()+";");
             } 
            cookieInfo=sb.toString();
			System.out.println(EntityUtils.toString(response.getEntity()));
/*
			HttpGet get=new HttpGet(hrHomeUrl);
			get.setHeader("Accept",ACCEPT);
			get.setHeader("User-Agent",USER_AGENT);
			get.setHeader("Host","www.hrloo.com");
			get.setHeader("Cookie",cookieInfo);
		//	response=client.execute(get);
			*/
		//	System.out.println(EntityUtils.toString(response.getEntity(),"UTF-8"));
			HttpGet redirectGet=new HttpGet(hrDataUrl+new Date().getTime());
			redirectGet.setHeader("Accept",ACCEPT);
			redirectGet.setHeader("User-Agent",USER_AGENT);
			redirectGet.setHeader("Referer","http://www.hrloo.com/");
			redirectGet.setHeader("Host","www.hrloo.com/");
			redirectGet.setHeader("Cookie",cookieInfo);
	//		response=client.execute(redirectGet);
		//	System.out.println(EntityUtils.toString(response.getEntity(),"UTF-8"));
			//打卡
			HttpGet daKaGet=new HttpGet(hrDaKaUrl);
			daKaGet.setHeader("Accept",ACCEPT);
			daKaGet.setHeader("User-Agent",USER_AGENT);
			daKaGet.setHeader("Cookie",cookieInfo);
			response=client.execute(daKaGet);
		//	System.out.println(EntityUtils.toString(response.getEntity(),"UTF-8"));
		} catch (Exception e) {
			log.info("【打卡请求异常】："+e.getMessage());
		}finally{
			try {
				client.close();
			} catch (IOException e) {
				log.info("【client关闭异常】："+e.getMessage());
			}
		}
	}
}
