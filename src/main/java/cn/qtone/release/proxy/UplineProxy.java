package cn.qtone.release.proxy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;

import cn.qtone.release.business.UplineBussiness;
import cn.qtone.release.model.MyFTPFile;
import cn.qtone.release.util.FileUtil;
import cn.qtone.release.util.FtpClientUtil;

public class UplineProxy {

	private static Log logger = LogFactory.getLog(UplineProxy.class);

	private static Log RLSE = LogFactory.getLog("RLSE."
			+ UplineProxy.class.getName());

	public List<MyFTPFile> analyseUplineSource(HashMap<String, Object> con)
			throws Exception {
		String ipAddress = (String) con.get("ftp.login.ipaddress");
		String port = (String) con.get("ftp.login.port");
		String ftpUser = (String) con.get("ftp.login.account");
		String ftpPassword = (String) con.get("ftp.login.password");

		String sourcePath = (String) con.get("rlse.source.path");
		String targetPath = (String) con.get("rlse.target.path");
		final String fileRange[] = (String[]) con.get("rlse.file.range");

		UplineBussiness uplineBussiness = new UplineBussiness();
		logger.info("login[" + ipAddress + ":" + port + "]");
		FtpClientUtil ftpClient = new FtpClientUtil(ipAddress,
				Integer.valueOf(port), ftpUser, ftpPassword);
		ftpClient.open();
		
		// FtpUpfile ftpUpFile = new FtpUpfile(ipAddress, Integer.valueOf(port)
		// .intValue(), ftpUser, ftpPassword);
		// ftpUpFile.login();
		logger.info("login successfully");

		logger.info("searching release files");

		FTPFileFilter filter = new FTPFileFilter() {

			public boolean accept(FTPFile file) {
				for (int fr = 0; fr < fileRange.length; fr++) {
					if (file != null && (file.getName().endsWith("." + fileRange[fr])
							|| file.isDirectory())) {
						return true;
					}
				}
				return false;
			}
		};

		List<MyFTPFile> list = uplineBussiness.listFTPFile(ftpClient,
				ipAddress, port, ftpUser, ftpPassword, sourcePath, filter);
		// ftpUpFile.logout();

		for (MyFTPFile f : list) {
			RLSE.info(f.getSourcePath().substring(sourcePath.length()) + 
					f.getFtpFile().getName() + " " + f.getFtpFile().getSize());
			logger.info(f.getSourcePath() + f.getFtpFile().getName() + " "
					+ f.getFtpFile().getSize());
		}
		logger.info("total " + list.size() + " files in " + sourcePath);
		// RLSE.info("total " + list.size() + " files in "+sourcePath);
		ftpClient.close();
		return list;
	}

	/**
	 * 
	 * @param con
	 * @param list
	 */
	public void backupFile(HashMap<String, Object> con, List<String> list) {
		String sourcePath = (String) con.get("rlse.source.path");
		String targetPath = (String) con.get("rlse.target.path");
		String backupPath = (String) con.get("rlse.backup.path");

		for (int i = 0; i < list.size(); i++) {
			File sourceFile = new File(targetPath
					+ list.get(i).substring(sourcePath.length()));
			File backupFile = new File(backupPath
					+ list.get(i).substring(sourcePath.length()));
			if (sourceFile.isFile() && sourceFile.exists()) {
				if (backupFile.exists()) {
					logger.info("backup file[" + backupFile + "] exist，ignore!");
				} else {
					logger.info("source file[" + sourceFile + "] backup");
					FileUtil.copyFile(backupFile, sourceFile);
					logger.info("done");
				}
			} else {
				logger.info("source file [" + sourceFile.getName()
						+ "] not exists!");
			}
		}
	}

	/**
	 * 
	 * @param con
	 * @param list
	 */
	public void backupFTPFile(HashMap<String, Object> con, List<MyFTPFile> list) {
		String sourcePath = (String) con.get("rlse.source.path");
		String targetPath = (String) con.get("rlse.target.path");
		String backupPath = (String) con.get("rlse.backup.path");

		for (int i = 0; i < list.size(); i++) {

			String source = list.get(i).getSourcePath()
					+ list.get(i).getFtpFile().getName();

			File targetFile = new File(targetPath
					+ source.substring(sourcePath.length()));
			File backupFile = new File(backupPath
					+ source.substring(sourcePath.length()));
			if (targetFile.isFile() && targetFile.exists()) {
				if (backupFile.exists()) {
					logger.info("backup file[" + backupFile + "] exist，ignore!");
				} else {
					logger.info("source file[" + targetFile + "] backup");
					FileUtil.copyFile(backupFile, targetFile);
					logger.info("done");
				}
			} else {
				logger.info("source file [" + targetFile.getName()
						+ "] not exists!");
			}
		}
	}

