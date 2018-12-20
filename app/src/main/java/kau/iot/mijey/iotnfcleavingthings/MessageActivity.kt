package kau.iot.mijey.iotnfcleavingthings

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.*
import android.nfc.tech.Ndef
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
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
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.io.UnsupportedEncodingException

/**
 * Crated by Yeji on 2018-12-16.
 *
 * reference:
 * https://developer.artik.cloud/documentation/tutorials/your-first-android-app.html
 * https://www.codexpedia.com/android/android-nfc-read-and-write-example/
 */

class MessageActivity : AppCompatActivity() {
    // Artik
    private lateinit var mUsersApi: UsersApi
    private lateinit var mMessagesApi: MessagesApi
    private var mAccessToken: String = ""
    private var havings = JSONArray("[\"my wallet\", \"my car key\", \"my house key\"]")
    private var resetHavings = "[\"my wallet\", \"my car key\", \"my house key\"]"

    // NFC
    private val ERROR_DETECTED = "No NFC tag detected!"
    private val WRITE_SUCCESS = "Text written to the NFC tag successfully!"
    private val WRITE_ERROR = "Error during writing, is the NFC tag close enough to your device?"

    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var pendingIntent: PendingIntent
    private lateinit var writeTagFilters: Array<IntentFilter>

    private var myTag: Tag? = null
    private var writeMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)

        /** Artik **/
        mAccessToken = ArtikAuthStateDAL(this).readAuthState().accessToken ?: ""

        setupArtikCloudApi()
        getUserInfo()
        getLatestMsg()

        //var havingsJson = JSONObject(havings)

        send_msg_button.setOnClickListener { postMsg(resetHavings, true) }
        get_msg_button.setOnClickListener { getLatestMsg() }

        /** NFC **/
        nfc_write_button.setOnClickListener {
            try {
                if (myTag == null) {
                    Toast.makeText(this, ERROR_DETECTED, Toast.LENGTH_SHORT).show()
                } else {
                    write(nfc_write_edit_text.text.toString(), myTag!!)
                    Toast.makeText(this, WRITE_SUCCESS, Toast.LENGTH_SHORT).show()
                }
            }catch (e: IOException) {
                Toast.makeText(this, WRITE_ERROR, Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            } catch (e: FormatException) {
                Toast.makeText(this, WRITE_ERROR, Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_SHORT).show()
            finish()
        }

        readFromIntent(intent)
        pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)
        val tagDetected = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT)
        writeTagFilters = arrayOf(tagDetected)
    }

    /*********************************************************************************************/
    /**************************************** Artik Cloud ****************************************/
    /*********************************************************************************************/

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
                            var data = result.data[0].data.toString()   // leavings

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

    private fun postMsg(nfcTag: String = resetHavings, doGetLastMsg: Boolean = false) {
        val msg = Message()
        msg.sdid = ArtikConfig.DEVICE_ID
        msg.data["leavings"] = nfcTag

        if (nfcTag == resetHavings)
            havings = JSONArray(resetHavings)

        try {
            mMessagesApi.sendMessageAsync(msg, object : ApiCallback<MessageIDEnvelope> {
                override fun onFailure(exc: ApiException, i: Int, stringListMap: Map<String, List<String>>) {
                    toast(exc.toString())
                }

                override fun onSuccess(result: MessageIDEnvelope, i: Int, stringListMap: Map<String, List<String>>) {
                    updateTextViewOnUIThread(send_msg_response_text_view, "Response: ${result.data}")
                    if (doGetLastMsg) getLatestMsg()
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

    /*********************************************************************************************/
    /************************************** NFC Read/Write ***************************************/
    /*********************************************************************************************/

    /** Read from NFC tag **/
    private fun readFromIntent(intent: Intent) {
        val action = intent.action
        if (NfcAdapter.ACTION_TAG_DISCOVERED == action
            || NfcAdapter.ACTION_TECH_DISCOVERED == action
            || NfcAdapter.ACTION_NDEF_DISCOVERED == action) {
            val rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            var msgs: Array<NdefMessage?>? = null

            if (rawMsgs != null) {
                msgs = arrayOfNulls(rawMsgs.size)
                for (i in rawMsgs.indices)
                    msgs[i] = rawMsgs[i] as NdefMessage
            }

            buildTagViews(msgs)
        }
    }

    private fun buildTagViews(msgs: Array<NdefMessage?>?) {
        if (msgs == null || msgs.isEmpty()) return
        if (msgs[0] == null) return

        var text = ""
        val payload = msgs[0]!!.records[0].payload
        val textEncoding = if (payload[0].toInt() and 128 == 0) Charsets.UTF_8 else Charsets.UTF_16 // Get the Text Encoding
        val languageCodeLength = payload[0].toInt() and 51 // Get the Language Code, e.g. "en"

        try {
            // Get the Text
            text = String(payload, languageCodeLength + 1, payload.size - languageCodeLength - 1, textEncoding)
        } catch (e: UnsupportedEncodingException) {
            Log.e("UnsupportedEncoding", e.toString())
        }

        // NFC 태그를 읽으면 Artik Cloud로 보내고 받아옴
        var i = 0
        while(true) {
            if (i >= havings.length())
                break
            var hv = havings.getString(i)
            Log.d("NFCTest", "hv: $hv, text: $text")

            if (hv == text) {
                havings.remove(i)
            } else {
                i += 1
            }
        }
        nfc_read_text_view.text = "NFC Content: $text\nHavings: ${havings.toString()}"

        postMsg(havings.toString(), true)
    }

    /** Write to NFC tag **/
    @Throws(IOException::class, FormatException::class)
    private fun write(text: String, tag: Tag) {
        val records = arrayOf(createRecord(text))
        val message = NdefMessage(records)
        val ndef = Ndef.get(tag)    // Get an instance of Ndef for the tag.

        ndef.connect()  // Enable I/O
        ndef.writeNdefMessage(message)  // Write the message
        ndef.close()    // Close the connection
    }

    @Throws(UnsupportedEncodingException::class)
    private fun createRecord(text: String): NdefRecord {
        val lang = "en"
        val textBytes = text.toByteArray()
        val langBytes = lang.toByteArray(charset("US-ASCII"))
        val langLength = langBytes.size
        val textLength = textBytes.size
        val payload = ByteArray(1 + langLength + textLength)

        // set status byte (see NDEF spec for actual bits)
        payload[0] = langLength.toByte()

        // copy langbytes and textbytes into payload
        System.arraycopy(langBytes, 0, payload, 1, langLength)
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength)

        return NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, ByteArray(0), payload)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent == null) return

        setIntent(intent)
        readFromIntent(intent)
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
            myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }
    }

    override fun onPause() {
        super.onPause()
        writeModeOff()
    }

    override fun onResume() {
        super.onResume()
        writeModeOn()
    }

    /** Enable write **/
    private fun writeModeOn() {
        writeMode = true
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null)
    }

    /** Disable write **/
    private fun writeModeOff() {
        writeMode = false
        nfcAdapter.disableForegroundDispatch(this)
    }
}
