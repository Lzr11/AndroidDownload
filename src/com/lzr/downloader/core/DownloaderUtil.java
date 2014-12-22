package com.lzr.downloader.core;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import com.lzr.downloader.core.Downloader.DownloadController;
import com.lzr.downloader.core.Downloader.OnDownloadListener;

public class DownloaderUtil {

	public static final String TAG=DownloaderUtil.class.getSimpleName();
	private static DownloaderUtil instance=null;
	private static Map<String,Downloader> downloaders=null;
	private DownloaderUtil(){
		
	}
	public static DownloaderUtil getInstance(){
		if(instance==null){
			synchronized(Object.class){
				if(instance==null)
				{
					instance=new DownloaderUtil();
					downloaders=new HashMap<String,Downloader>();
				}
			}
		}
		return instance;
	}
	public DownloadController download(String urlstr, String localfile, int threadcount,
            Context context, OnDownloadListener listener){
		Downloader downloader=null;
		if((downloader=downloaders.get(urlstr))==null){
			downloader=new Downloader(urlstr, localfile, threadcount, context, listener);
			downloaders.put(urlstr, downloader);
		}
		return downloader.download();
	}
	public Map<String,Downloader> getDownloaders(){
		return downloaders;
	}
}
