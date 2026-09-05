# ChaoVideo 整改目标

状态：开发中。对应 2026-09-05 确认的双项目整改清单，不代表全部完成。

## 执行顺序与验收方式

1. 先修搜索/分页、进度、投屏归属、下载的正确性；每项补回归验证。
2. 再改善导航、启动、主题、设置和播放页操作。
3. 补齐数据身份、测试/发布流程，最后扩展离线播放、字幕/音轨与服务端契约。
4. DLNA、PiP、全屏、弱网等必须另做真机验证，不以单测通过代替设备互操作验证。

## P0：首批已实现，设备回归待执行

- [x] V01 搜索/分类请求绑定请求版本与页码，取消旧请求，成功后才推进页码；失败可重试原页。
- [x] V01 推荐列表忽略旧响应；Banner 失败不再回退内容页码。空接口结果结束加载。
- [x] V02 Room 新增每部影片/每集独立进度表；v1→v2 迁移保留收藏与历史，兼容旧偏好设置进度。
- [x] V02 清空历史同时清理 Room 和旧续播偏好；从头播放/播放完成存储零位置。
- [x] V02 播放中每 5 秒节流保存；切集、离开页面保存；PiP 可见期间继续保存。
- [x] V03 恢复投屏同时验证媒体地址及设备当前状态；A 影片投屏不接管 B 的播放/历史。
- [x] V03 忽略旧媒体/旧设备状态回调，暂停/销毁时取消延迟重试；设备未提供媒体身份时不记录未经核验的电视进度。
- [ ] 真机：搜索 A→B 乱序网络、第二页失败重试、进程回收/画中画进度、电视 A + 手机 B、设备离线恢复。

## P0：下载正确性已实现

- [x] V04 Media3 持久下载任务/独立缓存，识别直链/HLS/DASH；提供进度、原因、暂停/继续、删除和重试。离线播放器禁用 HTTP 回退。
- [x] V04 使用真实媒体分段下载；合成 MP4/HLS/DASH 三格式通过暂停→503→重试→完成，关闭 HTTP 样本服务后原生解码并播完；验证删除任务。

## P1：体验与播放

- [ ] V05 已移除固定 3 秒启动等待及由此闲置的第三方启动图；语义图标和首页调试入口归入设置仍待整改。
- [ ] V06 全页面深浅色、横屏/大字体/小屏和触控区域检查。
- [x] V07 测试实际 `video/getVideoList?page=1`，校验 HTTP/业务状态和 records/video 结构；有超时/响应上限、对话框关闭取消和地址变化结果保护。
- [ ] V08 在线/离线已共享持久倍速和适应/裁剪/拉伸比例；全屏提供播放设置，暂停不误触发自动 PiP。离线重建保留暂停、位置和本媒体轨道选择。已验证准备期后台、暂停后返回、PiP 播放/暂停和全屏倍速；全屏 1→3→1→2 连续切换、重新打开第 2 集续播及解码结束后自动下一集已在 API 36 验证；真实硬件专项仍待回归。

## P1：工程

- [ ] C01 下载职责已拆到 VideoDownloads/VideoDownloadService，离线播放独立于 GSY；播放设置拆为 VideoPlaybackSettings/GsyPlaybackControls。剩余投屏会话和进度协调继续分离。
- [x] C02 API 基址命名空间 + 服务端媒体 ID，隔离同 ID/同 CDN 的收藏和分集进度；迟到响应和相对 URL 固定到发起请求的数据源。旧收藏标记 legacy 并保留原键，不猜测来源或自动合并。收藏/历史卡片显示来源。缺少 ID 时使用源内 URL 回退，无法保证 URL 变化后的身份延续。
- [x] C03 导出 v1/v2/v3/v4 schema；v2→v3 增加来源、v3→v4 增加明确分集列表，不改写旧键；新增数据库升级保留收藏/进度的测试；进度与历史写入使用事务。
- [ ] C04 CI 已加入构建/单测/发布配置回归、API 33/36 原生矩阵及完整本地 CI 入口；远端工作流尚未执行，真实设备专项独立验收。
- [ ] C05 已实现 Debug 进程内显式开启、有界脱敏、分享确认、旧原文清理和独立 FileProvider 导出，Release 禁止采集。环境签名/版本注入、APK 证书/版本/R8 映射核验与归档已实现；按数据源 HTTP 信任及真实发布凭据/远端流程验收仍待完成。

