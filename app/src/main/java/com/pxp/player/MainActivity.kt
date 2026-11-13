package com.pxp.player

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import java.io.File

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
        pxp.onOpenWeb = { openWebUrl(it) }
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
        if (e.type == T_GAME) {
            launchBuiltIn(e)
            return
        }
        val mime = if (e.type == T_VIDEO) "video/*" else "audio/*"
        try {
            val i = Intent(Intent.ACTION_VIEW)
            i.setDataAndType(e.uri, mime)
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(i)
        } catch (ex: Exception) {
            pxp.showMsg("No player found.\n(PXP-9002)")
        }
    }

    private fun launchBuiltIn(e: Entry) {
        val core = File(applicationInfo.nativeLibraryDir, "libppsspp_libretro_android.so")
        if (!core.exists()) {
            launchGame(e.uri)
            return
        }
        try {
            val i = Intent(this, GameActivity::class.java)
            i.putExtra("uri", e.uri.toString())
            i.putExtra("name", e.name)
            startActivity(i)
        } catch (ex: Exception) {
            launchGame(e.uri)
        }
    }

    private fun openWebUrl(url: String) {
        try {
            val i = Intent(this, BrowserActivity::class.java)
            i.putExtra("url", url)
            startActivity(i)
        } catch (e: Exception) {
            pxp.showMsg("Could not open browser.\n(PXP-9003)")
        }
    }

    private fun launchGame(uri: Uri) {
        val targets = listOf("org.ppsspp.ppsspp", "org.ppsspp.ppssppgold")
        for (pkg in targets) {
            try {
                val i = Intent(Intent.ACTION_VIEW)
                i.setClassName(pkg, "org.ppsspp.ppsspp.PpssppActivity")
                i.data = uri
                i.putExtra("org.ppsspp.ppsspp.Shortcuts", uri.toString())
                i.addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
                startActivity(i)
                return
            } catch (ex: Exception) {
            }
        }
        pxp.showMsg("Game engine not installed.\n(PXP-9001)")
    }

    private fun scan(uri: Uri) {
        Thread {
            val found = ArrayList<Entry>()
            try {
                walk(uri, DocumentsContract.getTreeDocumentId(uri), found, 0)
            } catch (e: Exception) {
            }
            runOnUiThread { pxp.setLibrary(found) }
        }.start()
    }

    private fun walk(tree: Uri, docId: String, out: MutableList<Entry>, depth: Int) {
        if (depth > 4 || out.size >= 600) return
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, docId)
        val cols = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        val cur = contentResolver.query(children, cols, null, null, null) ?: return
        cur.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0)
                val name = cursor.getString(1) ?: continue
                val mime = cursor.getString(2)
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    walk(tree, id, out, depth + 1)
                } else {
                    val type = typeOf(name)
                    if (type >= 0) {
                        out.add(Entry(name, DocumentsContract.buildDocumentUriUsingTree(tree, id), type))
                    }
                }
            }
        }
    }
}
