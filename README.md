# kolo — interface agnostic llm provider wrapper

---

> [!IMPORTANT]  
> This library is in development mode and the API is subject to change.

---

## How It Works

This library provides a unified way to translate requests and responses between different Large Language Model (LLM) providers. It allows developers to integrate with one interface (e.g., OpenAI’s API) while actually calling another provider (e.g., Anthropic, Mistral, etc.), without rewriting application logic.

---

### The Challenge

Each LLM provider exposes its own request and response format:

- **OpenAI** expects `messages` arrays with `role/content` parts.
- **Anthropic** expects a different structure, with `system` and `user/assistant` segments.
- **Mistral** and others have yet more variations.

If you want to connect a tool that only understands **one interface** (e.g., OpenAI) to a **different backend** (e.g., Anthropic), you would normally need to write a direct conversion for every pair.

This leads to a **Cartesian product problem**:

- For _N_ providers, you’d need _N × (N-1)_ conversions.

---

### The Solution: Intermittent Format

Instead of converting directly between every provider pair, this library introduces a **standard intermittent format**.

The process works in two steps:

1. **Normalize →** Convert provider-specific request into the intermittent format.
2. **Transform →** Convert intermittent format into the target provider’s request.

The same applies to responses:

1. Normalize response → Intermittent format.
2. Transform intermittent format → Desired interface (e.g., OpenAI-style).

With this approach, the number of conversions drops from _N × (N-1)_ to just _2 × N_ (one in and one out per provider).

---

### Documentation

📖 **[Complete Documentation](docs/README.md)** - Comprehensive guides and API reference

- **[Usage Guide](docs/usage-guide.md)** - Step-by-step instructions and examples

### Quick Start

```kotlin
// Add dependencies
dependencies {
    implementation("com.fatihcure.kolo:providers:1.4.0")
    implementation("com.fatihcure.kolo:normalizers:1.4.0")
    implementation("com.fatihcure.kolo:transformers:1.4.0")
}

// Basic usage
val provider = KoloProvider()
val kolo = provider.createKolo<OpenAIRequest, AnthropicRequest>()
val converted = kolo.convertRequest(openAIRequest)
```

---

### Key Features

- **Unified interface** across providers.
- **Reduced complexity** via intermittent format.
- **Supports request & response conversion** (including errors, metadata).
- **Streaming-compatible** for real-time use cases.
- **Extensible**: add new providers by defining just two mappings (to/from intermittent format).
