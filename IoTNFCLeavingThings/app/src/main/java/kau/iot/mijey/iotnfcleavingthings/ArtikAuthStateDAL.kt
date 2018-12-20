package kau.iot.mijey.iotnfcleavingthings

import android.app.Activity
import android.content.Context
import android.text.TextUtils
import android.util.Log
import net.openid.appauth.AuthState
import org.json.JSONException

/**
 * Crated by Yeji on 2018-12-16.
 *
 * reference: https://developer.artik.cloud/documentation/tutorials/your-first-android-app.html
 */

class ArtikAuthStateDAL {
    private val AUTH_PREFERENCES_NAME = "AuthStatePreference"
    private val AUTH_STATE = "AUTH_STATE"
    val activity: Activity

    constructor(activity: Activity) {
        this.activity = activity
    }

    fun readAuthState(): AuthState {
        val authPrefs = activity.getSharedPreferences(AUTH_PREFERENCES_NAME, Context.MODE_PRIVATE)
        val stateStr = authPrefs.getString(AUTH_STATE, null)
        if (!TextUtils.isEmpty(stateStr)) {
            try {
                return AuthState.jsonDeserialize(stateStr!!)
            } catch (ignore: JSONException) {
                Log.w("AUTH", ignore.message, ignore)
            }
        }
        return AuthState()
    }

    fun writeAuthState(state: AuthState) {
        val authPrefs = activity.getSharedPreferences(AUTH_PREFERENCES_NAME, Context.MODE_PRIVATE)
        authPrefs.edit()
            .putString(AUTH_STATE, state.jsonSerializeString())
            .apply()
    }

    fun clearAuthState() {
        activity.getSharedPreferences(AUTH_PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(AUTH_STATE)
            .apply()
    }
}