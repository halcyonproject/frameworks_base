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

package com.android.systemui.qs.panels.style

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.UserHandle
import android.provider.Settings
import androidx.compose.runtime.staticCompositionLocalOf
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Application
import com.android.systemui.dagger.qualifiers.Background
import com.android.systemui.dagger.qualifiers.Main
import com.android.systemui.settings.UserTracker
import com.android.systemui.util.settings.SystemSettings
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * CompositionLocal exposing whether the NestUI QS panel visual style is enabled.
 */
val LocalNestUiQsStyle = staticCompositionLocalOf { false }

/**
 * Single source of truth for the NestUI Quick Settings style.
 *
 * Setting contract (Settings.System "nestui_qs_panel"):
 * - 0 = NestUI (default when key is missing)
 * - 1 = Stock
 */
@SysUISingleton
class NestUiQsStyle
@Inject
constructor(
    private val systemSettings: SystemSettings,
    private val userTracker: UserTracker,
    @Application private val applicationScope: CoroutineScope,
    @Background private val bgDispatcher: CoroutineDispatcher,
    @Background private val bgHandler: Handler,
    @Main private val mainExecutor: Executor,
) {
    interface Listener {
        fun onNestUiChanged(enabled: Boolean)
    }

    private val listeners = CopyOnWriteArrayList<Listener>()

    // Synchronous initial read so the first frame never renders in the wrong style.
    private val _isNestUi =
        MutableStateFlow(readSetting(userTracker.userId))
    val isNestUi: StateFlow<Boolean> = _isNestUi.asStateFlow()

    private val settingObserver =
        object : ContentObserver(bgHandler) {
            override fun onChange(selfChange: Boolean) {
                updateState()
            }
        }

    private val userTrackerCallback =
        object : UserTracker.Callback {
            override fun onUserChanged(newUser: Int, userContext: Context) {
                applicationScope.launch(bgDispatcher) {
                    updateState()
                }
            }
        }

    init {
        // Register ContentObserver for Settings.System key
        systemSettings.registerContentObserverForUserAsync(
            Settings.System.getUriFor(SETTING_KEY),
            settingObserver,
            UserHandle.USER_ALL,
        )

        // Observe user switch to refresh for the newly active user
        userTracker.addCallback(userTrackerCallback, mainExecutor)
    }

    fun isNestUiEnabled(): Boolean = _isNestUi.value

    fun addListener(listener: Listener) {
        listeners.add(listener)
        listener.onNestUiChanged(isNestUiEnabled())
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    private fun readSetting(userId: Int): Boolean {
        // 0 = NestUI (default), 1 = Stock
        return systemSettings.getIntForUser(SETTING_KEY, DEFAULT_VALUE, userId) == 0
    }

    private fun updateState() {
        val currentUserId = userTracker.userId
        val enabled = readSetting(currentUserId)
        if (_isNestUi.value != enabled) {
            _isNestUi.value = enabled
            mainExecutor.execute {
                for (listener in listeners) {
                    listener.onNestUiChanged(enabled)
                }
            }
        }
    }

    companion object {
        const val SETTING_KEY = "nestui_qs_panel"
        const val DEFAULT_VALUE = 0 // 0 = NestUI
    }
}

