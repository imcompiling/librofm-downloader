package com.vishnurajeevan.libroabs

enum class SyncInterval{ H, D, W }

data class Config (
    val dataDir: String,
    val devMode: Boolean,
    val directoryTemplate: String,
    val dryRun: Boolean,
    val libroFmPassword: String,
    val libroFmUsername: String,
    val mediaDir: String,
    val port: Int,
    val renameChapters: Boolean,
    val syncInterval: SyncInterval,
    val verbose: Boolean,
    val writeTitleTag: Boolean
)