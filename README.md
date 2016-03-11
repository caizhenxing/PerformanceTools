#PerformanceTools
1、应用启动时间的测试
应用启动时间可以分为三类：（1）首次启动：应用首次启动花费的时间。
                         （2）非首次启动：应用非首次启动花费的时间。
                         （3）应用界面切换：应用界面内切换所花费的时间。
从软件测试的方向可以这样测试：
（1）通过ActivityManager来获取应用的启动时间。通过logcat过滤关键字”Displayed”来获取所有activity的打印时间，再通过activity name来获取所测应用的启动时间。例如这里是获取com.huawei.xdevice.monkeytest/.MainActivity这个activity的启动时间，可以通过搜索logcat 中的” Displayed com.huawei.xdevice.monkeytest/.MainActivity”这个关键字来查看剩余的启动时间。我们这里最后找到这条日志” I/ActivityManager( 3409): Displayed com.huawei.xdevice.monkeytest/.MainActivity: +453ms”，所以activity的启动时间就是453ms。
（2）通过命令行来启动activity并等待启动完成：adb shell am start –W –n package/.classname，这个命令会等待activity完全启动，并打印启动的总耗时。
总结：应用的启动时间其实就是am start的开始时间和displayed的完成时间，这两个时间之间的时间差值就是应用的启动时间了。如果要重复启动同一个activity，可以通过am force-stop package来实现强制关闭应用，这种方式比kill pid更好用。

  2、内存
   （1）通过ActivityManager.MemoryInfo类获得系统的内存信息，使用示例：
public long getAvailMemory(Activity app) {
        ActivityManager am = (ActivityManager)app.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.availMem >> 10;
    }
ActivityManager.MemoryInfo类中提供了一些属性包括availMem,lowMemory,threshold,totalMem等，可根据自己的场景来使用。上面是获取系统可用内存来计算应用启动前后占用的内存大小。
   （2）使用android提供的adb shell dumpsys meminfo | find “packagename” > D:\meminfo.txt 命令就可以获取对应包名占用的内存开销了。例如：16003 kB: com.huawei.xdevice.monkeytest (pid 25011 / activities) 这里就获取了对应应用的占用内存为16003KB。如果要获取某个进程的详细内存占用信息，通过adb shell dumpsys meminfo packagename or pid命令可以获取进程详细的内存占用信息
（3）使用android提供的procrank。（这种方式需要手机root）
    首先去google获取procrank、procmem、libpagemap.so三个文件。
然后push文件，执行 adb push procrank /system/xbin adb push procmem /system/xbin adb push libpagemap.so /system/lib
赋权 adb shell chmod 6755 /system/xbin/procrank adb shell chmod 6755 /system/xbin/procmem adb shell chmod 6755 /system/lib/libpagemap.so ,
再开启工具记录 adb shell procrank |grep packagename >/address/procrank.txt
剩下的就是整理测试数据了

  3、CPU
   （1）使用android提供的adb shell dumpsys cpuinfo |grep packagename >/address/cpu.txt来获取。
   （2）使用top命令adb shell top |grep packagename > /address/cpu.txt 来获取。

  4、GPU、帧率
   主要包括以下几个测试子项：
（1）界面过度绘制：主要通过人工进行测试，通过打开开发者选项中的”显示GPU过度绘制”来进行测试（只有android4.2以上才有）。
验收的标准：1、不允许出现黑色像素 2、不允许存在4x过度绘制 3、不允许存在面积超过屏幕1/4区域的3x过度绘制（淡红色区域）

   （2）屏幕滑动帧速率：
1.手机端需打开开发者选项中的 启用跟踪 后勾选 Graphics 和 View
2.启动 SDK 工具 Systrace 插件，勾选被测应用，点击 Systrace 插件，在弹出的对话框中设置持续抓取时间，在trace taps下面勾选 gfx 及 view 选项，
3.人滑动界面可以通过节拍来进行滑动或者扫动，帧率数据会保存到默认路径下，默认名称为 trace.html
4.将trace.html文件拷贝到linux系统下 通过命令进行转换，生成trace.csv文件
grep 'postFramebuffer' trace.html   | sed -e 's/.*]\W*//g' -e 's/:.*$//g' -e 's/\.//g' > trace.csv 
5.用excel打开文件计算得到帧率
   （3）屏幕滑动平滑度：
      方法如同帧率测试，唯一的差异就是最后的结果计算公式的差异

   （4）获取帧率fps：
       adb shell dumpsys SurfaceFlinger --list这个命令可以获取当前可视窗口列表，越下面的窗口越在上层，例如我要找当前窗口最上面显示的activity，这里应该是找com.tencent.mm/com.tencent.mm.ui.LauncherUI，也就是FocusedStackFrame下面的第一个activity。