	public void uplineFile(HashMap<String, Object> con, List<String> list) {
		String ipAddress = (String) con.get("ftp.login.ipaddress");
		String port = (String) con.get("ftp.login.port");
		String ftpUser = (String) con.get("ftp.login.account");
		String ftpPassword = (String) con.get("ftp.login.password");

		String sourcePath = (String) con.get("rlse.source.path");
		String targetPath = (String) con.get("rlse.target.path");

		logger.info("login[" + ipAddress + ":" + port + "] successfully...");
		FtpClientUtil ftpClient = new FtpClientUtil(ipAddress,
				Integer.valueOf(port), ftpUser, ftpPassword);
		ftpClient.open();
		// FtpUpfile ftpUpFile = new FtpUpfile(ipAddress, Integer.valueOf(port)
		// .intValue(), ftpUser, ftpPassword);
		// ftpUpFile.login();
		// System.out.println("成功");
		logger.info("begin release files");
		for (int i = 0; i < list.size(); i++) {
			File targetFile = new File(targetPath
					+ list.get(i).substring(sourcePath.length()));
			logger.info("download[" + targetFile.getPath() + "]...");
			try {
				File path = new File(targetFile.getParent());
				if (!path.exists())
					path.mkdirs();
				ftpClient.download(list.get(i), targetFile.getPath());
				logger.info("done");
			} catch (Exception e) {
				logger.info("failed");
				logger.error(null, e);
			}
		}
		ftpClient.close();
	}

	/**
	 * 上传到最终目录
	 * @param con
	 * @param list
	 */
	public void uplineFTPFile(HashMap<String, Object> con, List<MyFTPFile> list) {
		String ipAddress = (String) con.get("ftp.login.ipaddress");
		String port = (String) con.get("ftp.login.port");
		String ftpUser = (String) con.get("ftp.login.account");
		String ftpPassword = (String) con.get("ftp.login.password");

		String sourcePath = (String) con.get("rlse.source.path");
		String targetPath = (String) con.get("rlse.target.path");

		logger.info("login[" + ipAddress + ":" + port + "] successfully...");
		FtpClientUtil ftpClient = new FtpClientUtil(ipAddress,
				Integer.valueOf(port), ftpUser, ftpPassword);
		ftpClient.open();
		
		for (int i = 0; i < list.size(); i++) {
			String source = list.get(i).getSourcePath()
					+ list.get(i).getFtpFile().getName();

			File targetFile = new File(targetPath+File.separator
					+ source.substring(sourcePath.length()));
			logger.info("download[" + targetFile.getPath() + "]...");
			try {
				File path = new File(targetFile.getParent());
				if (!path.exists())
					path.mkdirs();
				ftpClient.download(source, targetFile.getPath());
				logger.info("done");
			} catch (Exception e) {
				logger.info("failed");
				logger.error(null, e);
			}
		}
		ftpClient.close();
	}
	
	/**
	 * 上传到中转目录
	 * @param con
	 * @param list
	 */
	public void uplineFTPFile1(HashMap<String, Object> con, List<MyFTPFile> list) {
		String ipAddress = (String) con.get("ftp.login.ipaddress");
		String port = (String) con.get("ftp.login.port");
		String ftpUser = (String) con.get("ftp.login.account");
		String ftpPassword = (String) con.get("ftp.login.password");

		String sourcePath = (String) con.get("rlse.source.path");
		String targetPath = (String) con.get("rlse.target.path");

		logger.info("login[" + ipAddress + ":" + port + "] successfully...");
		FtpClientUtil ftpClient = new FtpClientUtil(ipAddress,
				Integer.valueOf(port), ftpUser, ftpPassword);
		ftpClient.open();
		// FtpUpfile ftpUpFile = new FtpUpfile(ipAddress, Integer.valueOf(port)
		// .intValue(), ftpUser, ftpPassword);
		// ftpUpFile.login();
		// System.out.println("成功");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		String date = sdf.format(new Date());
		
		//判断是否有备份的重复目录
		File targetPathDate = new File(targetPath + File.separator + date);
		if(targetPathDate.exists()){
			SimpleDateFormat sdf1 = new SimpleDateFormat("_HHmmss");
			String time = sdf1.format(new Date());
			File targetPathFileMv = new File(targetPath + File.separator + date+time);
			try {
				FileUtils.moveDirectory(targetPathDate, targetPathFileMv);
			} catch (IOException e) {
				logger.error(null, e);
			}
		}

		for (int i = 0; i < list.size(); i++) {
			String source = list.get(i).getSourcePath()
					+ list.get(i).getFtpFile().getName();

			File targetFile = new File(targetPath+date+File.separator
					+ source.substring(sourcePath.length()));
			logger.info("download[" + targetFile.getPath() + "]...");
			try {
				File path = new File(targetFile.getParent());
				if (!path.exists())
					path.mkdirs();
				ftpClient.download(source, targetFile.getPath());
				logger.info("done");
			} catch (Exception e) {
				logger.info("failed");
				logger.error(null, e);
			}
		}
		ftpClient.close();
	}

