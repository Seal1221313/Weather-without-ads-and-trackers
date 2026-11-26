// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (c) 2026 Seal1221313 - see LICENSE and NOTICE

package io.github.seal1221313.weather

import android.app.ActivityManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.provider.Settings
import android.view.View
import java.util.Random
import kotlin.math.sin

class BackgroundView(ctx: Context) : View(ctx) {
    private val d = resources.displayMetrics.density
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rnd = Random(11)
    private val rect = RectF()
    private val low: Boolean
    private val still: Boolean

    private var last = 0L
    private var time = 0f
    private var scroll = 0f

    private var topC = 0xFF2E8BE6.toInt()
    private var botC = 0xFF8FD3FF.toInt()
    private var fromTop = topC
    private var fromBot = botC
    private var toTop = topC
    private var toBot = botC
    private var blend = 1f
    private var inited = false
    private var sky: Shader? = null
    private var sunGlow: Shader? = null
    private var moonGlow: Shader? = null

    private var night = false
    private var fog = false
    private var thunder = false
    private var dark = false
    private var precip = 0
    private var amount = 1
    private var cover = 0

    private val cloudUnit = Path()
    private val moonPath = Path()
    private val bolt = Path()
    private var flash = 0f
    private var nextFlash = 2f

    private var starN = 0
    private var stars = FloatArray(0)
    private var cloudN = 0
    private var cX = FloatArray(0)
    private var cY = FloatArray(0)
    private var cS = FloatArray(0)
    private var cV = FloatArray(0)
    private var cA = FloatArray(0)
    private var rainN = 0
    private var rain = FloatArray(0)
    private var rainLines = FloatArray(0)
    private var snowN = 0
    private var snow = FloatArray(0)

    init {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        low = am.isLowRamDevice || Build.VERSION.SDK_INT < 24
        still = try {
            Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        } catch (e: Exception) {
            false
        }
        cloudUnit.addCircle(-1.1f, 0.15f, 0.55f, Path.Direction.CW)
        cloudUnit.addCircle(-0.35f, -0.3f, 0.82f, Path.Direction.CW)
        cloudUnit.addCircle(0.5f, -0.1f, 0.72f, Path.Direction.CW)
        cloudUnit.addCircle(1.1f, 0.2f, 0.5f, Path.Direction.CW)
        cloudUnit.addRoundRect(RectF(-1.5f, 0.0f, 1.5f, 0.7f), 0.35f, 0.35f, Path.Direction.CW)
        val a = Path()
        a.addCircle(0f, 0f, 1f, Path.Direction.CW)
        val b = Path()
        b.addCircle(0.45f, -0.2f, 0.88f, Path.Direction.CW)
        moonPath.op(a, b, Path.Op.DIFFERENCE)
        setWillNotDraw(false)
    }

    fun setScroll(y: Float) {
        scroll = y
        if (still) invalidate()
    }

