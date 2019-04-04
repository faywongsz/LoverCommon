package com.yuning.game;

import java.util.List;

import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.GridView;
import android.widget.ProgressBar;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jayqqaa12.abase.core.ACache;
import com.jayqqaa12.abase.core.AHttp;
import com.jayqqaa12.abase.core.Abase;
import com.jayqqaa12.abase.kit.common.T;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.yuning.activity.BaseModeActivity;
import com.yuning.game.model.Apk;
import com.yuning.lovercommon.R;

public class GameMode extends BaseModeActivity {
	private static final String RECOMMEND_URL = "http://218.244.156.128:2345/api/app/recommend/list?uid=1&page=1&count=50";

	GridView gv;
	GridViewAdapter adapter;
	ProgressBar pb;
	List<Apk> list;
	
	Handler handler= new Handler();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.game_mode);
		
		initTitleBar(R.string.game_mode, 0, 0, null);
		
		Abase.setContext(this);
		gv = (GridView) findViewById(R.id.gridview);
		gv.setOverScrollMode(View.OVER_SCROLL_NEVER);
		pb = (ProgressBar) findViewById(R.id.pb);

		conn();
		
		startService(getServiceIntent());
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		stopService(getServiceIntent());
		
	}

	private void conn() {

		new AHttp().get(RECOMMEND_URL, ACache.TIME_HOUR, new RequestCallBack<String>() {
			@Override
			public void onFailure(HttpException error, String msg) {
				T.ShortToast(R.string.network_error);
			}

			@Override
			public void onSuccess(ResponseInfo<String> resp) {

				pb.setVisibility(View.GONE);

				String json = resp.result;
				try {
					String apps = new JSONObject(json).getJSONObject("data").getString("list");
					list = new Gson().fromJson(apps, new TypeToken<List<Apk>>() {}.getType());
					
					
					reset();

				} catch (Exception e) {
					ACache.create().clear();
					T.ShortToast(R.string.network_error);
					e.printStackTrace();
				}
			}
		});
	}

	public void reset() {
		if (list != null) {
			adapter = new GridViewAdapter(list, GameMode.this);
			gv.setAdapter(adapter);
		}
		
		update();
	}
	
	private void update() {
		if(adapter != null) adapter.notifyDataSetChanged();
		
		handler.postDelayed(new Runnable() {
			public void run() {
				update();
			}
		}, 1000);
	}
	
	private Intent getServiceIntent() {
		return new Intent(this, GameService.class);
	}
	
	public boolean isConnect() {
		return super.isConnect();
	}

	public void showOpenDeviceDialog() {
		super.showOpenDeviceDialog();
	}
}
