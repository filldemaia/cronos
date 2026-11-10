package com.filldemaia.cronos.radio.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Station(
    val id: String,
    val name: String,
    val tagline: String = "",
    val urls: List<String>,
    val logoText: String = "",
    val logoColor: String = "#FF7A1A",
    /** Nom del recurs drawable del logotip oficial; buit = inicials. */
    val logoRes: String = "",
    val bitrateKbps: Int = 128,
    val website: String? = null,
    val verified: String? = null,
    val isCustom: Boolean = false
) {
    fun primaryUrl(): String? = urls.firstOrNull()

    fun alternativeUrls(): List<String> = urls.drop(1)

    fun displayLogo(): String =
        logoText.ifBlank { name.take(2).uppercase() }

    fun isHls(url: String = primaryUrl().orEmpty()): Boolean =
        url.substringBefore('?').endsWith(".m3u8", ignoreCase = true)

    fun colorArgb(): Long {
        val raw = logoColor.removePrefix("#")
        return try {
            when (raw.length) {
                6 -> 0xFF000000L or raw.toLong(16)
                8 -> raw.toLong(16)
                else -> 0xFFFF7A1AL
            }
        } catch (_: NumberFormatException) {
            0xFFFF7A1AL
        }
    }
}

@Serializable
data class StationCatalog(
    val stations: List<Station> = emptyList()
)
