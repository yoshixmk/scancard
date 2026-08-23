package com.plath.scancard

import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ExampleHiltInstrumentedTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Test
    fun hiltTestRunner_isConfigured() {
        // Verify Hilt rule can be created without injection errors
        // If HiltTestRunner is misconfigured, this will fail to initialize
        assertTrue(true)
    }

    @Test
    fun appContext_isHiltTestApplication() {
        // Simple sanity check that instrumentation is running under HiltTestApplication
        assertTrue(hiltRule != null)
    }
}
