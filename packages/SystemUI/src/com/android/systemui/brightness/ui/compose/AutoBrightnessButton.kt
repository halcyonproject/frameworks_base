/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.brightness.ui.compose

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.UserHandle
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Application
import com.android.systemui.dagger.qualifiers.Background
import com.android.systemui.dagger.qualifiers.Main
import com.android.systemui.res.R
import com.android.systemui.settings.UserTracker
import com.android.systemui.util.settings.SystemSettings
import java.util.concurrent.Executor
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

val LocalAutoBrightnessController = staticCompositionLocalOf<AutoBrightnessController?> { null }

@SysUISingleton
class AutoBrightnessController
@Inject
constructor(
    private val context: Context,
    private val systemSettings: SystemSettings,
    private val userTracker: UserTracker,
    @Application private val applicationScope: CoroutineScope,
    @Background private val bgDispatcher: CoroutineDispatcher,
    @Background private val bgHandler: Handler,
    @Main private val mainExecutor: Executor,
) {
    val isAutoBrightnessAvailable: Boolean =
        context.resources.getBoolean(
            com.android.internal.R.bool.config_automatic_brightness_available
        )

    private val _isAutoBrightnessEnabled = MutableStateFlow(readMode(userTracker.userId))
    val isAutoBrightnessEnabled: StateFlow<Boolean> = _isAutoBrightnessEnabled.asStateFlow()

    private val observer =
        object : ContentObserver(bgHandler) {
            override fun onChange(selfChange: Boolean) {
                updateMode()
            }
        }

    private val userTrackerCallback =
        object : UserTracker.Callback {
            override fun onUserChanged(newUser: Int, userContext: Context) {
                applicationScope.launch(bgDispatcher) {
                    updateMode()
                }
            }
        }

    init {
        if (isAutoBrightnessAvailable) {
            systemSettings.registerContentObserverForUserAsync(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE),
                observer,
                UserHandle.USER_ALL,
            )
            userTracker.addCallback(userTrackerCallback, mainExecutor)
        }
    }

    private fun readMode(userId: Int): Boolean {
        return systemSettings.getIntForUser(
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
            userId,
        ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
    }

    private fun updateMode() {
        val enabled = readMode(userTracker.userId)
        if (_isAutoBrightnessEnabled.value != enabled) {
            _isAutoBrightnessEnabled.value = enabled
        }
    }

    fun toggleAutoBrightness() {
        if (!isAutoBrightnessAvailable) return
        applicationScope.launch(bgDispatcher) {
            val userId = userTracker.userId
            val currentMode =
                systemSettings.getIntForUser(
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                    userId,
                )
            val newMode =
                if (currentMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                } else {
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                }
            systemSettings.putIntForUser(
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                newMode,
                userId,
            )
            updateMode()
        }
    }
}

@Composable
fun AutoBrightnessButton(
    controller: AutoBrightnessController,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
) {
    if (!controller.isAutoBrightnessAvailable) return
    val isAuto by controller.isAutoBrightnessEnabled.collectAsStateWithLifecycle()
    val view = LocalView.current

    val activeBg = MaterialTheme.colorScheme.secondaryContainer
    val inactiveBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val activeFg = MaterialTheme.colorScheme.onSecondaryContainer
    val inactiveFg = MaterialTheme.colorScheme.onSurface

    val bgColor by animateColorAsState(if (isAuto) activeBg else inactiveBg, label = "AutoBrightnessBg")
    val fgColor by animateColorAsState(if (isAuto) activeFg else inactiveFg, label = "AutoBrightnessFg")

    val contentDescription = stringResource(R.string.accessibility_quick_settings_auto_brightness)
    val stateDescription =
        stringResource(
            if (isAuto) R.string.accessibility_auto_brightness_on
            else R.string.accessibility_auto_brightness_off
        )

    Box(
        modifier =
            modifier
                .size(height)
                .clip(CircleShape)
                .background(bgColor)
                .clickable(
                    role = Role.Switch,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        controller.toggleAutoBrightness()
                    },
                )
                .semantics {
                    this.contentDescription = contentDescription
                    this.stateDescription = stateDescription
                    this.role = Role.Switch
                    this.toggleableState = if (isAuto) ToggleableState.On else ToggleableState.Off
                },
        contentAlignment = Alignment.Center,
    ) {
        val iconRes =
            if (isAuto) {
                R.drawable.ic_qs_autobrightness
            } else {
                R.drawable.ic_brightness_full
            }
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = fgColor,
            modifier = Modifier.size(24.dp),
        )
    }
}

