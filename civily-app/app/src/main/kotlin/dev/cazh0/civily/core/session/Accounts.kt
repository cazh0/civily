package dev.cazh0.civily.core.session

import java.util.Locale

/**
 * The nations Civily holds a login for, and which of them the app is currently acting as.
 *
 * Every rule about accounts lives here as a pure function, so the store below it is nothing
 * but persistence and a flow. That split is what makes the rules testable without a `Context`.
 *
 * [all] is display order — name, case-insensitively — and stays that way through every
 * transformation. Alphabetical rather than most-recently-used on purpose: a list that reorders
 * itself after each switch moves the row the user is about to press again.
 */
data class Accounts(
    val all: List<Session>,
    val activeId: String?,
) {

    val active: Session? get() = all.firstOrNull { it.nationId == activeId }

    val hasRoom: Boolean get() = all.size < MAX

    /**
     * Adds a nation, or replaces the entry for one already stored, and makes it active.
     *
     * Replacing is the sign-in-again path: a nation whose token was rejected keeps its place
     * in the list rather than appearing twice.
     */
    fun with(session: Session): Accounts {
        // Why a hard failure rather than a silently dropped account: the switcher offers no
        // way in once the list is full, so reaching here means a new route to sign-in skipped
        // that check. A sixth account stored invisibly is the defect this makes impossible.
        require(hasRoom || all.any { it.nationId == session.nationId }) {
            "Signed in as '${session.nationId}' with $MAX accounts already stored"
        }
        return of(all.filterNot { it.nationId == session.nationId } + session, session.nationId)
    }

    /**
     * Forgets a nation's credentials.
     *
     * Forgetting the active one leaves no active account rather than promoting the next in
     * line. This path is also how a rejected token is dropped (spec §5), and quietly changing
     * which nation the app is acting as, in response to a failure, is how a user ends up
     * reading someone else's issues.
     */
    fun without(nationId: String): Accounts = of(
        all.filterNot { it.nationId == nationId },
        activeId.takeIf { it != nationId },
    )

    fun switchedTo(nationId: String): Accounts {
        // Why loud: the only callers pass an id taken from [all], so an unknown one means the
        // switcher and the store have drifted apart.
        require(all.any { it.nationId == nationId }) {
            "Switched to '$nationId', which is not a stored account"
        }
        return copy(activeId = nationId)
    }

    /**
     * Rewrites the active account, leaving the rest untouched.
     *
     * Order survives because every caller updates credentials or session data, never the name.
     * With no active account there is nothing to rewrite, which is the normal state one
     * response after a token was rejected.
     */
    fun mapActive(transform: (Session) -> Session): Accounts {
        val current = active ?: return this
        return copy(all = all.map { if (it.nationId == current.nationId) transform(it) else it })
    }

    companion object {
        /**
         * Why five: enough for the players who run a main, a puppet and a few more, and few
         * enough that the switcher stays a glance rather than a list to scroll. The legacy app
         * stored an unbounded table, which is also what let a stale row sit there for years.
         */
        const val MAX = 5

        val EMPTY = Accounts(emptyList(), null)

        fun of(sessions: List<Session>, activeId: String?) = Accounts(
            all = sessions.sortedBy { it.nationName.lowercase(Locale.US) },
            activeId = activeId,
        )
    }
}
