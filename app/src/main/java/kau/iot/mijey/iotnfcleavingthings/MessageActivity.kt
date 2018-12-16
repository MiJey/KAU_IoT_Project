package kau.iot.mijey.iotnfcleavingthings

import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
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

/**
 * Crated by Yeji on 2018-12-16.
 *
 * reference:
 * https://developer.artik.cloud/documentation/tutorials/your-first-android-app.html
 */

class MessageActivity : AppCompatActivity() {
    private val TAG = "MessageActivity"

    private var mUsersApi: UsersApi? = null
    private var mMessagesApi: MessagesApi? = null

    private var mAccessToken: String? = null
    private var mWelcome: TextView? = null
    private var mSendResponse: TextView? = null
    private var mGetLatestResponseId: TextView? = null
    private var mGetLatestResponseData: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)

        val authStateDAL = ArtikAuthStateDAL(this)
        mAccessToken = authStateDAL.readAuthState().getAccessToken()
        Log.v(TAG, "::onCreate get access token = $mAccessToken")

        val sendMsgBtn = findViewById(R.id.send_btn) as Button
        val getLatestMsgBtn = findViewById(R.id.getlatest_btn) as Button
        mWelcome = findViewById(R.id.welcome) as TextView
        mSendResponse = findViewById(R.id.sendmsg_response) as TextView
        mGetLatestResponseId = findViewById(R.id.getlatest_response_mid) as TextView
        mGetLatestResponseData = findViewById(R.id.getlatest_response_mdata) as TextView

        setupArtikCloudApi()

        getUserInfo()

        sendMsgBtn.setOnClickListener {
            Log.v(TAG, ": send button is clicked.")

            // Reset UI
            mSendResponse!!.setText("Response:")

            postMsg()
        }

        getLatestMsgBtn.setOnClickListener {
            Log.v(TAG, ": get latest message button is clicked.")

            // Reset UI
            mGetLatestResponseId!!.setText("id:")
            mGetLatestResponseData!!.setText("data:")

            // Now get the message
            getLatestMsg()
        }
    }

    private fun setupArtikCloudApi() {
        val mApiClient = ApiClient()
        mApiClient.setAccessToken(mAccessToken)

        mUsersApi = UsersApi(mApiClient)
        mMessagesApi = MessagesApi(mApiClient)
    }

    private fun getUserInfo() {
        val tag = "$TAG getSelfAsync"
        try {
            mUsersApi!!.getSelfAsync(object : ApiCallback<UserEnvelope> {
                override fun onFailure(exc: ApiException, statusCode: Int, map: Map<String, List<String>>) {
                    processFailure(tag, exc)
                }

                override fun onSuccess(result: UserEnvelope, statusCode: Int, map: Map<String, List<String>>) {
                    Log.v(TAG, "getSelfAsync::setupArtikCloudApi self name = " + result.data.fullName)
                    updateWelcomeViewOnUIThread("Welcome " + result.data.fullName)
                }

                override fun onUploadProgress(bytes: Long, contentLen: Long, done: Boolean) {}

                override fun onDownloadProgress(bytes: Long, contentLen: Long, done: Boolean) {}
            })
        } catch (exc: ApiException) {
            processFailure(tag, exc)
        }

    }

    private fun getLatestMsg() {
        val tag = "$TAG getLastNormalizedMessagesAsync"
        try {
            val messageCount = 1
            mMessagesApi!!.getLastNormalizedMessagesAsync(messageCount, ArtikConfig.DEVICE_ID, null,
                object : ApiCallback<NormalizedMessagesEnvelope> {
                    override fun onFailure(exc: ApiException, i: Int, stringListMap: Map<String, List<String>>) {
                        processFailure(tag, exc)
                    }

                    override fun onSuccess(
                        result: NormalizedMessagesEnvelope,
                        i: Int,
                        stringListMap: Map<String, List<String>>
                    ) {
                        Log.v(TAG, "getLastNormalizedMessagesAsync onSuccess latestMessage = " + result.data.toString())
                        var mid = ""
                        var data = ""
                        if (!result.data.isEmpty()) {
                            mid = result.data[0].mid
                            data = result.data[0].data.toString()
                        }
                        updateGetResponseOnUIThread(mid, data)
                    }

                    override fun onUploadProgress(bytes: Long, contentLen: Long, done: Boolean) {}

                    override fun onDownloadProgress(bytes: Long, contentLen: Long, done: Boolean) {}
                })

        } catch (exc: ApiException) {
            processFailure(tag, exc)
        }

    }

    private fun postMsg() {
        val tag = "$TAG sendMessageActionAsync"

        val msg = Message()
        msg.sdid = ArtikConfig.DEVICE_ID
        msg.data["things"] = "asdfadfTest"
        //        msg.getData().put("stepCount", 4393);
        //        msg.getData().put("heartRate", 110);
        //        msg.getData().put("description", "Run");
        //        msg.getData().put("activity", 2);

        try {
            mMessagesApi!!.sendMessageAsync(msg, object : ApiCallback<MessageIDEnvelope> {
                override fun onFailure(exc: ApiException, i: Int, stringListMap: Map<String, List<String>>) {
                    processFailure(tag, exc)
                }

                override fun onSuccess(result: MessageIDEnvelope, i: Int, stringListMap: Map<String, List<String>>) {
                    Log.v(TAG, "sendMessageActionAsync onSuccess response to sending message = " + result.data.toString())
                    updateSendResponseOnUIThread(result.data.toString())
                }

                override fun onUploadProgress(bytes: Long, contentLen: Long, done: Boolean) {}

                override fun onDownloadProgress(bytes: Long, contentLen: Long, done: Boolean) {}
            })
        } catch (exc: ApiException) {
            processFailure(tag, exc)
        }

    }


    internal fun showErrorOnUIThread(text: String, activity: Activity) {
        activity.runOnUiThread {
            val duration = Toast.LENGTH_LONG
            val toast = Toast.makeText(activity.applicationContext, text, duration)
            toast.show()
        }
    }

    private fun updateWelcomeViewOnUIThread(text: String) {
        this.runOnUiThread { mWelcome!!.setText(text) }
    }

    private fun updateGetResponseOnUIThread(mid: String, msgData: String) {
        this.runOnUiThread {
            mGetLatestResponseId!!.setText("id:$mid")
            mGetLatestResponseData!!.setText("data:$msgData")
        }
    }

    private fun updateSendResponseOnUIThread(response: String) {
        this.runOnUiThread { mSendResponse!!.setText("Response: $response") }
    }

    private fun processFailure(context: String, exc: ApiException) {
        val errorDetail = " onFailure with exception$exc"
        Log.w(context, errorDetail)
        exc.printStackTrace()
        showErrorOnUIThread(context + errorDetail, this@MessageActivity)
    }
}
