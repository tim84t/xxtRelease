package cn.qtone.release.main;

import java.io.FileInputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import cn.qtone.release.model.MyFTPFile;
import cn.qtone.release.proxy.UplineProxy;
import cn.qtone.release.util.Container;
import cn.qtone.release.util.Util;


public class RestoreMain {
	private static Properties conf;
	
	private static Log logger = LogFactory.getLog(RestoreMain.class);

	HashMap<String, Object> param = new HashMap<String, Object>();

	// 初始化参数
	private boolean init(String date) {
		try {

			// 获取FTP访问参数
			String ipAddress = conf.getProperty("ftp.login.ipaddress",
					"61.142.67.2");
			String port = conf.getProperty("ftp.login.port", "21");
			String ftpUser = conf.getProperty("ftp.login.account", "qtone");
			String ftpPassword = conf.getProperty("ftp.login.password",
					"qtone123!@#");

			String sourcePath = conf.getProperty("rlse.source.path",
					"/data/upfile/");
			String targetPath = conf.getProperty("rlse.target.path",
					"/data/xxtweb/");
			String backupPath = conf.getProperty("rlse.backup.path",
					"backup/");

			String fileRange[] = null;
			try {
				fileRange = conf.getProperty("upline.file.range",
						"class,html,htm").split(",");
			} catch (Exception e) {
			}
			if (null == fileRange) {
				System.out.println("上线文件类型参数空，不处理!");
				return false;
			}
			if (!sourcePath.endsWith("/"))
				sourcePath += "/";
			if (null == date || "".equals(date))
				date = Util.getCurrentDate("yyyyMMdd");
			sourcePath += date + "/";

			if (!targetPath.endsWith("/"))
				targetPath += "/";
			if (!backupPath.endsWith("/"))
				backupPath += "/";

			param.put("ftp.login.ipaddress", ipAddress);
			param.put("ftp.login.port", port);
			param.put("ftp.login.account", ftpUser);
			param.put("ftp.login.password", ftpPassword);

			param.put("upline.source.path", sourcePath);
			param.put("upline.target.path", targetPath);
			param.put("upline.backup.path", backupPath);
			param.put("upline.file.range", fileRange);
			return true;
		} catch (Exception e) {
			System.out.println(new Date().toString() + "(ERROR):"
					+ e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

//	@SuppressWarnings("static-access")
	public void running(String date) throws Exception {
		// TODO Auto-generated method stub
		// 初始化
		Util.info(" * * * 校讯通恢复程序启动...", true);
		// 取得配置文件
		Util.info("(STEP 1) 设置配置文件路径...", false);
		RestoreMain.conf = new Properties();
		try {
			RestoreMain.conf.load(new FileInputStream(Container.config_path));
			System.out.println("DONE");
		} catch (Exception e) {
			System.out.println("(ERROR): " + e.getMessage());
			return;
		}
		Util.info("(STEP 2) 加载程序运行参数...", false);
		if (init(date))
			System.out.println("DONE");
		else
			return;

		Util.info("(STEP 3)  检索要上线的文件...", false);
		UplineProxy uplineProxy = new UplineProxy();
		List<MyFTPFile> list = uplineProxy.analyseUplineSource(param);

		Util.info("(STEP 4) 根据上线文件恢复...", true);
		uplineProxy.restoreFTPFile(param, list);

		Util.info(" * * * 校讯通恢复程序结束...", true);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		RestoreMain obj = new RestoreMain();
		String date = "";
		try {
			date = args[0];
		} catch (Exception e) {
		}
		try {
			obj.running(date);
		} catch (Exception e) {
			logger.error(null,e);
		}
	}

}
