/*
 * Copyright (C) 2023 The LineageOS Project
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

package com.android.systemui.halcyon

import com.android.systemui.qs.QsEventLogger
import com.android.systemui.qs.pipeline.shared.TileSpec
import com.android.systemui.qs.shared.model.TileCategory
import com.android.systemui.qs.tileimpl.QSTileImpl
import com.android.systemui.qs.tiles.AmbientDisplayTile
import com.android.systemui.qs.tiles.AODTile
import com.android.systemui.qs.tiles.CaffeineTile
import com.android.systemui.qs.tiles.CellularTile
import com.android.systemui.qs.tiles.HeadsUpTile
import com.android.systemui.qs.tiles.SyncTile
import com.android.systemui.qs.tiles.WifiTile
import com.android.systemui.qs.tiles.base.shared.model.QSTileConfig;
import com.android.systemui.qs.tiles.base.shared.model.QSTilePolicy;
import com.android.systemui.qs.tiles.base.shared.model.QSTileUIConfig;
import com.android.systemui.res.R

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

@Module
interface QSModule {
    /** Inject AmbientDisplayTile into tileMap in QSModule */
    @Binds
    @IntoMap
    @StringKey(AmbientDisplayTile.TILE_SPEC)
    fun bindAmbientDisplayTile(ambientDisplayTile: AmbientDisplayTile): QSTileImpl<*>

    /** Inject AODTile into tileMap in QSModule */
    @Binds
    @IntoMap
    @StringKey(AODTile.TILE_SPEC)
    fun bindAODTile(aodTile: AODTile): QSTileImpl<*>

    /** Inject CaffeineTile into tileMap in QSModule */
    @Binds
    @IntoMap
    @StringKey(CaffeineTile.TILE_SPEC)
    fun bindCaffeineTile(caffeineTile: CaffeineTile): QSTileImpl<*>

    /** Inject CellularTile into tileMap in QSModule */
    @Binds
    @IntoMap
    @StringKey(CellularTile.TILE_SPEC)
    fun bindCellularTile(cellularTile: CellularTile): QSTileImpl<*>

    /** Inject HeadsUpTile into tileMap in QSModule */
    @Binds
    @IntoMap
    @StringKey(HeadsUpTile.TILE_SPEC)
    fun bindHeadsUpTile(headsUpTile: HeadsUpTile): QSTileImpl<*>

    /** Inject SyncTile into tileMap in QSModule */
    @Binds
    @IntoMap
    @StringKey(SyncTile.TILE_SPEC)
    fun bindSyncTile(syncTile: SyncTile): QSTileImpl<*>

    /** Inject WifiTile into tileMap in QSModule */
    @Binds
    @IntoMap
    @StringKey(WifiTile.TILE_SPEC)
    fun bindWifiTile(wifiTile: WifiTile): QSTileImpl<*>

    companion object {
        @Provides
        @IntoMap
        @StringKey(AmbientDisplayTile.TILE_SPEC)
        fun provideAmbientDisplayTileConfig(uiEventLogger: QsEventLogger): QSTileConfig {
            return QSTileConfig(
                tileSpec = TileSpec.create(AmbientDisplayTile.TILE_SPEC),
                uiConfig = QSTileUIConfig.Resource(
                    iconRes = R.drawable.ic_qs_ambient_display,
                    labelRes = R.string.quick_settings_ambient_display_label
                ),
                instanceId = uiEventLogger.getNewInstanceId(),
                category = TileCategory.DISPLAY
            )
        }

        @Provides
        @IntoMap
        @StringKey(AODTile.TILE_SPEC)
        fun provideAODTileConfig(uiEventLogger: QsEventLogger): QSTileConfig {
            return QSTileConfig(
                tileSpec = TileSpec.create(AODTile.TILE_SPEC),
                uiConfig = QSTileUIConfig.Resource(
                    iconRes = R.drawable.ic_qs_aod,
                    labelRes = R.string.quick_settings_aod_label
                ),
                instanceId = uiEventLogger.getNewInstanceId(),
                category = TileCategory.DISPLAY
            )
        }

        @Provides
        @IntoMap
        @StringKey(CaffeineTile.TILE_SPEC)
        fun provideCaffeineTileConfig(uiEventLogger: QsEventLogger): QSTileConfig {
            return QSTileConfig(
                tileSpec = TileSpec.create(CaffeineTile.TILE_SPEC),
                uiConfig = QSTileUIConfig.Resource(
                    iconRes = R.drawable.ic_qs_caffeine,
                    labelRes = R.string.quick_settings_caffeine_label
                ),
                instanceId = uiEventLogger.getNewInstanceId(),
                category = TileCategory.DISPLAY
            )
        }

        @Provides
        @IntoMap
        @StringKey(HeadsUpTile.TILE_SPEC)
        fun provideHeadsUpTileConfig(uiEventLogger: QsEventLogger): QSTileConfig {
            return QSTileConfig(
                tileSpec = TileSpec.create(HeadsUpTile.TILE_SPEC),
                uiConfig = QSTileUIConfig.Resource(
                    iconRes = R.drawable.ic_qs_heads_up,
                    labelRes = R.string.quick_settings_heads_up_label
                ),
                instanceId = uiEventLogger.getNewInstanceId(),
                category = TileCategory.DISPLAY
            )
        }

        @Provides
        @IntoMap
        @StringKey(SyncTile.TILE_SPEC)
        fun provideSyncTileConfig(uiEventLogger: QsEventLogger): QSTileConfig {
            return QSTileConfig(
                tileSpec = TileSpec.create(SyncTile.TILE_SPEC),
                uiConfig = QSTileUIConfig.Resource(
                    iconRes = R.drawable.ic_qs_sync,
                    labelRes = R.string.quick_settings_sync_label
                ),
                instanceId = uiEventLogger.getNewInstanceId(),
                category = TileCategory.CONNECTIVITY
            )
        }
    }
}
