# 在线内嵌字幕：实现、验证与未完成项

状态：V10 部分实现；不是“所有字幕/拖动场景已完成”。

## 已接入的路径

- GSY 12.1.0 的 `IjkExo2MediaPlayer.onCues(CueGroup)` 不处理字幕。`SubtitlePlayerManager` 用 Media3 renderer factory 的公开扩展点转发真实 `TextOutput`，保留 GSY 原来的扩展解码器优先级，不反射读取生产内核、不复制第三方播放器。
- 每个内核拥有一个 `GsyCueOutput`；原始/全屏视图共享一次订阅。准备前的最新 cue 可回放；换集、错误、完成、投屏接管和销毁清空显示，释放内核使已排队的回调失效。文本位置/样式及 bitmap cue 对象不被扁平化成字符串。
- 只枚举本媒体受支持的字幕轨道。有轨道时工具栏和全屏播放设置提供“关闭字幕 / 自动（媒体默认）/ 具体轨道”；没有轨道不显示入口。不把某一媒体的 track index 应用到另一媒体。
- `SubtitleView` 位于视频纹理上方、控制层下方；跟随系统字幕样式和字体缩放。普通播放预留进度条区域，PiP 使用按视口缩放的字幕并隐藏应用内控制条，系统 PiP 播放/暂停操作仍可用。
- 修复过一个真实渲染问题：在父视图 `onSizeChanged` 后设置 SubtitleView 的底部 padding，会裁剪其已经测量好的内部 Canvas。现改用底部比例定位。验证同时检查实际绘制像素与屏幕截图，不只检查 cue 列表/无障碍文本。
- PiP 的窗口尺寸变化不再触发 GSY 自动创建全屏克隆；进入小窗后远程暂停的迟到 UI 更新也不重新显示应用内控制条。

## 常规验证

在可丢弃 API 31+ 模拟器上执行 `scripts/verify-subtitles.sh emulator-5580`。所有 adb 命令必须明确序列号，不连接个人数据设备执行此脚本。

合成 MKV 包含真实 SubRip 英/法两轨；服务仅监听 127.0.0.1，不请求第三方视频。3 项原生回归分别覆盖：

1. 已缓冲字幕：暂停在 2 秒切换英→法，位置/暂停不变，全屏、关闭、后台返回、重新启用、小窗及系统暂停，检查真实字幕绘制像素。
2. 英文时间段：2 秒显示、10 秒空白间隙清空、22 秒显示结尾，拖动不误恢复播放。
3. 换到无字幕集：旧字幕清空、工具栏和全屏不留无效入口。

`verify-native-ci.sh` 包含这 3 项回归。真正 R8 Release 的 platform-only runner 另外检查字幕像素、关闭/自动重新启用，返回 `RELEASE_SUBTITLES_OK`；不链接 Debug 播放器实现来代替 Release 验证。

## 明确未解决：MKV 跨区间拖动后的长字幕预读

**常规 3 项通过不覆盖、也不代表下面的失败已修复。**

复现：同一 MKV 先从 2 秒拖到 10 秒、22 秒，再回 2 秒选择法语。法语长 cue 的编码包只位于 0.1 秒（延续至 29.9 秒）。重新加载从后面的 MKV 视频 cluster 开始，Media3 1.10.0 的法语 SampleQueue `write=0/read=0`，TextRenderer 收到 EOS；轨道选中成功但没有 cue。英文前段也不在该次重新加载的队列里。不是 SubtitleView 或线程转发丢掉了已收到的法语文本。

保留完整失败断言作为显式诊断入口（先运行常规脚本安装 Debug/test APK）：

```sh
adb -s emulator-5580 shell am instrument -w -r \
  -e class 'com.app.chao.chaoapp.playback.OnlineSubtitleExperienceTest#trackSelectionFullscreenPauseAndPipUseActualDecoder' \
  -e probe_mkv_preroll true \
  com.app.chao.chaoapp.test/androidx.test.runner.AndroidJUnitRunner
```

当前预期结果是法语 cue 断言失败；查看 instrumentation 的 `FAILURES`，不能以 adb 进程退出码 0 当作通过。未使用播放/暂停切换、假字幕或固定字幕文本蒙混断言。普通已缓冲轨道切换与跨区间重新加载是分开记录的验收场景。

下一步应评估保留网络/缓存语义的按轨道 seek/preroll 方案，并增加真实封装/大文件数据。不要为了短样本通过给所有视频盲目回到 0 秒重解码，也不要通过无限 back buffer 掩盖问题；这会造成大文件的网络、内存和解码成本。

## 其余未完成

- 明确字幕选择在同媒体失败重试/内核重建后的恢复；当前只保留同一内核内的全屏与前后台状态，不承诺跨内核或跨媒体轨道记忆。
- 外置字幕导入、时间偏移与文件供应商权限处理。
- 真实 HLS/DASH 字幕轨道、图片字幕渲染、重叠长 cue、多语言/大字号/OEM/电视专项。JVM 保留 bitmap cue 的测试不等于图片字幕的真机解码验收。
- API 33/x86_64 云端工作流、真实设备专项需要独立执行，不能用本地 API 36/arm64 代替。