## P2：功能与外部契约

- [x] V09 下载中心、系统文件选择器入口、持久 Uri 授权（供应商拒绝时提示临时权限）和缓存/本地播放器；`content://` 原生播放通过。不同文件供应商和撤权仍需专项验收。
- [ ] V10 在线音轨切换及 GSY 真实字幕输出已接入；有字幕时提供关闭/自动/轨道选择，原始/全屏/PiP 共享显示，无轨道隐藏入口。离线内嵌字幕/音轨已有 Media3 控件。**MKV 跨区间拖动后的长字幕预读、跨内核选择恢复、外置字幕导入尚未完成**；失败复现单独记录于 docs/ONLINE_SUBTITLES.md，不以常规绿色回归代替这些验收。
- [x] V11 定义并支持可选 episodeUrls 有序地址数组，覆盖旧集数/文件名推导；缺省/null 保留旧契约，明确空数组不推导分集，非法字段走请求失败。Parcelable/Room v4 保存列表，选集/全屏/自动下一集/续播使用实际 URL。服务端集序需稳定且只追加，不声称生产服务已接入新字段；详见 docs/EPISODE_API.md。
- [x] V12 评论接口未接入时隐藏空评论 Tab，保留简介及有剧集时的选集，不伪造评论数据。

## 首批验证

- 新增请求乱序/失败页重试状态测试、DLNA 媒体归属测试。
- 新增 Robolectric + Room：分集进度隔离、零位置/完成处理、清空历史保留收藏且不恢复旧进度、真实 v1 schema 升级至 v2。
- 已运行 `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease`：26 项测试通过，Debug/Release 构建通过。
- 电视、PiP、弱网设备测试未执行；未覆盖安装到用户手机。
- Release 仍为未签名产物，正式发布凭据由发布方配置。

## 第二批验证（2026-09-05）

- `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest`：37 项 JVM 测试通过，Debug/Release/测试 APK 构建通过，Lint 无问题。
- 专用 API 36 模拟器：2 项原生测试通过（MP4/HLS/DASH 下载故障/离线解码/删除 + content Uri 本地播放）。播放器实际输出视频帧，时长至少 3.5 秒并播放完成，不以“文件存在”代替可播验收。
- 已检查下载完成列表、离线播放画面的截图，并修正页面标题与新页面工具栏对比度。证据位于 `app/build/verification/offline/`。
- 复跑脚本 `scripts/verify-offline.sh`，合成素材生成脚本 `scripts/generate-offline-fixtures.sh`；测试仅用于可丢弃模拟器。
- Release 仍未签名；CI 仅已配置，未执行远端工作流。电视、长时间后台下载、空间不足、弱网和不同文件供应商仍未完成设备验收。

## 第三批验证（2026-09-05）

- `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest`：51 项 JVM 测试通过，Debug/Release/测试 APK 构建通过，Lint 无问题。
- API 36 专用模拟器：7 项原生测试通过。新增 5 项覆盖真实 GSY HTTP 播放、多音轨切换、全屏倍速、两个播放器间偏好共享、离线重建后暂停/字幕设置、字幕真实输出/无轨道时隐藏、准备期后台及 PiP 操作；原 2 项下载/本地播放测试同时复跑。
- 原生测试发现并修复：离线 `onSaveInstanceState` 在 `onStop` 已释放内核之后遗漏状态，导致重建误续播；暂停视频也启用自动 PiP；离线媒体标题被 ActionBar 标题缓存覆盖。PiP 完全不可见时暂停本地内核。
- 多次回归进一步发现 GSY 默认暂停依赖 `isPlaying()`，Exo 在切音轨缓冲期间返回 false 时会丢失暂停。`PlaybackVideoPlayer` 保留暂停意图并拦住迟到的缓冲状态回写；5 项确定性测试覆盖缓冲暂停/继续/新片重置，原生全屏暂停路径另外连续复跑 3 次。
- 实际安装混淆 Release 后发现并修复三类问题：`VideoHttpResponse` 被裁剪、`PageInfo<T>` 的反射泛型丢失导致列表出现 `LinkedTreeMap`、GSY 内核/缓存工厂反射构造器被裁剪；同时修复空异常消息再次触发错误处理崩溃。不是仅凭 Debug 构建推断 Release 可用。
- Release 平台独立回归返回 `RELEASE_PLAYBACK_OK`：显示真实本地 API JSON，原生解码 30 秒合成视频、播放进度推进、TextureView 输出多色帧，全屏反射实例化成功并能恢复/改变倍速。仅测试签名一份 APK 副本，正式 Release 仍未签名。
- 复跑：`scripts/verify-playback.sh emulator-5580`（API 31+）及配置 `ANDROID_HOME` 后执行 `scripts/verify-playback-release.sh emulator-5580`。后者结束时关闭本地 HTTP 服务/adb reverse，恢复 Debug 应用、标准测试 APK 及所改偏好文件；仅限可丢弃模拟器，不用于个人数据设备。
- 合成多轨素材由 `scripts/generate-playback-fixtures.sh` 生成；日志/截图位于 `app/build/verification/playback/`、`app/build/verification/playback-release/`。已检查字幕、全屏和 Release 视频帧截图。
- 未完成项继续保持开放：源命名空间、明确剧集地址契约、GSY 在线字幕、全页面主题/布局、正式签名/隐私发布、远端 CI 与电视/真实设备互操作。

