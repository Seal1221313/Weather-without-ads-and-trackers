package io.github.seal1221313.weather

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.widget.Toast
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object Updater {
    const val ACTION = "io.github.seal1221313.weather.INSTALL_STATUS"

    fun install(ctx: Context, url: String, onProgress: (Int) -> Unit) {
        if (!url.startsWith("https://github.com/" + Api.REPO + "/releases/download/")) throw IOException("bad url")
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15000
        c.readTimeout = 30000
        c.setRequestProperty("User-Agent", "WeatherApp/1.0 (Android)")
        try {
            if (c.responseCode != 200) throw IOException("HTTP " + c.responseCode)
            val host = c.url.host
            if (c.url.protocol != "https" || !(host.endsWith("github.com") || host.endsWith("githubusercontent.com"))) {
                throw IOException("bad host")
            }
            val total = c.contentLength
            val installer = ctx.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            params.setAppPackageName(ctx.packageName)
            if (total > 0) params.setSize(total.toLong())
            val id = installer.createSession(params)
            val s = installer.openSession(id)
            try {
                c.inputStream.use { inp ->
                    s.openWrite("weather.apk", 0L, if (total > 0) total.toLong() else -1L).use { out ->
                        val buf = ByteArray(32768)
                        var done = 0L
                        var last = -1
                        while (true) {
                            val n = inp.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            done += n
                            if (total > 0) {
                                val pct = (done * 100 / total).toInt()
                                if (pct != last) {
                                    last = pct
                                    onProgress(pct)
                                }
                            }
                        }
                        s.fsync(out)
                    }
                }
                val i = Intent(ACTION).setPackage(ctx.packageName)
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
                val pend = PendingIntent.getBroadcast(ctx, id, i, flags)
                s.commit(pend.intentSender)
            } catch (e: Exception) {
                s.abandon()
                throw e
            } finally {
                s.close()
            }
        } finally {
            c.disconnect()
        }
    }
}

class InstallReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val st = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
        if (st == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            @Suppress("DEPRECATION")
            val i = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try { ctx.startActivity(i) } catch (e: Exception) { }
            }
        } else if (st != PackageInstaller.STATUS_SUCCESS && st != PackageInstaller.STATUS_FAILURE_ABORTED) {
            Toast.makeText(ctx, R.string.upd_failed, Toast.LENGTH_LONG).show()
        }
    }
}

class ReplacedReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            try {
                val i = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
                if (i != null) {
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ctx.startActivity(i)
                }
            } catch (e: Exception) { }
        }
    }
}
