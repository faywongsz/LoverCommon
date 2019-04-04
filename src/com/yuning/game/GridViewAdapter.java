package com.yuning.game;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.text.format.Formatter;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.jayqqaa12.abase.core.ABitmap;
import com.jayqqaa12.abase.kit.sys.AppInfoKit;
import com.jayqqaa12.abase.kit.sys.SysIntentKit;
import com.yuning.game.engine.Consts;
import com.yuning.game.engine.DownloadEngine;
import com.yuning.game.model.Apk;
import com.yuning.game.model.MyDownloadInfo;
import com.yuning.lovercommon.R;
import com.lidroid.xutils.http.HttpHandler;
import com.lidroid.xutils.http.HttpHandler.State;
import com.squareup.otto.Subscribe;

public class GridViewAdapter extends BaseAdapter {
	private List<Apk> apps;
	private Context cxt;
	private static ABitmap iconBitmap = new ABitmap();


	final String file = Environment.getExternalStoragePublicDirectory("recommend").getPath();

	public GridViewAdapter(List<Apk> apps, Context cxt) {
		iconBitmap.configDefaultImage(R.drawable.game_mode_default_icon, R.drawable.game_mode_default_icon);
		this.cxt = cxt;
		this.apps = apps;
		DownloadEngine.bus.register(this);
	}

	@Override
	public int getCount() {
		return apps.size();
	}

	@Override
	public Object getItem(int position) {
		return apps.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Subscribe
	public void status(Intent intent) {

		int id = intent.getIntExtra("id", 0);
		HttpHandler.State status = (State) intent.getSerializableExtra("status");
		if (id == 0) return;

		for (Apk info : apps) {
			if (info.id == id) {
				info.state = status;
			}
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		View v = null;
		if (convertView != null) v = convertView;
		else v = View.inflate(cxt, R.layout.game_mode_item, null);

		View layout = v.findViewById(R.id.rt_layout);
		TextView tv = (TextView) v.findViewById(R.id.item_tv);
		TextView size = (TextView) v.findViewById(R.id.item_size);
		ImageView iv = (ImageView) v.findViewById(R.id.item_iv);
		ImageView flag = (ImageView) v.findViewById(R.id.item_iv_flag);
		Button btn_flag = (Button) v.findViewById(R.id.item_iv_flag_btn);

		final Apk app = apps.get(position);
		tv.setText(app.name);
		size.setText(cxt.getResources().getString(R.string.game_mode_size, Formatter.formatFileSize(cxt, app.size)));
		iconBitmap.display(iv, app.icon_url);
		if(position % 2 == position / 2) {
			layout.setBackgroundResource(R.drawable.game_mode_item_bg_1);
		} else {
			layout.setBackgroundResource(R.drawable.game_mode_item_bg_2);
		}

		if (AppInfoKit.isInstall(app.packagename)) {
			flag.setImageResource(R.drawable.game_mode_flag_run);
			btn_flag.setText(R.string.game_mode_start);
		} else if (AppInfoKit.isRightApk(Consts.DOWNLOAD_PATH + app.id + ".apk")) {
			flag.setImageResource(R.drawable.game_mode_flag_install);
			btn_flag.setText(R.string.game_mode_install);
		}else {
			switch (app.state) {
				case SUCCESS:
					break;
				case FAILURE:
				case STOPPED:
					flag.setImageResource(R.drawable.game_mode_flag_download);
					btn_flag.setText(R.string.game_mode_download);
					break;
				case WAITING:
				case STARTED:
				case LOADING:
					flag.setImageResource(R.drawable.game_mode_flag_loading);
					btn_flag.setText(R.string.game_mode_downloading);
					break;
			}
		}

		btn_flag.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// 判断是否 安装 没有就下载 有就运行 有下载就安装
				// if install open
				
				if(!((GameMode) cxt).isConnect()) {
					((GameMode) cxt).showOpenDeviceDialog();
					return;
				}

				if (AppInfoKit.isInstall(app.packagename)) {
					cxt.startActivity(AppInfoKit.getIntentFromPackage(app.packagename));
				}else if (AppInfoKit.isRightApk(Consts.DOWNLOAD_PATH + app.id + ".apk")) {
					SysIntentKit.install(cxt,Consts.DOWNLOAD_PATH + app.id + ".apk");
				} else {
					switch (app.state) {
						case SUCCESS:
							break;
						case FAILURE:
						case STOPPED:
							DownloadEngine.me.addTask(new MyDownloadInfo(app));
						case WAITING:
						case STARTED:
						case LOADING:
							break;
					}
				}

			}
		});
		return v;
	}
}
