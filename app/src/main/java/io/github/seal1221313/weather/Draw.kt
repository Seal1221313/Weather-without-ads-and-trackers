package io.github.seal1221313.weather

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.view.View

fun blendColor(a: Int, b: Int, t: Float): Int {
    val k = if (t < 0f) 0f else if (t > 1f) 1f else t
    return Color.argb(
        (Color.alpha(a) + (Color.alpha(b) - Color.alpha(a)) * k).toInt(),
        (Color.red(a) + (Color.red(b) - Color.red(a)) * k).toInt(),
        (Color.green(a) + (Color.green(b) - Color.green(a)) * k).toInt(),
        (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * k).toInt()
    )
}

fun tempColor(c: Double): Int {
    val stops = doubleArrayOf(-15.0, 0.0, 10.0, 20.0, 30.0, 38.0)
    val cols = intArrayOf(
        0xFF7C9CFF.toInt(), 0xFF4DC3FF.toInt(), 0xFF5FE3B0.toInt(),
        0xFFFFD84D.toInt(), 0xFFFF9340.toInt(), 0xFFFF5E5E.toInt()
    )
    if (c <= stops[0]) return cols[0]
    for (i in 1 until stops.size) {
        if (c <= stops[i]) return blendColor(cols[i - 1], cols[i], ((c - stops[i - 1]) / (stops[i] - stops[i - 1])).toFloat())
    }
    return cols[cols.size - 1]
}

object IconPainter {
    private val cloudUnit = Path()
    private val moonUnit = Path()
    private val bolt = Path()

    init {
        cloudUnit.addCircle(-1.1f, 0.15f, 0.55f, Path.Direction.CW)
        cloudUnit.addCircle(-0.35f, -0.3f, 0.82f, Path.Direction.CW)
        cloudUnit.addCircle(0.5f, -0.1f, 0.72f, Path.Direction.CW)
        cloudUnit.addCircle(1.1f, 0.2f, 0.5f, Path.Direction.CW)
        cloudUnit.addRoundRect(RectF(-1.5f, 0.0f, 1.5f, 0.7f), 0.35f, 0.35f, Path.Direction.CW)

        val a = Path()
        a.addCircle(0f, 0f, 1f, Path.Direction.CW)
        val b = Path()
        b.addCircle(0.5f, -0.3f, 0.85f, Path.Direction.CW)
        moonUnit.op(a, b, Path.Op.DIFFERENCE)

        bolt.moveTo(0.10f, 0.35f)
        bolt.lineTo(-0.22f, 0.75f)
        bolt.lineTo(0.0f, 0.75f)
        bolt.lineTo(-0.12f, 1.05f)
        bolt.lineTo(0.28f, 0.60f)
        bolt.lineTo(0.06f, 0.60f)
        bolt.lineTo(0.22f, 0.35f)
        bolt.close()
    }

    fun sun(c: Canvas, cx: Float, cy: Float, rad: Float, p: Paint) {
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = rad * 0.28f
        p.color = 0xFFFFC93C.toInt()
        for (i in 0 until 8) {
            val a = Math.PI / 4.0 * i
            val ca = Math.cos(a).toFloat()
            val sa = Math.sin(a).toFloat()
            c.drawLine(cx + ca * rad * 1.5f, cy + sa * rad * 1.5f, cx + ca * rad * 2.0f, cy + sa * rad * 2.0f, p)
        }
        p.style = Paint.Style.FILL
        c.drawCircle(cx, cy, rad, p)
    }

    fun moon(c: Canvas, cx: Float, cy: Float, rad: Float, p: Paint) {
        p.style = Paint.Style.FILL
        p.color = 0xFFEDEFF9.toInt()
        c.save()
        c.translate(cx, cy)
        c.rotate(-15f)
        c.scale(rad, rad)
        c.drawPath(moonUnit, p)
        c.restore()
    }

    fun cloud(c: Canvas, cx: Float, cy: Float, r: Float, color: Int, p: Paint) {
        val sc = r * 0.57f
        p.style = Paint.Style.FILL
        p.color = color
        c.save()
        c.translate(cx, cy + 0.2f * sc)
        c.scale(sc, sc)
        c.drawPath(cloudUnit, p)
        c.restore()
    }

