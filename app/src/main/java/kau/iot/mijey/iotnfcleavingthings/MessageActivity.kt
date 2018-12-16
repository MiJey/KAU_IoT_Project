package kau.iot.mijey.iotnfcleavingthings

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import cloud.artik.api.MessagesApi
import cloud.artik.api.UsersApi
import cloud.artik.client.ApiCallback
import cloud.artik.client.ApiClient
import cloud.artik.client.ApiException
import cloud.artik.model.Message
import cloud.artik.model.MessageIDEnvelope
import cloud.artik.model.NormalizedMessagesEnvelope
import cloud.artik.model.UserEnvelope
import kotlinx.android.synthetic.main.activity_message.*

/**
 * Crated by Yeji on 2018-12-16.
 *
 * reference:
 * https://developer.artik.cloud/documentation/tutorials/your-first-android-app.html
 */

class MessageActivity : AppCompatActivity() {
    private lateinit var mUsersApi: UsersApi
    private lateinit var mMessagesApi: MessagesApi
    private var mAccessToken: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)

        mAccessToken = ArtikAuthStateDAL(this).readAuthState().accessToken ?: ""

        setupArtikCloudApi()
        getUserInfo()

        send_msg_button.setOnClickListener { postMsg() }
        get_msg_button.setOnClickListener { getLatestMsg() }
    }

    private fun setupArtikCloudApi() {
        val mApiClient = ApiClient()
        mApiClient.setAccessToken(mAccessToken)

        mUsersApi = UsersApi(mApiClient)
        mMessagesApi = MessagesApi(mApiClient)
    }

    private fun getUserInfo() {
        try {
            mUsersApi.getSelfAsync(object : ApiCallback<UserEnvelope> {
                override fun onFailure(exc: ApiException, statusCode: Int, map: Map<String, List<String>>) {
                    toast(exc.toString())
                }

                override fun onSuccess(result: UserEnvelope, statusCode: Int, map: Map<String, List<String>>) {
                    updateTextViewOnUIThread(welcome_text_view, "Welcome ${result.data.fullName}")
                }

                override fun onUploadProgress(bytes: Long, contentLen: Long, done: Boolean) {}
                override fun onDownloadProgress(bytes: Long, contentLen: Long, done: Boolean) {}
            })
        } catch (exc: ApiException) {
            toast(exc.toString())
        }

    }

    private fun getLatestMsg() {
        try {
            val messageCount = 1
            mMessagesApi.getLastNormalizedMessagesAsync(messageCount, ArtikConfig.DEVICE_ID, null,
                object : ApiCallback<NormalizedMessagesEnvelope> {
                    override fun onFailure(exc: ApiException, i: Int, stringListMap: Map<String, List<String>>) {
                        toast(exc.toString())
                    }

                    override fun onSuccess(result: NormalizedMessagesEnvelope, i: Int, stringListMap: Map<String, List<String>>) {
                        if (!result.data.isEmpty()) {
                            var mid = result.data[0].mid
                            var data = result.data[0].data.toString()

                            updateTextViewOnUIThread(get_msg_response_id_text_view, "id: $mid")
                            updateTextViewOnUIThread(get_msg_response_data_text_view, "data: $data")
                        }
                    }

                    override fun onUploadProgress(bytes: Long, contentLen: Long, done: Boolean) {}
                    override fun onDownloadProgress(bytes: Long, contentLen: Long, done: Boolean) {}
                })

        } catch (exc: ApiException) {
            toast(exc.toString())
        }
    }

    private fun postMsg() {
        val msg = Message()
        msg.sdid = ArtikConfig.DEVICE_ID
        msg.data["things"] = "test 2018-12-17 00:10"

        try {
            mMessagesApi.sendMessageAsync(msg, object : ApiCallback<MessageIDEnvelope> {
                override fun onFailure(exc: ApiException, i: Int, stringListMap: Map<String, List<String>>) {
                    toast(exc.toString())
                }

                override fun onSuccess(result: MessageIDEnvelope, i: Int, stringListMap: Map<String, List<String>>) {
                    updateTextViewOnUIThread(send_msg_response_text_view, "Response: ${result.data}")
                }

                override fun onUploadProgress(bytes: Long, contentLen: Long, done: Boolean) {}
                override fun onDownloadProgress(bytes: Long, contentLen: Long, done: Boolean) {}
            })
        } catch (exc: ApiException) {
            toast(exc.toString())
        }
    }

    private fun updateTextViewOnUIThread(tv: TextView, text: String) {
        this.runOnUiThread { tv.text = text }
    }

    private fun toast(text: String) {
        this.runOnUiThread { Toast.makeText(this, text, Toast.LENGTH_SHORT).show() }
    }
}
