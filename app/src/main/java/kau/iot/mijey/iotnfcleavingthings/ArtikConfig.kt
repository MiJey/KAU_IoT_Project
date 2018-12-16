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
        // Artik Cloud - Applications - App Info 맨 밑에 AUTH REDIRECT URL 입력하는 칸이 있음
        // Gradle에서 manifestPlaceholders = [appAuthRedirectScheme: "kau.iot.mijey.iotnfcleavingthings://oauth2callback"] 수정해야 함
        // 매니페스트에서 <data android:scheme="kau.iot.mijey.iotnfcleavingthings" android:host="oauth2callback"/> 수정해야 함
        const val REDIRECT_URI = "kau.iot.mijey.iotnfcleavingthings://oauth2callback"
    }
}