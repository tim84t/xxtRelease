package cn.qtone.release.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Util {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println(Util.getCurrentDateTime());
	}

	public static String getCurrentDateTime() {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
	}

	public static String getCurrentDate(String format) {
		if (null == format || "".equals(format))
			format = "yyyy-MM-dd";
		return new SimpleDateFormat(format).format(new Date());
	}

	public static String submit(String postURL, String content, String charset) {
		BufferedReader reader = null;
		HttpURLConnection urlConn = null;
		InputStream in = null;

		int flag = 1;// 是否出错的标志，等于0表示出错
		String responseMessage = "";
		try {
			URL httpurl = new URL(postURL);
			urlConn = (HttpURLConnection) httpurl.openConnection();
			// url请求返回code值
			int code = urlConn.getResponseCode(); // 200发送成功
			System.out.println("url请求返回code值:" + code);
			String currentLine = "";
			if (code == 200) {
				in = urlConn.getInputStream();
				reader = new BufferedReader(new InputStreamReader(in));
				while ((currentLine = reader.readLine()) != null) {
					responseMessage = responseMessage + currentLine;
				}
				// System.out.println("responseMessage:" + responseMessage);
			} else {
				flag = 0;
				responseMessage = "submit warningmsg error, code is " + code;
				System.out.println(responseMessage);
			}
		} catch (Exception e) {
			flag = 0;
			e.printStackTrace();
		} finally {
			try {
				if (reader != null)
					reader.close();
			} catch (Exception e) {
			}
			try {
				if (in != null)
					in.close();
			} catch (Exception ex) {
			}
			urlConn.disconnect();
		}
		return flag == 1 ? responseMessage : null;
	}

	/**
	 * 输出信息，在信息前将自动添加当前时间
	 * 
	 * @param message
	 * @param hasEnter
	 *            是否需要换行输出
	 */
	public static void info(String message, boolean hasEnter) {
		if (hasEnter)
			System.out.println(getCurrentDateTime() + ": " + message);
		else
			System.out.print(getCurrentDateTime() + ": " + message);
	}

}
