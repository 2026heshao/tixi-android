# 题隙 · Tixi Android

把图片 / PDF / Word 里的题目经 MinerU 云端解析 → **人工确认兜底** → 本地题库 → 分类内顺序刷题。

单 Activity + Jetpack Compose（M3） + Room + Hilt + OkHttp，minSdk 24 / targetSdk 35。
本工程是《题隙-Android落地方案》v1.0 的代码落地，UI 基线为《题隙-设计规范系统》v1.4 与 21 屏原型。

---

## 1. 构建与运行

```bash
# 环境：JDK 17（D:\AndroidDev\jdk-17.0.20.1+1）、Android SDK 35、Gradle Wrapper 8.9
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest          # 20 个 JVM 单测
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`local.properties` 已写入 `sdk.dir=D:\AndroidDev\android-sdk`。

### 字体（H3 已落地）

内置 **Noto Serif SC 子集**：`res/font/noto_serif_sc_{400,500}.ttf`，各 3.19 MB（压缩进包约 1.9 MB）。

- 覆盖 **8377 个码位**：GB2312 汉字 6763 + 拉丁/希腊/数学算子（`≠ ≤ ≥ √ ∑ ∫ π α`）+ 圈号 `①②③` + 全角标点 + CJK 标点。
- 只绑定 `Normal` / `Medium` 两档，正好对上 `TixiTypography` 里宋体实际用到的字重，避免加载多余字重。
- 子集外的生僻字由系统字体回退链兜底，**不会出现豆腐块**，只是字形风格会略有差异。
- 重新生成：`python tools/make_font_subset.py`（需 `pip install fonttools brotli`，脚本会拉全量字体并按码位表重切）。
- 许可：SIL OFL-1.1，`assets/licenses/OFL-NotoSerifSC.txt` 随包分发。

> ⚠️ 踩坑记录：fontsource 的 `chinese-simplified` 子集**不含数学与希腊符号**，题干里的 `π √ ≤` 会掉进系统字体、与正文风格割裂。必须从全量字体按码位表切子集，不能直接拿通用简中包。

### 云端构建（GitHub Actions）

`.github/workflows/android.yml`：push 到 `main` 或手动 Run workflow → JDK 17 + Gradle 缓存 → 跑单测 → 出 debug 与 release（R8、未签名）两个 APK，产物在 Actions 运行的 Artifacts 里。

`local.properties` 不入库，CI 由 runner 的 `ANDROID_HOME` 提供 SDK 路径；`.gitattributes` 已把 `gradlew` 锁为 LF（否则 Linux runner 报 `bad interpreter`）。

### ⚠️ 中文路径与 JVM 单测
工程路径含中文（`D:\桌面\AI Port\...`）时，`testDebugUnitTest` 会在启动测试类时抛
`ClassNotFoundException`（Gradle Test Worker 的 classpath 编码问题，与代码无关）。
两种解法，任选其一：

1. 映射一个 ASCII 盘符后跑测试（已在本次交付中验证）：
   ```powershell
   subst X: "D:\桌面\AI Port\heshao-Tixi"
   cd X:\tixi-android; .\gradlew :app:testDebugUnitTest
   subst X: /D    # 用完解除
   ```
2. 把仓库移到纯 ASCII 路径（如 `D:\dev\tixi-android`）。

编译与打包不受影响，只有单元测试受影响。

---

## 2. 已实现范围

| 里程碑 | 内容 | 状态 |
|---|---|---|
| M0 骨架 | Gradle + version catalog + Hilt + 单 Activity + Room 7 表 + Theme token + 12 路由 | ✅ |
| M1 题库 | 分类 CRUD（删分类→未分类）、三种题型题目 CRUD、题干/标签搜索、题型管理、连续切题编辑 | ✅ |
| M2 MinerU | Token Keystore+AES-GCM 存储、四步上传/轮询/下载（官方 v4 接口）、错误分档、设置页 | ✅ 协议已用真实接口跑通；Token 通道待有效 API Token |
| M3 确认闸门 | 自研 markdown→题目解析、5 条乱码/质量规则、逐题确认/编辑/改分类、富内容禁止批量跳过、导入记录 | ✅ |
| M4 练习闭环 | 四题型状态机、提交/解析/自评、断点续练、本轮结算（内存统计）、冷启动引导 | ✅ |
| M5 加固 | 权限零申请（Photo Picker / OpenDocument / FileProvider 拍照）、错误态 UI、设计 QA | ⚠️ 代码就绪，真机回归待做 |

**不做**（与方案 §2.2 一致）：账号/云同步、随机练习、错题本、统计报表、本地 OCR、多级分类、去重与导出、自动判分。

---

## 3. 关键决策（方案附录 A / §8.2）

| 编号 | 结论 | 说明 |
|---|---|---|
| P1 题型 | 按 v1.4 三题型落地 | `choice / fill / essay` 差异化 UI，v0.1「通用题目」作废 |
| P2 接口 | 初版只接 v4 | `POST /api/v4/file-urls/batch` → PUT → `GET /api/v4/extract-results/batch/{id}` |
| P3 自定义题型 | **降级为通用展示**（题干 + 参考答案 + 解析，无提交/自评） | 题型管理仍可增删，编辑页可选题型 |
| P4 uv 约束 | 移除 | App 端无 Python，构建全走 Gradle |
| H1 公式渲染 | **自研 LaTeX 子集渲染器**（Compose 原生，无 WebView/KaTeX） | 支持 `\frac \sqrt ^ _` 与常用希腊字母/运算符；体积零增量、滚动不掉帧；不支持的命令原样输出不崩 |
| H2 MinerU 接口 | 已按官方文档核实 | 图片（png/jpg/webp/gif/bmp）v4 **直接支持**，无需封 PDF；单文件 ≤200MB / ≤200 页；`full_zip_url` 内含 `full.md` |
| H3 宋体/ColorFilter | 内置 Noto Serif SC 子集（8377 码位，400/500 两档）；护眼滤镜用 ColorMatrix（反相+色相回正） | 只改了 `TixiFontFamilies.Serif` 一处，全站标题/题干自动切换；滤镜不改源文件 |

---

## 4. 目录结构

```
app/src/main/java/com/tixi/app/
├── TixiApp.kt / MainActivity.kt
├── domain/
│   ├── model/        Question / AnswerData / RichBlock / QualityFlag / ImportDraft / SessionStats / ParseProtocol
│   ├── repository/   6 个 Repository 接口（UI 只依赖接口）
│   └── usecase/      MinerUMarkdownParser / DetectGarbage
├── data/
│   ├── local/        Entities(7 表) / DAO / Converters / Mappers / TixiDatabase
│   ├── remote/       MinerUClient（四步 + SafeUrl SSRF 防护 + 错误分档）/ dto
│   ├── security/     TokenStore（AndroidKeyStore AES-GCM + AtomicFile + noBackupFilesDir）
│   ├── preferences/  SettingsDataStore（主题三档 / 解析参数 / 引导完成）
│   ├── files/        ImportFileStore（Uri 复制、相机临时文件、FileProvider）
│   └── repository/   *RepositoryImpl
├── di/DataModule.kt
└── ui/
    ├── theme/        Color / Type / Shape / Motion / Theme（token 与规范 §1、§2、§3 一一对应）
    ├── common/       Buttons / AppBars / Inputs / RadioOption / Cards / Feedback / RichBlockView
    ├── navigation/   TixiRoutes（12 路由）+ TixiNavHost
    ├── onboarding/ home/ categories/ practice/ library/ questionedit/
    ├── importflow/   ImportRunner（Singleton 后台任务）/ source / task / review / history
    ├── categorymanage/ settings/
    └── RootViewModel.kt
