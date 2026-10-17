package com.hirahira.snowing.feature.control

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ControlRoute(viewModel: ControlViewModel = viewModel(factory = ControlViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ControlScreen(state = state, onEvent = viewModel::onEvent)
}
