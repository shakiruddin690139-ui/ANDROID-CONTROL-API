package com.example.data.model

enum class ConnectionType {
    NATIVE_APP_CHECK,
    API_KEY,
    OAUTH_BROWSER
}

enum class ConnectionStatus {
    CONNECTED,
    DISCONNECTED,
    CONFIGURING,
    ERROR
}

data class AiModelInfo(
    val id: String,
    val name: String,
    val description: String,
    val contextWindow: String,
    val isDefault: Boolean = false
)

data class AiProviderInfo(
    val id: String,
    val name: String,
    val tagline: String,
    val description: String,
    val connectionType: ConnectionType,
    val officialAuthUrl: String,
    val docsUrl: String,
    val supportedModels: List<AiModelInfo>,
    val defaultModelId: String,
    val supportsCodeGeneration: Boolean = true,
    val supportsImageGeneration: Boolean = false,
    val requiresProxy: Boolean = false
)

data class ProviderConnection(
    val providerId: String,
    val status: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val apiKey: String = "",
    val activeModelId: String = "",
    val connectedEmail: String = "",
    val lastCheckedTimestamp: Long = 0L,
    val latencyMs: Long = 0L,
    val errorMessage: String = "",
    val totalRequests: Int = 0
)

object AiProvidersRegistry {
    val GEMINI = AiProviderInfo(
        id = "gemini",
        name = "Google Gemini",
        tagline = "Official Firebase AI Logic Integration",
        description = "Frontier multimodal model by Google. App Check attestation with server-side security. Zero key leakage.",
        connectionType = ConnectionType.NATIVE_APP_CHECK,
        officialAuthUrl = "https://firebase.google.com/docs/ai-logic",
        docsUrl = "https://ai.google.dev",
        supportedModels = listOf(
            AiModelInfo("gemini-3.8-flash", "Gemini 3.8 Flash", "Versatile, lightning fast, state-of-the-art coding", "1M tokens", true),
            AiModelInfo("gemini-3.5-flash-lite", "Gemini 3.5 Flash-Lite", "Ultra-fast lightweight reasoning", "1M tokens"),
            AiModelInfo("gemini-4-argon", "Gemini 4 Argon", "Deep mathematical reasoning and complex code architecture", "2M tokens")
        ),
        defaultModelId = "gemini-3.8-flash",
        supportsCodeGeneration = true,
        supportsImageGeneration = true
    )

    val OPENAI = AiProviderInfo(
        id = "openai",
        name = "OpenAI",
        tagline = "GPT-4o & Reasoning Models",
        description = "Industry standard general and reasoning intelligence models for full-stack software development.",
        connectionType = ConnectionType.API_KEY,
        officialAuthUrl = "https://platform.openai.com/api-keys",
        docsUrl = "https://platform.openai.com/docs",
        supportedModels = listOf(
            AiModelInfo("gpt-4o", "GPT-4o", "High-intelligence flagship multimodal model", "128k tokens", true),
            AiModelInfo("gpt-4o-mini", "GPT-4o Mini", "Fast, cost-effective coding model", "128k tokens"),
            AiModelInfo("o3-mini", "o3-mini", "High-speed reasoning model specialized for STEM and code", "200k tokens")
        ),
        defaultModelId = "gpt-4o",
        supportsCodeGeneration = true,
        supportsImageGeneration = true
    )

    val CLAUDE = AiProviderInfo(
        id = "claude",
        name = "Anthropic Claude",
        tagline = "Claude 3.5 Sonnet & Haiku",
        description = "Frontier coding performance, nuanced architectural synthesis, and precise debugging capabilities.",
        connectionType = ConnectionType.API_KEY,
        officialAuthUrl = "https://console.anthropic.com/settings/keys",
        docsUrl = "https://docs.anthropic.com",
        supportedModels = listOf(
            AiModelInfo("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet", "World-class coding and system design", "200k tokens", true),
            AiModelInfo("claude-3-5-haiku-20241022", "Claude 3.5 Haiku", "Instantaneous code review and syntax fixes", "200k tokens")
        ),
        defaultModelId = "claude-3-5-sonnet-20241022",
        supportsCodeGeneration = true,
        supportsImageGeneration = false,
        requiresProxy = true
    )

