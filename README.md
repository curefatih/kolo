kolo — interface agnostic llm provider wrapper

---

# How It Works

This library provides a unified way to translate requests and responses between different Large Language Model (LLM) providers. It allows developers to integrate with one interface (e.g., OpenAI’s API) while actually calling another provider (e.g., Anthropic, Mistral, etc.), without rewriting application logic.

---

## The Challenge

Each LLM provider exposes its own request and response format:

* **OpenAI** expects `messages` arrays with `role/content` parts.
* **Anthropic** expects a different structure, with `system` and `user/assistant` segments.
* **Mistral** and others have yet more variations.

If you want to connect a tool that only understands **one interface** (e.g., OpenAI) to a **different backend** (e.g., Anthropic), you would normally need to write a direct conversion for every pair.

This leads to a **Cartesian product problem**:

* For *N* providers, you’d need *N × (N-1)* conversions.

---

## The Solution: Intermittent Format

Instead of converting directly between every provider pair, this library introduces a **standard intermittent format**.

The process works in two steps:

1. **Normalize →** Convert provider-specific request into the intermittent format.
2. **Transform →** Convert intermittent format into the target provider’s request.

The same applies to responses:

1. Normalize response → Intermittent format.
2. Transform intermittent format → Desired interface (e.g., OpenAI-style).

With this approach, the number of conversions drops from *N × (N-1)* to just *2 × N* (one in and one out per provider).

---

## Example Flow

Imagine you have:

* **Your tooling:** Supports OpenAI’s Chat Completions API.
* **Your backend provider:** Anthropic Claude.

### Without this library:

* You’d have to manually translate OpenAI → Anthropic, and Anthropic → OpenAI.

### With this library:

1. **Incoming request:**

   * Tool sends an OpenAI-style request.
   * Library normalizes it into the intermittent format.
2. **Outgoing to provider:**

   * Library transforms intermittent format into Anthropic’s request format.
   * Sends request to Anthropic.
3. **Incoming response:**

   * Anthropic’s response is normalized to intermittent format.
4. **Outgoing to tool:**

   * Library transforms intermittent format into OpenAI-style response.
   * Tool receives exactly what it expects, as if it talked to OpenAI.

---

## Streaming Support

The library also supports **streaming responses**, which many LLM APIs implement differently.

* The intermittent format defines a standard event structure (`delta`, `message_start`, `message_end`).
* Providers’ streaming events are normalized into this structure.
* The library can then re-stream them in the target provider’s event format.

This allows tools that expect OpenAI’s streaming protocol (`delta` events) to work seamlessly with Anthropic’s or any other provider’s streaming responses.

---

## Architecture Overview

```
             ┌───────────────┐
             │   Your Tool   │
             │ (OpenAI API)  │
             └───────┬───────┘
                     │ OpenAI-format request
                     ▼
             ┌───────────────┐
             │ Normalizer    │
             │ (OpenAI → IF) │
             └───────┬───────┘
                     │ Intermittent Format (IF)
                     ▼
             ┌───────────────┐
             │ Transformer   │
             │ (IF → Anthro) │
             └───────┬───────┘
                     │ Anthropic-format request
                     ▼
             ┌───────────────┐
             │   Provider    │
             │   (Claude)    │
             └───────┬───────┘
                     │ Anthropic response
                     ▼
             ┌───────────────┐
             │ Normalizer    │
             │ (Anthro → IF) │
             └───────┬───────┘
                     │ Intermittent Format (IF)
                     ▼
             ┌───────────────┐
             │ Transformer   │
             │ (IF → OpenAI) │
             └───────┬───────┘
                     │ OpenAI-format response
                     ▼
             ┌───────────────┐
             │   Your Tool   │
             └───────────────┘
```

---

## Key Features

* **Unified interface** across providers.
* **Reduced complexity** via intermittent format.
* **Supports request & response conversion** (including errors, metadata).
* **Streaming-compatible** for real-time use cases.
* **Extensible**: add new providers by defining just two mappings (to/from intermittent format).

