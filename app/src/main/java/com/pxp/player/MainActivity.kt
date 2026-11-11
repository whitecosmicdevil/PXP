
package com.pxp.player

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
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
            i.putExtra(
                DocumentsContract.EXTRA_INITIAL_URI,
                DocumentsContract.buildDocumentUri(
                    "com.android.externalstorage.documents", "primary:"
                )
            )
            startActivityForResult(i, 1)
        }
        setContentView(pxp)
        val saved = getSharedPreferences("pxp", MODE_PRIVATE).getString("card", null)
        if (saved != null) scan(Uri.parse(saved))
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

    private fun scan(uri: Uri) {
        Thread {
            val found = ArrayList<String>()
            try {
                walk(uri, DocumentsContract.getTreeDocumentId(uri), found, 0)
            } catch (e: Exception) {
            }
            runOnUiThread { pxp.setGames(found) }
        }.start()
    }

    private fun walk(tree: Uri, docId: String, out: MutableList<String>, depth: Int) {
        if (depth > 4 || out.size >= 300) return
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, docId)
        val cols = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        val cur = contentResolver.query(children, cols, null, null, null) ?: return
        cur.use {
            while (it.moveToNext()) {
                val id = it.getString(0)
                val name = it.getString(1) ?: continue
                val mime = it.getString(2)
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    walk(tree, id, out, depth + 1)
                } else {
                    val l = name.lowercase()
                    if (l.endsWith(".iso") || l.endsWith(".cso") || l.endsWith(".pbp")) {
                        out.add(name)
                    }
                }
            }
        }
    }
}
