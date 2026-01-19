package com.frotagestor.interfaces

import kotlinx.serialization.Serializable

sealed class BackupResult {
    data class Success(val filename: String, val size: Long) : BackupResult()
    data class Error(val message: String) : BackupResult()
}

data class BackupInfo(
    val status: String,
    val lastBackup: String?,
    val count: Int
)

@Serializable
data class BackupExecuteResponse(
    val success: Boolean,
    val message: String,
    val filename: String? = null,
    val size: String? = null
)

@Serializable
data class StatusResponse(
    val version: String,
    val uptime: String,
    val cpu: String,
    val cpuModel: String,
    val memoryUsed: String,
    val memoryTotal: String,
    val dbStatus: String,
    val dbDetails: String,
    val backupStatus: String,
    val backupLast: String?,
    val backupCount: Int
)
