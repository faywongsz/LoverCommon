package com.yuning.game.engine;


import android.os.Environment;

public class Consts {

	public static final int BUS_DOWNLOAD_LOADING = 1;
	public static final int BUS_DOWNLOAD_SUCCESS = 2;

	public static String SAVE_PATH = Environment.getExternalStorageDirectory() + "/ustore/";
	public static String DOWNLOAD_PATH = SAVE_PATH + "download/";
}
