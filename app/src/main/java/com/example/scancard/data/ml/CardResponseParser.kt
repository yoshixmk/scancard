package com.example.scancard.data.ml

import org.json.JSONArray
import org.json.JSONObject

data class ExtractedCard(val term: String, val definition: String)

class CardResponseParser {
    fun parse(response: String): List<ExtractedCard> {
        // Strategy 1: Direct JSON parse
        try {
            val jsonArray = JSONArray(response)
            return parseJsonArray(jsonArray)
        } catch (e: Exception) {}

        // Strategy 2: Look for JSON array inside text
        try {
            val start = response.indexOf("[")
            val end = response.lastIndexOf("]")
            if (start != -1 && end != -1 && end > start) {
                val jsonPart = response.substring(start, end + 1)
                return parseJsonArray(JSONArray(jsonPart))
            }
        } catch (e: Exception) {}

        // Strategy 3: Regex for "term": "...", "definition": "..."
        try {
            val cards = mutableListOf<ExtractedCard>()
            val pattern = "\"term\"\\s*:\\s*\"([^\"]*)\"\\s*,\\s*\"definition\"\\s*:\\s*\"([^\"]*)\"".toRegex()
            pattern.findAll(response).forEach { match ->
                cards.add(ExtractedCard(match.groupValues[1], match.groupValues[2]))
            }
            if (cards.isNotEmpty()) return cards
        } catch (e: Exception) {}

        return emptyList()
    }

    private fun parseJsonArray(jsonArray: JSONArray): List<ExtractedCard> {
        val result = mutableListOf<ExtractedCard>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            result.add(ExtractedCard(
                obj.getString("term"),
                obj.getString("definition")
            ))
        }
        return result
    }
}