    private fun drops(c: Canvas, cx: Float, cy: Float, r: Float, p: Paint, n: Int) {
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = r * 0.12f
        p.color = 0xFF6CB8FF.toInt()
        val xs = if (n == 2) floatArrayOf(-0.25f, 0.25f) else floatArrayOf(-0.5f, 0f, 0.5f)
        for (x in xs) {
            val x0 = cx + x * r
            c.drawLine(x0 + r * 0.14f, cy + r * 0.45f, x0, cy + r * 0.8f, p)
        }
        p.style = Paint.Style.FILL
    }

    fun draw(c: Canvas, ic: Icon, cx: Float, cy: Float, r: Float, p: Paint) {
        p.shader = null
        val white = 0xFFF4F7FA.toInt()
        val gray = 0xFFC3CCD8.toInt()
        when (ic) {
            Icon.CLEAR_DAY -> sun(c, cx, cy, r * 0.46f, p)
            Icon.CLEAR_NIGHT -> moon(c, cx, cy, r * 0.7f, p)
            Icon.PARTLY_DAY -> {
                sun(c, cx - r * 0.3f, cy - r * 0.3f, r * 0.3f, p)
                cloud(c, cx + r * 0.12f, cy + r * 0.2f, r * 0.8f, white, p)
            }
            Icon.PARTLY_NIGHT -> {
                moon(c, cx - r * 0.3f, cy - r * 0.3f, r * 0.45f, p)
                cloud(c, cx + r * 0.12f, cy + r * 0.2f, r * 0.8f, white, p)
            }
            Icon.CLOUDY -> {
                cloud(c, cx + r * 0.25f, cy - r * 0.25f, r * 0.65f, gray, p)
                cloud(c, cx - r * 0.05f, cy + r * 0.1f, r * 0.9f, white, p)
            }
            Icon.FOG -> {
                cloud(c, cx, cy - r * 0.25f, r * 0.9f, gray, p)
                p.style = Paint.Style.STROKE
                p.strokeCap = Paint.Cap.ROUND
                p.strokeWidth = r * 0.12f
                p.color = 0xCCF4F7FA.toInt()
                c.drawLine(cx - r * 0.5f, cy + r * 0.5f, cx + r * 0.5f, cy + r * 0.5f, p)
                c.drawLine(cx - r * 0.3f, cy + r * 0.75f, cx + r * 0.7f, cy + r * 0.75f, p)
                c.drawLine(cx - r * 0.6f, cy + r * 1.0f, cx + r * 0.3f, cy + r * 1.0f, p)
                p.style = Paint.Style.FILL
            }
            Icon.DRIZZLE -> {
                cloud(c, cx, cy - r * 0.2f, r * 0.9f, gray, p)
                drops(c, cx, cy, r, p, 2)
            }
            Icon.RAIN -> {
                cloud(c, cx, cy - r * 0.2f, r * 0.9f, gray, p)
                drops(c, cx, cy, r, p, 3)
            }
            Icon.SNOW -> {
                cloud(c, cx, cy - r * 0.2f, r * 0.9f, gray, p)
                p.style = Paint.Style.FILL
                p.color = 0xFFFFFFFF.toInt()
                c.drawCircle(cx - r * 0.5f, cy + r * 0.6f, r * 0.1f, p)
                c.drawCircle(cx, cy + r * 0.8f, r * 0.1f, p)
                c.drawCircle(cx + r * 0.5f, cy + r * 0.6f, r * 0.1f, p)
            }
            Icon.THUNDER -> {
                cloud(c, cx, cy - r * 0.25f, r * 0.9f, 0xFF9AA5B5.toInt(), p)
                p.style = Paint.Style.FILL
                p.color = 0xFFFFD24A.toInt()
                c.save()
                c.translate(cx, cy)
                c.scale(r, r)
                c.drawPath(bolt, p)
                c.restore()
            }
        }
        p.style = Paint.Style.FILL
    }
}

