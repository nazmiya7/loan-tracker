package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.DashboardSummary
import com.example.ui.components.DashboardSection
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun dashboard_screenshot() {
        val sampleSummary = DashboardSummary(
            currentOutstandingPrincipal = 1156287.0,
            lastRecordedInterestCharged = 4649.0,
            estimatedAnnualRate = 7.35,
            totalPaymentsMade = 378865.0,
            totalInterestCharged = 31799.0,
            initialPrincipal = 1592956.0,
            principalReductionPercent = 27.4
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                DashboardSection(
                    summary = sampleSummary,
                    onUseInSimulator = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
    }
}