	public void restoreFile(HashMap<String, Object> con, List<File> list) {
		// String sourcePath = (String) con.get("rlse.source.path");
		String targetPath = (String) con.get("rlse.target.path");
		String backupPath = (String) con.get("rlse.backup.path");

		for (int i = 0; i < list.size(); i++) {
			File targetFile = new File(targetPath
					+ list.get(i).getAbsolutePath()
							.substring(backupPath.length()));
			File backupFile = list.get(i);
			logger.info("file [" + backupFile + "] is restoring to ["
					+ targetFile + "]...");
			FileUtil.copyFile(targetFile, backupFile);
			logger.info("done");
		}
	}

	/**
	 * 根据上线内容还原文件
	 * 
	 * @param con
	 * @param list
	 */
	public void restoreFTPFile(HashMap<String, Object> con, List<MyFTPFile> list) {
		String sourcePath = (String) con.get("rlse.source.path");
		String targetPath = (String) con.get("rlse.target.path");
		String backupPath = (String) con.get("rlse.backup.path");

		for (int i = 0; i < list.size(); i++) {

			String source = list.get(i).getSourcePath()
					+ list.get(i).getFtpFile().getName();

			File sourceFile = new File(targetPath
					+ source.substring(sourcePath.length()));
			File backupFile = new File(backupPath
					+ source.substring(sourcePath.length()));
			if (backupFile.isFile() && backupFile.exists()) {
				logger.info("file [" + sourceFile + "] is restoring...");
				FileUtil.copyFile(sourceFile, backupFile);
				logger.info("done");
			} else {
				logger.info("backup file [" + backupFile
						+ "] exists，restore fail!");
			}
		}
	}

	public List<File> analyseFallbackFiles(HashMap<String, Object> con,
			String fallbackDate) {

//		String sourcePath = (String) con.get("rlse.source.path");
//		String targetPath = (String) con.get("rlse.target.path");
		String backupPath = (String) con.get("rlse.backup.path");

//		final String fileRange[] = (String[]) con.get("rlse.file.range");

		UplineBussiness uplineBussiness = new UplineBussiness();
		logger.info("searching release files");

		List<File> list = uplineBussiness.listFile(backupPath);
		// ftpUpFile.logout();

		for (File f : list) {
			RLSE.info(f.getPath() + " " + f.length());
			logger.info(f.getPath() + " " + f.length());
		}

		logger.info("total " + list.size() + " backup files in " + backupPath);
		// RLSE.info("total " + list.size() + " backup files in "+sourcePath);
		return list;
	}

	public boolean checksum(HashMap<String, Object> param, String module) {
		String sourcePath = (String) param.get("rlse.source.path");
		String targetPath = (String) param.get("rlse.target.path");
		File logFile = new File("logs/rlse_" + module + "_check.log");
		boolean flag = true;

//		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
//		String date = sdf.format(new Date());
		
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(logFile));

			String str = "";
			while ((str = br.readLine()) != null) {

				String strs[] = str.split(" ");
				if (strs.length == 2) {//去除时间
//					String targetFileName = targetPath + date + File.separator+ strs[0];
					String targetFileName = targetPath + File.separator+ strs[0];
					File targetFile = new File(targetFileName);
					long sourceFileSize = Long.valueOf(strs[1]);
					if (sourceFileSize != targetFile.length()) {
						logger.info("checksum fail [" + targetFile
								+ "],expect:" + sourceFileSize + ",actual:"
								+ targetFile.length());
						flag = false;
					}
				}
			}

		} catch (FileNotFoundException e) {
			flag = false;
			logger.error(null, e);
		} catch (IOException e) {
			flag = false;
			logger.error(null, e);
		} finally {
			try {
				if (br != null) {
					br.close();
				}

			} catch (IOException e) {
				logger.error(null, e);
			}
		}

		return flag;
	}

	public boolean checksumFallback(HashMap<String, Object> param,
			List<File> list) {
		String targetPath = (String) param.get("rlse.target.path");
		String backupPath = (String) param.get("rlse.backup.path");
		boolean flag = true;

		for (File file : list) {
			String targetFileName = targetPath
					+ file.getAbsolutePath().substring(backupPath.length());
			File targetFile = new File(targetFileName);
			if (file.length() != targetFile.length()) {
				logger.info("checksum fail [" + targetFile + "],expect:"
						+ file.length() + ",actual:" + targetFile.length());
				flag = false;
			}
		}

		return flag;
	}
}