class Glyph(ctx: Context, private val kind: Int) : View(ctx) {
    private val d = resources.displayMetrics.density
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    override fun onDraw(c: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        p.strokeWidth = 2f * d
        p.color = Color.WHITE
        when (kind) {
            0 -> {
                c.drawCircle(cx - 2f * d, cy - 2f * d, 6f * d, p)
                c.drawLine(cx + 2.5f * d, cy + 2.5f * d, cx + 8f * d, cy + 8f * d, p)
            }
            1 -> {
                val r = 7f * d
                c.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), 40f, 280f, false, p)
                val th = Math.toRadians(320.0)
                val px = cx + (r * Math.cos(th)).toFloat()
                val py = cy + (r * Math.sin(th)).toFloat()
                val nx = Math.cos(th).toFloat()
                val ny = Math.sin(th).toFloat()
                val tx = -ny
                val ty = nx
                path.reset()
                path.moveTo(px + tx * 5f * d, py + ty * 5f * d)
                path.lineTo(px + nx * 4f * d, py + ny * 4f * d)
                path.lineTo(px - nx * 4f * d, py - ny * 4f * d)
                path.close()
                p.style = Paint.Style.FILL
                c.drawPath(path, p)
            }
            else -> {
                path.reset()
                path.moveTo(cx - 5f * d, cy - 2f * d)
                path.lineTo(cx, cy + 3f * d)
                path.lineTo(cx + 5f * d, cy - 2f * d)
                c.drawPath(path, p)
            }
        }
    }
}

class HourlyView(ctx: Context) : View(ctx) {
    private val d = resources.displayMetrics.density
    private val sd = resources.displayMetrics.scaledDensity
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val fill = Path()
    private var hours: List<Hour> = emptyList()
    private var fmt: Fmt? = null
    private var nowLabel = ""
    private val colW = 58f * d

    fun set(h: List<Hour>, f: Fmt, now: String) {
        hours = h
        fmt = f
        nowLabel = now
        requestLayout()
        invalidate()
    }

    override fun onMeasure(wm: Int, hm: Int) {
        setMeasuredDimension(maxOf(1, (hours.size * colW).toInt()), (178f * d).toInt())
    }

    override fun onDraw(c: Canvas) {
        val f = fmt ?: return
        if (hours.isEmpty()) return
        var mn = Double.MAX_VALUE
        var mx = -Double.MAX_VALUE
        for (h in hours) {
            if (!h.temp.isNaN()) {
                if (h.temp < mn) mn = h.temp
                if (h.temp > mx) mx = h.temp
            }
        }
        if (mn > mx) return
        val top = 114f * d
        val bot = 160f * d
        val span = if (mx - mn < 1.0) 1.0 else mx - mn
        val ys = FloatArray(hours.size)
        for (i in hours.indices) {
            val t = if (hours[i].temp.isNaN()) mn else hours[i].temp
            ys[i] = (bot - ((t - mn) / span) * (bot - top)).toFloat()
        }

        path.reset()
        for (i in hours.indices) {
            val x = colW * (i + 0.5f)
            if (i == 0) path.moveTo(x, ys[0]) else {
                val px = colW * (i - 0.5f)
                val mid = (px + x) / 2f
                path.cubicTo(mid, ys[i - 1], mid, ys[i], x, ys[i])
            }
        }
        fill.set(path)
        fill.lineTo(colW * (hours.size - 0.5f), bot + 14f * d)
        fill.lineTo(colW * 0.5f, bot + 14f * d)
        fill.close()
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, top, 0f, bot + 14f * d, 0x40FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        c.drawPath(fill, p)
        p.shader = null
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2.2f * d
        p.strokeCap = Paint.Cap.ROUND
        p.color = Color.WHITE
        c.drawPath(path, p)

        p.textAlign = Paint.Align.CENTER
        for (i in hours.indices) {
            val h = hours[i]
            val x = colW * (i + 0.5f)

            p.style = Paint.Style.FILL
            p.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            p.color = 0xCCFFFFFF.toInt()
            p.textSize = 12f * sd
            val label = if (i == 0) nowLabel else if (h.time.length >= 16) h.time.substring(11, 16) else ""
            c.drawText(label, x, 20f * d, p)

            IconPainter.draw(c, Wmo.icon(h.code, h.isDay), x, 52f * d, 13f * d, p)

            if (h.pop >= 10) {
                p.style = Paint.Style.FILL
                p.color = 0xFF8FD0FF.toInt()
                p.textSize = 11f * sd
                c.drawText(h.pop.toString() + "%", x, 84f * d, p)
            }

            p.style = Paint.Style.FILL
            p.color = Color.WHITE
            c.drawCircle(x, ys[i], 3.2f * d, p)
            p.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            p.textSize = 14f * sd
            c.drawText(f.temp(h.temp), x, ys[i] - 10f * d, p)
        }
    }
}

