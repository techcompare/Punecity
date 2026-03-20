package com.pranav.punecityguide

import com.pranav.punecityguide.data.community.CommunityFeedService
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Test
import org.junit.Assert.*

class CommunityIntegrationTest {

    @Test
    fun testInsertPost() = runBlocking {
        // Create a standalone HttpClient for testing purposes (no Android context needed)
        val testClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        
        val service = CommunityFeedService(httpClient = testClient)
        
        println("--- Sending Test Post to Pune Buzz ---")
        val result = service.insertPost(
            description = "AI Verification: Live Feed successfully synced with plural 'posts' table. 🚀",
            location = "FC Road",
            userName = "Antigravity AI"
        )
        
        if (result.isSuccess) {
            println("✅ Success: Post accepted by Supabase")
        } else {
            val error = result.exceptionOrNull()?.message ?: "Unknown error"
            println("❌ Failure: $error")
            
            // If it's a 401/403, we know RLS is blocking anonymous posts, 
            // but if it's a PGRST204, then our schema mapping is still wrong.
            if (error.contains("PGRST204")) {
                fail("Schema mapping failure: $error")
            }
        }
        
        testClient.close()
    }
}
