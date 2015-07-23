package cn.qtone.release.onekeyupline;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * 用于上线文件本地打包
 * 
 * @author 林子龙
 * 
 */
public class LocalPackage {
	public void upline(String[] args) throws FileNotFoundException, IOException {
		for (int i = 0; i < args.length; i++) {
			if (StringUtils.isBlank(args[i])) {
				System.out.println("输入的第" + i + "个参数不能为空。");
				return;
			}
		}
		System.out
				.println("开始从本地复制文件到本地打包上线路径。参数：本地应用根路径:"
						+ args[0]
						+ ",本地打包上线路径："
						+ args[1]
						+ (args.length == 3 ? "，项目类型(:web root folder)："
								+ args[2] : ""));
		String localPath = args[0];
		String serverPath = args[1];
		String projectType = "", webRootFolder = "";
		if (args.length == 3) {
			String tmp = args[2];
			if ("app".equals(tmp)) {
				// 应用程序
				projectType = tmp;
			} else if (tmp.startsWith("subweb")) {
				// sub web工程
				projectType = tmp.split(":")[0];
				webRootFolder = tmp.split(":")[1];
			}
		}
		List<String> svnFileList = OneKeyUpline.getFileList(projectType);
		List<String> subClass = OneKeyUpline
				.getSubClass(localPath, svnFileList,projectType);
		svnFileList.addAll(subClass);
		List<String> localfiles = OneKeyUpline.getRealPath(localPath,
				svnFileList, projectType, webRootFolder);
		List<String> serverFiles = OneKeyUpline.getRealPath(serverPath,
				svnFileList, projectType, webRootFolder);
		for (int i = 0; i < localfiles.size(); i++) {
			System.out.println("上传文件：" + localfiles.get(i) + " 到 "
					+ serverFiles.get(i));
			FileUtils.copyFile(new File(localfiles.get(i)), new File(
					serverFiles.get(i)));
		}
		System.out.println("上传完毕，共处理文件" + (serverFiles.size()) + "个。");
		if (subClass.size() > 0) {
			System.out.println("自动上传内部类、枚举等" + (subClass.size()) + "个。");
		}
	}
}
