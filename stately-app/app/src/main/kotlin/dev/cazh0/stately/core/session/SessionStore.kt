package dev.cazh0.stately.core.session

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Owns the signed-in session and the only copy of the user's credentials.
 *
 * Why a dedicated preferences file rather than the default `PreferenceManager` store the
 * legacy app used: that store also holds every user setting, so anything that backs up,
 * exports or dumps settings carries the autologin token with it. A separate file keeps
 * credentials out of reach of code that has no business seeing them.
 *
 * Why not EncryptedSharedPreferences: app-private storage is already unreadable to other
 * apps, and the threat it would defend against — physical access to a rooted device — also
 * exposes the key. It would add a dependency (spec §1.4) for no change in outcome.
 */
class SessionStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    private val _current = MutableStateFlow(read())

    /** Observed by the UI so sign-out propagates without anyone having to remember to notify. */
    val currentFlow: StateFlow<Session?> = _current.asStateFlow()

    /** Read by the network layer on every request; cheap because it never touches disk. */
    val current: Session? get() = _current.value

    fun signIn(session: Session) = write(session)

    fun signOut() {
        prefs.edit { clear() }
        _current.value = null
    }

    /** NationStates rotates the PIN on most responses; persisting it keeps the session alive. */
    fun updatePin(pin: String) = mutate { it.copy(pin = pin) }

    fun updateAutologin(autologin: String) = mutate { it.copy(autologin = autologin) }

    fun updateRegion(regionId: String) = mutate { it.copy(regionId = regionId) }

    fun updateWaMembership(isMember: Boolean) = mutate { it.copy(isWaMember = isMember) }

    /**
     * Why synchronized rather than [MutableStateFlow.update]: `update` may re-run its lambda
     * under contention, and this one writes to disk. Several responses can carry a rotated PIN
     * at once, so the write has to happen exactly once per change.
     */
    @Synchronized
    private fun mutate(transform: (Session) -> Session) {
        val updated = transform(_current.value ?: return)
        if (updated == _current.value) return
        write(updated)
    }

    @Synchronized
    private fun write(session: Session) {
        persist(session)
        _current.value = session
    }

    private fun persist(session: Session) = prefs.edit {
        putString(KEY_NATION_ID, session.nationId)
        putString(KEY_NATION_NAME, session.nationName)
        putString(KEY_AUTOLOGIN, session.autologin)
        putString(KEY_PIN, session.pin)
        putString(KEY_REGION, session.regionId)
        putBoolean(KEY_WA_MEMBER, session.isWaMember)
    }

    private fun read(): Session? {
        val nationId = prefs.getString(KEY_NATION_ID, null) ?: return null
        val autologin = prefs.getString(KEY_AUTOLOGIN, null) ?: return null
        return Session(
            nationId = nationId,
            nationName = prefs.getString(KEY_NATION_NAME, null) ?: nationId,
            autologin = autologin,
            pin = prefs.getString(KEY_PIN, null),
            regionId = prefs.getString(KEY_REGION, null),
            isWaMember = prefs.getBoolean(KEY_WA_MEMBER, false),
        )
    }

    private companion object {
        const val FILE_NAME = "stately_session"
        const val KEY_NATION_ID = "nation_id"
        const val KEY_NATION_NAME = "nation_name"
        const val KEY_AUTOLOGIN = "autologin"
        const val KEY_PIN = "pin"
        const val KEY_REGION = "region"
        const val KEY_WA_MEMBER = "wa_member"
    }
}