## 第四批验证（2026-09-05）：来源隔离

- API 来源是规范化后的完整 API 基址（含协议、端口、路径），并非媒体/CDN 域名。Retrofit 实例捕获来源，A 请求在切到 B 后到达仍属于 A；相同源/ID 的播放签名 URL 变化不创建另一收藏。客户端来源及已存键不从服务端 JSON 反序列化，API 的 download: ID 不冒充私有下载。
- Room v3 保留旧行/主键及每集进度，来源未知的旧记录独立保留为 legacy。实际 API 36 模拟器 v2→v3 升级前后逐列核对：73 条旧影片、73 条旧进度完全保留，来源字段为 legacy/null，证据为 `app/build/verification/sources/native-migration.txt`。
- 69 项 JVM 测试、Debug/Release/AndroidTest 构建通过，Lint 无问题；原 7 项下载/播放原生回归全部复跑通过，新增 1 项真实 Retrofit 来源切换测试：同 ID 双收藏、A 续播不少于 11000ms、B 进度保持 23000ms，实际只请求 A 媒体。
- 检查收藏来源截图后修正网格卡片 wrap_content 导致来源文字高度截断；新增原生断言核对来源末行未省略且完整位于文字区域内。当前截图能区分端口和 /a/、/b/ 路径，工具栏整体对比度仍归 V06。
- 混淆 Release 样本改为相对媒体 URL，再次获得 RELEASE_PLAYBACK_OK，确认来源包装及相对解析在实际 R8 产物中工作。测试结束已恢复 Debug/标准测试入口、偏好，移除测试端口映射并停止样本服务。
- 来源复跑脚本：`scripts/verify-sources.sh emulator-5580`。日志/截图位于 `app/build/verification/sources/`，使用实际合成视频，不访问外部影片。仅用于可丢弃模拟器，产生独立测试收藏/历史并保留旧数据。
- 限制：修改 API 基址视为另一来源；旧记录不推断归属；无 ID 的 URL 回退不承诺跨 URL 变化合并；正式发布/隐私、远端 CI、真实电视与硬件验收继续开放。

## 第五批验证（2026-09-05）：明确分集地址

