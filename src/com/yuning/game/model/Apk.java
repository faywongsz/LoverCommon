package com.yuning.game.model;


import java.io.Serializable;

import com.jayqqaa12.abase.model.Bean;
import com.lidroid.xutils.db.annotation.NoAutoIncrement;
import com.lidroid.xutils.db.annotation.Table;
import com.lidroid.xutils.http.HttpHandler;

@Table(name="push_apk")
public class Apk extends Bean  implements Serializable{

	private static final long serialVersionUID = 1L;

	@NoAutoIncrement
	public int id;
	public int version_code;
	public long size;
	
	public int download_type;

	public String version_name;
	public String icon_url;
	public String  img_url;
	public String download_url;
	public String name;
	public String des;
	public String packagename;
	public String modifydate;
	
	

	public HttpHandler.State state = HttpHandler.State.STOPPED;


	
	
	@Override
	public String toString() {
		return "id= "+id +" name = "+name +" icon_url= "+icon_url+" img_url= "+img_url+" download_url= "+download_url;
	}


 
	

}
