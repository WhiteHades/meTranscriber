package com.metranscriber.app.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.metranscriber.app.theme.MeTranscriberTheme
import org.junit.Rule
import org.junit.Test

class TranscriberScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun screen_displaysTitle() {
    composeTestRule.setContent {
      MeTranscriberTheme {
        TranscriberScreen()
      }
    }
    
    // Check for "ME" and "TRANSCRIBER" title parts
    composeTestRule.onNodeWithText("ME").assertExists()
    composeTestRule.onNodeWithText("TRANSCRIBER").assertExists()
  }
}