- 新增可选 `episodeUrls` 有序字符串数组：非空列表优先于旧集数与推导路径，支持源内相对 URL 与绝对 HTTP(S) URL；缺省/null 兼容旧接口，明确空数组不推导分集。严格拒绝非字符串、空项和非 HTTP(S) 地址，避免 Gson 把数字变成相对路径或过滤空项后串集。
- Parcelable 与 Room v4 保留明确列表和来源；v3→v4 只增加可空列。JVM 验证 v1/v2/v3 迁移链、收藏元数据更新、同源 ID 签名更新后每集进度不变。原生升级前后逐列核对 91 条影片和 91 条进度，所有原有字段保持一致，新列为 null；证据为 `app/build/verification/playback/native-episode-migration.txt`。
- 新增真实 API→GSY 场景：不规则路径及查询参数原样用于请求；全屏 1→3→1→2 连续切集，倍速保留；第 1 集位置不少于 11500ms、第 2 集不少于 22500ms 分别保存。从收藏数据重新打开第 2 集续播，解码自然结束后自动播放第 3 集。没有请求推导文件或切换后的 API 路径。已查看全屏/自动下一集截图。
- 修正旧文件名推导只修改路径，不修改查询参数/片段；路径绑定签名仍需服务端提供明确 URL，客户端不伪造签名。接口约定及集序只追加/保持不变的要求记录在 `docs/EPISODE_API.md`，尚未声称生产服务返回了新字段。
- 最新全量验证：80 项 JVM 测试、8 项播放/下载原生用例和 1 项来源原生用例全部通过，Debug/Release/AndroidTest 构建通过，Lint 无问题。明确分集场景随全套播放回归复跑两次通过。
- 实际混淆 Release 样本把旧 video 指向不存在的文件，明确列表只提供相对 `multi-track.mkv`，仍返回 RELEASE_PLAYBACK_OK：Gson 字段适配器、来源绑定、原生解码帧/30 秒时长/进度、全屏反射与倍速均通过，服务器日志未请求旧推导地址。正式 Release 仍未签名；测试恢复 Debug/标准测试入口及偏好，无遗留端口映射。
- 保持双项目总目标进行中：音乐稳定来源身份、主题/导航、批量与本地分类、在线字幕、发布隐私、远端 CI 和真实硬件专项仍待实施/验收，不以本批通过替代。

## 测试隔离补充（2026-09-05）

- 双项目来源原生用例的 teardown 改为同步保存原 API，避免 apply() 在 runner 退出前尚未落盘。来源复跑脚本另保存 API 偏好快照，以 EXIT trap 在成功/失败时恢复原文件或原缺省状态并停止测试进程，不清除收藏/观看记录。
- 本次未修改视频生产逻辑，80 项 JVM 与来源原生用例复跑通过；播放/下载及混淆 Release 最近结果仍见第五批。残留的已确认测试 loopback 地址已清理，未操作用户手机。

## 第六批验证（2026-09-05）：诊断隐私与发布版验证

- API 日志从默认输出原文改为 Debug 显式开启、仅本进程有效，Release 禁止采集；请求头/正文、URL 路径/参数和异常消息不写入诊断或 logcat。最多旁读 1 MiB + 1 字节，原始 ResponseBody 保持完整且不由诊断文件托管，清空/淘汰日志及磁盘写入失败不破坏业务响应。
- 严格 JSON 流式脱敏覆盖常见凭据、签名、个人信息字段及 URL、Bearer/Basic、JWT、嵌入 JSON/常见本地路径；非 JSON、畸形、深度/节点数或体积超限保存省略说明。明确告知“有限规则脱敏，不保证任意自定义字段匿名”；复制正文/分享前确认，分享 chooser 仅接收二次脱敏的独立快照。
- cache/diagnostics-v1 与 diagnostic-exports 各限 20 个文件/20 MiB、单条/单导出 2 MiB、保留 24 小时；启动/下次访问清理，读取/导出拒绝已过期选择，不依赖后台定时器。后台删除旧 api-response-logs 原文，FileProvider 仅暴露导出目录。关闭/清空使旧采集票据失效，迟到响应不重新生成日志。
- 文件操作/格式化移至页面后台线程；补齐深浅色诊断动作对比度、系统栏避让。修复确认复制时后台返回触发列表刷新、使 content 变空而崩溃的问题，改用确认前的脱敏快照。Release 空状态明确禁用采集，不再提示点击禁用的开关。
- 新增 12 项 JVM 回归（4 脱敏、8 存储/响应完整/限制/迁移/在途失效/过期读取），累计 **92 项通过，零失败/错误/跳过**；Debug/Release/AndroidTest 构建通过，**Lint: No issues found**，原日志页 NestedWeights 布局也已消除。
- 新增 2 项 API 36 原生回归，真实 HTTP 请求、默认关闭/手动开启、原始字节、超限摘要、真实在途清空、预览/复制/分享及页面重建通过。复制确认中进入后台/返回仍正确；深浅色按钮至少 4.5:1 对比度。拦截系统 chooser 并通过 FileProvider 读取快照验证脱敏，未实际发送到其他应用/服务器；结束恢复剪贴板、停用采集和清空诊断。
- 8 项播放/下载、1 项来源全部复跑通过，加上新诊断共 **11 项原生用例**。诊断复跑脚本 scripts/verify-diagnostics.sh；构建/原生日志和深浅色截图位于 app/build/verification/diagnostics/，已逐张检查。
- 平台独立 runner 在真正混淆 Release 中新增 RELEASE_DIAGNOSTIC_PRIVACY_OK：采集开关 disabled/unchecked、正确的 Release 空状态、旧原文清理、旧 FileProvider root 拒绝读取。清理后重新创建自己的 canary 再读取旧 URI，只接受无映射异常，不以“文件不存在”误报 root 验证成功；随后删除 canary。
- 全部安装/命令限定专用 emulator-5580，未操作连接的用户手机；已恢复 Debug 和 AndroidJUnitRunner、font_scale=1.0，无遗留 adb reverse 或 loopback 测试 API；音乐 Release 脚本按设计清空可丢弃应用数据后恢复默认来源，视频保留测试前偏好。生产签名产物仍未配置；测试签名只用于独立 APK 副本。
- 总目标继续进行中：按源 HTTP 信任、签名/版本/映射发布流程、全页面主题/导航、视频在线字幕、播放/投屏状态协调拆分、远端及模拟器 CI、真实硬件/OEM/电视专项不计作本批完成。

