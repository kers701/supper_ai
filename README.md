# AI Chat (supper_ai) - 自定义模型 Android 聊天应用

支持 OpenAI 兼容接口的 Android AI 聊天应用。可自行配置 API Key、Base URL 和模型，支持多套配置切换、深度思考、联网搜索与应用内更新。

**当前版本：1.0.0**

## 功能

- 类 ChatGPT 聊天界面（流式输出）
- 自定义模型（Base URL + API Key + Model）
- 多模型配置管理与快速切换
- 支持 OpenAI / DeepSeek / 硅基流动 / 通义 / 本地 Ollama 等兼容接口
- **深度思考**（DeepSeek `thinking`）
- **联网搜索**（通义等 `enable_search`）
- **输入法避让**：键盘弹出时输入框自动抬升
- **应用内更新**：启动检查 GitHub Releases，可下载安装
- 系统提示词、Temperature 可配置
- 深色 / 浅色主题跟随系统

## 快速开始

1. 安装 APK（见 [Releases](https://github.com/kers701/supper_ai/releases)）
2. 点击右上角调谐图标 → 编辑或添加模型
3. 填写 Base URL、API Key、Model 后保存即可对话

### 常见配置示例

| 服务 | Base URL | Model |
|------|----------|-------|
| OpenAI | `https://api.openai.com/v1` | `gpt-4o` |
| DeepSeek | `https://api.deepseek.com` | `deepseek-v4-flash` |
| 硅基流动 | `https://api.siliconflow.cn/v1` | `deepseek-ai/DeepSeek-V3` |
| 通义千问 | `https://dashscope.aliyuncs.com/compatible-mode/v1` | `qwen-plus` |

DeepSeek 可在编辑模型中打开「深度思考」；通义等可打开「联网搜索」。

## 检查更新

顶栏系统更新图标可手动检查；启动时也会静默检查 GitHub Releases。有新版本可直接下载安装。

## GitHub Actions

- 普通推送 → Debug APK（Artifacts）
- 提交说明含 **发布** → Release 签名 APK + GitHub Release
- 需配置 Secrets：`RELEASE_KEYSTORE_BASE64`、`RELEASE_STORE_PASSWORD`、`RELEASE_KEY_ALIAS`、`RELEASE_KEY_PASSWORD`

## 技术栈

Kotlin · Jetpack Compose · Material 3 · OkHttp/Retrofit · DataStore · MVVM

## License

MIT
