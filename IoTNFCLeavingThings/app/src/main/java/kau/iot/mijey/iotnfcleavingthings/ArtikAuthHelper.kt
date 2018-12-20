package kau.iot.mijey.iotnfcleavingthings

import android.net.Uri
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues

/**
 * Crated by Yeji on 2018-12-16.
 *
 * reference:
 * https://developer.artik.cloud/documentation/tutorials/your-first-android-app.html
 */

class ArtikAuthHelper {
    companion object {
        const val ARTIKCLOUD_AUTHORIZE_URI = "https://accounts.artik.cloud/signin"
        const val ARTIKCLOUD_TOKEN_URI = "https://accounts.artik.cloud/token"
        const val USED_INTENT = "USED_INTENT"

        // 매니페스트에서 <action android:name="kau.iot.mijey.iotnfcleavingthings.ARTIKCLOUD_AUTHORIZATION_RESPONSE"/> 수정해야 함
        const val INTENT_ARTIKCLOUD_AUTHORIZATION_RESPONSE = "kau.iot.mijey.iotnfcleavingthings.ARTIKCLOUD_AUTHORIZATION_RESPONSE"
    }

    fun createAuthorizationRequest(): AuthorizationRequest {
        val serviceConfiguration = AuthorizationServiceConfiguration(
            Uri.parse(ARTIKCLOUD_AUTHORIZE_URI),
            Uri.parse(ARTIKCLOUD_TOKEN_URI),
            null
        )

        val builder = AuthorizationRequest.Builder(
            serviceConfiguration,
            ArtikConfig.CLIENT_ID,
            ResponseTypeValues.CODE,
            Uri.parse(ArtikConfig.REDIRECT_URI)
        )

        return builder.build()
    }
}