## 第七批验证（2026-09-05）：签名/版本/映射归档与 CI 入口

- 双项目接入环境版本与签名配置；普通构建继续 versionCode 1 / versionName 1.0、Release unsigned。显式版本必须成对且合法，显式签名必须同时提供版本与全部四项签名输入；缺项、不可读文件或错误密码失败，不悄悄输出 unsigned 冒充发布包。Debug 签名身份保持原调试证书。
- 新增 package-release.py：依次 JVM 测试、Debug/Release Lint、真实混淆构建，验 apksigner 成功退出/唯一期望身份、APK 内部包名/版本与非 debuggable；对 R8 mapping 正文校验 pg_map_hash，再与 DEX 的 pg-map-id 对应，拒绝错版或截断映射。不把 APK 名字、已打印证书或 mapping 文件存在当作验收。
- APK/mapping/版本与 Git 状态清单/SHA256SUMS 作为一个不可覆盖 ZIP 归档；不包含私钥、密码或密钥库路径，不自动上传或发布。脏工作区如实标记，不声称可重建或代替正式 tag 审核；商店版本单调性仍由发布方核对。Python 单元回归每项目 8 项通过，覆盖密钥/版本缺项、错误证书、debuggable、陈旧/拆分/跨包输出、路径逃逸、错映射/正文篡改、非零验签退出、哈希与归档不可覆盖。
- verify-release-config.py 用临时 2 天有效 RSA/PKCS12 证书，真实构建版本 42 / 0.0.0-verification。默认 unsigned、版本缺项/越界、部分签名配置、正确签名/归档、错误证书/映射、错误密码全部按预期验收，返回 RELEASE_CONFIG_OK；证书结束删除，测试 ZIP 留在 verification/release-config/TEST-ONLY-release.zip，不混入正式归档目录。脚本不继承调用方的实际发布签名输入，最后恢复默认 Debug/Release/AndroidTest 产物，未将此临时证书安装到设备。
- 实测发现 build-tools 37 将签名输出从 Signer #1 改为 V3.0 Signer；已支持两种格式并仍要求成功验签、单一身份及匹配指纹，没有因格式变化跳过证书校验。验证记录与默认恢复结果见 app/build/verification/release-config/。
- 新增 verify-native-ci.sh：合成媒体、全部常规原生、真正混淆 Release 顺序执行；只接收 API 33+ 可丢弃 emulator-*，每条 adb 明确序列号，退出停止本应用。API 36 专用 emulator-5580 已返回 NATIVE_CI_OK，11 项常规原生及本项目全部 Release 标记通过。
- Android workflow 加入打包策略/临时证书回归及 API 33/36 x86_64 原生矩阵，保留日志/截图；新增仅手动、先依赖完整验证 workflow 成功、再在 release environment 下运行的签名归档 workflow（验证阶段不传发布秘密），密钥放 runner 私有临时文件并清理，不使用配置缓存或发布 Gradle 缓存。双项目 actionlint 1.7.7 检查通过；**尚未推送或执行远端 Actions，不将本地 API 36 当作 API 33/云端 x86_64 验收**。
- 当前 **93 项 JVM + 8 项 Python 策略测试 + 11 项常规原生**通过，Debug/Release/AndroidTest 构建及 Debug/Release Lint 均通过（No issues found），额外临时证书签名/归档和真正混淆 Release 黑盒通过。正式证书/Secrets/环境审核人、长期映射存储和远端 CI 执行由发布方配置，详见 docs/RELEASE.md。
- 发布审查另外发现 Retrofit 503 重试分支仍将完整 URL 写入 logcat。先以 Robolectric 回归重现凭据/路径/查询泄露，再移除 URL，仅保留重试次数；实际重试两次、第三次成功的行为不变，新增 1 项 JVM 验证无请求秘密，最新 93 项全量通过。
- 总目标保持进行中：按源 HTTP 信任、主题/导航、视频在线字幕、播放/投屏状态职责拆分、远端及真实硬件/OEM/电视专项仍有明确未完成项；本批没有提交、推送或使用正式私钥。

