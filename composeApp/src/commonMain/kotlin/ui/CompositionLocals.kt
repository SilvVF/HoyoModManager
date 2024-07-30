package ui

import androidx.compose.runtime.compositionLocalOf
import core.api.DataApi

val LocalDataApi = compositionLocalOf<DataApi> { error("Not provided") }