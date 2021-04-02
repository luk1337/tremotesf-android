/*
 * Copyright (C) 2017-2021 Alexey Rochev <equeim@gmail.com>
 *
 * This file is part of Tremotesf.
 *
 * Tremotesf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tremotesf is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.equeim.tremotesf.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.equeim.tremotesf.utils.Logger

object AppForegroundTracker : Logger {
    val hasStartedActivity = MutableStateFlow(false)
    val foregroundServiceStarted = MutableStateFlow(false)

    private val scope = GlobalScope + Dispatchers.Unconfined

    val appInForeground = combine(hasStartedActivity, foregroundServiceStarted, Boolean::or)
        .stateIn(scope, SharingStarted.Eagerly, false)

    fun Flow<Boolean>.dropUntilInForeground() = dropWhile { !it }

    init {
        appInForeground
            .onEach { inForeground ->
                if (inForeground) {
                    info("App is in foreground")
                } else {
                    info("App is in background")
                }
            }
            .launchIn(scope)
    }
}
