AndroidDownload
===============
小弟参考了网络上android多线程下载（支持断点续传）
由于git的操作不是太熟练，所以暂且把library源码上传（其实是我不知道怎么把几个项目提交到github上的一个项目中来）
（待会我会把library的jar包也上传上去）
这个是用也很简单
只需几步操作即可

1 使你的android project引用此工程（其实只需要把library添加至你的工程的lib包就可以了）
2 DownloadController downloadController =
                DownloaderUtil.getInstance().download("http://download.haozip.com/haozip_v2.8_tiny.exe", "/mnt/sdcard/QQMusic.exe", 4, getApplicationContext(), listener);
3 第一个参数为下载地址，第二个参数为下载到手机上的路径，带三个参数为开启的线程数目（多线程下载吗，对一个文件开几个线程进行下载）
        第四个参数为context,第五个参数为OnDownladListener的接口实例(就是下载的回调)
4 返回的是一个下载控制器
   可以随时暂停下载 downloadController.pause();
   可以随时取消下载 downloadController.cancel();
   
   
   ！！！待会我会把我的测试demo也上传至guithub叫做androiddownload_test
