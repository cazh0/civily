package dev.cazh0.civily.core.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NsUrlTest {

    @Test
    fun `site form actions stay on the NationStates host`() {
        assertEquals(
            "https://www.nationstates.net/page=enact_dilemma/dilemma=633",
            NsUrl.siteAction("page=enact_dilemma/dilemma=633")?.toString(),
        )
        assertEquals(
            "https://www.nationstates.net/page=enact_dilemma/dilemma=633",
            NsUrl.siteAction("/page=enact_dilemma/dilemma=633")?.toString(),
        )
        assertEquals(
            "https://www.nationstates.net/page=enact_dilemma/dilemma=633",
            NsUrl.siteAction(
                "https://www.nationstates.net/page=enact_dilemma/dilemma=633",
            )?.toString(),
        )
        assertEquals(
            "https://www.nationstates.net/page=enact_dilemma/dilemma=633",
            NsUrl.siteAction(
                "https://nationstates.net/page=enact_dilemma/dilemma=633",
            )?.toString(),
        )
    }

    @Test
    fun `site form actions reject other absolute hosts`() {
        assertNull(NsUrl.siteAction("https://example.com/page=enact_dilemma/dilemma=633"))
        assertNull(NsUrl.siteAction("http://www.nationstates.net/page=enact_dilemma/dilemma=633"))
    }

    @Test
    fun `newspaper artwork ids become slot filenames`() {
        assertEquals(
            "https://www.nationstates.net/images/newspaper/g1-2.jpg",
            NsUrl.newspaperImage("g1", slot = 2),
        )
        assertEquals(
            "https://www.nationstates.net/images/newspaper/g1-1.jpg",
            NsUrl.newspaperImage(" g1 ", slot = 1),
        )
        assertEquals(
            "https://www.nationstates.net/images/newspaper/g1-2.jpg",
            NsUrl.newspaperImage("g1-2.jpg", slot = 2),
        )
    }

    @Test
    fun `site images keep the name the page wrote`() {
        assertEquals(
            "https://www.nationstates.net/images/newspaper/y108-1.jpg",
            NsUrl.siteImage("/images/newspaper/y108-1.jpg"),
        )
        // Not a slot pattern, and not ours to correct: whatever the page names, we fetch.
        assertEquals(
            "https://www.nationstates.net/images/newspaper/p15.jpg",
            NsUrl.siteImage("/images/newspaper/p15.jpg"),
        )
        assertEquals(
            "https://www.nationstates.net/images/flags/Default.svg",
            NsUrl.siteImage("https://nationstates.net/images/flags/Default.svg"),
        )
    }

    @Test
    fun `site images off the NationStates host are refused`() {
        assertNull(NsUrl.siteImage("https://example.com/images/newspaper/y108-1.jpg"))
        assertNull(NsUrl.siteImage("//example.com/images/newspaper/y108-1.jpg"))
        assertNull(NsUrl.siteImage("http://www.nationstates.net/images/newspaper/y108-1.jpg"))
        assertNull(NsUrl.siteImage("  "))
    }
}
