package com.plath.scancard.domain.model

enum class CardStatus {
    NEW,
    LEARNING,
    REVIEW
}

enum class FilterType {
    ALL,
    NEW,
    LEARNING,
    REVIEW
}

enum class LanguagePreference {
    ENGLISH,
    JAPANESE
}

enum class ExtractionStatus {
    NONE,
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}
