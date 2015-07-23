package cn.qtone.release.main;

import java.io.File;
import java.text.SimpleDateFormat;
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


public class UplineMain {
	// 配置文件
	private static Properties conf;
	
	private static Log logger = LogFactory.getLog(UplineMain.class);

	HashMap<String, Object> param = new HashMap<String, Object>();

	// 初始化参数
	private boolean init(String module, String fallbackDate) {
		try {
			// 初始化
			logger.info(" * * * begin release...");
			// 取得配置文件
			logger.info("(STEP 1) load config file...");
			UplineMain.conf = new Properties();
			UplineMain.conf.load(UplineMain.class.getClassLoader().getResourceAsStream(Container.config_path));
			logger.info("DONE");
			logger.info("(STEP 2) load config...");

			// 获取FTP访问参数
			String ipAddress = conf.getProperty("ftp.login.ipaddress");
			String port = conf.getProperty("ftp.login.port");
			String ftpUser = conf.getProperty("ftp.login.account");
			String ftpPassword = conf.getProperty("ftp.login.password");

			String sourcePath = conf.getProperty("rlse.source.path."+module,
					"/data/upfile/");
			String targetPath = conf.getProperty("rlse.target.path."+module,
					"/data/xxtweb/");
			String backupPath = conf.getProperty("rlse.backup.path."+module,
					"backup/");
			String isbackup = conf.getProperty("rlse.isbackup."+module,"1");

			String fileRange[] = null;
			try {
				fileRange = conf.getProperty("rlse.file.range."+module,
						"class,html,htm").split(",");
			} catch (Exception e) {
				logger.error(null,e);
			}
			if (null == fileRange) {
				logger.error("上线文件类型参数空，不处理!");
				return false;
			}
			
			String date = Util.getCurrentDate("yyyyMMdd");
			
			if (!sourcePath.endsWith("/"))
				sourcePath += "/";
			sourcePath += date + "/";
			
			if (!backupPath.endsWith("/"))
				backupPath += "/";
			if("".equals(fallbackDate)){
				backupPath += date + "/";
			}else{
				backupPath += fallbackDate + "/";
			}
			
			if (!targetPath.endsWith("/"))
				targetPath += "/";

			param.put("ftp.login.ipaddress", ipAddress);
			param.put("ftp.login.port", port);
			param.put("ftp.login.account", ftpUser);
			param.put("ftp.login.password", ftpPassword);

			param.put("rlse.source.path", sourcePath);
			param.put("rlse.target.path", targetPath);
			param.put("rlse.backup.path", backupPath);
			param.put("rlse.file.range", fileRange);
			param.put("rlse.isbackup", isbackup);
			return true;
		} catch (Exception e) {
			logger.error(null, e);
		}
		return false;
	}

	public void running(String module, String action, String date) throws Exception {

		init(module,date);
		UplineProxy uplineProxy = new UplineProxy();
		if("rlse".equals(action)){
			logger.info("(STEP 3) searching release files...");
			List<MyFTPFile> list = uplineProxy.analyseUplineSource(param);
			
			if("1".equals(param.get("rlse.isbackup"))){//最终目录，需先备份
				logger.info("(STEP 4) backup release files...");
				uplineProxy.backupFTPFile(param, list);
				logger.info("(STEP 4) begin release...");
				uplineProxy.uplineFTPFile(param, list);
			}else{//上传到中转目录，时间目录
				uplineProxy.uplineFTPFile(param, list);
			}

			logger.info("(STEP 5) begin checksum...");
			boolean flag = uplineProxy.checksum(param,module);
			if(flag){
				logger.info(" * * * (END) end release successfully...");
			}else{
				logger.info(" * * * (END) release fail...");
			}
			
		}else if("fallback".equals(action)){
			logger.info("(STEP 3) searching fallback files...");
			
			String fallbackDate = date;
			if(fallbackDate == null || "".equals(fallbackDate)){
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
				date = sdf.format(new Date());
			}
			
			List<File> list = uplineProxy.analyseFallbackFiles(param,fallbackDate);
			
			logger.info("(STEP 4) fallback according to the release files...");
			uplineProxy.restoreFile(param, list);
			logger.info("(STEP 5) begin checksum...");
			boolean flag = uplineProxy.checksumFallback(param,list);
			if(flag){
				logger.info(" * * * fallback completed successfully......");
			}else{
				logger.info(" * * * fallback completed fail!!......");
			}
			
		}else if("checksum".equals(action)){
			uplineProxy.checksum(param,module);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String module = "";
		String action = "";
		if(args.length >= 2){
			module = args[0];
			action = args[1];
//			System.setProperty("module", module);
//			DOMConfigurator.configure("log4j.xml");
			try {
				UplineMain obj = new UplineMain();
				if(args.length == 3){
					obj.running(module,action,args[2]);
				}else{
					obj.running(module,action,"");
				}
				
			} catch (Exception e) {
				logger.error(null,e);
			}
		}
	}

}
