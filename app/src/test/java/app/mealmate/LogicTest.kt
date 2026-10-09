package app.mealmate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicTest {
    @Test
    fun mifflinNorm() {
        val man = Profile(male = true, age = 30, heightCm = 180, weightKg = 80.0, activity = 0, goal = 1)
        assertEquals(1780.0, Nutrition.bmr(man), 0.01)
        assertEquals(2136.0, Nutrition.target(man).kcal, 0.01)
        val woman = man.copy(male = false)
        assertEquals(1614.0, Nutrition.bmr(woman), 0.01)
        // доли БЖУ в сумме дают всю калорийность
        val t = Nutrition.target(woman.copy(goal = 0))
        assertEquals(t.kcal, t.protein * 4 + t.fat * 9 + t.carbs * 4, 0.01)
    }

    @Test
    fun parsesGeminiResponse() {
        val body = """{"candidates":[{"content":{"parts":[{"text":"skip","thought":true},{"text":"```json\n{\"recipes\":[{\"name\":\"Омлет\",\"kcal\":\"320\",\"protein\":21.5,\"timeMinutes\":10,\"extra\":1,\"description\":null,\"ingredients\":[{\"name\":\"яйца\",\"amount\":\"4 шт\"},{\"name\":\"сыр\",\"have\":false}]}]}\n```"}]},"finishReason":"STOP"}]}"""
        val result = AppJson.decodeFromString<RecipesResult>(Gemini.extractJson(Gemini.parseResponse(body)))
        val recipe = result.recipes.single()
        assertEquals("Омлет", recipe.name)
        assertEquals(320.0, recipe.kcal, 0.0)
        assertEquals(21.5, recipe.protein, 0.0)
        assertEquals("", recipe.description)
        assertTrue(recipe.ingredients[0].have)
        assertFalse(recipe.ingredients[1].have)
    }

    @Test
    fun reportsErrors() {
        assertThrows(GeminiException::class.java) { Gemini.parseResponse("""{"candidates":[]}""") }
        assertTrue("VPN" in Gemini.errorMessage(400, """{"error":{"message":"User location is not supported for the API use."}}"""))
        assertTrue("Ключ" in Gemini.errorMessage(400, """{"error":{"message":"API key not valid."}}"""))
        assertTrue("лимит" in Gemini.errorMessage(429, ""))
    }

    @Test
    fun parsesWikimediaSearch() {
        val body = """{"query":{"pages":{"7":{"index":2,"imageinfo":[{"thumburl":"https://x/second.jpg"}]},"5":{"index":1,"imageinfo":[{"thumburl":"https://x/first.jpg","extmetadata":{"Artist":{"value":"<a href=\"u\">Juerg  Vollmer</a> from Zürich"},"LicenseShortName":{"value":"CC BY-SA 2.0"}}}]}}}}"""
        val photo = Photos.parseSearch(body)
        assertEquals("https://x/first.jpg", photo.url)
        assertEquals("Juerg Vollmer from Zürich, CC BY-SA 2.0, Wikimedia Commons", photo.credit)
        assertEquals("", Photos.parseSearch("""{"batchcomplete":""}""").url)
    }

    @Test
    fun requestCarriesImageAndPrompt() {
        val body = Gemini.requestBody("привет", byteArrayOf(1, 2, 3))
        assertTrue("\"inline_data\"" in body)
        assertTrue("AQID" in body)
        assertTrue("привет" in body)
        assertTrue("application/json" in body)
    }
}
