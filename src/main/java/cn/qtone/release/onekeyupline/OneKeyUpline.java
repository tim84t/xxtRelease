/**
 * 
 */
package cn.qtone.release.onekeyupline;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/**
 * 一键上线(window->linux) 程序根据svn提交日志（"./up_file_list.txt"）和设置的本地应用根路径和服务端应用根路径
 * 通过sftp协议把.class文件和静态文件上传到服务端
 * 
 * @author 林子龙
 * 
 */
public class OneKeyUpline {

	/**
	 * 上传文件
	 */
	public static void main(String[] args) {
		if (args.length != 5 && args.length != 6 && args.length != 2
				&& args.length != 3) {
			System.out
					.println("参数个数不对。拉文件到服务器需要输入五个参数或6个参数。拉文件到本地目录需要两个参数或3个参数，当前："
							+ args.length + "个");
			return;
		}
		try {
			if (args.length == 2 || args.length == 3) {
				new LocalPackage().upline(args);
			} else {
				new OneKeyUpline().upline(args);
			}

		} catch (FileNotFoundException e) {
			System.out.println("没有找到列表文件'./up_file_list.txt'");
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("上传文件失败，请检查参数和网络");
		}
	}

	/**
	 * 上传文件
	 */
	public void upline(String[] args) throws FileNotFoundException,
			IOException, JSchException, SftpException {
		for (int i = 0; i < args.length; i++) {
			if (StringUtils.isBlank(args[i])) {
				System.out.println("输入的第" + i + "个参数不能为空。");
				return;
			}
		}
		System.out.println("开始从本地复制文件到服务端。当前输入参数:(本地应用根路径:" + args[0]
				+ ",服务端应用根路径：" + args[1] + ",服务端IP:" + args[2] + ",服务端用户名:"
				+ args[3] + ",服务端密码：" + args[4] + ","
				+ (args.length == 6 ? "项目类型(:web root folder)：" + args[5] : "")
				+ ")");
		String localPath = args[0];
		String serverPath = args[1];
		String serverIp = args[2];
		String serverUserName = args[3];
		String serverPwd = args[4];
		String projectType = "", webRootFolder = "";
		if (args.length == 6) {
			String tmp = args[5];
			if ("app".equals(tmp)) {
				// 应用程序
				projectType = tmp;
			} else if (tmp.startsWith("subweb")) {
				// sub web工程
				projectType = tmp.split(":")[0];
				webRootFolder = tmp.split(":")[1];
			}
		}
		List<String> svnFileList = getFileList(projectType);
		List<String> subClass = getSubClass(localPath, svnFileList, projectType);
		svnFileList.addAll(subClass);
		List<String> localfiles = getRealPath(localPath, svnFileList,
				projectType, webRootFolder);
		List<String> serverFiles = getRealPath(serverPath, svnFileList,
				projectType, webRootFolder);

		JSch jsch = new JSch();
		Session session = jsch.getSession(serverUserName, serverIp);

		session.setPassword(serverPwd);
		Properties sshConfig = new Properties();
		sshConfig.put("StrictHostKeyChecking", "no");
		session.setConfig(sshConfig);
		session.connect();
		Channel channel = session.openChannel("sftp");
		channel.connect();
		ChannelSftp c = (ChannelSftp) channel;
		int i = 0;
		for (; i < serverFiles.size(); i++) {
			String serverFile = serverFiles.get(i);
			String localFile = localfiles.get(i);
			this.mkdir(serverFile, c);
			System.out.println("上传文件：" + localFile + " 到 " + serverFile);
			c.put(localFile, serverFile);
		}
		System.out.println("上传完毕，共处理文件" + (serverFiles.size()) + "个。");

	}

	/**
	 * 建目录
	 */
	private void mkdir(String serverFile, ChannelSftp c) {
		String serverDir = "";
		String[] dirs = serverFile.split("/");
		for (String dir : dirs) {
			if (StringUtils.isBlank(dir) || dir.indexOf(".") != -1) {
				continue;
			}
			serverDir += "/" + dir;
			try {
				c.mkdir(serverDir);
			} catch (Exception e) {
			}
		}
	}

