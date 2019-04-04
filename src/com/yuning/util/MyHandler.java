/*
 * Author:Wang Lei
 */

package com.yuning.util;

import android.os.Handler;
import android.os.Message;

public abstract class MyHandler extends Handler {
	private long mTimer = 30;
	private long mLastTimer = 0;
	private boolean is_running = false;

	public MyHandler() {
		super();
	}

	@Override
	public void handleMessage(Message msg) {
		update();
	}

	private void sleep(long delayMillis) {
		removeMessages(0);
		sendMessageDelayed(obtainMessage(0), delayMillis);
	}

	private void update() {
		long now = System.currentTimeMillis();
		long realTimer = now - mLastTimer;

		if (realTimer > mTimer) {
			mLastTimer = now;
			OnTimer();
		}

		if (is_running) {
			sleep(mTimer);
		} else {
			OnTimerStop();
		}
	}

	public void startTimer() {
		is_running = true;
		OnTimerStart();
		update();
	}

	public void stopTimer() {
		is_running = false;
	}

	public boolean getIsRunning() {
		return is_running;
	}

	public void setTimer(long timer) {
		mTimer = timer;
	}

	protected abstract void OnTimerStart();

	protected abstract void OnTimer();

	protected abstract void OnTimerStop();
}