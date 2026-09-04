# ChaoVideo

Android 网络视频播放器，支持影片列表、连续剧选集、本地播放、画中画和 DLNA 投屏。

## 当前能力

- 推荐、分类、搜索、分页加载、空状态、失败提示和重试。
- 视频封面和相对媒体地址解析，ExoPlayer 内核播放及失败重试。
- 连续剧选集、自动播放下一集、全屏选集、倍速和断点续播。
- 收藏、观看历史、搜索历史、视频下载和画中画。
- 局域网 DLNA/UPnP 设备发现、投屏、暂停/继续、快进/后退、音量、换设备和主动停止。
- 离开播放页不会停止电视端播放；下次发起投屏时会替换并停止上一次投屏。
- 首页按钮可修改、恢复及测试 API 地址；`home_fab3` 用于停止当前投屏。
- Debug 构建会打印格式化 JSON；超长响应完整保存在应用缓存，可通过长按 API 地址按钮查看、复制或分享。

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

连续剧由 `episodes > 0` 判定，集数地址根据已有 `video` 字段生成。相对封面和视频地址按当前 API 地址解析。为兼容用户配置的局域网 HTTP 媒体服务，应用明确保留 HTTP 支持；生产环境建议使用 HTTPS。

## 外部依赖与发布说明

- 当前投屏采用不需要外部密钥的 DLNA/UPnP。Google Cast SDK 未接入，因为正式接入必须由发布方提供并配置 Cast Receiver App ID；仓库没有伪造该值。
- 评论页显示明确空状态；服务端尚未定义评论接口和字段，因此客户端没有臆造评论数据。
- Release 构建目前是未签名 APK；正式发布签名及密钥不存放在仓库中，需要发布方提供。
- 未接入崩溃监控或统计 SDK，也没有填写监控密钥。后续接入需要明确选型、隐私策略和有效密钥。
- DLNA 的设备发现与控制需要同一局域网内的真实电视或投屏接收器，模拟器只能验证本地播放、界面和生命周期，不能替代真实设备互操作测试。
