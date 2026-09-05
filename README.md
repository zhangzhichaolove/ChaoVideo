# ChaoVideo

开发中的整改目标与逐项验收：[整改清单](REMEDIATION.md)。

Android 网络视频播放器，支持影片列表、连续剧选集、本地播放、画中画和 DLNA 投屏。

## 当前能力

- 推荐、分类、搜索、分页加载、空状态、失败提示和重试。
- 视频封面和相对媒体地址解析，ExoPlayer 内核播放及失败重试。
- 连续剧选集、自动播放下一集、全屏选集、断点续播；在线和离线共享持久倍速及适应/裁剪/拉伸画面比例，全屏可直接打开播放设置。
- 在线媒体有多个受支持音轨时提供音轨切换；在线 GSY 已接入真实内嵌字幕、关闭/自动/轨道选择及全屏/PiP 显示。离线播放器支持内嵌字幕与音轨选择，无字幕时隐藏入口。MKV 跨区间拖动长字幕、跨内核选择恢复及外置字幕导入仍未完成，详见 [在线字幕验证与限制](docs/ONLINE_SUBTITLES.md)。
- 收藏、观看历史、搜索历史和画中画。Media3 下载中心支持直链、HLS/DASH 点播的持久离线缓存、进度、暂停、继续、失败原因、重试和删除。
- 下载完成后使用仅缓存播放器，不回退联网；系统文件选择器可打开本地视频，尝试保留 Uri 读取授权，不申请全盘存储权限。
- 局域网 DLNA/UPnP 设备发现、投屏、暂停/继续、快进/后退、音量、换设备和主动停止。
- 离开播放页不会停止电视端播放；下次发起投屏时会替换并停止上一次投屏。
- 移除固定 3 秒启动等待。首页按钮可修改、恢复及测试 API 地址（实际列表端点、HTTP/业务状态和数据结构）；`home_fab3` 用于停止当前投屏。
- API 诊断默认关闭，仅 Debug 手动开启且只在当前进程有效；有体积/保存时限、常见凭据脱敏和分享前确认，Release 禁止采集。详见下方隐私诊断说明。

## 构建要求

- JDK 17
- Android SDK 37
- Android Gradle Plugin 9.4.0
- Gradle 9.7.1（使用仓库内 Wrapper）
- 最低 Android 6.0（API 23）

完整验证命令：

```bash
./gradlew clean assembleDebug assembleRelease lintDebug testDebugUnitTest
```

产物：

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release-unsigned.apk`

## API 约定

默认地址定义在 `VideoApis.HOST`，运行时可由用户修改。当前客户端只依赖：

```text
GET video/getVideoBanner
GET video/getVideoList?page=...
GET video/getVideoList?page=...&search=...
GET video/getVideoList?page=...&classes=...
```

连续剧优先读取有序 `episodeUrls` 数组，每集直接使用明确地址；未返回该字段时兼容 `episodes` + `video` 文件名规则。完整定义及服务端集序稳定要求见[分集地址接口契约](docs/EPISODE_API.md)。相对封面和视频地址按发起该请求的 API 基址解析，即使请求途中切换设置或后来从收藏打开也不重定向到新来源。为兼容用户配置的局域网 HTTP 媒体服务，应用明确保留 HTTP 支持；生产环境建议使用 HTTPS。

## 外部依赖与发布说明

- 当前投屏采用不需要外部密钥的 DLNA/UPnP。Google Cast SDK 未接入，因为正式接入必须由发布方提供并配置 Cast Receiver App ID；仓库没有伪造该值。
- 服务端尚未定义评论接口和字段，播放页隐藏空评论入口，不臆造评论数据。
- 默认 Release 为未签名 APK；已支持环境注入版本/签名、实际证书与 R8 mapping 核验及不可覆盖归档。正式凭据由发布方提供，详见[发布与 CI 流程](docs/RELEASE.md)。
- 未接入崩溃监控或统计 SDK，也没有填写监控密钥。后续接入需要明确选型、隐私策略和有效密钥。
- DLNA 的设备发现与控制需要同一局域网内的真实电视或投屏接收器，模拟器只能验证本地播放、界面和生命周期，不能替代真实设备互操作测试。

## 离线下载与设备回归

- 下载前识别容器，并由 Media3 检查点播清单和可用轨道。HLS/DASH 下载媒体分段及选中轨道，不是把清单改名为 mp4。
- 离线内容位于应用私有目录，与在线播放器的可淘汰缓存隔离；删除任务会删除缓存，卸载应用会删除全部离线内容。下载时要求网络可用且设备存储不低；应用后台由前台下载服务处理。
- 直播、需要单独离线 DRM 许可证的内容不在当前下载能力范围内；过期地址可失败并提示重试，客户端不生成服务端许可证或续签 URL。
- `scripts/generate-offline-fixtures.sh` 使用 ffmpeg 生成 4 秒测试图与正弦音，仓库内 MP4/HLS/DASH 样本不依赖外部影片。
- 在 **专用可丢弃 API 29+ 模拟器**执行 `scripts/verify-offline.sh emulator-5580`。脚本仅接受 `emulator-*`，用例会删除应用内原有下载任务、创建/删除自己的本地样本并产生测试观看记录，不用于个人数据设备。
- 原生回归使用实际下载服务完成三个格式的暂停→503 失败→重试→完成；关闭样本 HTTP 服务后，经实际播放器解码视频帧并播完；随后验证任务删除。另外验证 `content://` 本地视频解码（无全盘存储权限）。日志和截图保存在 `app/build/verification/offline/`。
- 系统文件选择器的不同供应商、授权被撤回、长视频/空间耗尽、后台限制、无外网 NAS、电视互操作等仍须设备专项验收。