    fun setWeather(code: Int, isDay: Boolean, cloudPct: Int) {
        night = !isDay
        fog = code == 45 || code == 48
        thunder = code >= 95
        precip = when (code) {
            in 51..57, in 61..67, in 80..82, in 95..99 -> 1
            in 71..77, 85, 86 -> 2
            else -> 0
        }
        amount = when (code) {
            55, 57, 65, 67, 75, 82, 86, 95, 96, 99 -> 2
            51, 53, 56, 61, 66, 71, 77, 80, 85 -> 0
            else -> 1
        }
        cover = when {
            precip > 0 || fog || code == 3 -> 3
            code == 2 -> 2
            code == 1 -> 1
            cloudPct > 60 -> 2
            cloudPct > 25 -> 1
            else -> 0
        }
        dark = precip > 0

        val pal: LongArray = when {
            thunder -> if (night) longArrayOf(0xFF14151F, 0xFF2E3146) else longArrayOf(0xFF2B2F45, 0xFF59607A)
            precip == 2 -> if (night) longArrayOf(0xFF1E2735, 0xFF4B5A70) else longArrayOf(0xFF7F94AB, 0xFFD5E0EA)
            precip == 1 -> if (night) longArrayOf(0xFF11161F, 0xFF2A3340) else longArrayOf(0xFF3E4C5E, 0xFF7C8B9C)
            fog -> if (night) longArrayOf(0xFF2A2F36, 0xFF4A525C) else longArrayOf(0xFF8A949E, 0xFFC3CAD0)
            cover == 3 -> if (night) longArrayOf(0xFF1C2230, 0xFF3B4558) else longArrayOf(0xFF6B7A8F, 0xFFAEB9C6)
            cover == 2 -> if (night) longArrayOf(0xFF131A33, 0xFF34406B) else longArrayOf(0xFF4A90D9, 0xFFA9C9E8)
            else -> if (night) longArrayOf(0xFF0B1026, 0xFF2B3A67) else longArrayOf(0xFF2E8BE6, 0xFF8FD3FF)
        }
        toTop = pal[0].toInt()
        toBot = pal[1].toInt()
        if (!inited) {
            topC = toTop
            botC = toBot
            blend = 1f
            inited = true
        } else {
            fromTop = topC
            fromBot = botC
            blend = 0f
        }
        rebuildSky()
        seed()
        invalidate()
    }

    private fun rebuildSky() {
        sky = LinearGradient(0f, 0f, 0f, height.toFloat(), topC, botC, Shader.TileMode.CLAMP)
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        val r = w * 0.11f
        sunGlow = RadialGradient(
            0f, 0f, r * 3.4f, intArrayOf(Color.argb(170, 255, 236, 160), Color.argb(0, 255, 236, 160)), null, Shader.TileMode.CLAMP
        )
        moonGlow = RadialGradient(
            0f, 0f, r * 3f, intArrayOf(Color.argb(80, 200, 215, 255), Color.argb(0, 200, 215, 255)), null, Shader.TileMode.CLAMP
        )
        rebuildSky()
        seed()
    }

    private fun seed() {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f) return

        starN = if (low) 25 else 55
        stars = FloatArray(starN * 4)
        for (i in 0 until starN) {
            stars[i * 4] = rnd.nextFloat()
            stars[i * 4 + 1] = rnd.nextFloat()
            stars[i * 4 + 2] = rnd.nextFloat()
            stars[i * 4 + 3] = rnd.nextFloat()
        }

        cloudN = when (cover) {
            0 -> 0
            1 -> 3
            2 -> 5
            else -> if (low) 5 else 8
        }
        cX = FloatArray(cloudN)
        cY = FloatArray(cloudN)
        cS = FloatArray(cloudN)
        cV = FloatArray(cloudN)
        cA = FloatArray(cloudN)
        for (i in 0 until cloudN) {
            val back = i % 2 == 0
            cS[i] = (if (back) 0.17f + rnd.nextFloat() * 0.08f else 0.10f + rnd.nextFloat() * 0.08f) * w
            cX[i] = rnd.nextFloat() * (w + cS[i] * 3f) - cS[i] * 1.5f
            cY[i] = (0.04f + rnd.nextFloat() * 0.4f) * h
            cV[i] = (if (back) 0.006f else 0.012f) + rnd.nextFloat() * 0.006f
            cA[i] = if (back) 0.7f else 1f
        }

        rainN = if (precip == 1) when (amount) { 0 -> 50; 1 -> 110; else -> 200 } else 0
        if (low) rainN /= 2
        rain = FloatArray(rainN * 3)
        rainLines = FloatArray(rainN * 4)
        for (i in 0 until rainN) {
            rain[i * 3] = rnd.nextFloat() * (w + 80f * d)
            rain[i * 3 + 1] = rnd.nextFloat() * h
            rain[i * 3 + 2] = (700f + rnd.nextFloat() * 500f) * d
        }

