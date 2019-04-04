package com.yuning.game.engine;

import java.io.File;
import java.util.List;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.jayqqaa12.abase.core.ADao;
import com.jayqqaa12.abase.core.Abase;
import com.jayqqaa12.abase.core.Abus;
import com.jayqqaa12.abase.download.DownloadManager;
import com.jayqqaa12.abase.download.DownloadService;
import com.jayqqaa12.abase.kit.IntentKit;
import com.jayqqaa12.abase.kit.common.L;
import com.jayqqaa12.abase.kit.common.T;
import com.jayqqaa12.abase.kit.network.NetworkKit;
import com.jayqqaa12.abase.kit.sys.SdCardKit;
import com.jayqqaa12.abase.kit.sys.SysIntentKit;
import com.jayqqaa12.abase.kit.ui.NotifiyKit;
import com.yuning.game.GameMode;
import com.yuning.game.model.Apk;
import com.yuning.game.model.MyDownloadInfo;
import com.yuning.lovercommon.R;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.HttpHandler;
import com.lidroid.xutils.http.HttpHandler.State;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;

public class DownloadEngine {

	public static Abus bus = new Abus();

	Context context = Abase.getContext();

	private boolean nofiyExit;

	public static DownloadEngine me = new DownloadEngine();

	public boolean checkError() {

		if (!NetworkKit.isConnectingToInternet()) {
			T.ShortToast(context.getString(R.string.download_network_wrong));
			return false;
		}
		if (!SdCardKit.isCanUseSdCard()) {
			T.ShortToast(context.getString(R.string.download_no_sdcard));
			return false;
		}
		if (SdCardKit.getAvailableSDRom() * 1024 < 50) {
			T.ShortToast(context.getString(R.string.download_no_enough_storage));
			return false;
		}
		return true;
	}

	public boolean isDownloading() {
		List<MyDownloadInfo> list = getDownloadManager().getDownloadList();
		for (MyDownloadInfo info : list) {
			if (info.state == HttpHandler.State.LOADING) return true;
			if (info.state == HttpHandler.State.WAITING) return true;
			if (info.state == HttpHandler.State.STARTED) return true;
		}
		return false;
	}

	public DownloadManager<MyDownloadInfo> getDownloadManager() {
		if (DownloadService.getDownloadManager() == null) DownloadService
				.setDownloadManager(new DownloadManager<MyDownloadInfo>(MyDownloadInfo.class));

		return (DownloadManager<MyDownloadInfo>) DownloadService.getDownloadManager();
	}

	public void remuseAllTask() {
		List<MyDownloadInfo> list = getDownloadManager().getDownloadList();
		for (MyDownloadInfo info : list) {
			if ( info.state == State.STOPPED ) remuseTask(info);
		}

	}

	public void removeTask(String path) {
		MyDownloadInfo item = findByPath(path);
		if (item != null) removeTask(item);
	}

	public void removeTask(int app_id) {
		MyDownloadInfo item = findByAppId(app_id);
		if (item != null) removeTask(item);
	}

	public void removeTask(MyDownloadInfo item) {
		// if (item.apk.pause())
		// {
		// T.ShortToast("当前下载 不可删除");
		// return;
		// }

		getDownloadManager().removeDownload(item);
		deleteNofiy();
	}

	public void remuseTask(MyDownloadInfo item) {
		if (!checkError()) return;

		L.i("remuse taks  and apk=" + item.apk);
		showNofiy(item.apk);
		sendMsg(HttpHandler.State.WAITING, item.app_id, 0);

		getDownloadManager().resumeDownload(item, new MyCallBack(item));
	}

	public void addTask(final MyDownloadInfo info) {
		if (!checkError()) return;
		sendMsg(HttpHandler.State.WAITING, info.app_id, 0);
		MyDownloadInfo old = findByAppId(info.app_id);
		if (old != null) {
			remuseTask(old);
		} else {
			new ADao().save(info.apk);
			showNofiy(info.apk);
			getDownloadManager().addNewDownload(info, new MyCallBack(info));
		}

	}

