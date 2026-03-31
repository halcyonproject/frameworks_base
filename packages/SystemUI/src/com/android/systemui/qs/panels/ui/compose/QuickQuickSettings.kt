/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.systemui.qs.panels.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.compose.animation.scene.ContentScope
import com.android.systemui.compose.modifiers.sysuiResTag
import com.android.systemui.grid.ui.compose.VerticalSpannedGrid
import com.android.systemui.qs.composefragment.ui.GridAnchor
import com.android.systemui.qs.panels.style.LocalNestUiQsStyle
import com.android.systemui.qs.panels.ui.compose.infinitegrid.CommonTileDefaults
import com.android.systemui.qs.panels.ui.compose.infinitegrid.Tile
import com.android.systemui.qs.panels.ui.viewmodel.BounceableTileViewModel
import com.android.systemui.qs.panels.ui.viewmodel.QuickQuickSettingsViewModel
import com.android.systemui.qs.shared.ui.QuickSettings.Elements.toElementKey
import com.android.systemui.res.R

@Composable
fun ContentScope.QuickQuickSettings(
    viewModel: QuickQuickSettingsViewModel,
    modifier: Modifier = Modifier,
    listening: () -> Boolean,
) {
    val columns = viewModel.columns
    val sizedTiles = viewModel.tileViewModels
    val tiles = sizedTiles.fastMap { it.tile }
    val squishiness by viewModel.squishinessViewModel.squishiness.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val isNestUi = LocalNestUiQsStyle.current
    BoxWithConstraints(modifier = modifier) {
        val totalWidth = maxWidth
        val columnSpacing =
            if (isNestUi && columns == 4) {
                val tileHeight = CommonTileDefaults.TileHeight
                val computedGap = (totalWidth - tileHeight * 4) / 3
                if (computedGap >= 8.dp) computedGap
                else dimensionResource(R.dimen.nestui_qs_tile_margin_horizontal)
            } else if (isNestUi) {
                dimensionResource(R.dimen.nestui_qs_tile_margin_horizontal)
            } else {
                dimensionResource(R.dimen.qs_tile_margin_horizontal)
            }
        val rowSpacing =
            if (isNestUi) {
                dimensionResource(R.dimen.nestui_qs_tile_margin_vertical)
            } else {
                dimensionResource(R.dimen.qs_tile_margin_vertical)
            }

        GridAnchor()

        val bounceables =
            remember(sizedTiles) { List(sizedTiles.size) { BounceableTileViewModel() } }
        val spans by remember(sizedTiles) { derivedStateOf { sizedTiles.fastMap { it.width } } }
        VerticalSpannedGrid(
            columns = columns,
            columnSpacing = columnSpacing,
            rowSpacing = rowSpacing,
            spans = spans,
            modifier = Modifier.fillMaxWidth().sysuiResTag("qqs_tile_layout"),
            keys = { sizedTiles[it].tile.spec },
        ) { spanIndex, column, isFirstInColumn, isLastInColumn ->
            val it = sizedTiles[spanIndex]
            Element(it.tile.spec.toElementKey(), Modifier) {
                Tile(
                    tile = it.tile,
                    iconOnly = it.isIcon,
                    squishiness = { squishiness },
                    coroutineScope = scope,
                    bounceableInfo =
                        bounceables.bounceableInfo(
                            it,
                            index = spanIndex,
                            column = column,
                            columns = columns,
                            isFirstInRow = isFirstInColumn,
                            isLastInRow = isLastInColumn,
                        ),
                    tileHapticsViewModelFactory = viewModel.tileHapticsViewModelFactory,
                    // There should be no QuickQuickSettings when the details view is enabled.
                    detailsViewModel = null,
                    isVisible = listening,
                )
            }
        }
    }

    TileListener(tiles, listening)
}
