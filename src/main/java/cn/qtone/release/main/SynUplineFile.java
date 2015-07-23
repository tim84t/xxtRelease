package cn.qtone.release.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import cn.qtone.release.util.FileUtil;


public class SynUplineFile {
	private static String trunkPath = "D:/svnProject/xxt2_gd/trunk";
	private static String tagsPath = "D:/svnProject/xxt2_gd/tags";
	private static String classPath = "D:/resin-3.1.6/webapps/xxt";
	private static String uplinePath = "F:/my work/平台功能更新/20100819/upfile";
	private static String upfileList = "F:/my work/平台功能更新/20100819/filelist.txt";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		int flag = 1; // 操作标志 0 同步到tagsPath,1 把编译好的代码放到待上传目录

		if (!trunkPath.endsWith("/"))
			trunkPath += "/";
		if (!tagsPath.endsWith("/"))
			tagsPath += "/";
		if (!classPath.endsWith("/"))
			classPath += "/";
		if (!uplinePath.endsWith("/"))
			uplinePath += "/";

		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(upfileList));

//			StringBuffer sb = new StringBuffer();
			String upfile = null;
			while ((upfile = br.readLine()) != null) {
				upfile = upfile.trim();
				if (!upfile.startsWith("/"))
					upfile = "/" + upfile;
				File trunkFile = new File(trunkPath + upfile);
				File tagsFile = new File(tagsPath + upfile);
				if (upfile.toLowerCase().endsWith(".java"))
					upfile = "/WEB-INF/classes"
							+ upfile.substring(4, upfile.length() - 5)
							+ ".class";
				else if (upfile.toLowerCase().startsWith("/web")) {
					upfile = upfile.substring(4);
				} else if (upfile.toLowerCase().startsWith("/src")) {
					upfile = "/WEB-INF/classes" + upfile.substring(4);
				}
				File classFile = new File(classPath + upfile);
				File uplineFile = new File(uplinePath + upfile);
				// System.out.println(trunkFile.getPath());
				// System.out.println(tagsFile.getPath());
				// System.out.println(classFile.getPath());
				// System.out.println(uplineFile.getPath());
				// System.out.println("-------------------------");
				if (0 == flag) {
					FileUtil.copyFile(tagsFile, trunkFile);
				} else if (1 == flag) {
					FileUtil.copyFile(uplineFile, classFile);
				}
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
