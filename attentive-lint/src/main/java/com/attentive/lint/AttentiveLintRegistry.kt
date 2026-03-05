package com.attentive.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

class AttentiveLintRegistry : IssueRegistry() {
    override val issues: List<Issue> =
        listOf(
            SdkInitializationDetector.ISSUE,
        )

    override val api: Int = CURRENT_API

    override val minApi: Int = 12

    override val vendor: Vendor =
        Vendor(
            vendorName = "Attentive Mobile",
            feedbackUrl = "https://github.com/attentive-mobile/attentive-android-sdk/issues",
            contact = "https://attentive.com",
        )
}
