/*
 * Copyright (C) 2026 Halcyon Project
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

package com.android.systemui.qs.shared.ui

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import android.os.UserHandle

const val SETTINGS_NESTUI_QS = "nestui_qs_panel"

val LocalIsNestUIEnabled = compositionLocalOf { true }

@Composable
fun rememberIsNestUIEnabled(context: Context): Boolean {
    var isEnabled by remember {
        mutableStateOf(
            Settings.System.getInt(context.contentResolver, SETTINGS_NESTUI_QS, 0) == 0
        )
    }
    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                isEnabled = Settings.System.getInt(
                    context.contentResolver, SETTINGS_NESTUI_QS, 0
                ) == 0
            }

            override fun onChange(selfChange: Boolean, uri: android.net.Uri?) {
                isEnabled = Settings.System.getInt(
                    context.contentResolver, SETTINGS_NESTUI_QS, 0
                ) == 0
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor(SETTINGS_NESTUI_QS), false, observer, UserHandle.USER_ALL
        )
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    return isEnabled
}
