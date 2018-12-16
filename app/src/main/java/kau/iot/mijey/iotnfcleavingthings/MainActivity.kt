package kau.iot.mijey.iotnfcleavingthings

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.Message
import android.support.customtabs.CustomTabsIntent
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import net.openid.appauth.*

/**
 * Crated by Yeji on 2018-12-16.
 *
 * reference:
 * https://developer.artik.cloud/documentation/tutorials/your-first-android-app.html
 * https://www.codexpedia.com/android/android-nfc-read-and-write-example/
 */

class MainActivity : AppCompatActivity() {
    val ARTIK_TAG = "ArtikAuthTest"
    lateinit var mAuthorizationService: AuthorizationService
    lateinit var mAuthStateDAL: ArtikAuthStateDAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mAuthorizationService = AuthorizationService(this)
        mAuthStateDAL = ArtikAuthStateDAL(this)

        artik_login_button.setOnClickListener {
            try {
                doAuth()
            } catch (e: Exception) {
                Log.d(ARTIK_TAG, "doAuth exception")
                e.printStackTrace()
            }
        }

        nfc_activity_button.setOnClickListener {
            val intent = Intent(this, NFCActivity::class.java)
            startActivity(intent)
        }
    }

    /** Artik authentication **/
    // File OAuth call with Authorization Code with PKCE method
    // https://developer.artik.cloud/documentation/getting-started/authentication.html#authorization-code-with-pkce-method
    private fun doAuth() {
        Log.d(ARTIK_TAG, "doAuth Start")
        val authorizationRequest = ArtikAuthHelper().createAuthorizationRequest()
        val authorizationIntent = PendingIntent.getActivity(
            this,
            authorizationRequest.hashCode(),
            Intent(ArtikAuthHelper.INTENT_ARTIKCLOUD_AUTHORIZATION_RESPONSE, null, this, MainActivity::class.java),
            0)
        Log.d(ARTIK_TAG, "doAuth authorizationIntent: $authorizationIntent")

        /* request sample with custom tabs */
        val builder = CustomTabsIntent.Builder()
        val customTabsIntent = builder.build()

        mAuthorizationService.performAuthorizationRequest(authorizationRequest, authorizationIntent, customTabsIntent)
        Log.d(ARTIK_TAG, "doAuth end")
    }

    override fun onStart() {
        super.onStart()
        checkIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        checkIntent(intent)
    }

    private fun checkIntent(intent: Intent?) {
        if (intent != null) {
            val action = intent.action
            when (action) {
                ArtikAuthHelper.INTENT_ARTIKCLOUD_AUTHORIZATION_RESPONSE -> {
                    Log.d(ARTIK_TAG, "checkIntent action = " + action + " intent.hasExtra(USED_INTENT) = " + intent.hasExtra(ArtikAuthHelper.USED_INTENT))
                    if (!intent.hasExtra(ArtikAuthHelper.USED_INTENT)) {
                        handleAuthorizationResponse(intent)
                        intent.putExtra(ArtikAuthHelper.USED_INTENT, true)
                    }
                }
                else -> Log.d(ARTIK_TAG, "checkIntent action = " + action!!)
            }// do nothing
        } else {
            Log.d(ARTIK_TAG, "checkIntent intent is null!")
        }
    }

    private fun handleAuthorizationResponse(intent: Intent) {
        val response = AuthorizationResponse.fromIntent(intent)
        val error = AuthorizationException.fromIntent(intent)

        Log.d(ARTIK_TAG, "Entering handleAuthorizationResponse with response from Intent = " + response!!.jsonSerialize().toString())

        if (response != null) {
            if (response.authorizationCode != null) { // Authorization Code method: succeeded to get code
                val authState = AuthState(response, error)
                Log.d(ARTIK_TAG, "Received code = " + response.authorizationCode + "\n make another call to get token ...")

                // File 2nd call to get the token
                mAuthorizationService.performTokenRequest(
                    response.createTokenExchangeRequest()
                ) { tokenResponse, exception ->
                    if (tokenResponse != null) {
                        authState.update(tokenResponse, exception)
                        mAuthStateDAL.writeAuthState(authState) //store into persistent storage for use later
                        val text = String.format("Received token response [%s]", tokenResponse.jsonSerializeString())
                        Log.d(ARTIK_TAG, text)
                        startMessageActivity()
                    } else {
                        val context = applicationContext
                        Log.d(ARTIK_TAG, "Token Exchange failed", exception)
                        val text = "Token Exchange failed"
                        val duration = Toast.LENGTH_LONG
                        val toast = Toast.makeText(context, text, duration)
                        toast.show()
                    }
                }
            } else { // come here w/o authorization code. For example, signup finish and user clicks "Back to login"
                Log.d(ARTIK_TAG, "additionalParameter = " + response.additionalParameters.toString())

                if (response.additionalParameters["status"].equals("login_request", ignoreCase = true)) {
                    // ARTIK Cloud instructs the app to display a sign-in form
                    doAuth()
                } else {
                    Log.d(ARTIK_TAG, response.jsonSerialize().toString())
                }
            }

        } else {
            Log.d(ARTIK_TAG, "Authorization Response is null ")
            Log.d(ARTIK_TAG, "Authorization Exception = " + error!!)
        }
    }

    private fun startMessageActivity() {
        val msgActivityIntent = Intent(this, MessageActivity::class.java)
        startActivity(msgActivityIntent)
    }
}
