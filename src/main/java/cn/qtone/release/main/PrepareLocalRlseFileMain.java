package cn.qtone.release.main;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import cn.qtone.release.model.MyFTPFile;
import cn.qtone.release.proxy.UplineProxy;
import cn.qtone.release.util.Container;
import cn.qtone.release.util.Util;

public class PrepareLocalRlseFileMain {
	// 配置文件
	private static Properties conf;
	
	static String srcMainJava = "src"+File.separator+"main"+File.separator+"java"+File.separator;
	static String srcMainResource = "src"+File.separator+"main"+File.separator+"resources"+File.separator;
	static String srcMainWebapp = "src"+File.separator+"main"+File.separator+"webapp"+File.separator+"";

	private static Log logger = LogFactory
			.getLog(PrepareLocalRlseFileMain.class);

	HashMap<String, Object> param = new HashMap<String, Object>();

	// 初始化参数
	private boolean init(String module) {
		try {
			// 初始化
			logger.info(" * * * begin release...");
			// 取得配置文件
			logger.info("(STEP 1) load config file...");
			PrepareLocalRlseFileMain.conf = new Properties();
			PrepareLocalRlseFileMain.conf.load(PrepareLocalRlseFileMain.class
					.getClassLoader()
					.getResourceAsStream(Container.config_path));
			logger.info("(STEP 2) load config...");

			String localpath = conf.getProperty("rlse.local.path." + module,
					"/data/upfile/");
			String filename = conf.getProperty("rlse.local.file." + module,
					"/data/xxtweb/");
			String targetpath = conf.getProperty("rlse.local.targetpath."
					+ module, "backup/");

			String fileRange[] = null;
			try {
				fileRange = conf.getProperty("rlse.file.range." + module,
						"class,html,htm").split(",");
			} catch (Exception e) {
				logger.error(null, e);
			}
			if (null == fileRange) {
				logger.error("上线文件类型参数空，不处理!");
				return false;
			}

			param.put("rlse.local.path", localpath);
			param.put("rlse.local.file", filename);
			param.put("rlse.local.targetpath", targetpath);
			param.put("rlse.file.range", fileRange);
			return true;
		} catch (Exception e) {
			logger.error(null, e);
		}
		return false;
	}

	public void running(String module) throws Exception {
		init(module);
		String localpath = (String) param.get("rlse.local.path");
		String fileName = (String) param.get("rlse.local.file");
		String targetpath = (String) param.get("rlse.local.targetpath");

		File file = new File(fileName);
		List<String> lines = IOUtils.readLines(new FileInputStream(file));
		List<String> rlseFilenames = new LinkedList<String>();

		// 获取相对目录
		for (String line : lines) {
			if (StringUtils.isBlank(line) || line.endsWith(".properties")) {
				continue;
			}

			// java文件
			int index;
			String targetfile = "";
			if ((index = line.indexOf("src/main/java/")) != -1) {
				if (line.endsWith(".java")) {
					targetfile = line.replace(".java", ".class");
				} else {
					targetfile = line;
				}
				targetfile = targetfile.substring(
						index + "src/main/java/".length(), targetfile.length());
				targetfile = File.separator + "WEB-INF" + File.separator
						+ "classes" + File.separator + targetfile;
				rlseFilenames.add(targetfile.replace("/", File.separator));
			}

			if ((index = line.indexOf("src/main/resources/")) != -1) {
				if (line.endsWith(".java")) {
					targetfile = line.replace(".java", ".class");
				} else {
					targetfile = line;
				}
				targetfile = targetfile.substring(
						index + "src/main/resources/".length(),
						targetfile.length());
				targetfile = File.separator + "WEB-INF" + File.separator
						+ "classes" + File.separator + targetfile;
				rlseFilenames.add(targetfile.replace("/", File.separator));
			}

			if ((index = line.indexOf("src/main/webapp/")) != -1) {
				if (line.endsWith(".java")) {
					targetfile = line.replace(".java", ".class");
				} else {
					targetfile = line;
				}
				targetfile = targetfile.substring(
						index + "src/main/webapp/".length(),
						targetfile.length());
				targetfile = File.separator + targetfile;
				rlseFilenames.add(targetfile.replace("/", File.separator));
			}
		}
		
		Set<String> pathSet = new HashSet<String>();
		for (String relativePath : rlseFilenames) {
			String localFile = localpath + relativePath;
			pathSet.add(FilenameUtils.getFullPath(localFile));//上线包
			String targetFile = targetpath + relativePath;
			
			File local = new File(localFile);
			File target = new File(targetFile);
			if(!local.isDirectory()){
				FileUtils.copyFile(local, target);
			}
		}
		
		List<String> innerClasses = getInnerClass(pathSet);
		//复制内部类
		for (String relativePath : rlseFilenames) {
			
			for(String str : innerClasses){
				String temstr = FilenameUtils.getFullPath(relativePath)+FilenameUtils.getBaseName(relativePath);
				if(FilenameUtils.wildcardMatch(str, temstr+"*")){
					FileUtils.copyFile(new File(localpath+str), new File(targetpath+str));
				}
			}
		}
	
	}

	/**
	 * 获取内部类相对路径
	 */
	public static List<String> getInnerClass(Set<String> pathSet) {
		List<String> result = new LinkedList<String>();
		
		for(String path : pathSet){
			File curPath = new File(path);
			for(File curFile : curPath.listFiles()){
				if(!curFile.isDirectory() && curFile.getName().contains("$")){
					int index = 0;
					if ((index = curFile.getPath().indexOf(srcMainWebapp)) != -1) {
						String targetfile = curFile.getPath().substring(
								index + srcMainWebapp.length(),
								curFile.getPath().length());
						targetfile = File.separator + targetfile;
						result.add(targetfile);
					}
				}
			}
		}
		return result;
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String module = "test";
		PrepareLocalRlseFileMain obj = new PrepareLocalRlseFileMain();
		try {
			obj.running(module);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}