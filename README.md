# AI Chat - 自定义模型 Android 聊天应用

一个支持 OpenAI 兼容接口的 Android AI 聊天应用。用户可自行配置 API Key、Base URL 和模型名称，支持多套模型配置随时切换。

## 功能

- ✅ 类 ChatGPT 聊天界面（流式输出）
- ✅ 自定义模型（Base URL + API Key + Model）
- ✅ 多模型配置管理与快速切换
- ✅ 支持 OpenAI / DeepSeek / 硅基流动 / 通义 / 本地 Ollama 转发等兼容接口
- ✅ 系统提示词、Temperature 可配置
- ✅ 深色 / 浅色主题跟随系统

## 快速开始

### 1. 克隆仓库

```bash
git clone <your-repo-url>
cd AiChatApp
```

### 2. 用 Android Studio 打开

- 打开 Android Studio → Open → 选择本项目根目录
- 等待 Gradle 同步完成（首次会自动生成 Gradle Wrapper）
- 连接真机或启动模拟器，点击 Run

### 3. 配置模型

1. 打开 App 后点击右上角 **调谐图标**
2. 选择已有模型点击编辑，或「添加新模型」
3. 填写 Base URL、API Key、Model 名称后保存即可开始对话

### 常见 Base URL 示例

| 服务 | Base URL | 示例 Model |
|------|----------|------------|
| OpenAI | `https://api.openai.com/v1` | `gpt-4o` |
| DeepSeek | `https://api.deepseek.com/v1` | `deepseek-chat` |
| 硅基流动 | `https://api.siliconflow.cn/v1` | `deepseek-ai/DeepSeek-V3` |
| 通义千问（兼容） | `https://dashscope.aliyuncs.com/compatible-mode/v1` | `qwen-plus` |
| 本地 Ollama | `http://localhost:11434/v1` | `llama3` |

## GitHub Actions 自动打包

工作流模式与 [wallpaper_app](https://github.com/kers701/wallpaper_app) 一致。

### 普通构建（Debug）

- 推送到 `main` / `master`、PR、或手动触发
- 自动构建 **Debug APK**
- 在 Actions → Artifacts 下载 `aichat-debug-apk`

### 正式发布（Release）

当提交说明包含以下任一关键字时，会触发 **Release 签名构建**：

| 关键字 | 行为 |
|--------|------|
| **发布** | 构建正式版 + 自动创建 GitHub Release |
| 正式 / release / re构建 / `[re]` / `re ` 等 | 仅构建正式版 APK（上传 Artifact，不建 Release） |
| 推送 `v*` 标签 | 构建正式版 + 创建对应 Tag 的 GitHub Release |

提交示例：

```bash
git commit -m "发布 1.0.0 首个正式版"
# 或
git commit -m "发布 v1.0.1 修复流式输出"
git push
```

若提交信息里能匹配到 `vX.Y.Z` 或 `X.Y.Z`，会用作 Release Tag；否则使用 `v日期-短SHA`。

### 配置 Release 签名 Secrets（必填）

在仓库 **Settings → Secrets and variables → Actions** 中添加：

| Secret 名称 | 说明 |
|-------------|------|
| `RELEASE_KEYSTORE_BASE64` | keystore 文件的 base64 编码（`base64 -w0 your.keystore`） |
| `RELEASE_STORE_PASSWORD` | keystore 密码 |
| `RELEASE_KEY_ALIAS` | 密钥别名 |
| `RELEASE_KEY_PASSWORD` | 密钥密码 |

本地也可放置 `keystore.properties`（已在 `.gitignore` 中）：

```properties
storeFile=/绝对路径/到/your.keystore
storePassword=xxx
keyAlias=xxx
keyPassword=xxx
```

## 技术栈

- Kotlin + Jetpack Compose
- Material 3
- OkHttp + Retrofit（SSE 流式）
- DataStore（本地配置持久化）
- MVVM

## 项目结构

```
app/src/main/java/com/aichat/app/
├── data/           # 数据模型 + Repository
├── network/        # API 定义与聊天请求
├── ui/
│   ├── chat/       # 聊天界面
│   └── theme/      # 主题
├── viewmodel/      # ViewModel
└── MainActivity.kt
```

## License

MIT
