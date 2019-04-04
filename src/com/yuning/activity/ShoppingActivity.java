package com.yuning.activity;

import com.yuning.lovercommon.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

public class ShoppingActivity extends Activity {

	private static final String URL = "http://115.29.197.230:8888/";
	private WebView mWebView;
	private ProgressDialog mProgressDialog;
	
	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.shopping_activity);
		
		mWebView = (WebView) findViewById(R.id.web_view);
		ImageView back = (ImageView) findViewById(R.id.title_back);
		((TextView) findViewById(R.id.title_name)).setText(R.string.shopping_title);
		back.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		
		mWebView.loadUrl(URL);
		mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
		mWebView.getSettings().setDomStorageEnabled(true);
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.setWebViewClient(new SampleWebViewClient());
		mWebView.setWebChromeClient(new SampleWebChromeClient());
		
		showProgressDialog(null, getString(R.string.shopping_loading));
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && mWebView.canGoBack()) {
			mWebView.goBack();
			return true;
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}
	
	public void showProgressDialog(String title,String message) {
		if(mProgressDialog==null){
			 mProgressDialog = ProgressDialog.show(this, title, message, true, true);
		} else if (mProgressDialog.isShowing()) {
			mProgressDialog.setTitle(title);
			mProgressDialog.setMessage(message);
		}
		mProgressDialog.show();
		mProgressDialog.setCancelable(false);
	}
	
	public void hideProgressDialog(){
		if(mProgressDialog!=null&&mProgressDialog.isShowing()){
			mProgressDialog.dismiss();
		}
	}
	
	private class SampleWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}
	}
	
	private class SampleWebChromeClient extends WebChromeClient {
		@Override
		public void onProgressChanged(WebView view, int newProgress) {
			if(newProgress == 100) {
				hideProgressDialog();
			}
		}
	}
}
