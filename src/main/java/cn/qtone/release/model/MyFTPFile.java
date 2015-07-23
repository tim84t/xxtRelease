package cn.qtone.release.model;

import org.apache.commons.net.ftp.FTPFile;

public class MyFTPFile {
	/**
	 * 
	 */
	private String sourcePath;
	private FTPFile ftpFile;

	public String getSourcePath() {
		return sourcePath;
	}

	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	public FTPFile getFtpFile() {
		return ftpFile;
	}

	public void setFtpFile(FTPFile ftpFile) {
		this.ftpFile = ftpFile;
	}
}