adb shell dumpsys SurfaceFlinger --latency com.tencent.mm/com.tencent.mm.ui.LauncherUI 这个命令可以获取对应的activity刷新的时间戳和频率。第一行表示刷新的频率，后面还有128行时间戳，每一行时间戳代表刷新了一帧，所以可以通过获取第一帧的时间戳和最后一帧的时间戳，两者相减就得到刷新128帧的总用时，时间戳的单位是ns，总用时和总帧数都可以获取，那就可以计算出来帧率了。
 GPU的测试目前业界使用的均为硬件来进行，软件测试的数据相较硬件差异较大，对于帧率及帧方差的测试标准而言，需对待不同机型设定不同的标准

   5、功耗battery
从这几个方面入手测试：（1）测试手机安装目标apk前后，待机功耗无明显差异
                    （2）常见使用场景中能够正常进入待机，待机电流在正常范围之内                    （3）长时间连续使用应用无异常耗电的现象
测试方法：
第一种：采用市场上提供的第三方工具，如电池管家之类的。
第二种：自写工具进行。这里一般会使用3种方法
o	第一种基于android提供的PowerManager.WakeLock来进行，
o	第二种比较复杂一点，功耗的计算=CPU消耗+Wake lock消耗+数据传输消耗+GPS消耗+Wi-Fi连接消耗，
o	第三种通过 adb shell dumpsys battery来获取。由于手机连接usb的时候会充电，通过adb shell dumpsys battery set usb 0可以设置usb不充电，再次调用adb shell dumpsys battery 可以发现USB powered: false被设置成了false。adb shell dumpsys battery set usb 1可以设置usb继续充电，adb shell dumpsys battery 就可以看到USB powered: true设置成了true。 adb shell dumpsys battery reset可以将battery状态恢复到默认状态。

  6、流量Flow
    包括以下测试项：
	应用首次启动流量提示
	应用后台连续运行 2 小时的流量值
	应用高负荷运行的流量峰值（应用极限操作）
	应用中等负荷运行时的流量均值（应用正常操作）
流量测试一般都是用软件来进行的，主要分为两类，一类是第三方工具如流量宝之类的应用。二类是自研工具进行测试。
自研工具有以下方法：
1.	通过 tcodump 抓包，再通过 wireshake 直接读取包信息来获得流量
2.	首先获得被测应用的 uid 信息，可以通过 adb shell dumpsys package “package name”来获取对应包名的详细信息，其中有userId的信息，如userId=10149 gids=[3003, 1028, 1015, 3001, 3002]； 然后在未操作应用之前，我们可以通过查看 adb shell cat /proc/uid_stat/“userId”/tcp_rcv， adb shell cat /proc/uid_stat/“userId”/tcp_snd 获取到应用的起始的接收及发送的流量，然后我们再操作应用，再次通过上述 2 条命令可以获取到应用的结束的接收及发送的流量，通过相减及得到应用的整体流量消耗。
3.	adb shell cat /proc/”pid”/net/dev
这边的wlan0代表wifi 上传下载量标识! 上传下载量单位是字节,可以/1024换算成KB.这里可以看到下载的字节数46508913、数据包101283 和 发送的字节数2107448、数据包22080.这里的数据并不是对应pid进程产生的数据，而是手机内的总上行量和下行量，也就是全部进程一起产生的流量。
小技巧：wlan0这些值如何初始化0 很简单 你打开手机飞行模式再关掉就清0了
性能监控工具
![desc1](https://github.com/echo77/PerformanceTools/blob/master/screenshots/1.jpg)
![desc2](https://github.com/echo77/PerformanceTools/blob/master/screenshots/2.jpg)
![desc3](https://github.com/echo77/PerformanceTools/blob/master/screenshots/3.jpg)
![desc4](https://github.com/echo77/PerformanceTools/blob/master/screenshots/4.jpg)