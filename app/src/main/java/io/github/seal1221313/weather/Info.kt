package io.github.seal1221313.weather

import android.content.Context
import java.util.Locale

enum class Icon { CLEAR_DAY, CLEAR_NIGHT, PARTLY_DAY, PARTLY_NIGHT, CLOUDY, FOG, DRIZZLE, RAIN, SNOW, THUNDER }

object Wmo {
    fun icon(code: Int, day: Boolean): Icon = when (code) {
        0, 1 -> if (day) Icon.CLEAR_DAY else Icon.CLEAR_NIGHT
        2 -> if (day) Icon.PARTLY_DAY else Icon.PARTLY_NIGHT
        45, 48 -> Icon.FOG
        51, 53, 55, 56, 57 -> Icon.DRIZZLE
        61, 63, 65, 66, 67, 80, 81, 82 -> Icon.RAIN
        71, 73, 75, 77, 85, 86 -> Icon.SNOW
        95, 96, 99 -> Icon.THUNDER
        else -> Icon.CLOUDY
    }

    fun text(code: Int): Int = when (code) {
        0 -> R.string.c_clear
        1 -> R.string.c_mostly_clear
        2 -> R.string.c_partly
        45, 48 -> R.string.c_fog
        51, 53, 55 -> R.string.c_drizzle
        56, 57 -> R.string.c_fdrizzle
        61 -> R.string.c_lrain
        63 -> R.string.c_rain
        65 -> R.string.c_hrain
        66, 67 -> R.string.c_frain
        71 -> R.string.c_lsnow
        73 -> R.string.c_snow
        75 -> R.string.c_hsnow
        77 -> R.string.c_grains
        80, 81 -> R.string.c_showers
        82 -> R.string.c_hshowers
        85, 86 -> R.string.c_snowshowers
        95 -> R.string.c_thunder
        96, 99 -> R.string.c_thunder_hail
        else -> R.string.c_overcast
    }
}

class Fmt(private val ctx: Context, val preset: Int) {
    private fun u(id: Int): String = ctx.getString(id)

    fun tempUnit(): String = if (preset == 2) "°F" else "°C"

    fun temp(c: Double): String {
        if (c.isNaN()) return "–"
        val v = if (preset == 2) c * 9.0 / 5.0 + 32.0 else c
        return Math.round(v).toString() + "°"
    }

    fun wind(kmh: Double): String {
        if (kmh.isNaN()) return "–"
        return when (preset) {
            0 -> Math.round(kmh).toString() + " " + u(R.string.u_kmh)
            1 -> Math.round(kmh / 3.6).toString() + " " + u(R.string.u_ms)
            else -> Math.round(kmh * 0.621371).toString() + " " + u(R.string.u_mph)
        }
    }

    fun pressure(hpa: Double): String {
        if (hpa.isNaN()) return "–"
        return when (preset) {
            0 -> Math.round(hpa).toString() + " " + u(R.string.u_hpa)
            1 -> Math.round(hpa * 0.750062).toString() + " " + u(R.string.u_mmhg)
            else -> String.format(Locale.US, "%.2f", hpa * 0.02953) + " " + u(R.string.u_inhg)
        }
    }

    fun dist(m: Double): String {
        if (m.isNaN()) return "–"
        val km = m / 1000.0
        val v = if (preset == 2) km * 0.621371 else km
        val unit = if (preset == 2) u(R.string.u_mi) else u(R.string.u_km)
        val num = if (v >= 10) Math.round(v).toString() else String.format(Locale.US, "%.1f", v)
        return "$num $unit"
    }

    fun precip(mm: Double): String {
        if (mm.isNaN()) return "–"
        return if (preset == 2) String.format(Locale.US, "%.2f", mm / 25.4) + " " + u(R.string.u_in)
        else String.format(Locale.US, "%.1f", mm) + " " + u(R.string.u_mm)
    }
}

fun moonAge(): Double {
    val days = (System.currentTimeMillis() - 947182440000L) / 86400000.0
    val syn = 29.530588853
    var a = days % syn
    if (a < 0) a += syn
    return a
}

fun minutesOf(hm: String): Int {
    if (hm.length < 5) return 0
    return try { hm.substring(0, 2).toInt() * 60 + hm.substring(3, 5).toInt() } catch (e: Exception) { 0 }
}
