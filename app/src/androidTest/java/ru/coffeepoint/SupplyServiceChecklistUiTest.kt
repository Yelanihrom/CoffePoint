package ru.coffeepoint

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import ru.coffeepoint.feature.service.component.ChecklistRow
import ru.coffeepoint.feature.service.model.buildSupplyServiceChecklist
import ru.coffeepoint.feature.service.model.canFinishSupplyService
import ru.coffeepoint.shared.component.AppDialogButton
import ru.coffeepoint.ui.theme.CoffeePointTheme

class SupplyServiceChecklistUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun content() {
        compose.setContent {
            CoffeePointTheme {
                var items by remember { mutableStateOf(buildSupplyServiceChecklist()) }
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    items.forEachIndexed { index, item ->
                        ChecklistRow(item.checklistItem.checked, item.checklistItem.title,
                            item.checklistItem.description, item.warningText,
                            hintStartPadding = 12.dp, hintEmphasis = item.hintEmphasis) { checked ->
                            items = items.mapIndexed { i, row ->
                                if (i == index) row.copy(checklistItem = row.checklistItem.copy(checked = checked)) else row
                            }
                        }
                    }
                    AppDialogButton("Завершить", enabled = items.canFinishSupplyService(), onClick = {})
                }
            }
        }
    }

    @Test fun newInstructionsExpandIndependentlyAndOnlyMixersHaveHoseWarning() {
        content()
        val mixers = buildSupplyServiceChecklist()[0].checklistItem.description
        val system = buildSupplyServiceChecklist()[1].checklistItem.description
        compose.onNodeWithText("Помыть миксеры").assertIsDisplayed()
        compose.onAllNodesWithText("Не забыть одеть силиконовые шланги").assertCountEquals(1)
        compose.onNodeWithText("Продуть и почистить УЗК").assertDoesNotExist()
        compose.onAllNodes(isToggleable()).assertCountEquals(2)
        compose.onNodeWithText(mixers).assertDoesNotExist()
        compose.onNodeWithText(system).assertDoesNotExist()
        compose.onNodeWithText("Помыть миксеры").performClick()
        compose.onNodeWithText(mixers).assertIsDisplayed()
        compose.onNodeWithText(system).assertDoesNotExist()
        compose.onNodeWithText("Общий тест системы").performScrollTo().performClick()
        compose.onNodeWithText(system).performScrollTo().assertIsDisplayed()
        val hintNode = compose.onNodeWithText(system).fetchSemanticsNode()
        val styled = hintNode.config[SemanticsProperties.Text].single()
        val emphasis = styled.spanStyles.single { it.item.fontWeight == FontWeight.Bold }
        assertEquals("Критерий проверки:", styled.text.substring(emphasis.start, emphasis.end))
        val checkboxLeft = compose.onAllNodes(isToggleable())[1].fetchSemanticsNode().boundsInRoot.left
        assertEquals("Hint must align with checkbox", checkboxLeft, hintNode.boundsInRoot.left, 1f)
    }

    @Test fun completionRequiresBothChecksAndUncheckingBlocksItAgain() {
        content()
        compose.onNodeWithText("ЗАВЕРШИТЬ").assertIsNotEnabled()
        for (index in 0..1) {
            compose.onAllNodes(isToggleable())[index].performClick()
            if (index < 1) compose.onNodeWithText("ЗАВЕРШИТЬ").assertIsNotEnabled()
        }
        compose.onNodeWithText("ЗАВЕРШИТЬ").assertIsEnabled()
        compose.onAllNodes(isToggleable())[1].performClick()
        compose.onNodeWithText("ЗАВЕРШИТЬ").assertIsNotEnabled()
    }
}
