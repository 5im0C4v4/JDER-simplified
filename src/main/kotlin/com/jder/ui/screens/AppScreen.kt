package com.jder.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jder.data.ERDiagramRepository
import com.jder.domain.model.ERDiagramState
import com.jder.ui.theme.ThemeState

@Composable
fun AppScreen(state: ERDiagramState, repository: ERDiagramRepository, themeState: ThemeState) {
    Column(modifier = Modifier.fillMaxSize()) {
        ERDiagramScreen(state = state, repository = repository, themeState = themeState)
    }
}