class DailyView(ctx: Context) : View(ctx) {
    private val d = resources.displayMetrics.density
    private val sd = resources.displayMetrics.scaledDensity
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var days: List<Day> = emptyList()
    private var names: List<String> = emptyList()
    private var fmt: Fmt? = null
    private var cur = Double.NaN
    private val rowH = 52f * d

    fun set(dl: List<Day>, nm: List<String>, f: Fmt, current: Double) {
        days = dl
        names = nm
        fmt = f
        cur = current
        requestLayout()
        invalidate()
    }

    override fun onMeasure(wm: Int, hm: Int) {
        setMeasuredDimension(MeasureSpec.getSize(wm), (days.size * rowH).toInt())
    }

    override fun onDraw(c: Canvas) {
        val f = fmt ?: return
        if (days.isEmpty()) return
        var gmin = Double.MAX_VALUE
        var gmax = -Double.MAX_VALUE
        for (x in days) {
            if (!x.min.isNaN() && x.min < gmin) gmin = x.min
            if (!x.max.isNaN() && x.max > gmax) gmax = x.max
        }
        if (gmin > gmax) return
        val span = if (gmax - gmin < 1.0) 1.0 else gmax - gmin
        val w = width.toFloat()
        val barL = 192f * d
        val barR = w - 52f * d
        val bw = barR - barL

        for (i in days.indices) {
            val day = days[i]
            val cy = rowH * i + rowH / 2f

            if (i > 0) {
                p.style = Paint.Style.FILL
                p.color = 0x22FFFFFF
                c.drawRect(0f, rowH * i, w, rowH * i + 1f, p)
            }

            p.style = Paint.Style.FILL
            p.textAlign = Paint.Align.LEFT
            p.typeface = Typeface.create(if (i == 0) "sans-serif-medium" else "sans-serif", Typeface.NORMAL)
            p.color = Color.WHITE
            p.textSize = 15f * sd
            c.drawText(if (i < names.size) names[i] else "", 0f, cy + 5.5f * d, p)

            IconPainter.draw(c, Wmo.icon(day.code, true), 94f * d, cy, 13f * d, p)

            if (day.pop >= 20) {
                p.style = Paint.Style.FILL
                p.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                p.color = 0xFF8FD0FF.toInt()
                p.textSize = 11f * sd
                p.textAlign = Paint.Align.LEFT
                c.drawText(day.pop.toString() + "%", 114f * d, cy + 4f * d, p)
            }

            p.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            p.textSize = 15f * sd
            p.textAlign = Paint.Align.RIGHT
            p.color = 0xB3FFFFFF.toInt()
            c.drawText(f.temp(day.min), barL - 10f * d, cy + 5.5f * d, p)
            p.textAlign = Paint.Align.LEFT
            p.color = Color.WHITE
            p.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            c.drawText(f.temp(day.max), barR + 10f * d, cy + 5.5f * d, p)

            p.style = Paint.Style.FILL
            p.color = 0x2EFFFFFF
            rect.set(barL, cy - 3f * d, barR, cy + 3f * d)
            c.drawRoundRect(rect, 3f * d, 3f * d, p)

            val x0 = barL + ((day.min - gmin) / span).toFloat() * bw
            var x1 = barL + ((day.max - gmin) / span).toFloat() * bw
            if (x1 < x0 + 6f * d) x1 = x0 + 6f * d
            p.shader = LinearGradient(x0, 0f, x1, 0f, tempColor(day.min), tempColor(day.max), Shader.TileMode.CLAMP)
            rect.set(x0, cy - 3f * d, x1, cy + 3f * d)
            c.drawRoundRect(rect, 3f * d, 3f * d, p)
            p.shader = null

            if (i == 0 && !cur.isNaN()) {
                var mx = barL + ((cur - gmin) / span).toFloat() * bw
                if (mx < x0) mx = x0
                if (mx > x1) mx = x1
                p.color = Color.WHITE
                c.drawCircle(mx, cy, 4.5f * d, p)
                p.style = Paint.Style.STROKE
                p.strokeWidth = 1.5f * d
                p.color = 0x66000000
                c.drawCircle(mx, cy, 4.5f * d, p)
            }
        }
    }
}

