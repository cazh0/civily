package dev.cazh0.civily.core.text

import org.junit.Assert.assertEquals
import org.junit.Test

class ClassificationTest {

    @Test
    fun `a kind of state takes an article`() {
        assertEquals(Classification.Article.A, Classification.article("Corrupt Dictatorship"))
        assertEquals(Classification.Article.A, Classification.article("Capitalist Paradise"))
        assertEquals(Classification.Article.A, Classification.article("Left-Leaning College State"))
        assertEquals(Classification.Article.A, Classification.article("New York Times Democracy"))
    }

    @Test
    fun `a vowel takes an rather than a`() {
        assertEquals(
            Classification.Article.An,
            Classification.article("Inoffensive Centrist Democracy"),
        )
        assertEquals(Classification.Article.An, Classification.article("Anarchy"))
        assertEquals(Classification.Article.An, Classification.article("Authoritarian Democracy"))
    }

    @Test
    fun `a group of people takes none`() {
        assertEquals(Classification.Article.None, Classification.article("Democratic Socialists"))
        assertEquals(Classification.Article.None, Classification.article("Iron Fist Consumerists"))
        assertEquals(
            Classification.Article.None,
            Classification.article("Liberal Democratic Socialists"),
        )
    }

    @Test
    fun `surrounding space does not decide the article`() {
        assertEquals(Classification.Article.An, Classification.article("  Anarchy  "))
    }

    @Test
    fun `an absent classification takes no article`() {
        assertEquals(Classification.Article.None, Classification.article(""))
        assertEquals(Classification.Article.None, Classification.article("   "))
    }
}
