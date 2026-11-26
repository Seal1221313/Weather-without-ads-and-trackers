package io.github.seal1221313.weather

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

object Api {
    private fun get(url: String): String {
        if (!url.startsWith("https://")) throw IOException("https only")
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 12000
        c.readTimeout = 15000
        c.setRequestProperty("User-Agent", "WeatherApp/1.0 (Android)")
        try {
            if (c.responseCode != 200) throw IOException("HTTP " + c.responseCode)
            if (c.url.protocol != "https") throw IOException("https only")
            return c.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    private fun q(v: Double): String = String.format(Locale.US, "%.4f", v)

    fun forecast(lat: Double, lon: Double): String = get(
        "https://api.open-meteo.com/v1/forecast?latitude=" + q(lat) + "&longitude=" + q(lon) +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code," +
            "cloud_cover,pressure_msl,wind_speed_10m,wind_direction_10m,wind_gusts_10m" +
            "&minutely_15=precipitation" +
            "&hourly=temperature_2m,precipitation_probability,weather_code,visibility,uv_index,is_day,dew_point_2m" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max," +
            "precipitation_sum,precipitation_probability_max" +
            "&timezone=auto&forecast_days=10&forecast_minutely_15=8&wind_speed_unit=kmh"
    )

    fun air(lat: Double, lon: Double): String = get(
        "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=" + q(lat) + "&longitude=" + q(lon) +
            "&current=european_aqi,us_aqi,pm10,pm2_5,nitrogen_dioxide,ozone,sulphur_dioxide," +
            "alder_pollen,birch_pollen,grass_pollen,mugwort_pollen,olive_pollen,ragweed_pollen&timezone=auto"
    )

    fun geocode(name: String, lang: String): List<Place> {
        val r = JSONObject(
            get(
                "https://geocoding-api.open-meteo.com/v1/search?name=" + URLEncoder.encode(name, "UTF-8") +
                    "&count=10&language=" + lang + "&format=json"
            )
        )
        val arr = r.optJSONArray("results") ?: return emptyList()
        val out = ArrayList<Place>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val sub = listOf(o.optString("admin1", ""), o.optString("country", "")).filter { it.isNotEmpty() }.joinToString(", ")
            out.add(Place(o.getString("name"), sub, o.getDouble("latitude"), o.getDouble("longitude")))
        }
        return out
    }

    const val REPO = "Seal1221313/Weather-without-ads-and-trackers"

    class Update(val tag: String, val num: Int, val url: String)

    fun latestRelease(): Update? {
        val j = JSONObject(get("https://api.github.com/repos/$REPO/releases/latest"))
        val tag = j.optString("tag_name", "")
        val num = tag.substringAfterLast('.').toIntOrNull() ?: return null
        val assets = j.optJSONArray("assets") ?: return null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            val u = a.optString("browser_download_url", "")
            if (a.optString("name", "").endsWith(".apk") && u.startsWith("https://github.com/$REPO/releases/download/")) {
                return Update(tag, num, u)
            }
        }
        return null
    }
}
