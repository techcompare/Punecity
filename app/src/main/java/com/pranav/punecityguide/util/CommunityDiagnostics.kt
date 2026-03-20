package com.pranav.punecityguide.util

import android.util.Log
import com.pranav.punecityguide.data.community.CommunityFeedService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object CommunityDiagnostics {
    private val service = CommunityFeedService()

    fun runTestPost() {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("DIAGNOSTICS", "--- Starting Community Post Test ---")
            val result = service.insertPost(
                description = "AI Diagnostic: Pune Buzz is online! 🚀 #AutomatedTest",
                location = "Deccan",
                userName = "Antigravity AI"
            )
            
            result.onSuccess {
                Log.d("DIAGNOSTICS", "SUCCESS: Test post published successfully!")
            }.onFailure { error ->
                Log.e("DIAGNOSTICS", "FAILURE: Test post failed with: ${error.message}")
            }
        }
    }
}