class NowcastView(ctx: Context) : View(ctx) {
    private val d = resources.displayMetrics.density
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var vals: List<Double> = emptyList()

    fun set(v: List<Double>) {
        vals = v
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        if (vals.isEmpty()) return
        val n = vals.size
        val gap = 5f * d
        val bw = (width - gap * (n - 1)) / n
        var mx = 0.5
        for (v in vals) if (v > mx) mx = v
        val h = height.toFloat()
        p.style = Paint.Style.FILL
        for (i in 0 until n) {
            val x = i * (bw + gap)
            val bh = if (vals[i] < 0.05) 3f * d else maxOf(5f * d, (vals[i] / mx).toFloat() * h)
            p.color = if (vals[i] < 0.05) 0x33FFFFFF else 0xFF6CB8FF.toInt()
            rect.set(x, h - bh, x + bw, h)
            c.drawRoundRect(rect, 3f * d, 3f * d, p)
        }
    }
}

class ScaleBar(ctx: Context, private val colors: IntArray, private val bounds: FloatArray) : View(ctx) {
    private val d = resources.displayMetrics.density
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    var value: Float = -1f
        set(v) {
            field = v
            invalidate()
        }

    override fun onDraw(c: Canvas) {
        val n = colors.size
        val gap = 3f * d
        val sw = (width - gap * (n - 1)) / n
        val h = 6f * d
        val cy = height / 2f
        p.style = Paint.Style.FILL
        for (i in 0 until n) {
            p.color = colors[i]
            val x = i * (sw + gap)
            rect.set(x, cy - h / 2f, x + sw, cy + h / 2f)
            c.drawRoundRect(rect, h / 2f, h / 2f, p)
        }
        if (value < 0f) return
        var idx = n - 1
        var frac = 1f
        for (i in 0 until n) {
            if (value <= bounds[i]) {
                val lo = if (i == 0) 0f else bounds[i - 1]
                idx = i
                frac = (value - lo) / (bounds[i] - lo)
                break
            }
        }
        val x = idx * (sw + gap) + frac * sw
        p.color = Color.WHITE
        c.drawCircle(x, cy, 6f * d, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.5f * d
        p.color = 0x66000000
        c.drawCircle(x, cy, 6f * d, p)
    }
}

class CompassView(ctx: Context) : View(ctx) {
    private val d = resources.displayMetrics.density
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arrow = Path()
    var dir: Int = 0
        set(v) {
            field = v
            invalidate()
        }

    override fun onDraw(c: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val r = minOf(width, height) / 2f - 2f * d
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.5f * d
        p.color = 0x66FFFFFF
        c.drawCircle(cx, cy, r, p)
        p.style = Paint.Style.FILL
        p.color = 0xB3FFFFFF.toInt()
        c.drawCircle(cx, cy - r + 3f * d, 1.6f * d, p)
        c.save()
        c.rotate((dir + 180).toFloat(), cx, cy)
        arrow.reset()
        arrow.moveTo(cx, cy - r + 7f * d)
        arrow.lineTo(cx - 5f * d, cy + 9f * d)
        arrow.lineTo(cx, cy + 4f * d)
        arrow.lineTo(cx + 5f * d, cy + 9f * d)
        arrow.close()
        p.color = Color.WHITE
        c.drawPath(arrow, p)
        c.restore()
    }
}

class MoonView(ctx: Context) : View(ctx) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val lit = Path()
    private val half = Path()
    private val ell = Path()
    var age: Double = 0.0
        set(v) {
            field = v
            invalidate()
        }

