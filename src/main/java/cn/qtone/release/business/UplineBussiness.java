package cn.qtone.release.business;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;

import cn.qtone.release.model.MyFTPFile;
import cn.qtone.release.util.FtpClientUtil;


public class UplineBussiness {
	
	private static Log logger = LogFactory.getLog(UplineBussiness.class);
	
	/**
	 * 递归
	 * @param ftpClient
	 * @param ipAddress
	 * @param port
	 * @param ftpUser
	 * @param ftpPassword
	 * @param sourcePath
	 * @param fileRange
	 * @return
	 * @throws Exception
	 */
	public List<String> listFile(FtpClientUtil ftpClient, String ipAddress,
			String port, String ftpUser, String ftpPassword, String sourcePath,
			String[] fileRange) throws Exception {
		List<String> downList = ftpClient.getFileNameList(sourcePath);
		if (null == downList)
			return null;
		ArrayList<String> list = new ArrayList<String>();
		for (int i = 0; i < downList.size(); i++) {
			boolean isUplineFile = false;
			for (int fr = 0; fr < fileRange.length; fr++) {
				if (downList.get(i).endsWith("."+fileRange[fr])) {
					isUplineFile = true;
					break;
				}
			}
			if (isUplineFile) {
				list.add(downList.get(i));
				//System.out.println("[F]" + downList.get(i));
			} else {
				//System.out.println("[D]" + downList.get(i)+ "/");
				List<String> subList = listFile(ftpClient, ipAddress, port,
						ftpUser, ftpPassword, downList.get(i)
								+ "/", fileRange);
				list.addAll(subList);
			}
		}
		return list;
	}
	
	/**
	 * 递归
	 * @param ftpClient
	 * @param ipAddress
	 * @param port
	 * @param ftpUser
	 * @param ftpPassword
	 * @param sourcePath
	 * @param fileRange
	 * @return
	 * @throws Exception
	 */
	public List<MyFTPFile> listFTPFile(FtpClientUtil ftpClient, String ipAddress,
			String port, String ftpUser, String ftpPassword, String sourcePath,
			FTPFileFilter filter) throws Exception {
		List<MyFTPFile> downList = ftpClient.getFTPFileList(sourcePath,filter);
		ArrayList<MyFTPFile> list = new ArrayList<MyFTPFile>();
		
		if (null == downList){
			return null;
		}
		
		for (int i = 0; i < downList.size(); i++) {
			FTPFile f = downList.get(i).getFtpFile();
			if (!f.isDirectory()) {
				list.add(downList.get(i));
			} else {
				List<MyFTPFile> subList = listFTPFile(ftpClient, ipAddress, port,
						ftpUser, ftpPassword, sourcePath+downList.get(i).getFtpFile().getName()
								+ "/", filter);
				list.addAll(subList);
			}
		}
		return list;
	}

	public List<File> listFile(String sourcePath) {
		File baseDir = new File(sourcePath);
		List<File> list = new ArrayList<File>();
		
		File[] files = baseDir.listFiles();
		
		if(files == null || files.length == 0){
			return list;
		}
		
		for(File file : files){
			if(!file.isDirectory()){
				list.add(file);
			}else{
				List<File> subList = listFile(file.getAbsolutePath());
				list.addAll(subList);
			}
		}
		
		return list;
	}
}
