package com.jder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jder.data.ClassDiagramRepository
import com.jder.data.ERDiagramRepository
import com.jder.data.LogicDiagramRepository
import com.jder.data.RelationalDiagramRepository
import com.jder.data.SequenceDiagramRepository
import com.jder.data.UseCaseDiagramRepository
import com.jder.domain.model.ClassDiagramState
import com.jder.domain.model.DiagramType
import com.jder.domain.model.ERDiagramState
import com.jder.domain.model.LogicDiagramState
import com.jder.domain.model.RelationalDiagramState
import com.jder.domain.model.SequenceDiagramState
import com.jder.domain.model.UseCaseDiagramState
import com.jder.ui.components.DiagramTabRow
import com.jder.ui.dialogs.DiagramTypeDialog
import com.jder.ui.theme.ThemeState

@Composable
fun AppScreen(
    erState: ERDiagramState,
    relationalState: RelationalDiagramState,
    useCaseState: UseCaseDiagramState,
    sequenceState: SequenceDiagramState,
    classState: ClassDiagramState,
    logicState: LogicDiagramState,
    erRepository: ERDiagramRepository,
    relationalRepository: RelationalDiagramRepository,
    useCaseRepository: UseCaseDiagramRepository,
    sequenceRepository: SequenceDiagramRepository,
    classRepository: ClassDiagramRepository,
    logicRepository: LogicDiagramRepository,
    themeState: ThemeState,
    currentType: DiagramType?,
    onTypeChange: (DiagramType) -> Unit,
) {
    if (currentType == null) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        DiagramTypeDialog(themeState = themeState, onTypeSelected = onTypeChange)
        return
    }
    Column(modifier = Modifier.fillMaxSize()) {
        DiagramTabRow(
            currentType = currentType,
            onTypeChange = onTypeChange,
            isModified = { type ->
                when (type) {
                    DiagramType.ER -> erState.isModified
                    DiagramType.RELATIONAL -> relationalState.isModified
                    DiagramType.CLASS_DIAGRAM -> classState.isModified
                    DiagramType.USE_CASE -> useCaseState.isModified
                    DiagramType.SEQUENCE -> sequenceState.isModified
                    DiagramType.LOGIC -> logicState.isModified
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (currentType) {
                DiagramType.ER ->
                    ERDiagramScreen(
                        state = erState,
                        repository = erRepository,
                        themeState = themeState,
                    )
                DiagramType.RELATIONAL ->
                    RelationalDiagramScreen(
                        state = relationalState,
                        repository = relationalRepository,
                        themeState = themeState,
                    )
                DiagramType.CLASS_DIAGRAM ->
                    ClassDiagramScreen(
                        state = classState,
                        repository = classRepository,
                        themeState = themeState,
                    )
                DiagramType.USE_CASE ->
                    UseCaseDiagramScreen(
                        state = useCaseState,
                        repository = useCaseRepository,
                        themeState = themeState,
                    )
                DiagramType.SEQUENCE ->
                    SequenceDiagramScreen(
                        state = sequenceState,
                        repository = sequenceRepository,
                        themeState = themeState,
                    )
                DiagramType.LOGIC ->
                    LogicDiagramScreen(
                        state = logicState,
                        repository = logicRepository,
                        themeState = themeState,
                    )
            }
        }
    }
}