## 第八批验证（2026-09-05）：在线字幕接入与小窗绘制

- 新增 SubtitlePlayerManager/GsyCueOutput/GsySubtitleDisplay：通过公开 renderer factory 扩展点转发 GSY 原本忽略的真实 Media3 cue，保留时间/文本/位图数据；单内核单订阅，支持准备前回放、主线程显示和释放后丢弃排队回调。全屏/原视图共享字幕，换集/错误/完成/投屏/销毁清理。
- 在线字幕菜单仅在当前媒体有受支持轨道时出现，提供关闭/自动/具体轨道。3 项原生覆盖已缓冲英法轨道暂停切换、时间段/seek gap、全屏、关闭后后台返回/重新启用、PiP/系统暂停以及无字幕下一集；以真实解码 cue、非透明 Canvas 像素和截图联合验收。
- 原生截图发现字幕列表正确但画面没有字：父视图尺寸回调后设置 SubtitleView padding 裁掉内部 Canvas。改用底部定位比例，真实字幕显示恢复。另发现小窗窗口方向被 GSY 当作全屏旋转；增加 PiP 过渡/模式保护，隐藏小窗应用内控制条并阻止远程暂停后的迟到 UI 再显示；小窗字幕改按视口缩放。新增 1 项 JVM 控制层进出回归，连同 3 项 cue 生命周期回归，使 JVM 合计 97 项。
- 真正混淆 Release 的 platform-only runner 检查字幕文字及绘制像素、关闭/自动重新启用，新增 RELEASE_SUBTITLES_OK。已检查普通英文、全屏法文、小窗法文、结尾及 Release 字幕截图；没有用无障碍标签存在代替真实绘制。
- 本地 API 36 arm64 的完整 native-ci 返回 NATIVE_CI_OK：8 项播放/下载 + 3 项字幕 + 1 项来源 + 2 项诊断，共 **14 项常规原生**；RELEASE_PLAYBACK_OK、RELEASE_SUBTITLES_OK、RELEASE_DIAGNOSTIC_PRIVACY_OK 通过。**97 项 JVM**、Debug/Release/AndroidTest 构建和双 Lint 通过。远端矩阵仍未执行，不能以本地 API 36 代替 API 33/x86_64。
- **重要未完成项**：原先把时间段拖动与法语长 cue 切换串联的探索用例仍失败。已定位为 MKV 回拖从更晚视频 cluster 重新加载，法语字幕 SampleQueue 无样本/EOS，而非显示层丢字。保留原失败断言，通过 probe_mkv_preroll=true 独立复现；常规 3 项将时间段和已缓冲轨道切换分别验收，未将这个已知失败计作通过。没有引入强制恢复播放、假 cue、无限 back buffer 或全片从 0 重新解码的规避逻辑。详见 docs/ONLINE_SUBTITLES.md，V10 保持未完成。
- 字幕选择目前仅在同一内核的全屏/前后台保留；同媒体失败重试的轨道恢复与外置字幕导入仍开放。总目标继续推进：上述字幕边界、按源 HTTP 信任、主题/导航、播放/投屏状态职责拆分、远端发布及真实设备/OEM/电视专项；本批没有操作个人手机、提交或推送代码。
