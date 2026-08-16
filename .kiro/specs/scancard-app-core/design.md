# ScanCard - Design Document

## Overview

ScanCard is an Android application that enables users to photograph book pages using the device camera, perform automatic document correction, and extract flashcard term/definition pairs using on-device LLMs (Gemma 4 family).

### System Characteristics

| Characteristic | Description |
|---------------|-------------|
| **Platform** | Android (OS 8.0 / API Level 26+) |
| **Language** | Kotlin 2.2+ |
| **UI Framework** | Jetpack Compose with Material 3 |
| **Architecture** | MVVM + Clean Architecture |
| **DI Framework** | Hilt |

### Key Capabilities

1. **Batch Scanning**: ML Kit Document Scanner integration.
2. **On-Device OCR**: ML Kit Text Recognition (Japanese/English).
3. **Multi-Model AI**: Support for Gemma 4 (E2B, E4B) and Gemma 2 via LiteRT LM.
4. **Authenticated Download**: Personal Access Token (PAT) based model download from Hugging Face.
5. **3D Study**: Interactive flashcard study with animations.
6. **Local Persistence**: Room database for decks, cards, and scans.
7. **Edge-to-Edge**: Modern UI supporting Android 15 system bar transparent rendering.

---

## Architecture

### High-Level Architecture Diagram

```mermaid
graph TB
    subgraph "Presentation Layer"
        UI[Jetpack Compose UI]
        VM[ViewModels]
    end

    subgraph "Domain Layer"
        UC[Use Cases]
        M[Domain Models/Configs]
    end

    subgraph "Data Layer"
        R[Repositories]
        DAO[Room DAOs]
        Auth[AuthManager / PAT]
        Worker[WorkManager / OkHttp]
    end

    subgraph "External SDKs"
        ML[ML Kit]
        MP[LiteRT LM Engine]
    end

    UI --> VM
    VM --> UC
    UC --> M
    UC --> R
    R --> DAO
    R --> Auth
    R --> Worker
    R --> ML
    R --> MP
```

### Technology Stack

| Layer | Technology | Purpose |
|-------|------------|---------|
| UI | Jetpack Compose + Material 3 | Declarative UI, 3D animations |
| Architecture | MVVM + Clean Architecture | Separation of concerns |
| DI | Hilt | Dependency injection |
| Database | Room Database | Local data persistence |
| Auth | SharedPreferences (Encrypted) | Hugging Face PAT management |
| Background | WorkManager | Reliable model downloads with redirect handling |
| Network | OkHttp | Authenticated downloads with relative URL resolution |
| OCR | ML Kit Text Recognition v2 | Japanese/English extraction |
| AI | LiteRT LM SDK | Gemma 4 model orchestration (Engine/Conversation) |

---

## Core Components

### 1. AI & Model Management
- **ModelManager**: Manages model state, installation checks, and triggers background downloads via `WorkManager`.
- **ModelDownloadWorker**: `CoroutineWorker` that performs authenticated downloads using OkHttp, resolves relative redirects, and emits progress.
- **GemmaCardExtractor**: Wraps LiteRT LM Engine and Conversation APIs, handles model initialization and asynchronous inference.
- **CardResponseParser**: Multi-strategy parser (JSON, Regex, Text-Search) for AI output.

### 2. Authentication
- **AuthManager**: Manages the Hugging Face Personal Access Token (PAT), providing tokens for gated model access during download.

### 3. Data Persistence (Room)
- **AppDatabase**: Main entry point for Room.
- **Entities**: `Deck` (id, title, timestamps), `Card` (term, definition, learned status), `Scan` (raw OCR text, image path).
- **DAOs**: Reactive `Flow`-based access to data.

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
    val downloadUrl: String
)
```

### AI States (`ModelState`)

```kotlin
sealed class ModelState {
    object Idle : ModelState()
    data class Downloading(val progress: Float) : ModelState()
    object Ready : ModelState()
    data class Error(val message: String) : ModelState()
}
```

---

## UI Layer Design

### Screen Structure
1. **Home Screen**: Deck list and management.
2. **Scan Screen**: Multi-page document capture with a fixed bottom bar for processing.
3. **Extraction Preview**: Model selection, PAT setup, download progress, and AI trigger.
4. **Deck Detail**: Review extracted cards and start study.
5. **Study Screen**: 3D flip card interaction with filters.
6. **Export Screen**: TSV/CSV format selection and sharing.

---

## Implementation Rationale

- **LiteRT LM (Modern)**: Replaces MediaPipe GenAI for better stability and support for the latest Gemma 4 architectures (PLE, MTP).
- **PAT Authentication**: Chosen over OAuth for higher reliability in developer/early-access tools, avoiding common Redirect URI issues.
- **Edge-to-Edge**: Implemented using `enableEdgeToEdge()` and `systemBarsPadding()` to meet Android 15 requirements while maintaining usability.
- **Clean Architecture**: Separates AI/OCR implementation details from the core business logic of managing flashcards.
