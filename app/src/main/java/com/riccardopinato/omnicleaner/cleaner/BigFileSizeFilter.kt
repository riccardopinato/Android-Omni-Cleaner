package com.riccardopinato.omnicleaner.cleaner

enum class BigFileSizeFilter(
    val label: String,
    val minimumBytes: Long,
) {
    SIZE_10MB("> 10 MB", 10L * 1024L * 1024L),
    SIZE_50MB("> 50 MB", 50L * 1024L * 1024L),
    SIZE_100MB("> 100 MB", 100L * 1024L * 1024L),
    SIZE_500MB("> 500 MB", 500L * 1024L * 1024L),
    SIZE_1GB("> 1 GB", 1024L * 1024L * 1024L),
}