	public void stopTask(int app_id) {
		MyDownloadInfo info = findByAppId(app_id);
		if (info != null) stopTask(info);
	}

	public void stopTask(MyDownloadInfo item) {

		// if (item.apk.pause())
		// {
		// T.ShortToast("当前下载不可暂停");
		// return;
		// }

		getDownloadManager().stopDownload(item);
	}

	public MyDownloadInfo findByPath(String path) {
		if (path == null) return null;
		for (MyDownloadInfo info : getDownloadManager().getDownloadList()) {
			if (path.equals(info.fileSavePath)) return info;
		}
		return null;
	}

	public MyDownloadInfo findByAppId(int app_id) {
		for (MyDownloadInfo info : getDownloadManager().getDownloadList()) {
			if (info.app_id == app_id) return info;
		}

		return null;
	}

	public class MyCallBack extends RequestCallBack<File> {
		private MyDownloadInfo item;
		private int prog;

		public MyCallBack(MyDownloadInfo item) {
			this.item = item;
		}

		@Override
		public void onStart() {
			super.onStart();
			sendMsg(HttpHandler.State.STARTED, item.app_id, prog);
		}

		@Override
		public void onStopped() {
			super.onStopped();
			sendMsg(HttpHandler.State.STOPPED, item.app_id, prog);

			deleteNofiy();
		}

		@Override
		public void onLoading(long total, long current, boolean isUploading) {
			super.onLoading(total, current, isUploading);
			this.prog = (int) (current * 100 / total);

			sendMsg(HttpHandler.State.LOADING, item.app_id, prog);
		}

		@Override
		public void onSuccess(ResponseInfo<File> file) {

			sendMsg(HttpHandler.State.SUCCESS, item.app_id, prog);
			bus.post(Consts.BUS_DOWNLOAD_SUCCESS);
			deleteNofiy();
			// auto instlll
			// BirdRootKit.install(context, file.result.toString(), null);

			SysIntentKit.install(context, file.result.toString());
		}

		@Override
		public void onFailure(HttpException ex, String msg) {
			sendMsg(HttpHandler.State.STOPPED, item.app_id, 0);

			int code = ex.getExceptionCode();
			if (!checkError()) showErrorNofiy(item.apk);
			else if (code == 416) sendMsg(HttpHandler.State.SUCCESS, item.app_id, prog);
			else showErrorNofiy(item.apk);

			L.i("download fail msg =" + msg + " code = " + ex.getExceptionCode());
		}

	};

	private void sendMsg(final HttpHandler.State status, final int id, final int prog) {
		bus.post(IntentKit.getIntent(new String[] { "status", "id", "prog" }, status, id, prog));

	}

	private void deleteNofiy() {
		if (!isDownloading()) {
			NotifiyKit.deleteNotification(1);
			nofiyExit = false;
		}
	}

	private void showErrorNofiy(Apk apk) {
		if (apk == null) return;

		deleteNofiy();

		Intent intent = new Intent(context, GameMode.class);
		intent.setData(Uri.parse("custom://" + System.currentTimeMillis()));
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra("apk", apk);
		PendingIntent pi = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		NotifiyKit.showNotification(context, 2, android.R.drawable.stat_sys_download, apk.name + "下载失败", "点击打开",
				pi, true);
	}

	private void showNofiy(Apk apk) {
		if (nofiyExit) return;

		L.i("show nofiy  apk =" + apk);

		if (apk == null || apk.name == null) return;

		Intent intent = new Intent(context, GameMode.class);
		intent.setData(Uri.parse("custom://" + System.currentTimeMillis()));
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		PendingIntent pi = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		NotifiyKit.showNotification(context, 1, android.R.drawable.stat_sys_download, apk.name + "正在下载", "点击打开",
				pi, false);
		nofiyExit = true;
	}
}
