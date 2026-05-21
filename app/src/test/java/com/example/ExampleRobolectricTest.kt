package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-xhdpi")
class ExampleRobolectricTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testAppFlow() {
        try {
            // App starts on note list, which should be empty initially.
            rule.onNodeWithText("No notes yet. Tap + to start organizing.").assertExists()
            
            // Click FAB to add note
            rule.onNodeWithTag("add_note_fab").performClick()
            rule.waitForIdle()

            // Type title and content
            rule.onNodeWithTag("title_input").performTextInput("My Test Note")
            rule.onNodeWithTag("content_input").performTextInput("This is a test.")
            rule.waitForIdle()

            // Press back to save
            rule.onNodeWithTag("back_button").performClick()
            rule.waitForIdle()

            // Now we should see the note card
            rule.onNodeWithText("My Test Note").assertExists()
            rule.onNodeWithText("This is a test.").assertExists()

            // Open Note
            rule.onNodeWithText("My Test Note").performClick()
            rule.waitForIdle()

            // Open comments
            rule.onNodeWithTag("comments_button").performClick()
            rule.waitForIdle()

            rule.onNodeWithTag("back_button").performClick()
        } catch (e: Exception) {
            println(rule.onRoot().printToString())
            throw e
        } catch (e: AssertionError) {
            println(rule.onRoot().printToString())
            throw e
        }
    }
}
