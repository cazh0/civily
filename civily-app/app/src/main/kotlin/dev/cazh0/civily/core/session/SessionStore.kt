package dev.cazh0.civily.core.session

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists [Accounts] and publishes it. The only copy of the user's credentials.
 *
 * Every rule about which nations exist and which one is active lives in [Accounts]; this class
 * decides nothing. It reads the file once at construction, applies a pure transformation, and
 * writes the result back.
 *
 * Why a dedicated preferences file rather than the default `PreferenceManager` store the
 * legacy app used: that store also holds every user setting, so anything that backs up,
 * exports or dumps settings carries the autologin tokens with it. A separate file keeps
 * credentials out of reach of code that has no business seeing them.
 *
 * Why not EncryptedSharedPreferences: app-private storage is already unreadable to other
 * apps, and the threat it would defend against — physical access to a rooted device — also
 * exposes the key. It would add a dependency (spec §1.4) for no change in outcome.
 */
class SessionStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    private val _accounts = MutableStateFlow(read())

    /**
     * Observed by the UI, so switching and signing out propagate without anyone having to
     * remember to notify.
     */
    val accounts: StateFlow<Accounts> = _accounts.asStateFlow()

    /** Read by the network layer on every request; cheap because it never touches disk. */
    val current: Session? get() = _accounts.value.active

    fun signIn(session: Session) = update { it.with(session) }

    /**
     * Makes another stored nation the one the app acts as. No network: each account keeps its
     * own token and PIN, and the next request re-establishes a session from them if needed.
     */
    fun switchTo(nationId: String) = update { it.switchedTo(nationId) }

    fun forget(nationId: String) = update { it.without(nationId) }

    /** NationStates rotates the PIN on most responses; persisting it keeps the session alive. */
    fun updatePin(pin: String) = update { accounts -> accounts.mapActive { it.copy(pin = pin) } }

    fun updateAutologin(autologin: String) =
        update { accounts -> accounts.mapActive { it.copy(autologin = autologin) } }

    fun updateRegion(regionId: String) =
        update { accounts -> accounts.mapActive { it.copy(regionId = regionId) } }

    fun updateWaMembership(isMember: Boolean) =
        update { accounts -> accounts.mapActive { it.copy(isWaMember = isMember) } }

    /**
     * Why synchronized rather than [MutableStateFlow.update]: `update` may re-run its lambda
     * under contention, and this one writes to disk. Several responses can carry a rotated PIN
     * at once, so the write has to happen exactly once per change.
     */
    @Synchronized
    private fun update(transform: (Accounts) -> Accounts) {
        val updated = transform(_accounts.value)
        if (updated == _accounts.value) return
        persist(updated)
        _accounts.value = updated
    }

    /**
     * Why the file is cleared first: forgetting a nation must leave none of its credentials
     * behind. Rewriting at most five accounts costs less than tracking which keys a change
     * orphaned, and one editor applies as one atomic commit either way.
     */
    private fun persist(accounts: Accounts) = prefs.edit {
        clear()
        putString(KEY_IDS, accounts.all.joinToString(SEPARATOR) { it.nationId })
        putString(KEY_ACTIVE, accounts.activeId)
        accounts.all.forEach { session ->
            putString(key(session.nationId, FIELD_NAME), session.nationName)
            putString(key(session.nationId, FIELD_AUTOLOGIN), session.autologin)
            putString(key(session.nationId, FIELD_PIN), session.pin)
            putString(key(session.nationId, FIELD_REGION), session.regionId)
            putString(key(session.nationId, FIELD_FLAG), session.flagUrl)
            putBoolean(key(session.nationId, FIELD_WA_MEMBER), session.isWaMember)
        }
    }

    private fun read(): Accounts {
        val ids = prefs.getString(KEY_IDS, null)
            ?.split(SEPARATOR)
            ?.filter { it.isNotEmpty() }
            .orEmpty()

        // Why an entry without a token is skipped rather than trusted: this is the one place
        // the app reads state it did not just write, and a nation with no autologin cannot
        // authenticate anything. Listing it would show a signed-in nation whose every request
        // fails, which is exactly what spec §5 forbids.
        val sessions = ids.mapNotNull { id ->
            val autologin = prefs.getString(key(id, FIELD_AUTOLOGIN), null) ?: return@mapNotNull null
            Session(
                nationId = id,
                nationName = prefs.getString(key(id, FIELD_NAME), null) ?: id,
                autologin = autologin,
                pin = prefs.getString(key(id, FIELD_PIN), null),
                regionId = prefs.getString(key(id, FIELD_REGION), null),
                isWaMember = prefs.getBoolean(key(id, FIELD_WA_MEMBER), false),
                flagUrl = prefs.getString(key(id, FIELD_FLAG), null).orEmpty(),
            )
        }

        val activeId = prefs.getString(KEY_ACTIVE, null)
            ?.takeIf { id -> sessions.any { it.nationId == id } }

        return Accounts.of(sessions, activeId)
    }

    private fun key(nationId: String, field: String) = "$nationId.$field"

    private companion object {
        const val FILE_NAME = "civily_session"

        const val KEY_IDS = "accounts"
        const val KEY_ACTIVE = "active"

        const val FIELD_NAME = "name"
        const val FIELD_AUTOLOGIN = "autologin"
        const val FIELD_PIN = "pin"
        const val FIELD_REGION = "region"
        const val FIELD_WA_MEMBER = "wa_member"
        const val FIELD_FLAG = "flag"

        // Why a newline: nation ids come from NsId.fromName, which can leave any punctuation a
        // player used in their name. A newline is the one character it cannot produce.
        const val SEPARATOR = "\n"
    }
}