	/**
	 * 获取内部类
	 */
	public static List<String> getSubClass(String path, List<String> fileList,
			String projectType) {
		List<String> result = new LinkedList<String>();
		for (String fileStr : fileList) {
			if (fileStr.indexOf(".java") == -1) {
				continue;
			}
			String[] fileSubs = fileStr.split("src");
			fileStr = fileSubs[fileSubs.length - 1];
			if ("app".equals(projectType)) {
				// 应用程序，不需/WEB-INF/classes目录
				fileStr = fileStr.replace(".java", ".class");
			} else {
				fileStr = "/WEB-INF/classes"
						+ fileStr.replace(".java", ".class");
			}
			String fileName = FilenameUtils.getBaseName(fileStr);
			File dir = new File(path + FilenameUtils.getFullPath(fileStr));
//			System.out.println("dir:::"+path + FilenameUtils.getFullPath(fileStr));
			for (String otherFileStr : dir.list()) {
				if (StringUtils.isBlank(otherFileStr)) {
					continue;
				}
				if (otherFileStr.startsWith(fileName + "$")
						&& otherFileStr.endsWith(".class")) {
					result.add(FilenameUtils.getFullPath(fileStr)
							+ otherFileStr);
				}
			}
		}
		return result;
	}

	/**
	 * 获取目标文件全路径名
	 */
	public static List<String> getRealPath(String path, List<String> fileList,
			String projectType, String webRootFolder) {
		List<String> result = new LinkedList<String>();
		for (String upfile : fileList) {
			if (upfile.indexOf(".java") != -1) {
				String[] fileSubs = upfile.split("src");
				upfile = fileSubs[fileSubs.length - 1];
				if ("app".equals(projectType)) {
					// 应用程序，不需/web-inf/classes目录
					upfile = upfile.replace(".java", ".class");
				}else{
					upfile = "/WEB-INF/classes" + upfile.replace(".java", ".class");
				}
			} else {
				String splitFlag = "web";
				if ("subweb".equals(projectType)) {
					// sub web工程，web root folder不一定是web，要指定
					splitFlag = webRootFolder;
				}
				String[] fileSubs = upfile.split(splitFlag);
				upfile = fileSubs[fileSubs.length - 1];
			}
			result.add(path + upfile);
		}
		return result;
	}

	/**
	 * 获取内部类全路径名
	 */
	public static List<String> getSubClass(String prePath, String subPathName) {
		List<String> result = new LinkedList<String>();
		File curPath = new File(FilenameUtils
				.getFullPath(prePath + subPathName));
		String destineName = FilenameUtils.getBaseName(prePath + subPathName);
		for (File path : curPath.listFiles()) {
			String fileName = path.getName();
			if (StringUtils.isBlank(fileName)) {
				continue;
			}
			if (fileName.startsWith(destineName + "$")
					&& fileName.endsWith(".class")) {
				result.add(FilenameUtils.getFullPath(subPathName) + "/"
						+ fileName);
			}
		}
		return result;
	}

	/**
	 * 根据文件获取文件列表
	 */
	public static List<String> getFileList(String projectType)
			throws FileNotFoundException, IOException {
		List<String> lines = IOUtils.readLines(new FileInputStream(new File(
				"./up_file_list.txt")));
		List<String> results = new LinkedList<String>();
		for (String line : lines) {
			if (StringUtils.isBlank(line)) {
				continue;
			}
			if ("app".equals(projectType)) {
				// 应用程序的项目，只有java代码，没有页面
				if (line.indexOf(".java") == -1) {
					continue;
				}
			}
			if (StringUtils.isBlank(FilenameUtils.getExtension(line))) {
				System.out.println("目录条目不处理：" + line);
				continue;
			}
			results.add(line);
		}
		return results;
	}

}
