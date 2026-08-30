package com.orbitglitch.fullscreenbrowsers.data

import java.util.UUID

/**
 * Represents a sub-app configuration.
 *
 * @param id           Unique identifier (UUID string).
 * @param title        Display name shown on the list and home-screen shortcut.
 * @param url          The URL the full-screen browser will open.
 * @param iconUrl      Optional URL used to fetch the app icon.
 * @param volumeDownJs JavaScript injected when Volume Down is pressed (empty = disabled).
 * @param volumeUpJs   JavaScript injected when Volume Up is pressed (empty = disabled).
 */
data class SubApp(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val url: String = "",
    val iconUrl: String = "",
    val volumeDownJs: String = "",
    val volumeUpJs: String = ""
)