        snowN = if (precip == 2) when (amount) { 0 -> 50; 1 -> 90; else -> 150 } else 0
        if (low) snowN /= 2
        snow = FloatArray(snowN * 5)
        for (i in 0 until snowN) {
            snow[i * 5] = rnd.nextFloat() * w
            snow[i * 5 + 1] = rnd.nextFloat() * h
            snow[i * 5 + 2] = (30f + rnd.nextFloat() * 50f) * d
            snow[i * 5 + 3] = rnd.nextFloat() * 6.28f
            snow[i * 5 + 4] = (1.3f + rnd.nextFloat() * 2.2f) * d
        }
    }

    private fun cloudColor(): Int = when {
        night && dark -> Color.argb(170, 40, 46, 60)
        night && cover == 3 -> Color.argb(130, 70, 80, 100)
        night -> Color.argb(70, 170, 180, 205)
        dark -> Color.argb(210, 92, 102, 118)
        cover == 3 -> Color.argb(200, 214, 220, 228)
        else -> Color.argb(215, 255, 255, 255)
    }

    private fun buildBolt(w: Float, h: Float) {
        bolt.reset()
        var x = w * (0.2f + rnd.nextFloat() * 0.6f)
        var y = 0f
        bolt.moveTo(x, y)
        while (y < h * 0.5f) {
            y += h * 0.05f + rnd.nextFloat() * h * 0.04f
            x += (rnd.nextFloat() - 0.5f) * w * 0.12f
            bolt.lineTo(x, y)
        }
    }

    override fun onWindowVisibilityChanged(v: Int) {
        super.onWindowVisibilityChanged(v)
        if (v == VISIBLE) {
            last = 0L
            invalidate()
        }
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f) return

        val now = System.nanoTime()
        var dt = if (last == 0L) 0f else (now - last) / 1e9f
        last = now
        if (dt > 0.1f) dt = 0.1f
        if (still) dt = 0f
        time += dt

        if (blend < 1f) {
            blend = if (still) 1f else minOf(1f, blend + dt / 0.9f)
            topC = blendColor(fromTop, toTop, blend)
            botC = blendColor(fromBot, toBot, blend)
            rebuildSky()
        }

        p.style = Paint.Style.FILL
        p.shader = sky
        c.drawRect(0f, 0f, w, h, p)
        p.shader = null

        if (night && cover < 3 && !fog) {
            val k = if (cover == 2) 0.5f else 1f
            for (i in 0 until starN) {
                val ph = stars[i * 4 + 2]
                val tw = 0.5f + 0.5f * sin(time * (1f + ph) + ph * 9f)
                p.color = Color.argb((60 + 190 * tw * k).toInt(), 255, 255, 255)
                c.drawCircle(stars[i * 4] * w, stars[i * 4 + 1] * h * 0.6f - scroll * 0.1f, (0.6f + stars[i * 4 + 3]) * d, p)
            }
        }

        if (cover < 3 && !fog) {
            val r = w * 0.11f
            c.save()
            c.translate(w * 0.78f, h * 0.15f - scroll * 0.25f)
            if (!night) {
                val pulse = 1f + 0.04f * sin(time * 1.2f)
                c.scale(pulse, pulse)
                p.shader = sunGlow
                c.drawCircle(0f, 0f, r * 3.4f, p)
                p.shader = null
                c.save()
                c.rotate(time * 5f)
                p.style = Paint.Style.STROKE
                p.strokeCap = Paint.Cap.ROUND
                p.strokeWidth = r * 0.14f
                p.color = Color.argb(if (cover == 0) 70 else 40, 255, 240, 190)
                for (i in 0 until 12) {
                    c.drawLine(r * 1.5f, 0f, r * (if (i % 2 == 0) 2.4f else 2.0f), 0f, p)
                    c.rotate(30f)
                }
                c.restore()
                p.style = Paint.Style.FILL
                p.color = 0xFFFFF3C4.toInt()
                c.drawCircle(0f, 0f, r, p)
            } else {
                p.shader = moonGlow
                c.drawCircle(0f, 0f, r * 3f, p)
                p.shader = null
                c.rotate(-20f)
                p.color = 0xFFF4F1DE.toInt()
                c.save()
                c.scale(r, r)
                c.drawPath(moonPath, p)
                c.restore()
            }
            c.restore()
        }

        if (cloudN > 0) {
            val base = cloudColor()
            val ba = Color.alpha(base)
            for (i in 0 until cloudN) {
                val s = cS[i]
                cX[i] += cV[i] * w * dt
                if (cX[i] - s * 2f > w) cX[i] = -s * 2f
                p.color = base
                p.alpha = (ba * cA[i]).toInt()
                c.save()
                c.translate(cX[i], cY[i] - scroll * 0.15f)
                c.scale(s, s)
                c.drawPath(cloudUnit, p)
                c.restore()
            }
        }

        if (fog) {
            p.color = Color.argb(if (night) 28 else 50, 255, 255, 255)
            for (k in 0 until 4) {
                val y = h * (0.22f + 0.16f * k)
                val off = sin(time * 0.2f + k * 1.7f) * w * 0.12f
                rect.set(-w * 0.3f + off, y - h * 0.07f, w * 1.3f + off, y + h * 0.07f)
                c.drawOval(rect, p)
            }
        }

        if (rainN > 0) {
            for (i in 0 until rainN) {
                val j = i * 3
                val v = rain[j + 2]
                rain[j + 1] += v * dt
                rain[j] -= v * 0.18f * dt
                val len = v * 0.035f
                if (rain[j + 1] > h + len) {
                    rain[j + 1] = -len
                    rain[j] = rnd.nextFloat() * (w + 80f * d)
                }
                if (rain[j] < -40f * d) rain[j] += w + 80f * d
                val k = i * 4
                rainLines[k] = rain[j]
                rainLines[k + 1] = rain[j + 1]
                rainLines[k + 2] = rain[j] + len * 0.18f
                rainLines[k + 3] = rain[j + 1] - len
            }
            p.style = Paint.Style.STROKE
            p.strokeCap = Paint.Cap.ROUND
            p.strokeWidth = 1.4f * d
            p.color = Color.argb(120, 205, 225, 255)
            c.drawLines(rainLines, 0, rainN * 4, p)
            p.style = Paint.Style.FILL
        }

        if (snowN > 0) {
            p.style = Paint.Style.FILL
            p.color = Color.argb(225, 255, 255, 255)
            for (i in 0 until snowN) {
                val j = i * 5
                snow[j + 1] += snow[j + 2] * dt
                snow[j] += sin(time * 0.8f + snow[j + 3]) * 22f * d * dt
                if (snow[j + 1] > h + 10f) {
                    snow[j + 1] = -10f
                    snow[j] = rnd.nextFloat() * w
                }
                c.drawCircle(snow[j], snow[j + 1], snow[j + 4], p)
            }
        }

        if (thunder && !still) {
            if (time > nextFlash) {
                flash = 1f
                nextFlash = time + 3f + rnd.nextFloat() * 6f
                buildBolt(w, h)
            }
            if (flash > 0f) {
                p.style = Paint.Style.FILL
                p.color = Color.argb((flash * 70).toInt(), 255, 255, 255)
                c.drawRect(0f, 0f, w, h, p)
                if (flash > 0.55f) {
                    p.style = Paint.Style.STROKE
                    p.strokeWidth = 3f * d
                    p.strokeJoin = Paint.Join.ROUND
                    p.color = Color.argb(230, 255, 255, 255)
                    c.drawPath(bolt, p)
                    p.style = Paint.Style.FILL
                }
                flash -= dt * 2.2f
            }
        }

        if (!still && windowVisibility == VISIBLE) {
            if (low) postInvalidateDelayed(40) else postInvalidateOnAnimation()
        }
    }
}
