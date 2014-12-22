package com.lzr.downloader.core;
 
 import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.lzr.Dao.Dao;
import com.lzr.entity.DownloadInfo;
import com.lzr.entity.LoadInfo;
 
 public class Downloader {
     private String urlstr;// ���صĵ�ַ
     private String localfile;// ����·��
     private int threadcount;// �߳���
     private OnDownloadListener downloadListener;//���ؼ�����
     private Context context; //������
     
     private DownloadController downloadController;//���ؿ�����
     private int fileSize;// ��Ҫ���ص��ļ��Ĵ�С
     private List<DownloadInfo> Downloadinfos;// ���������Ϣ��ļ���
     private LoadInfo loadInfo;//������Ϣ������
     private static final int INIT = 1;//�����������ص�״̬����ʼ��״̬����������״̬����ͣ״̬
     private static final int DOWNLOADING = 2;
     private static final int PAUSE = 3;
     private int state = INIT;
 
     public Downloader(String urlstr, String localfile, int threadcount,
             Context context, OnDownloadListener listener) {
         this.urlstr = urlstr;
         this.localfile = localfile;
         this.threadcount = threadcount;
         this.downloadListener=listener;
         this.context = context;
         
         this.downloadController=new DownloadController();
     }
     /**
      * ���ؼ�����
      * @author w_yifan
      *
      */
     public interface OnDownloadListener{
    	 public void onStart(int total);
    	 public void onDownLoading(int current,int total,int increment);
    	 public void onCompleted();
     }
     private Handler mHandler=new Handler(){
    	public void handleMessage(Message msg) {
    		if(msg.what==1&&msg.obj.equals(urlstr)){
    			loadInfo.setComplete(loadInfo.getComplete()+msg.arg1);
    			downloadListener.onDownLoading(loadInfo.getComplete(), loadInfo.getFileSize(),msg.arg1);
    			if(loadInfo.getComplete()==loadInfo.getFileSize()){
    				 Log.v("TAG", "download is completed");
    				 state=INIT;
    	    		 Dao.getInstance(context).delete(urlstr);
    	    		 DownloaderUtil.getInstance().getDownloaders().remove(urlstr);
    	    		 downloadListener.onCompleted();
    			}
    		}
    	}; 
     };
     /**
      *�ж��Ƿ��������� 
      */
     public boolean isdownloading() {
         return state == DOWNLOADING;
     }
     /**
      * �õ�downloader�����Ϣ
      * ���Ƚ����ж��Ƿ��ǵ�һ�����أ�����ǵ�һ�ξ�Ҫ���г�ʼ������������������Ϣ���浽���ݿ���
      * ������ǵ�һ�����أ��Ǿ�Ҫ�����ݿ��ж���֮ǰ���ص���Ϣ����ʼλ�ã�����Ϊֹ���ļ���С�ȣ�������������Ϣ���ظ�������
      */
     public void initDownloaderInfors() {
         if (isFirst(urlstr)) {
             Log.v("TAG", "isFirst");
             init();
             int range = fileSize / threadcount;
             Downloadinfos = new ArrayList<DownloadInfo>();
             for (int i = 0; i < threadcount - 1; i++) {
                 DownloadInfo info = new DownloadInfo(i, i * range, (i + 1)* range - 1, 0, urlstr);
                 Downloadinfos.add(info);
             }
             DownloadInfo info = new DownloadInfo(threadcount - 1,(threadcount - 1) * range, fileSize - 1, 0, urlstr);
             Downloadinfos.add(info);
             //����infos�е����ݵ����ݿ�
             Dao.getInstance(context).saveInfos(Downloadinfos);
             //����һ��LoadInfo��������������ľ�����Ϣ
             loadInfo = new LoadInfo(fileSize, 0, urlstr);
         } else {
             //�õ����ݿ������е�urlstr���������ľ�����Ϣ
             Downloadinfos = Dao.getInstance(context).getInfos(urlstr);
             Log.v("TAG", "not isFirst size=" + Downloadinfos.size());
             int fileSize = 0;
             int fileCompeleteSize = 0;
             for (DownloadInfo info : Downloadinfos) {
                 fileCompeleteSize += info.getCompeleteSize();
                 fileSize += info.getEndPos() - info.getStartPos() + 1;
             }
             loadInfo = new LoadInfo(fileSize, fileCompeleteSize, urlstr);
         }
     }
 
     /**
      * ��ʼ��(��δ���ص��ļ�)
      */
     private void init() {
         try {
             URL url = new URL(urlstr);
             HttpURLConnection connection = (HttpURLConnection) url.openConnection();
             connection.setConnectTimeout(5000);
             connection.setRequestMethod("GET");
             fileSize = connection.getContentLength();
 
             File file = new File(localfile);
             if (!file.exists()) {
                 file.createNewFile();
             }
             // ���ط����ļ�
             RandomAccessFile accessFile = new RandomAccessFile(file, "rwd");
             accessFile.setLength(fileSize);
             accessFile.close();
             connection.disconnect();
         } catch (Exception e) {
             e.printStackTrace();
         }
     }  
     /**
      * �ж��Ƿ��ǵ�һ�� ����
      */
     private boolean isFirst(String urlstr) {
         return Dao.getInstance(context).isHasInfors(urlstr);
     }
 
     /**
      * �����߳̿�ʼ��������
      */
     public DownloadController download() {
    	 if(state!=DOWNLOADING)
    		 new DownloadTask().execute();
         return this.downloadController;
     }
     private class DownloadTask extends AsyncTask<Void, Void, Void>{

		@Override
		protected Void doInBackground(Void... params) {
			// TODO Auto-generated method stub
			initDownloaderInfors();
			return null;
		}
    	@Override
    	protected void onPostExecute(Void result) {
    		// TODO Auto-generated method stub
    	
    		if (Downloadinfos != null) {
                if (state == DOWNLOADING)
                    return;
                state = DOWNLOADING;
                downloadListener.onStart(loadInfo.getFileSize());
                for (DownloadInfo info : Downloadinfos) {
                    new DownloadThread(info.getThreadId(), info.getStartPos(),
                            info.getEndPos(), info.getCompeleteSize(),
                            info.getUrl()).start();
                }
            }
    	}
     }
     
     public class DownloadThread extends Thread {
         private int threadId;
         private int startPos;
         private int endPos;
         private int compeleteSize;
         private String urlstr;
 
         public DownloadThread(int threadId, int startPos, int endPos,
                 int compeleteSize, String urlstr) {
             this.threadId = threadId;
             this.startPos = startPos;
             this.endPos = endPos;
             this.compeleteSize = compeleteSize;
             this.urlstr = urlstr;
         }
         @Override
         public void run() {
             HttpURLConnection connection = null;
             RandomAccessFile randomAccessFile = null;
             InputStream is = null;
             try {
                 URL url = new URL(urlstr);
                 connection = (HttpURLConnection) url.openConnection();
                 connection.setConnectTimeout(5000);
                 connection.setRequestMethod("GET");
                 // ���÷�Χ����ʽΪRange��bytes x-y;
                 connection.setRequestProperty("Range", "bytes="+(startPos + compeleteSize) + "-" + endPos);
 
                 randomAccessFile = new RandomAccessFile(localfile, "rwd");
                 randomAccessFile.seek(startPos + compeleteSize);
                 // ��Ҫ���ص��ļ�д�������ڱ���·���µ��ļ���
                 is = connection.getInputStream();
                 byte[] buffer = new byte[4096];
                 int length = -1;
                 while ((length = is.read(buffer)) != -1) {
                     randomAccessFile.write(buffer, 0, length);
                     compeleteSize += length;
                     // �������ݿ��е�������Ϣ
                     Dao.getInstance(context).updataInfos(threadId, compeleteSize, urlstr);
                     // ����Ϣ��������Ϣ�������������Խ��������и���
                     Message message = Message.obtain();
                     message.what = 1;
                     message.obj = urlstr;
                     message.arg1 = length;
                     mHandler.sendMessage(message);
                     if (state == PAUSE) {
                    	 Log.v("TAG", "download is pause");
                         return;
                     }
                 }
             } catch (Exception e) {
                 e.printStackTrace();
             }  
         }
     }
     
     
     /**
      * ���ؿ�����
      * @author w_yifan
      *
      */
     public class DownloadController{
    	//������ͣ
    	 public void pause(){
    		 state=PAUSE;
    	 }
    	
    	 public void cancel(){
    		 state=PAUSE;
    		 Dao.getInstance(context).delete(urlstr);
    		 File downFile=new File(localfile);
    		 if(downFile.exists()){
    			 try{
    			     if(!downFile.delete())
    			    	 throw new Exception("file delete is failed,please delete it yourself");
    			     Log.v("TAG", "download is cancelled");
    			 }catch(Exception e){
    				 e.printStackTrace();
    			 }finally{
    				 if(downFile!=null)
    					 downFile=null;
    			 }
    		 }
    	 }
     }
 }