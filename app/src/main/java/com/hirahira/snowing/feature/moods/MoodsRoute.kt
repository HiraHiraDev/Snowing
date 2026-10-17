package com.hirahira.snowing.feature.moods

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MoodsRoute(viewModel: MoodsViewModel = viewModel(factory = MoodsViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MoodsScreen(state = state, onEvent = viewModel::onEvent)
}
