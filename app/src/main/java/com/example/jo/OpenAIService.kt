package com.example.jo

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

// Request model
data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val max_tokens: Int
)

data class Message(
    val role: String,
    val content: String
)

// Response model
data class ChatCompletionResponse(
    val id: String,
    val `object`: String, // `object` is a reserved keyword, so we use backticks
    val created: Long,
    val model: String,
    val choices: List<Choice>
)

data class Choice(
    val index: Int,
    val message: Message,
    val finish_reason: String
)

interface OpenAIService {

    // Ensure that Authorization header is set correctly
    @Headers("Authorization: Bearer API_KEY") // Replace YOUR_API_KEY with your actual OpenAI API key
    @POST("chat/completions")
    suspend fun getChatCompletion(@Body request: ChatRequest): ChatCompletionResponse
}