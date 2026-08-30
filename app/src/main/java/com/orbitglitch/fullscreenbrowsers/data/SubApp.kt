package com.orbitglitch.fullscreenbrowsers.data

import java.util.UUID

/**
 * Represents a sub-app configuration.
 *
 * @param id                    Unique identifier (UUID string).
 * @param title                 Display name shown on the list and home-screen shortcut.
 * @param url                   The URL the full-screen browser will open.
 * @param iconUrl               Optional URL used to fetch the app icon.
 * @param hideNavigationBar     Toggle to hide/show system navigation bar.
 * @param hideStatusBar         Toggle to hide/show status bar.
 * @param punchHolePadding      Padding in dp for punch-hole (0 = fills to top/edge).
 * @param fixDoubleClick        Toggle to ignore rapid double touches at exact coordinates.
 * @param doubleClickThresholdMs Time window (in ms) to ignore duplicate touches.
 * @param volumeDownJs          JavaScript injected when Volume Down is pressed (empty = disabled).
 * @param volumeUpJs            JavaScript injected when Volume Up is pressed (empty = disabled).
 */
data class SubApp(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val url: String = "",
    val iconUrl: String = "",
    val hideNavigationBar: Boolean = true,
    val hideStatusBar: Boolean = true,
    val punchHolePadding: Int = 0,
    val fixDoubleClick: Boolean = true,
    val doubleClickThresholdMs: Int = 150,
    val volumeDownJs: String = "",
    val volumeUpJs: String = ""
)