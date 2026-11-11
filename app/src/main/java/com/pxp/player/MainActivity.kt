package com.pxp.player

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager

class MainActivity : Activity() {
    private lateinit var pxp: PxpView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        pxp = PxpView(this)
        pxp.onPickFolder = {
            val i = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            i.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
            startActivityForResult(i, 1)
        }
        pxp.onOpen = { open(it) }
        setContentView(pxp)
        val saved = getSharedPreferences("pxp", MODE_PRIVATE).getString("card", null)
        if (saved != null) scan(Uri.parse(saved))
    }

    override fun onBackPressed() {
        if (!pxp.goBack()) super.onBackPressed()
    }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if (e.action == KeyEvent.ACTION_DOWN && pxp.onPadKey(e.keyCode)) return true
        return super.dispatchKeyEvent(e)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
            }
            getSharedPreferences("pxp", MODE_PRIVATE)
                .edit().putString("card", uri.toString()).apply()
            scan(uri)
        }
    }

    private fun open(e: Entry) {
        if (e.type
