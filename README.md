jocean-android
============

jocean's android 工具库

2014-08-19:  release 0.0.5 版本：
  1、基于jocean-event-api-0.0.5-SNAPSHOT 中提供的 内嵌BizStep派生类/实例，自动注册OnEvent/OnDelayed 注解的方法作为事件/延迟事件 作为事件处理方法 的特性，改造 BitmapAgent 模块的流程可读性

2014-06-11:  release 0.0.4 版本：
  1、实现彩色的圆形进度条 ShowProgress2Flow
  2、http://rdassist.widget-inc.com:65480/browse/CHANNEL-143: 在SIVLoader.startLoadBitmap方法处理中，new URI之前，检查参数 String url是否为null，若为null，则直接返回null transaction.

2014-05-22： release 0.0.3 版本：
  1、 添加BitmapAgent.loadFromCacheOnly方法，对于指定的URI只尝试从内存或disk cache获取CompositeBitmap
  2、将 rose-main 中的公共工具类ImageViewable & ImageLoader 更名为 SIVHolder & SIVLoader并迁移到 jocean-android 模块中的 org.jocean.android.view 子包中
  3、依赖 jocean-rosa-0.0.6，适应BlobAgent接口更改
  4、修改CompositeBitmap中的位图绘制方式，从原来的固定长宽位图分片，一个实际图片内容由多个位图分片多次绘制显示，改进为使用原生Bitmap，只在ROM版本>=3.0.x(SDK Version 11)时，尝试进行已回收的位图重用（匹配 宽、高和Bitmap.Config）。原有的CompositeBitmap更名为 MultiBitmap，保留
  5、fix bug: NPE，在回调BitmapReactor之前，先检查还否为nul
  6、将 CompositeBitmapDrawable 更改为从 BitmapDrawable 派生
  7、捕获 inBitmap 选项使得BitmapFactory.decodeXXX 失败的情况，重新再次解码
  8、设置 SIVLoader 的 BitmapAgent 的单次交互的最大重试次数 为1
