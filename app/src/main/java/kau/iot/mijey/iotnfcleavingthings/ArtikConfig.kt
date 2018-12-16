package kau.iot.mijey.iotnfcleavingthings

/**
 * Crated by Yeji on 2018-12-16.
 *
 * reference:
 * https://developer.artik.cloud/documentation/tutorials/your-first-android-app.html
 */

class ArtikConfig {
    companion object {
        const val CLIENT_ID = "ff3bddf628b5411f96cc556ae2def6f3" // AKA application ID
        const val DEVICE_ID = "2830627e7d9e4f279fda7edafca644a6"

        // MUST be consistent with "AUTH REDIRECT URL" of your application set up at the developer.artik.cloud
        const val REDIRECT_URI = "kau.iot.mijey.iotnfcleavingthings://oauth2callback"
    }
}