package dev.cazh0.civily.core.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountsTest {

    @Test
    fun `empty has no active account and room to spare`() {
        assertNull(Accounts.EMPTY.active)
        assertTrue(Accounts.EMPTY.hasRoom)
    }

    @Test
    fun `signing in adds the nation and makes it active`() {
        val accounts = Accounts.EMPTY.with(session("testlandia", "Testlandia"))

        assertEquals(listOf("testlandia"), accounts.all.map { it.nationId })
        assertEquals("testlandia", accounts.active?.nationId)
    }

    @Test
    fun `signing in again replaces the stored account rather than duplicating it`() {
        val accounts = Accounts.EMPTY
            .with(session("testlandia", "Testlandia", autologin = "old"))
            .with(session("olympus", "Olympus"))
            .with(session("testlandia", "Testlandia", autologin = "new"))

        assertEquals(2, accounts.all.size)
        assertEquals("new", accounts.active?.autologin)
        assertEquals("testlandia", accounts.activeId)
    }

    @Test
    fun `accounts are held in case-insensitive name order`() {
        val accounts = Accounts.EMPTY
            .with(session("zeta", "zeta"))
            .with(session("alpha", "Alpha"))
            .with(session("mid", "Middle"))

        assertEquals(listOf("Alpha", "Middle", "zeta"), accounts.all.map { it.nationName })
    }

    @Test
    fun `the ceiling is five nations`() {
        val full = fiveAccounts()

        assertFalse(full.hasRoom)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a sixth nation is refused loudly rather than dropped`() {
        fiveAccounts().with(session("sixth", "Sixth"))
    }

    @Test
    fun `a full list still accepts a fresh token for a nation it already holds`() {
        val refreshed = fiveAccounts().with(session("n2", "Nation 2", autologin = "new"))

        assertEquals(Accounts.MAX, refreshed.all.size)
        assertEquals("new", refreshed.active?.autologin)
    }

    @Test
    fun `forgetting the active nation leaves no active nation`() {
        val accounts = Accounts.EMPTY
            .with(session("testlandia", "Testlandia"))
            .with(session("olympus", "Olympus"))
            .without("olympus")

        assertEquals(listOf("testlandia"), accounts.all.map { it.nationId })
        assertNull(accounts.active)
    }

    @Test
    fun `forgetting another nation leaves the active one alone`() {
        val accounts = Accounts.EMPTY
            .with(session("testlandia", "Testlandia"))
            .with(session("olympus", "Olympus"))
            .without("testlandia")

        assertEquals("olympus", accounts.active?.nationId)
    }

    @Test
    fun `forgetting a nation that is not stored changes nothing`() {
        val accounts = Accounts.EMPTY.with(session("testlandia", "Testlandia"))

        assertEquals(accounts, accounts.without("olympus"))
    }

    @Test
    fun `switching changes the active nation and nothing else`() {
        val accounts = Accounts.EMPTY
            .with(session("testlandia", "Testlandia"))
            .with(session("olympus", "Olympus"))
            .switchedTo("testlandia")

        assertEquals("testlandia", accounts.active?.nationId)
        assertEquals(2, accounts.all.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `switching to a nation that is not stored fails loudly`() {
        Accounts.EMPTY.with(session("testlandia", "Testlandia")).switchedTo("olympus")
    }

    @Test
    fun `a rotated pin lands on the active nation only`() {
        val accounts = Accounts.EMPTY
            .with(session("testlandia", "Testlandia", pin = "1"))
            .with(session("olympus", "Olympus", pin = "2"))
            .mapActive { it.copy(pin = "rotated") }

        assertEquals("rotated", accounts.active?.pin)
        assertEquals("1", accounts.all.first { it.nationId == "testlandia" }.pin)
    }

    @Test
    fun `a rotated pin with nobody signed in changes nothing`() {
        val signedOut = Accounts.EMPTY
            .with(session("testlandia", "Testlandia", pin = "1"))
            .without("testlandia")

        assertEquals(signedOut, signedOut.mapActive { it.copy(pin = "rotated") })
    }

    @Test
    fun `an expired pin is not offered as one`() {
        val expired = session("testlandia", "Testlandia", pin = Session.INVALID_PIN)

        assertNull(expired.validPin)
    }

    private fun fiveAccounts(): Accounts =
        (1..Accounts.MAX).fold(Accounts.EMPTY) { accounts, index ->
            accounts.with(session("n$index", "Nation $index"))
        }

    private fun session(
        nationId: String,
        nationName: String,
        autologin: String = "token-$nationId",
        pin: String? = null,
    ) = Session(
        nationId = nationId,
        nationName = nationName,
        autologin = autologin,
        pin = pin,
        regionId = null,
        isWaMember = false,
        flagUrl = "",
    )
}