## 播放设置与混淆版回归

- 倍速范围 0.25–2 倍，默认 1 倍；默认保留原始比例。设置仅影响手机本地内核，不声称电视支持相同设置。音轨选择依据实际解码能力，不沿用其他影片的轨道索引。
- 离线页面重建/后台返回保留暂停意图、位置及当前媒体的字幕/音轨选择；暂停状态不自动进入 PiP，已进入 PiP 时系统动作可播放/暂停。
- `scripts/generate-playback-fixtures.sh` 生成 30 秒测试图、两个正弦音轨和内嵌字幕，无外部影片。API 31+ 专用模拟器运行 `scripts/verify-playback.sh emulator-5580`，覆盖明确分集地址/连续全屏选集/自动下一集、设置/PiP/字幕及原下载回归，共 8 项原生测试。
- 配置 Android SDK 的 `ANDROID_HOME`，运行 `scripts/verify-playback-release.sh emulator-5580`。脚本用公开 Debug 密钥签名混淆 Release 的副本，使用仅依赖 Android 平台的测试入口，验证 API 泛型反序列化、明确相对分集 URL、真实解码帧/时长/进度、GSY 全屏反射和倍速；退出时恢复 Debug/标准测试 APK 及所改偏好，关闭测试服务和端口映射。会留下合成影片测试历史，仅用于可丢弃模拟器。
- Release 保留必要的 Gson 泛型模型和 GSY 反射构造器；脚本生成的测试签名 APK 不用于正式发布。日志/截图分别位于 `app/build/verification/playback/` 与 `app/build/verification/playback-release/`。

## 数据源身份与迁移

- 来源使用规范化完整 API 基址（协议/主机/端口/路径）的 SHA-256 命名空间，影片使用服务端 ID；媒体 URL 是可变属性。同源同 ID 的签名 URL 更新仍是同一收藏，不同来源同 ID 则独立记录。
- 无服务端 ID 时退回源内完整媒体 URL；URL 变化后的稳定身份需要服务端提供固定 ID。修改 API 基址会建立另一来源，客户端不自动认定两个 API 是同一个库。
- Room v3 的来源升级与 v4 的明确分集列升级不修改旧收藏、原主键、观看历史和每集进度。旧版本未记录来源的数据标记为“旧记录（来源未知）”，不按 CDN 地址或当前 API 猜测归属。新源不继承匿名 URL 偏好进度；旧记录仍可使用已保存地址。
- 收藏/历史展示来源，可长按查看完整来源并操作收藏。本地文件、离线下载与 API 来源分开，服务端 JSON 不可写入客户端来源/持久键。
- `scripts/verify-sources.sh emulator-5580` 在专用可丢弃模拟器验证真实 Retrofit 迟到响应、同 ID 双收藏、切换 B 后从 A 收藏实际续播、B 进度不变，保留证据于 `app/build/verification/sources/`。与 8 项播放/下载、3 项在线字幕和 2 项诊断回归一起构成 **14 项当前常规原生用例**，另有 **97 项 JVM 测试**及混淆 Release 黑盒验证（含字幕像素及关闭/启用）；已知 MKV 长字幕 seek 失败场景另行保留诊断入口，不计作通过；设备与正式发布专项仍独立验收。

## 隐私友好的 API 诊断