    val GROQ = AiProviderInfo(
        id = "groq",
        name = "Groq LPU",
        tagline = "Ultra-Fast Inference Engine",
        description = "Hardware-accelerated LPUs delivering near-instant 500+ tokens/sec code generation.",
        connectionType = ConnectionType.API_KEY,
        officialAuthUrl = "https://console.groq.com/keys",
        docsUrl = "https://console.groq.com/docs",
        supportedModels = listOf(
            AiModelInfo("llama-3.3-70b-versatile", "Llama 3.3 70B", "Fast open-weight developer intelligence", "128k tokens", true),
            AiModelInfo("mixtral-8x7b-32768", "Mixtral 8x7B", "MoE high-throughput reasoning", "32k tokens")
        ),
        defaultModelId = "llama-3.3-70b-versatile",
        supportsCodeGeneration = true,
        supportsImageGeneration = false
    )

    val HUGGINGFACE = AiProviderInfo(
        id = "huggingface",
        name = "Hugging Face",
        tagline = "Inference API & Open Source Hub",
        description = "Direct access to premier open-source models like DeepSeek, Qwen Coder, and StarCoder.",
        connectionType = ConnectionType.API_KEY,
        officialAuthUrl = "https://huggingface.co/settings/tokens",
        docsUrl = "https://huggingface.co/docs",
        supportedModels = listOf(
            AiModelInfo("deepseek-ai/DeepSeek-R1", "DeepSeek R1", "Open-source reasoning champion", "64k tokens", true),
            AiModelInfo("Qwen/Qwen2.5-Coder-32B-Instruct", "Qwen 2.5 Coder 32B", "Code specialized neural network", "128k tokens")
        ),
        defaultModelId = "deepseek-ai/DeepSeek-R1",
        supportsCodeGeneration = true,
        supportsImageGeneration = false
    )

    val OPENROUTER = AiProviderInfo(
        id = "openrouter",
        name = "OpenRouter",
        tagline = "Unified Multi-Model Gateway",
        description = "Single unified key to query 100+ AI models with automatic fallbacks and competitive rates.",
        connectionType = ConnectionType.API_KEY,
        officialAuthUrl = "https://openrouter.ai/keys",
        docsUrl = "https://openrouter.ai/docs",
        supportedModels = listOf(
            AiModelInfo("auto", "Auto Router", "Dynamically routes to optimal available model", "128k tokens", true),
            AiModelInfo("anthropic/claude-3.5-sonnet", "Claude 3.5 Sonnet (via OpenRouter)", "Best-in-class coding", "200k tokens"),
            AiModelInfo("meta-llama/llama-3.3-70b-instruct", "Llama 3.3 70B (via OpenRouter)", "Open source power", "128k tokens")
        ),
        defaultModelId = "auto",
        supportsCodeGeneration = true,
        supportsImageGeneration = false
    )

    val COHERE = AiProviderInfo(
        id = "cohere",
        name = "Cohere",
        tagline = "Command R+ Enterprise Models",
        description = "Enterprise-grade language models optimized for complex multi-step reasoning and retrieval.",
        connectionType = ConnectionType.API_KEY,
        officialAuthUrl = "https://dashboard.cohere.com/api-keys",
        docsUrl = "https://docs.cohere.com",
        supportedModels = listOf(
            AiModelInfo("command-r-plus", "Command R+", "Complex enterprise multi-step logic", "128k tokens", true),
            AiModelInfo("command-r", "Command R", "Fast conversational and task engine", "128k tokens")
        ),
        defaultModelId = "command-r-plus",
        supportsCodeGeneration = true,
        supportsImageGeneration = false
    )

    val ALL_PROVIDERS = listOf(GEMINI, OPENAI, CLAUDE, GROQ, HUGGINGFACE, OPENROUTER, COHERE)

    fun getProvider(id: String): AiProviderInfo {
        return ALL_PROVIDERS.find { it.id == id } ?: GEMINI
    }
}
