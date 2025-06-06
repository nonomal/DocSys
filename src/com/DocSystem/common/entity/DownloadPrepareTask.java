package com.DocSystem.common.entity;

import java.util.List;

import com.DocSystem.entity.Doc;
import com.DocSystem.entity.Repos;

public class DownloadPrepareTask 
{
	public final static int download_local_folder 		 = 0;
	public final static int download_repos_folder 		 = 1;
	public final static int download_verRepos_folder 	 = 2;
	public final static int download_remoteServer_folder = 3;
	public final static int download_with_docList 		 = 4;
	
	public String id;
	
	public Integer type = 0; //0: compress dedicated folder 1: download repos's folder 2:download verRepos's folder or file

	public String info = "下载准备中...";

	public Long createTime;
	
	public boolean stopFlag = false;
	
	public Repos repos;
	public Doc doc;
	public ReposAccess reposAccess;
	
	//基于文件列表的打包下载
	public List<Doc> docList;

	//历史版本下载
	public String commitId = null;
	public Integer downloadAll = null;
	public Integer needDeletedEntry = null;
	public Integer historyType = null;
	
	//需要直接压缩的目录
	public String inputPath;
	public String inputName;
	public boolean deleteInput = false;
	
	//压缩文件存放路径
	public String targetPath;
	public String targetName;
	public Long targetSize;
	
	public int status;  //1:压缩中 2:压缩成功 3:压缩失败
}
