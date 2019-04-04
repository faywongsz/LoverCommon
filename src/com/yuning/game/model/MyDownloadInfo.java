package com.yuning.game.model;


import com.jayqqaa12.abase.download.DownloadInfo;
import com.lidroid.xutils.db.annotation.Foreign;
import com.lidroid.xutils.db.annotation.Table;
import com.yuning.game.engine.Consts;

@Table(name="recommend_download")
public class MyDownloadInfo extends DownloadInfo
{
	public int app_id;

	@Foreign(foreign = "id" ,column="apk_id")
	public Apk apk=new Apk();
	
	public MyDownloadInfo() {
	}
	
	public MyDownloadInfo(Apk apk) {
		
		this.apk =apk;
		this.app_id = apk.id;
		this.downloadUrl=apk.download_url;
		this.autoResume=true;
		this.fileLength=apk.size;
		this.fileName= apk.id+".apk";
		this.fileSavePath=Consts.DOWNLOAD_PATH + this.fileName;
	}

 
	
 

}
