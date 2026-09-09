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
- 等待 Gradle 同步完成
- 连接真机或启动模拟器，点击 Run

### 3. 配置模型

1. 打开 App 后点击右上角 **调谐图标**
2. 选择已有模型点击编辑，或「添加新模型」
3. 填写：
   - **显示名称**：随便起名，如 `我的 GPT-4o`
   - **Base URL**：例如 `https://api.openai.com/v1`
   - **API Key**：你的密钥
   - **Model 名称**：例如 `gpt-4o` / `deepseek-chat` / `qwen-plus` 等
4. 保存后即可开始对话

### 常见 Base URL 示例

| 服务 | Base URL | 示例 Model |
|------|----------|------------|
| OpenAI | `https://api.openai.com/v1` | `gpt-4o` |
| DeepSeek | `https://api.deepseek.com/v1` | `deepseek-chat` |
| 硅基流动 | `https://api.siliconflow.cn/v1` | `deepseek-ai/DeepSeek-V3` |
| 通义千问（兼容） | `https://dashscope.aliyuncs.com/compatible-mode/v1` | `qwen-plus` |
| 本地 Ollama | `http://localhost:11434/v1` | `llama3` |

## GitHub Actions 自动打包

已配置 `.github/workflows/build.yml`：

- 推送到 `main` / `master` 或手动触发
- 自动构建 **Debug APK** 和 **Release APK（未签名）**
- 产物可在 Actions 页面 → 对应运行记录 → Artifacts 中下载

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

## 后续可扩展

- [ ] 对话历史持久化（Room）
- [ ] 多会话管理
- [ ] Markdown / 代码高亮完整渲染
- [ ] 图片上传（Vision 模型）
- [ ] API Key 使用 Android Keystore 加密
- [ ] 导出对话

## License

MIT