- 诊断采集**默认关闭，仅 Debug 可手动开启，且只在当前进程有效**；重启进程自动关闭，旋转页面不重新授权。Release 入口可查看已有脱敏缓存，但不允许开启采集，不自动上传。
- 请求仅记录方法、无用户信息/路径/查询参数/片段的 origin、请求编号、收到响应头的耗时，以及 HTTP 状态和已知正文大小；不记录请求头、请求正文或网络异常消息，不把响应正文/URL 打印到 logcat。
- 最多旁读 1 MiB 正文（额外 1 字节用于判断超限），递归过滤常见 token、密码、Cookie、签名、个人信息字段；URL 只保留 origin，过滤 Bearer/Basic、JWT、嵌入的序列化 JSON 和常见本地路径。非 JSON、损坏、过深/过于复杂或超限正文只保存省略说明。
- 这是**有限规则脱敏，不是匿名化保证**；自定义字段、媒体名称或主机名仍可能包含私人信息。复制正文/分享均先显示检查提示，剪贴板在支持的系统标记为敏感。预览与正文复制上限 65,536 字符，较大内容提示通过文件分享。
- 原始响应体不被消费或替换，清空/淘汰诊断文件不影响 Retrofit/OkHttp 读取完整原始响应；诊断存储失败不使成功请求失败。关闭采集或清空会使先前在途采集票据失效，迟到响应不会重新生成已清空日志。
- 脱敏记录位于私有 `cache/diagnostics-v1`；分享前再次脱敏，并在 `cache/diagnostic-exports` 创建独立快照。FileProvider 仅暴露导出目录，存储目录和旧 `api-response-logs` 原文目录不再提供 URI；升级后启动/访问时后台删除旧原文文件。
- 两个目录各限 20 个文件/20 MiB，单条记录/导出各限 2 MiB，24 小时过期；启动及下一次存储访问时清理，不在应用停用时运行定时删除。读取/导出也检查过期，停留页面的旧选择不会续存过期日志。“清空”删除当前记录与本应用的导出快照，已由接收方另存的副本不受影响。
- 页面文件列表/读取/格式化/导出在后台执行，诊断页独立适配深浅色动作文字对比度和系统栏避让。复制确认期间退到后台再返回，仍使用用户点选时的脱敏快照，避免刷新选择引起崩溃。
- `scripts/verify-diagnostics.sh emulator-5580` 用真实 loopback HTTP、原生页面、剪贴板和 FileProvider 验证默认关闭、手动采集、原始字节完整、大响应省略、清空后迟到响应、复制/分享前确认及重建。分享 chooser 被测试拦截，**没有实际向其他应用或服务器发送诊断**。脚本仅接受可丢弃 API 33+ 模拟器，清空诊断缓存并恢复剪贴板，不清空媒体库；日志和深浅色截图位于 `app/build/verification/diagnostics/`。
- 真正混淆 Release 黑盒另外验证采集开关关闭且不可启用、旧原文清理、旧 FileProvider root 拒绝读取；返回 `RELEASE_DIAGNOSTIC_PRIVACY_OK`。这是测试证书签署的 APK 副本，不是正式发布签名。

## 发布与 CI

- `scripts/package-release.py` 从环境读取完整发布版本/签名配置；构建、单测、Debug/Release Lint 后验 APK 内部版本、非 debuggable、期望证书和 R8 mapping 正文/DEX 身份，归档 APK、mapping、Git/版本清单及 SHA-256，不自动上传或发版。
- `python3 -m unittest discover -s scripts/tests -v` 运行 8 项打包策略测试；`scripts/verify-release-config.py` 用一次性测试证书验证真正 Gradle 签名/版本注入及失败路径，结束恢复默认构建产物。测试归档标为 TEST-ONLY，不能作为正式发布凭据。
- `scripts/verify-native-ci.sh emulator-5580` 汇总本项目全部常规原生与混淆 Release 用例，仅用于可丢弃 API 33+ 模拟器。GitHub 工作流增加 API 33/36 原生矩阵及独立的手动签名归档入口；远端执行和正式发布环境尚待仓库持有方配置、验收。
- 密钥输入、版本约束、长期映射保存、Actions environment/Secret 说明及命令见[发布文档](docs/RELEASE.md)。

## 在线字幕验证（V10 部分完成）

- `scripts/verify-subtitles.sh emulator-5580` 生成英/法双轨合成 MKV，执行 3 项真实解码/绘制/切换/暂停/全屏/PiP/无字幕集用例；检查实际 Canvas 像素并留存截图。
- 新的字幕/小窗代码经 Debug/Release 构建、双 Lint、97 项 JVM、完整 14 项常规原生及真正混淆 Release 验证。Release 增加 `RELEASE_SUBTITLES_OK`，不依赖 Debug 类判断结果。
- **未解决**：跨视频 cluster 回拖后，起始于更早位置的长 MKV 字幕包可能未被 Media3 重新读取。保留会失败的 `probe_mkv_preroll` 诊断入口；常规绿色回归不表示此缺陷已修复。具体复现、证据与下一步见 [ONLINE_SUBTITLES.md](docs/ONLINE_SUBTITLES.md)。