```

---

## 5. 数据模型（Room v1）

`categories` / `question_types` / `questions` / `import_drafts` / `draft_questions` / `import_records` / `practice_progress`。

- 删分类：`ON DELETE SET NULL` + 事务内 `UPDATE questions SET categoryId=NULL`，题目落「未分类」（categoryId = null）。
- 分类内顺序：`ORDER BY sortOrder ASC, id ASC`；入库追加 `max(sortOrder)+1`。
- `question_types.key` 是 SQLite 保留字，SQL 里必须写成 `` `key` ``。
- 本轮练习统计（题数/用时/自评掌握/掌握率/薄弱点）只在 `PracticeViewModel` 内存维护，**不写库**（沿用 R3）。

---

## 6. 识别质量闸门（R1）

`DetectGarbage` 对每道草稿题输出 `QualityFlag`：

| 规则 | 阈值 | 处置 |
|---|---|---|
| SUSPECT_GARBLED | U+FFFD / □ / 私用区字符占比 > 2% | 待核对徽标 + 顶部 WarningBanner |
| TOO_SHORT | 题干 < 8 字 | 待核对 |
| BAD_OPTIONS | 选择题选项 < 2 或含空选项 | 待核对 |
| NO_ANSWER | 答案为空 | 待核对 |
| HAS_RICH_CONTENT | 含表格/公式/原图 | 强制人工确认，**不允许**批量跳过 |

「全部确认并入库」遇到 `needsReview` 草稿时弹二次确认。

---

## 7. 设置页与解析参数

- Token：掩码 + 眼睛切换 + Keystore 加密落盘；**不进日志、不进备份**。
- 题型管理 / 分类管理：列表式内联增改，删题型不丢题、删分类移入未分类均 toast 明示。
- 主题：跟随系统 / 浅色 / 深色。
- 解析参数：语言（ch）、启用表格、启用公式、轮询间隔（3/5/10 s）。
- 数据：清空题库（二次确认，Token 与设置保留）。

---

## 8. 已知限制 / 后续

1. **真机未回归**：当前环境无连接设备，§9.1 端到端脚本、§9.3 设备矩阵（含 API 24 的宋体回退与 ColorFilter）尚未执行。
2. **MinerU 真实样本质量未验证**（R1 结论仍需 ≥20 份样本实测，产出《质量报告》）。
3. 富内容公式仅支持 LaTeX 子集；表格/公式编辑在题目编辑页，草稿确认页可改题干/选项/答案/分类与块内容。
4. 分类管理页用图标按钮删除，未实现原型里的右滑删除手势（功能等价，少一个手势）。
5. 冷启动第 4 步（首题入库）只在「引导期间完成一次导入」后出现，其余路径直接进工作台。
6. **MinerU 凭证分两种**：控制台里 `Secret Key`（40 位）**不能**直接当 Bearer Token，会返回 `A0202 user authenticate failed`；要用「API 管理」页创建的 Token。App 的设置页要填的是后者。
7. **免登录轻量接口**（`/api/v1/agent/parse/*`）无需 Token，但限制 10 MB / 20 页、只出 markdown、PDF 走轻量模型不保证表格与公式，暂未接入。

---

## 9. 验证记录

- `:app:assembleDebug` ✅（APK 23.7 MB，含 6.4 MB 字体；debug 还含 material-icons-extended）
- `:app:assembleRelease` ✅（R8 后 5.99 MB，未签名）
- `:app:testDebugUnitTest` ✅ 20/20（Converters 6、DetectGarbage 5、MarkdownParser 5、SafeUrl 4）
- MinerU 真实链路 ✅ 2026-11-22 用 `/api/v1/agent` 免登录通道跑通：提交 → OSS 签名 PUT → 轮询 → 取 `full.md`，2 道题正确识别
- KSP / Hilt / Room 编译期校验通过；R8 release 构建见构建日志
