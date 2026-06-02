package com.example.daywise.data.remote

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken

/**
 * Sealed class representing the result of parsing user input via Gemini.
 */
sealed class ParsedInput {
    data class FoodParsed(val items: List<FoodItem>) : ParsedInput()
    data class ExerciseParsed(val items: List<ExerciseItem>) : ParsedInput()
    data class Unknown(val message: String) : ParsedInput()
    data class Error(val message: String) : ParsedInput()
}

data class FoodItem(
    val name: String,
    val calories: Int,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
    val servingSize: String = ""
)

data class ExerciseItem(
    val name: String,
    val durationMinutes: Int,
    val caloriesBurned: Int
)

/**
 * Service that uses Gemini to parse natural-language food/exercise input into structured data.
 */
class GeminiService(private val apiKey: String) {

    private val model = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = apiKey,
        generationConfig = generationConfig {
            temperature = 0.1f
            maxOutputTokens = 1024
        }
    )

    private val gson = Gson()

    companion object {
        private val SYSTEM_PROMPT = """
You are a nutrition and exercise data parser. The user will describe food they ate or exercise they did in natural language.

Your job is to extract structured data and return ONLY valid JSON — no markdown, no explanation, no extra text.

If the input is about food, return:
{"type": "food", "items": [{"name": "food name", "calories": 100, "protein_g": 10.0, "carbs_g": 20.0, "fat_g": 5.0, "serving_size": "1 bowl (assumed)"}]}

If the input is about exercise, return:
{"type": "exercise", "items": [{"name": "Exercise Name", "duration_minutes": 30, "calories_burned": 300}]}

If the input is a greeting, conversational phrase, single word (like "hello", "wow", "test"), garbage input, or does not describe any specific food or exercise, return:
{"type": "unknown", "message": "I couldn't identify any food or exercise in your message. Try saying something like 'I had 3 scrambled eggs for breakfast' or 'walked for 45 minutes'!"}

Rules:
- Estimate reasonable nutritional values if not provided.
- If the user does not specify a food quantity, you MUST estimate and provide a realistic assumed quantity in the 'serving_size' field, marked with '(assumed)' (e.g., '1 bowl (assumed)', '150g (assumed)'). If they specify it, just provide it (e.g., '3 eggs', '200g').
- Support multiple items in a single input (e.g., "rice and chicken" → 2 food items).
- Return ONLY raw JSON. No markdown code fences, no commentary.
- Do NOT output empty items arrays. If no food or exercise is found, you MUST return the 'unknown' JSON type instead.
        """.trimIndent()
    }

    /**
     * Sends user text to Gemini for parsing into structured food/exercise data.
     */
    suspend fun parseInput(userText: String): ParsedInput {
        val prompt = "$SYSTEM_PROMPT\n\nUser input: $userText"
        return try {
            val response = try {
                model.generateContent(prompt)
            } catch (e: Exception) {
                // If the default model (gemini-2.5-flash) throws an error, try gemini-3.5-flash fallback!
                if (model.modelName == "gemini-2.5-flash") {
                    println("GeminiService: gemini-2.5-flash failed (${e.message}). Retrying query with gemini-3.5-flash fallback model...")
                    val fallbackModel = GenerativeModel(
                        modelName = "gemini-3.5-flash",
                        apiKey = apiKey,
                        generationConfig = model.generationConfig
                    )
                    fallbackModel.generateContent(prompt)
                } else {
                    throw e
                }
            }

            val rawText = response.text?.trim() ?: return ParsedInput.Error("Empty response from Gemini")

            // Strip markdown code fences if present
            val jsonText = rawText
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            parseJsonResponse(jsonText)
        } catch (e: Exception) {
            ParsedInput.Error("Failed to process input: ${e.message}")
        }
    }

    private fun parseJsonResponse(json: String): ParsedInput {
        return try {
            val root = gson.fromJson(json, JsonObject::class.java)
            val type = root.get("type")?.asString ?: return ParsedInput.Unknown("No type field in response")

            when (type) {
                "food" -> {
                    val itemsJson = root.getAsJsonArray("items")?.toString()
                        ?: return ParsedInput.Error("Missing items array for food")
                    val rawItems: List<RawFoodItem> = gson.fromJson(
                        itemsJson,
                        object : TypeToken<List<RawFoodItem>>() {}.type
                    )
                    if (rawItems.isEmpty()) {
                        return ParsedInput.Unknown("I couldn't identify any food items in your message. Try saying something like 'I had 3 scrambled eggs for breakfast'!")
                    }
                    val items = rawItems.map { raw ->
                        FoodItem(
                            name = raw.name,
                            calories = raw.calories,
                            proteinG = raw.protein_g,
                            carbsG = raw.carbs_g,
                            fatG = raw.fat_g,
                            servingSize = raw.serving_size ?: ""
                        )
                    }
                    ParsedInput.FoodParsed(items)
                }

                "exercise" -> {
                    val itemsJson = root.getAsJsonArray("items")?.toString()
                        ?: return ParsedInput.Error("Missing items array for exercise")
                    val rawItems: List<RawExerciseItem> = gson.fromJson(
                        itemsJson,
                        object : TypeToken<List<RawExerciseItem>>() {}.type
                    )
                    if (rawItems.isEmpty()) {
                        return ParsedInput.Unknown("I couldn't identify any exercise items in your message. Try saying something like 'walked for 45 minutes'!")
                    }
                    val items = rawItems.map { raw ->
                        ExerciseItem(
                            name = raw.name,
                            durationMinutes = raw.duration_minutes,
                            caloriesBurned = raw.calories_burned
                        )
                    }
                    ParsedInput.ExerciseParsed(items)
                }

                "unknown" -> {
                    val message = root.get("message")?.asString ?: "Unrecognized input"
                    ParsedInput.Unknown(message)
                }

                else -> ParsedInput.Unknown("Unexpected type: $type")
            }
        } catch (e: JsonSyntaxException) {
            ParsedInput.Error("Failed to parse Gemini response as JSON: ${e.message}")
        } catch (e: Exception) {
            ParsedInput.Error("Error parsing response: ${e.message}")
        }
    }

    // Internal data classes matching the snake_case JSON from Gemini
    private data class RawFoodItem(
        val name: String = "",
        val calories: Int = 0,
        val protein_g: Float = 0f,
        val carbs_g: Float = 0f,
        val fat_g: Float = 0f,
        val serving_size: String = ""
    )

    private data class RawExerciseItem(
        val name: String = "",
        val duration_minutes: Int = 0,
        val calories_burned: Int = 0
    )
}
