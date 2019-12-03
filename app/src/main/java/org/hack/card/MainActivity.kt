package org.hack.card

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentFilter.MalformedMimeTypeException
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.os.Bundle
import android.view.Menu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.io.IOException


class MainActivity : AppCompatActivity() {
    private var btnWrite: FloatingActionButton? = null
    private val info: TextView by lazy { ActivityCompat.requireViewById<TextView>(this, R.id.textView) }
    private val nfcAdapter: NfcAdapter by lazy { (this.getSystemService(Context.NFC_SERVICE) as NfcManager).defaultAdapter }
    private var dump: Dump? = null
    private var writeMode = false
    private var pendingWriteDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        val toolbar: Toolbar = findViewById(R.id.toolbar) as Toolbar
//        setSupportActionBar(toolbar)
        try {
            if (!nfcAdapter.isEnabled) {
                info.setText(R.string.error_nfc_is_disabled)
            }
        } catch (e: Exception) {
            info.setText(R.string.error_nfc_not_available)
        }

        pendingWriteDialog = ProgressDialog(this@MainActivity)
        pendingWriteDialog?.isIndeterminate = true
        pendingWriteDialog?.setMessage("Waiting for card...")
        pendingWriteDialog?.setCancelable(true)
        pendingWriteDialog?.setOnCancelListener { writeMode = false }
        btnWrite = ActivityCompat.requireViewById(this, R.id.btn_write)
        btnWrite?.setOnClickListener {
            writeMode = true
            pendingWriteDialog?.show()
        }
        val btnLoad = ActivityCompat.requireViewById<FloatingActionButton>(this, R.id.btn_load)
        btnLoad.setOnClickListener {
            val intent = Intent(applicationContext, DumpListActivity::class.java)
            startActivityForResult(intent, REQUEST_OPEN_DUMP)
        }
        val startIntent = intent
        if (startIntent != null && startIntent.action == NfcAdapter.ACTION_TECH_DISCOVERED) {
            handleIntent(startIntent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean { // Inflate the menu; this adds items to the action bar if it is present.
//getMenuInflater().inflate(R.menu.menu_main, menu);
        return true
    }

    override fun onResume() {
        super.onResume()
        /**
         * It's important, that the activity is in the foreground (resumed). Otherwise
         * an IllegalStateException is thrown.
         */
        setupForegroundDispatch(this as Activity, nfcAdapter)
    }

    override fun onPause() {
        /**
         * Call this before onPause, otherwise an IllegalArgumentException is thrown as well.
         */
        stopForegroundDispatch(this, nfcAdapter)
        super.onPause()
    }

    @SuppressLint("MissingSuperCall")
    override fun onNewIntent(intent: Intent) {
        /**
         * This method gets called, when a new Intent gets associated with the current activity instance.
         * Instead of creating a new activity, onNewIntent will be called. For more information have a look
         * at the documentation.
         *
         * In our case this method gets called, when the user attaches a Tag to the device.
         */
        handleIntent(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OPEN_DUMP && resultCode == RESULT_OK) {
            if (data != null) {
                handleIntent(data)
            }
        }
    }

    private fun handleIntent(intent: Intent) {
        info.text = ""
        val dumpsDir = applicationContext.getExternalFilesDir(null)
        val action = intent.action
        var shouldSave = false
        try {
            if (NfcAdapter.ACTION_TECH_DISCOVERED == action) {
                val tag = intent.getParcelableExtra<Tag>(
                    NfcAdapter.EXTRA_TAG
                )
                if (writeMode && dump != null) {
                    pendingWriteDialog?.hide()
                    info.append("Writing to card...")
                    dump?.write(tag)
                } else {
                    info.append("Reading from card...")
                    dump = Dump.fromTag(tag)
                    shouldSave = true
                }
            } else if (INTENT_READ_DUMP == action) {
                val file = File(dumpsDir, intent.getStringExtra("filename"))
                info.append("Reading from file...")
                dump = Dump.fromFile(file)
            }
            info.append("\nCard UID: " + dump?.uidAsString)
            info.append("\n\n  --- Sector #8: ---\n")
            val blocks = dump?.dataAsStrings
            if (blocks != null) {
                for (i in blocks.indices) {
                    info.append("\n" + i + "] " + (blocks[i] ?: ""))
                }
            }
            info.append("\n\n  --- Extracted data: ---\n")
            info.append("\nCard number:      " + dump?.cardNumberAsString)
            info.append("\nCurrent balance:  " + dump?.balanceAsString)
//            info.append("\nLast usage date:  " + dump?.lastUsageDateAsString)
            info.append("\nLast validator:   " + dump?.lastValidatorIdAsString)
            if (shouldSave) {
                info.append("\n\n Saving dump ... ")
                val save = dumpsDir?.let { dump?.save(it) }
                info.append("\n " + save?.canonicalPath)
            }
            if (writeMode) {
                info.append("\n\n Successfully wrote this dump!")
            }
        } catch (e: IOException) {
            info.append("\nError: \n$e")
            dump = null
        } finally {
            if (writeMode) {
                writeMode = false
            }
        }
        (if (dump == null) btnWrite?.hide() else btnWrite?.show())
    }

    companion object {
        const val REQUEST_OPEN_DUMP = 1
        const val INTENT_READ_DUMP = "org.hack.card.INTENT_READ_DUMP"
        /**
         * @param activity The corresponding [Activity] requesting the foreground dispatch.
         * @param adapter The [NfcAdapter] used for the foreground dispatch.
         */
        fun setupForegroundDispatch(activity: Activity, adapter: NfcAdapter) {
            val intent = Intent(activity.applicationContext, activity.javaClass)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            val pendingIntent =
                PendingIntent.getActivity(activity.applicationContext, 0, intent, 0)
            val filters = arrayOfNulls<IntentFilter>(1)
            val techList =
                arrayOf(
                    arrayOf(
                        MifareClassic::class.java.name
                    )
                )
            // Notice that this is the same filter as in our manifest.
            filters[0] = IntentFilter()
            filters[0]?.addAction(NfcAdapter.ACTION_TECH_DISCOVERED)
            filters[0]?.addCategory(Intent.CATEGORY_DEFAULT)
            try {
                filters[0]?.addDataType("*/*")
            } catch (e: MalformedMimeTypeException) {
                throw RuntimeException("Check your mime type.")
            }
            adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList)
        }

        /**
         * @param activity The corresponding [Activity] requesting to stop the foreground dispatch.
         * @param adapter The [NfcAdapter] used for the foreground dispatch.
         */
        fun stopForegroundDispatch(activity: Activity?, adapter: NfcAdapter) {
            adapter.disableForegroundDispatch(activity)
        }
    }
}
