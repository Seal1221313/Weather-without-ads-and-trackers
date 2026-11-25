package io.github.seal1221313.weather

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class Place(val name: String, val sub: String, val lat: Double, val lon: Double, val gps: Boolean = false) {
    fun toJson(): JSONObject = JSONObject().put("n", name).put("s", sub).put("la", lat).put("lo", lon).put("g", gps)
    fun key(): String = String.format(Locale.US, "%.2f,%.2f", lat, lon)

    companion object {
        fun from(o: JSONObject): Place =
            Place(o.getString("n"), o.optString("s", ""), o.getDouble("la"), o.getDouble("lo"), o.optBoolean("g", false))
    }
}

class Hour(val time: String, val temp: Double, val code: Int, val pop: Int, val isDay: Boolean)

class Day(
    val date: String, val code: Int, val max: Double, val min: Double,
    val sunrise: String, val sunset: String, val uv: Double, val precip: Double, val pop: Int
)

class Air(
    val euAqi: Int, val usAqi: Int, val pm25: Double, val pm10: Double,
    val no2: Double, val o3: Double, val so2: Double, val pollen: List<Pair<String, Double>>
)

class Weather(
    val time: String, val temp: Double, val feels: Double, val humidity: Int, val isDay: Boolean,
    val code: Int, val cloud: Int, val pressure: Double, val wind: Double, val windDir: Int, val gust: Double,
    val visibility: Double, val uv: Double, val dew: Double,
    val hours: List<Hour>, val days: List<Day>, val nowcast: List<Double>, val air: Air?
)

object Parser {
    private fun d(a: JSONArray?, i: Int): Double =
        if (a == null || i < 0 || i >= a.length()) Double.NaN else a.optDouble(i, Double.NaN)

    private fun n(a: JSONArray?, i: Int): Int =
        if (a == null || i < 0 || i >= a.length()) 0 else a.optInt(i, 0)

    private fun s(a: JSONArray?, i: Int): String =
        if (a == null || i < 0 || i >= a.length()) "" else a.optString(i, "")

    private fun hm(x: String): String = if (x.length >= 16) x.substring(11, 16) else x

    fun parse(f: String, a: String?): Weather {
        val j = JSONObject(f)
        val cur = j.getJSONObject("current")
        val h = j.getJSONObject("hourly")
        val dl = j.getJSONObject("daily")

        val ht = h.getJSONArray("time")
        val ct = cur.getString("time")
        val key = ct.substring(0, 13)
        var start = 0
        for (i in 0 until ht.length()) {
            if (ht.getString(i).startsWith(key)) { start = i; break }
        }

        val hT = h.optJSONArray("temperature_2m")
        val hC = h.optJSONArray("weather_code")
        val hP = h.optJSONArray("precipitation_probability")
        val hD = h.optJSONArray("is_day")
        val hV = h.optJSONArray("visibility")
        val hU = h.optJSONArray("uv_index")
        val hDew = h.optJSONArray("dew_point_2m")

        val hours = ArrayList<Hour>()
        val end = minOf(ht.length(), start + 48)
        for (i in start until end) {
            hours.add(Hour(ht.getString(i), d(hT, i), n(hC, i), n(hP, i), n(hD, i) == 1))
        }

        val dt = dl.getJSONArray("time")
        val dC = dl.optJSONArray("weather_code")
        val dMax = dl.optJSONArray("temperature_2m_max")
        val dMin = dl.optJSONArray("temperature_2m_min")
        val dSr = dl.optJSONArray("sunrise")
        val dSs = dl.optJSONArray("sunset")
        val dUv = dl.optJSONArray("uv_index_max")
        val dPr = dl.optJSONArray("precipitation_sum")
        val dPp = dl.optJSONArray("precipitation_probability_max")
        val days = ArrayList<Day>()
        for (i in 0 until dt.length()) {
            days.add(
                Day(
                    dt.getString(i), n(dC, i), d(dMax, i), d(dMin, i),
                    hm(s(dSr, i)), hm(s(dSs, i)), d(dUv, i), d(dPr, i), n(dPp, i)
                )
            )
        }

        val nc = ArrayList<Double>()
        val m = j.optJSONObject("minutely_15")?.optJSONArray("precipitation")
        if (m != null) {
            for (i in 0 until minOf(m.length(), 8)) {
                val v = m.optDouble(i, Double.NaN)
                nc.add(if (v.isNaN()) 0.0 else v)
            }
        }

        var air: Air? = null
        if (a != null) {
            try {
                val c = JSONObject(a).getJSONObject("current")
                val pol = ArrayList<Pair<String, Double>>()
                for (k in arrayOf("alder", "birch", "grass", "mugwort", "olive", "ragweed")) {
                    val v = c.optDouble(k + "_pollen", Double.NaN)
                    if (!v.isNaN()) pol.add(Pair(k, v))
                }
                air = Air(
                    c.optInt("european_aqi", -1), c.optInt("us_aqi", -1),
                    c.optDouble("pm2_5", Double.NaN), c.optDouble("pm10", Double.NaN),
                    c.optDouble("nitrogen_dioxide", Double.NaN), c.optDouble("ozone", Double.NaN),
                    c.optDouble("sulphur_dioxide", Double.NaN), pol
                )
            } catch (e: Exception) {
                air = null
            }
        }

        return Weather(
            ct, cur.optDouble("temperature_2m", Double.NaN), cur.optDouble("apparent_temperature", Double.NaN),
            cur.optInt("relative_humidity_2m", 0), cur.optInt("is_day", 1) == 1,
            cur.optInt("weather_code", 0), cur.optInt("cloud_cover", 0),
            cur.optDouble("pressure_msl", Double.NaN), cur.optDouble("wind_speed_10m", Double.NaN),
            cur.optInt("wind_direction_10m", 0), cur.optDouble("wind_gusts_10m", Double.NaN),
            d(hV, start), d(hU, start), d(hDew, start),
            hours, days, nc, air
        )
    }
}
