package cn.qtone.release.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.apache.commons.net.ftp.FTPReply;

import cn.qtone.release.model.MyFTPFile;

public class FtpClientUtil {
	FTPClient ftpClient;
	private String server;
	private int port;
	private String userName;
	private String userPassword;

	private static Log logger = LogFactory.getLog(FtpClientUtil.class);
	
	public FtpClientUtil(String server, int port, String userName,
			String userPassword) {
		this.server = server;
		this.port = port;
		this.userName = userName;
		this.userPassword = userPassword;
	}

	/**
	 * 链接到服务器
	 * 
	 * @return
	 */
	public boolean open() {
		if (ftpClient != null && ftpClient.isConnected())
			return true;
		try {
			int reply;
			ftpClient = new FTPClient();
			ftpClient.connect(server, port);
			reply = ftpClient.getReplyCode();
			if(!FTPReply.isPositiveCompletion(reply)){
				ftpClient.disconnect();
				logger.error("FTP server refused connection.");
				System.exit(1);
			}
			ftpClient.login(userName, userPassword);
			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);// binary default
			ftpClient.enterLocalPassiveMode();
			return true;
		} catch (Exception e) {
            if (ftpClient.isConnected()){
                try                {
                	ftpClient.disconnect();
                }catch (IOException f){
                   logger.error(null,f);
                }
            }
            logger.error("Could not connect to server.",e);
        	return false;
		}
	}

	public boolean cd(String dir) {
//		boolean f = false;

			try {
				ftpClient.changeWorkingDirectory(dir);
			} catch (IOException e) {
				e.printStackTrace();
			}

		return true;
	}

	/**
	 * 上传文件到FTP服务器
	 * 
	 * @param localPathAndFileName
	 *            本地文件目录和文件名
	 * @param ftpFileName
	 *            上传后的文件名
	 * @param ftpDirectory
	 *            FTP目录如:/path1/pathb2/,如果目录不存在回自动创建目录
	 * @throws Exception
	 */
	public boolean upload(String localDirectoryAndFileName, String ftpFileName,
			String ftpDirectory) throws Exception {
		if (!open())
			return false;
		FileInputStream is = null;
		OutputStream os = null;
		try {
			char ch = ' ';
			if (ftpDirectory.length() > 0)
				ch = ftpDirectory.charAt(ftpDirectory.length() - 1);
			for (; ch == '/' || ch == '\\'; ch = ftpDirectory
					.charAt(ftpDirectory.length() - 1))
				ftpDirectory = ftpDirectory.substring(0,
						ftpDirectory.length() - 1);

			int slashIndex = ftpDirectory.indexOf(47);
			int backslashIndex = ftpDirectory.indexOf(92);
			int index = slashIndex;
			String dirall = ftpDirectory;
			if (backslashIndex != -1 && (index == -1 || index > backslashIndex))
				index = backslashIndex;
			String directory = "";
			while (index != -1) {
				if (index > 0) {
					String dir = dirall.substring(0, index);
					directory = directory + "/" + dir;
//					int rsp = ftpClient.sendCommand("XMKD " + directory + "\r\n");
				}
				dirall = dirall.substring(index + 1);
				slashIndex = dirall.indexOf(47);
				backslashIndex = dirall.indexOf(92);
				index = slashIndex;
				if (backslashIndex != -1
						&& (index == -1 || index > backslashIndex))
					index = backslashIndex;
			}
//			int rsp1 = ftpClient.sendCommand("XMKD " + ftpDirectory + "\r\n");

			os = ftpClient.storeFileStream(ftpDirectory + "/" + ftpFileName);
			File file_in = new File(localDirectoryAndFileName);
			is = new FileInputStream(file_in);
			byte bytes[] = new byte[1024];
			int i;
			while ((i = is.read(bytes)) != -1)
				os.write(bytes, 0, i);
			// 清理垃圾

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			if (is != null)
				is.close();
			if (os != null)
				os.close();
		}
	}

	/**
	 * 从FTP服务器上下载文件并返回下载文件长度
	 * 
	 * @param ftpDirectoryAndFileName
	 * @param localDirectoryAndFileName
	 * @return
	 * @throws Exception
	 */
	public long download(String ftpDirectoryAndFileName,
			String localDirectoryAndFileName) throws Exception {
		long result = 0;
		ftpClient.setBufferSize(1024);
		if (!open())
			return result;
		InputStream is = null;
		FileOutputStream os = null;
		try {
			is = ftpClient.retrieveFileStream(ftpDirectoryAndFileName);
			if(is == null){
				return 0;
			}
			java.io.File outfile = new java.io.File(localDirectoryAndFileName);
			os = new FileOutputStream(outfile);
			byte[] bytes = new byte[1024];
			int c = -1;
			while ((c = is.read(bytes)) != -1) {
				os.write(bytes, 0, c);
				result = result + c;
			}
			
			os.flush();
			ftpClient.completePendingCommand();
			
		} catch (Exception e) {
			throw e;
		} finally {
			if (is != null)
				is.close();
			if (os != null)
				os.close();

		}
		return result;
	}

	/**
	 * 返回FTP目录下的文件列表
	 * 
	 * @param ftpDirectory
	 * @return
	 */
	public List<String> getFileNameList(String ftpDirectory) {
		List<String> list = new ArrayList<String>();
		if (!open())
			return list;
		try {
			/**
			isr = new InputStreamReader(ftpClient.nameList(ftpDirectory));
			br = new BufferedReader(isr);
			String filename = "";
			while ((filename = br.readLine()) != null) {
				list.add(filename);
			}*/
			String [] names = ftpClient.listNames(ftpDirectory);
			for(String name:names){
				list.add(name);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}
	
	/**
	 * 返回FTP目录下的文件列表
	 * 
	 * @param ftpDirectory
	 * @return
	 */
	public List<MyFTPFile> getFTPFileList(String ftpDirectory, FTPFileFilter filter) {
		List<MyFTPFile> list = new ArrayList<MyFTPFile>();
		if (!open())
			return list;
		try {
			FTPFile [] fs = (FTPFile[]) ftpClient.listFiles(ftpDirectory, filter);
			logger.debug("list file size="+fs.length+" from "+ftpDirectory);
			for(FTPFile f:fs){
				MyFTPFile f1 = new MyFTPFile();
				f1.setFtpFile(f);
				f1.setSourcePath(ftpDirectory);
				list.add(f1);
			}
		} catch (Exception e) {
			logger.error(null,e);
		}
		return list;
	}
	
	/**
	 * 对FTP中文件全称进行修改
	 * 
	 * @param orgfile
	 * @param destfile
	 * @throws Exception
	 */
	public void rename(String orgfile, String destfile) throws Exception {
		ftpClient.rename(orgfile, destfile);
	}
	
	/**
	 * 删除FTP上的文件
	 * 
	 * @param ftpDirAndFileName
	 */
	public boolean deleteFile(String ftpDirAndFileName) {
		if (!open())
			return false;
		try {
			ftpClient.sendCommand("DELE " + ftpDirAndFileName + "\r\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * 删除FTP目录
	 * 
	 * @param ftpDirectory
	 */
	public boolean deleteDirectory(String ftpDirectory) {
		if (!open())
			return false;
		try {
			ftpClient.sendCommand("XRMD " + ftpDirectory + "\r\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * 关闭链接
	 */
	public void close() {
		try {
			if (ftpClient != null && ftpClient.isConnected())
				ftpClient.logout();
				ftpClient.disconnect();
		} catch (Exception e) {
			logger.error(null,e);
		}
	}
}