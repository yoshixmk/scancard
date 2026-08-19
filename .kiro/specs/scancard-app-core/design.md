# ScanCard - Design Document

## Overview

ScanCard is an Android application that enables users to photograph book pages using the device camera, perform automatic document correction, and extract flashcard term/definition pairs using on-device LLMs (Gemma 4 family) via the **LiteRT LM SDK**.

### Key Capabilities

1. **Batch Scanning**: ML Kit Document Scanner integration.
2. **On-Device OCR**: ML Kit Text Recognition (Japanese/English).
3. **Multi-Model AI**: Support for Gemma 4 (E2B, E4B) via LiteRT LM.
4. **Google Play AI Delivery**: Models are delivered on-demand as **AI Packs**, avoiding third-party authentication.
5. **3D Study**: Interactive flashcard study with animations.
6. **Local Persistence**: Room database for decks, cards, and scans.

---

## Architecture

### Technology Stack

| Layer | Technology | Purpose |
|-------|------------|---------|
| UI | Jetpack Compose + Material 3 | Declarative UI, 3D animations |
| DI | Hilt | Dependency injection |
| Database | Room Database | Local data persistence |
| Delivery | **Google Play AI Delivery** | Secure on-demand model distribution |
| OCR | ML Kit Text Recognition v2 | Japanese/English extraction |
| AI | **LiteRT LM SDK** | Gemma 4 model orchestration (Engine/Conversation) |

---

## Core Components

### 1. AI & Model Management
- **ModelManager**: Interfaces with `AiPackManager` to track AI Pack states (`PENDING`, `DOWNLOADING`, `COMPLETED`) and trigger downloads.
- **GemmaCardExtractor**: Wraps LiteRT LM Engine and Conversation APIs, handles model initialization and asynchronous inference using the downloaded pack assets.

---

## Data Models

### Model Configuration (`ModelConfig`)

```kotlin
data class ModelConfig(
    val id: String,
    val name: String,
    val description: String,
    val sizeGb: Double,
    val fileName: String,
    val aiPackName: String
)
```