    override fun onDraw(c: Canvas) {
        val r = minOf(width, height) / 2f - 2f
        val cx = width / 2f
        val cy = height / 2f
        p.style = Paint.Style.FILL
        p.color = 0xFF2A3350.toInt()
        c.drawCircle(cx, cy, r, p)
        val f = (age / 29.530588853).toFloat()
        val th = 2.0 * Math.PI * f
        val cs = Math.cos(th)
        val rx = (r * Math.abs(cs)).toFloat()
        half.reset()
        ell.reset()
        lit.reset()
        half.addArc(RectF(cx - r, cy - r, cx + r, cy + r), -90f, 180f)
        half.close()
        ell.addOval(RectF(cx - rx, cy - r, cx + rx, cy + r), Path.Direction.CW)
        if (cs > 0) lit.op(half, ell, Path.Op.DIFFERENCE) else lit.op(half, ell, Path.Op.UNION)
        c.save()
        if (f > 0.5f) c.scale(-1f, 1f, cx, cy)
        p.color = 0xFFF4F1DE.toInt()
        c.drawPath(lit, p)
        c.restore()
    }
}

class SunArcView(ctx: Context) : View(ctx) {
    private val d = resources.displayMetrics.density
    private val sd = resources.displayMetrics.scaledDensity
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var riseT = "06:00"
    private var setT = "18:00"
    private var now = "12:00"
    private var riseLbl = ""
    private var setLbl = ""

    fun set(sunrise: String, sunset: String, nowHm: String, rl: String, sl: String) {
        riseT = sunrise
        setT = sunset
        now = nowHm
        riseLbl = rl
        setLbl = sl
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val r = minOf(w / 2f - 40f * d, 92f * d)
        val cx = w / 2f
        val cy = 106f * d
        rect.set(cx - r, cy - r, cx + r, cy + r)

        val a = minutesOf(riseT)
        val b = minutesOf(setT)
        val n = minutesOf(now)
        val frac = if (b > a) ((n - a).toFloat() / (b - a).toFloat()).coerceIn(0f, 1f) else 0f
        val isDay = n in a..b

        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f * d
        p.color = 0x55FFFFFF
        p.pathEffect = DashPathEffect(floatArrayOf(5f * d, 5f * d), 0f)
        c.drawArc(rect, 180f, 180f, false, p)
        p.pathEffect = null

        if (isDay || n > b) {
            p.strokeWidth = 3f * d
            p.strokeCap = Paint.Cap.ROUND
            p.color = 0xFFFFC93C.toInt()
            c.drawArc(rect, 180f, 180f * frac, false, p)
        }

        p.strokeWidth = 1.5f * d
        p.color = 0x55FFFFFF
        c.drawLine(cx - r - 14f * d, cy, cx + r + 14f * d, cy, p)

        if (isDay) {
            val ang = Math.PI * frac
            val sx = cx - (r * Math.cos(ang)).toFloat()
            val sy = cy - (r * Math.sin(ang)).toFloat()
            p.style = Paint.Style.FILL
            p.color = 0x44FFC93C
            c.drawCircle(sx, sy, 15f * d, p)
            p.color = 0xFFFFE08A.toInt()
            c.drawCircle(sx, sy, 8f * d, p)
        }

        p.style = Paint.Style.FILL
        p.textAlign = Paint.Align.CENTER
        p.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        p.color = Color.WHITE
        p.textSize = 15f * sd
        c.drawText(riseT, cx - r, cy + 22f * d, p)
        c.drawText(setT, cx + r, cy + 22f * d, p)
        p.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        p.color = 0xA6FFFFFF.toInt()
        p.textSize = 11f * sd
        c.drawText(riseLbl, cx - r, cy + 38f * d, p)
        c.drawText(setLbl, cx + r, cy + 38f * d, p)
    }
}
