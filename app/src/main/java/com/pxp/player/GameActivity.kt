package com.pxp.player

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.InputDevice
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.GLRetroViewData
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class GameActivity : ComponentActivity() {
    private lateinit var root: FrameLayout
    private lateinit var loading: LoadingView
    private var retro: GLRetroView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        val uriStr = intent.getStringExtra("uri")
        val name = intent.getStringExtra("name") ?: "game.iso"

        root = FrameLayout(this)
        root.setBackgroundColor(Color.BLACK)
        loading = LoadingView(this, name)
        root.addView(
            loading,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        setContentView(root)

        if (uriStr == null) {
            fail("No game selected.\n(PXP-9201)")
            return
        }
        Thread { prepare(Uri.parse(uriStr), name) }.start()
    }

    private fun setStatus(label: String, progress: Float) {
        runOnUiThread {
            loading.label = label
            loading.progress = progress
        }
    }

    private fun fail(msg: String) {
        runOnUiThread {
            root.removeAllViews()
            val tv = TextView(this)
            tv.setTextColor(Color.WHITE)
            tv.textSize = 22f
            tv.gravity = Gravity.CENTER
            tv.text = msg + "\n\nTap to go back"
            root.addView(
                tv,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            root.setOnClickListener { finish() }
        }
    }

    private fun prepare(uri: Uri, name: String) {
        try {
            val system = File(filesDir, "system")
            copyAssets(system)
            File(filesDir, "saves").mkdirs()
            val gamesDir = File(cacheDir, "games")
            gamesDir.mkdirs()
            val safe = name.replace(Regex("[^A-Za-z0-9._ -]"), "_")
            val target = File(gamesDir, safe)
            val total = contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
            if (!(target.exists() && total > 0 && target.length() == total)) {
                gamesDir.listFiles()?.forEach { it.delete() }
                copyGame(uri, target, total)
            }
            runOnUiThread { startEngine(target) }
        } catch (e: Exception) {
            fail("Could not load the game.\n(PXP-9202) " + e.javaClass.simpleName)
        }
    }

    private fun copyAssets(system: File) {
        val marker = File(system, "PPSSPP/.pxp_done")
        if (marker.exists()) return
        val top = assets.list("PPSSPP")
        if (top == null || top.isEmpty()) return
        setStatus("Preparing system files...", -1f)
        copyAssetDir("PPSSPP", File(system, "PPSSPP"))
        marker.parentFile?.mkdirs()
        marker.writeText("ok")
    }

    private fun copyAssetDir(path: String, dest: File) {
        val list = assets.list(path) ?: return
        if (list.isEmpty()) {
            dest.parentFile?.mkdirs()
            assets.open(path).use { input ->
                FileOutputStream(dest).use { out -> input.copyTo(out) }
            }
        } else {
            dest.mkdirs()
            for (n in list) copyAssetDir("$path/$n", File(dest, n))
        }
    }

    private fun copyGame(uri: Uri, target: File, total: Long) {
        val tmp = File(target.path + ".part")
        val input = contentResolver.openInputStream(uri) ?: throw IOException("open failed")
        setStatus("Copying game...", if (total > 0) 0f else -1f)
        input.use { src ->
            FileOutputStream(tmp).use { out ->
                val buf = ByteArray(1 shl 20)
                var done = 0L
                var lastPct = -1
                while (true) {
                    val n = src.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    done += n
                    if (total > 0) {
                        val pct = (done * 100 / total).toInt()
                        if (pct != lastPct) {
                            lastPct = pct
                            setStatus("Copying game...", pct / 100f)
                        }
                    }
                }
            }
        }
        if (target.exists()) target.delete()
        tmp.renameTo(target)
    }

    private fun handlePad(view: GLRetroView, code: Int, down: Boolean) {
        if (code == KEY_FAST) {
            view.frameSpeed = if (down) 4 else 1
        } else {
            view.sendKeyEvent(if (down) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP, code)
        }
    }

    private fun startEngine(game: File) {
        try {
            loading.label = "Starting game..."
            loading.progress = 1f
            val data = GLRetroViewData(this)
            data.coreFilePath = "libppsspp_libretro_android.so"
            data.gameFilePath = game.absolutePath
            data.systemDirectory = File(filesDir, "system").absolutePath
            data.savesDirectory = File(filesDir, "saves").absolutePath
            val view = GLRetroView(this, data)
            lifecycle.addObserver(view)
            retro = view
            root.addView(
                view,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            val pad = PadView(this) { code, down -> handlePad(view, code, down) }
            root.addView(
                pad,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            loading.bringToFront()
            loading.animate()
                .alpha(0f)
                .setDuration(600)
                .setStartDelay(1800)
                .withEndAction { root.removeView(loading) }
                .start()
        } catch (e: Exception) {
            fail("Could not start the game engine.\n(PXP-9203) " + e.javaClass.simpleName)
        }
    }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        val view = retro
        val code = e.keyCode
        val isPad = (e.source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
            (e.source and InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD
        if (view != null && isPad && code != KeyEvent.KEYCODE_BACK) {
            if (code == KeyEvent.KEYCODE_BUTTON_R2) {
                if (e.action == KeyEvent.ACTION_DOWN) handlePad(view, KEY_FAST, true)
                if (e.action == KeyEvent.ACTION_UP) handlePad(view, KEY_FAST, false)
                return true
            }
            if (e.repeatCount == 0 &&
                (e.action == KeyEvent.ACTION_DOWN || e.action == KeyEvent.ACTION_UP)
            ) {
                view.sendKeyEvent(e.action, code)
            }
            return true
        }
        return super.dispatchKeyEvent(e)
    }
}
