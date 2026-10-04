package com.abhishek.inkora

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.abhishek.inkora.ui.components.EmptyNotesState
import com.abhishek.inkora.ui.theme.InkoraTheme
import org.junit.Rule
import org.junit.Test

class HomeUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun home_showsBrand_and_emptyState() {
        rule.setContent {
            InkoraTheme {
                EmptyNotesState()
            }
        }
        rule.onNodeWithText("A quiet notebook").assertIsDisplayed()
    }

    @Test fun fab_hasCreateDescription() {
        rule.setContent {
            InkoraTheme {
                com.abhishek.inkora.ui.components.InkoraFab(onClick = {})
            }
        }
        rule.onNodeWithContentDescription("Create new note").assertIsDisplayed()
    }
}
