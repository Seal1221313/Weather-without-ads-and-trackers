package io.github.seal1221313.weather

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.Locale

class Tile(val root: View, val value: TextView, val sub: TextView)

class MainActivity : Activity() {
    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    private val CARD = Color.argb(66, 0, 0, 0)
    private val ui = Handler(Looper.getMainLooper())

    private lateinit var prefs: SharedPreferences
    private lateinit var fmt: Fmt
    private lateinit var bg: BackgroundView
    private lateinit var scroll: ScrollView
    private lateinit var content: LinearLayout
    private lateinit var body: LinearLayout

    private lateinit var cityTv: TextView
    private lateinit var unitsTv: TextView
    private lateinit var refreshBtn: Glyph
    private lateinit var dateTv: TextView
    private lateinit var tempTv: TextView
    private lateinit var condTv: TextView
    private lateinit var hiloTv: TextView
    private lateinit var feelsTv: TextView

    private lateinit var nowCard: LinearLayout
    private lateinit var nowTv: TextView
    private lateinit var nowView: NowcastView
    private lateinit var hourlyView: HourlyView
    private lateinit var dailyView: DailyView

    private lateinit var airCard: LinearLayout
    private lateinit var airValTv: TextView
    private lateinit var airLvlTv: TextView
    private lateinit var airSubTv: TextView
    private lateinit var airBar: ScaleBar
    private lateinit var airChipVal: Array<TextView>
    private lateinit var pollenCard: LinearLayout
    private lateinit var pollenList: LinearLayout

    private lateinit var tWind: Tile
    private lateinit var tHum: Tile
    private lateinit var tPress: Tile
    private lateinit var tUv: Tile
    private lateinit var tVis: Tile
    private lateinit var tCloud: Tile
    private lateinit var tPrec: Tile
    private lateinit var tDay: Tile
    private lateinit var compass: CompassView
    private lateinit var uvBar: ScaleBar

    private lateinit var sunArc: SunArcView
    private lateinit var moonView: MoonView
    private lateinit var moonTv: TextView
    private lateinit var moonSub: TextView
    private lateinit var footTv: TextView

    private var insT = 0
    private var insB = 0
    private var place: Place? = null
    private var weather: Weather? = null
    private var weatherKey = ""
    private var saved = ArrayList<Place>()
    private var preset = 0
    private var lastLoad = 0L
    private var gen = 0
    private var asked = false
    private var entered = false
    private var spin: ObjectAnimator? = null

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
    private fun di(v: Float): Int = dp(v).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("w", Context.MODE_PRIVATE)
        preset = prefs.getInt("preset", defaultPreset())
        fmt = Fmt(this, preset)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        buildUi()
        loadSaved()

        val pj = prefs.getString("cur", null)
        if (pj != null) {
            try { place = Place.from(JSONObject(pj)) } catch (e: Exception) { place = null }
        }
        val cf = prefs.getString("cf", null)
        val ck = prefs.getString("cp", null)
        val pl = place
        if (cf != null && ck != null && pl != null && pl.key() == ck) {
            try {
                weather = Parser.parse(cf, prefs.getString("ca", null))
                weatherKey = ck
            } catch (e: Exception) {
                weather = null
            }
        }
        lastLoad = prefs.getLong("ct", 0L)
        render()
        checkUpdate()
    }

    private fun checkUpdate() {
        Thread {
            try {
                val u = Api.latestRelease()
                val pi = packageManager.getPackageInfo(packageName, 0)
                @Suppress("DEPRECATION")
                val cur = pi.versionCode
                if (u != null && u.num > cur) {
                    val curName = pi.versionName ?: cur.toString()
                    ui.post { if (!isFinishing) showUpdate(u, curName) }
                }
            } catch (e: Exception) {
            }
        }.start()
    }

    private fun showUpdate(u: Api.Update, curName: String) {
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(R.string.upd_title)
            .setMessage(getString(R.string.upd_msg, u.tag.removePrefix("v"), curName))
            .setPositiveButton(R.string.upd_btn) { _, _ ->
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u.url)))
                } catch (e: Exception) {
                }
            }
            .setNegativeButton(R.string.upd_later, null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        val p = place
        if (p == null) {
            if (!asked) {
                asked = true
                locate()
            }
        } else if (System.currentTimeMillis() - lastLoad > 10 * 60 * 1000L) {
            if (p.gps) locate() else refresh()
        }
    }

    private fun defaultPreset(): Int {
        val l = Locale.getDefault()
        return when {
            l.country == "US" || l.country == "LR" || l.country == "MM" -> 2
            l.language == "uk" || l.language == "ru" || l.language == "be" -> 1
            else -> 0
        }
    }

    // ---------------------------------------------------------------- UI building

    private fun tv(sp: Float, bold: Boolean = false): TextView {
        val t = TextView(this)
        t.setTextColor(Color.WHITE)
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        if (bold) t.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        return t
    }

    private fun rounded(color: Int, r: Float): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(color)
        g.cornerRadius = r
        return g
    }

    private fun lp(w: Int, h: Int): LinearLayout.LayoutParams = LinearLayout.LayoutParams(w, h)

    private fun card(titleRes: Int, padded: Boolean = true): LinearLayout {
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.background = rounded(CARD, dp(22f))
        val side = if (padded) di(16f) else 0
        c.setPadding(side, di(14f), side, if (padded) di(16f) else di(8f))
        val t = tv(12f)
        t.text = getString(titleRes).uppercase()
        t.alpha = 0.7f
        t.letterSpacing = 0.08f
        val tl = lp(MATCH, WRAP)
        tl.bottomMargin = di(8f)
        if (!padded) tl.leftMargin = di(16f)
        c.addView(t, tl)
        val l = lp(MATCH, WRAP)
        l.topMargin = di(12f)
        body.addView(c, l)
        return c
    }

    private fun tile(titleRes: Int, side: View?, below: View?): Tile {
        val root = FrameLayout(this)
        root.background = rounded(CARD, dp(20f))
        root.setPadding(di(14f), di(12f), di(14f), di(12f))
        root.minimumHeight = di(112f)
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        val title = tv(12f)
        title.text = getString(titleRes).uppercase()
        title.alpha = 0.7f
        title.letterSpacing = 0.08f
        col.addView(title)
        val v = tv(26f)
        v.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        v.setSingleLine()
        v.ellipsize = TextUtils.TruncateAt.END
        val vl = lp(WRAP, WRAP)
        vl.topMargin = di(6f)
        col.addView(v, vl)
        val s = tv(13f)
        s.alpha = 0.75f
        col.addView(s)
        if (below != null) {
            val bl = lp(MATCH, di(18f))
            bl.topMargin = di(8f)
            col.addView(below, bl)
        }
        root.addView(col, FrameLayout.LayoutParams(MATCH, WRAP))
        if (side != null) {
            root.addView(side, FrameLayout.LayoutParams(di(52f), di(52f), Gravity.END or Gravity.CENTER_VERTICAL))
            v.maxWidth = di(110f)
        }
        return Tile(root, v, s)
    }

    private fun tileRow(a: Tile, b: Tile) {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        val l1 = lp(0, WRAP)
        l1.weight = 1f
        l1.rightMargin = di(6f)
        val l2 = lp(0, WRAP)
        l2.weight = 1f
        l2.leftMargin = di(6f)
        r.addView(a.root, l1)
        r.addView(b.root, l2)
        val l = lp(MATCH, WRAP)
        l.topMargin = di(12f)
        body.addView(r, l)
    }

    private fun circleBtn(v: View, onClick: () -> Unit): View {
        val f = FrameLayout(this)
        f.background = rounded(Color.argb(50, 0, 0, 0), dp(20f))
        f.addView(v, FrameLayout.LayoutParams(MATCH, MATCH))
        f.setOnClickListener { onClick() }
        return f
    }

    private fun buildUi() {
        val root = FrameLayout(this)
        bg = BackgroundView(this)
        root.addView(bg, FrameLayout.LayoutParams(MATCH, MATCH))

        scroll = ScrollView(this)
        scroll.isVerticalScrollBarEnabled = false
        scroll.overScrollMode = View.OVER_SCROLL_NEVER
        content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        scroll.addView(content, FrameLayout.LayoutParams(MATCH, WRAP))
        root.addView(scroll, FrameLayout.LayoutParams(MATCH, MATCH))
        scroll.viewTreeObserver.addOnScrollChangedListener { bg.setScroll(scroll.scrollY.toFloat()) }

        root.setOnApplyWindowInsetsListener { _, ins ->
            insT = ins.systemWindowInsetTop
            insB = ins.systemWindowInsetBottom
            applyPad()
            ins
        }
        applyPad()
        setContentView(root)

        // header
        val header = LinearLayout(this)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL
        val cityBox = LinearLayout(this)
        cityBox.orientation = LinearLayout.HORIZONTAL
        cityBox.gravity = Gravity.CENTER_VERTICAL
        cityTv = tv(22f, true)
        cityTv.setSingleLine()
        cityTv.ellipsize = TextUtils.TruncateAt.END
        cityBox.addView(cityTv, lp(WRAP, WRAP))
        cityBox.addView(Glyph(this, 3), lp(di(24f), di(24f)))
        cityBox.setOnClickListener { showPlaces() }
        header.addView(cityBox, lp(0, di(44f)).also { it.weight = 1f })

        val bs = lp(di(40f), di(40f))
        bs.leftMargin = di(8f)
        header.addView(circleBtn(Glyph(this, 0)) { showPlaces() }, bs)
        refreshBtn = Glyph(this, 1)
        header.addView(circleBtn(refreshBtn) { val p = place; if (p != null) { if (p.gps) locate() else refresh() } }, bs)
        unitsTv = tv(13f, true)
        unitsTv.gravity = Gravity.CENTER
        header.addView(circleBtn(unitsTv) { showUnits() }, bs)
        content.addView(header, lp(MATCH, WRAP))

        // hero
        val hero = LinearLayout(this)
        hero.orientation = LinearLayout.VERTICAL
        hero.gravity = Gravity.CENTER_HORIZONTAL
        hero.setPadding(0, di(24f), 0, di(20f))
        dateTv = tv(14f)
        dateTv.alpha = 0.8f
        hero.addView(dateTv)
        tempTv = tv(108f)
        tempTv.typeface = Typeface.create("sans-serif-thin", Typeface.NORMAL)
        tempTv.includeFontPadding = false
        tempTv.setPadding(di(14f), 0, 0, 0)
        hero.addView(tempTv)
        condTv = tv(22f, true)
        hero.addView(condTv)
        hiloTv = tv(15f)
        hiloTv.alpha = 0.9f
        val hl = lp(WRAP, WRAP)
        hl.topMargin = di(4f)
        hero.addView(hiloTv, hl)
        feelsTv = tv(14f)
        feelsTv.alpha = 0.75f
        hero.addView(feelsTv)
        content.addView(hero, lp(MATCH, WRAP))

        body = LinearLayout(this)
        body.orientation = LinearLayout.VERTICAL
        content.addView(body, lp(MATCH, WRAP))

        // nowcast
        nowCard = card(R.string.nowcast)
        nowTv = tv(17f, true)
        nowCard.addView(nowTv)
        nowView = NowcastView(this)
        val nl = lp(MATCH, di(36f))
        nl.topMargin = di(10f)
        nowCard.addView(nowView, nl)

        // hourly
        val hc = card(R.string.hourly, false)
        val hs = HorizontalScrollView(this)
        hs.isHorizontalScrollBarEnabled = false
        hs.overScrollMode = View.OVER_SCROLL_NEVER
        hourlyView = HourlyView(this)
        hs.addView(hourlyView)
        hc.addView(hs, lp(MATCH, WRAP))

        // daily
        val dc = card(R.string.daily)
        dailyView = DailyView(this)
        dc.addView(dailyView, lp(MATCH, WRAP))

        // air
        airCard = card(R.string.air)
        val arow = LinearLayout(this)
        arow.orientation = LinearLayout.HORIZONTAL
        arow.gravity = Gravity.BOTTOM
        airValTv = tv(44f)
        airValTv.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        arow.addView(airValTv)
        airLvlTv = tv(18f, true)
        val ll = lp(WRAP, WRAP)
        ll.leftMargin = di(12f)
        ll.bottomMargin = di(8f)
        arow.addView(airLvlTv, ll)
        airCard.addView(arow)
        airSubTv = tv(13f)
        airSubTv.alpha = 0.75f
        airCard.addView(airSubTv)
        airBar = ScaleBar(
            this,
            intArrayOf(0xFF50F0E6.toInt(), 0xFF50CCAA.toInt(), 0xFFF0E641.toInt(), 0xFFFF5050.toInt(), 0xFF960032.toInt(), 0xFF7D2181.toInt()),
            floatArrayOf(20f, 40f, 60f, 80f, 100f, 120f)
        )
        val bl = lp(MATCH, di(18f))
        bl.topMargin = di(10f)
        bl.bottomMargin = di(12f)
        airCard.addView(airBar, bl)
        val chips = LinearLayout(this)
        chips.orientation = LinearLayout.HORIZONTAL
        val names = arrayOf("PM2.5", "PM10", "NO2", "O3", "SO2")
        val vals = ArrayList<TextView>()
        for (nm in names) {
            val col = LinearLayout(this)
            col.orientation = LinearLayout.VERTICAL
            col.gravity = Gravity.CENTER_HORIZONTAL
            val v = tv(17f, true)
            val n = tv(11f)
            n.alpha = 0.65f
            n.text = nm
            col.addView(v)
            col.addView(n)
            vals.add(v)
            val cl = lp(0, WRAP)
            cl.weight = 1f
            chips.addView(col, cl)
        }
        airChipVal = vals.toTypedArray()
        airCard.addView(chips)
        val unit = tv(11f)
        unit.alpha = 0.5f
        unit.text = "µg/m³"
        unit.gravity = Gravity.END
        airCard.addView(unit, lp(MATCH, WRAP))

        // pollen
        pollenCard = card(R.string.pollen)
        pollenList = LinearLayout(this)
        pollenList.orientation = LinearLayout.VERTICAL
        pollenCard.addView(pollenList)

        // tiles
        compass = CompassView(this)
        uvBar = ScaleBar(
            this,
            intArrayOf(0xFF4CAF50.toInt(), 0xFFFFEB3B.toInt(), 0xFFFF9800.toInt(), 0xFFF44336.toInt(), 0xFF9C27B0.toInt()),
            floatArrayOf(3f, 6f, 8f, 11f, 14f)
        )
        tWind = tile(R.string.t_wind, compass, null)
        tHum = tile(R.string.t_hum, null, null)
        tPress = tile(R.string.t_press, null, null)
        tUv = tile(R.string.t_uv, null, uvBar)
        tVis = tile(R.string.t_vis, null, null)
        tCloud = tile(R.string.t_cloud, null, null)
        tPrec = tile(R.string.t_precip, null, null)
        tDay = tile(R.string.t_day, null, null)
        tileRow(tWind, tHum)
        tileRow(tPress, tUv)
        tileRow(tVis, tCloud)
        tileRow(tPrec, tDay)

        // sun & moon
        val sc = card(R.string.sun_moon)
        sunArc = SunArcView(this)
        sc.addView(sunArc, lp(MATCH, di(150f)))
        val mrow = LinearLayout(this)
        mrow.orientation = LinearLayout.HORIZONTAL
        mrow.gravity = Gravity.CENTER_VERTICAL
        moonView = MoonView(this)
        mrow.addView(moonView, lp(di(44f), di(44f)))
        val mcol = LinearLayout(this)
        mcol.orientation = LinearLayout.VERTICAL
        moonTv = tv(16f, true)
        moonSub = tv(13f)
        moonSub.alpha = 0.75f
        mcol.addView(moonTv)
        mcol.addView(moonSub)
        val mcl = lp(WRAP, WRAP)
        mcl.leftMargin = di(14f)
        mrow.addView(mcol, mcl)
        val mrl = lp(MATCH, WRAP)
        mrl.topMargin = di(6f)
        sc.addView(mrow, mrl)

        footTv = tv(12f)
        footTv.alpha = 0.6f
        footTv.gravity = Gravity.CENTER
        val fl = lp(MATCH, WRAP)
        fl.topMargin = di(18f)
        body.addView(footTv, fl)
    }

    private fun applyPad() {
        content.setPadding(di(16f), insT + di(8f), di(16f), insB + di(28f))
    }

    // ---------------------------------------------------------------- render

    private fun dirName(deg: Int): String {
        val names = resources.getStringArray(R.array.wind_dirs)
        val i = ((deg + 22.5) / 45.0).toInt() % 8
        return names[i]
    }

    private fun dayName(date: String, idx: Int): String {
        if (idx == 0) return getString(R.string.today)
        return try {
            val cal = Calendar.getInstance()
            cal.set(date.substring(0, 4).toInt(), date.substring(5, 7).toInt() - 1, date.substring(8, 10).toInt())
            val s = SimpleDateFormat("EEE", Locale.getDefault()).format(cal.time)
            s.replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            date
        }
    }

    private fun dateLine(t: String): String {
        return try {
            val cal = Calendar.getInstance()
            cal.set(t.substring(0, 4).toInt(), t.substring(5, 7).toInt() - 1, t.substring(8, 10).toInt())
            val s = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(cal.time)
            s.replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            ""
        }
    }

    private fun lvl(i: Int): String = getString(
        when (i) {
            0 -> R.string.lv0
            1 -> R.string.lv1
            2 -> R.string.lv2
            3 -> R.string.lv3
            else -> R.string.lv4
        }
    )

    private fun setSub(t: Tile, s: String) {
        t.sub.text = s
        t.sub.visibility = if (s.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun render() {
        unitsTv.text = fmt.tempUnit()
        val pl = place
        cityTv.text = pl?.name ?: getString(R.string.app_name)
        val w = weather
        if (w == null) {
            tempTv.text = "–"
            condTv.text = ""
            hiloTv.text = ""
            feelsTv.text = ""
            dateTv.text = ""
            body.visibility = View.GONE
            return
        }
        body.visibility = View.VISIBLE

        bg.setWeather(w.code, w.isDay, w.cloud)
        dateTv.text = dateLine(w.time)
        tempTv.text = fmt.temp(w.temp)
        condTv.text = getString(Wmo.text(w.code))
        val d0 = if (w.days.isNotEmpty()) w.days[0] else null
        hiloTv.text = if (d0 != null) getString(R.string.hi_lo, fmt.temp(d0.max), fmt.temp(d0.min)) else ""
        feelsTv.text = getString(R.string.feels, fmt.temp(w.feels))

        // nowcast
        if (w.nowcast.isEmpty()) {
            nowCard.visibility = View.GONE
        } else {
            nowCard.visibility = View.VISIBLE
            val wet = w.nowcast.map { it >= 0.1 }
            nowTv.text = if (wet[0]) {
                val idx = wet.indexOfFirst { !it }
                if (idx < 0) getString(R.string.rain_long) else getString(R.string.rain_stop, idx * 15)
            } else {
                val idx = wet.indexOfFirst { it }
                if (idx < 0) getString(R.string.rain_none) else getString(R.string.rain_start, idx * 15)
            }
            nowView.set(w.nowcast)
        }

        hourlyView.set(w.hours, fmt, getString(R.string.now))
        val names = ArrayList<String>()
        for (i in w.days.indices) names.add(dayName(w.days[i].date, i))
        dailyView.set(w.days, names, fmt, w.temp)

        // air
        val a = w.air
        if (a == null || a.euAqi < 0) {
            airCard.visibility = View.GONE
        } else {
            airCard.visibility = View.VISIBLE
            val li = when {
                a.euAqi <= 20 -> 0
                a.euAqi <= 40 -> 1
                a.euAqi <= 60 -> 2
                a.euAqi <= 80 -> 3
                a.euAqi <= 100 -> 4
                else -> 5
            }
            airValTv.text = a.euAqi.toString()
            airLvlTv.text = getString(
                when (li) {
                    0 -> R.string.aq0
                    1 -> R.string.aq1
                    2 -> R.string.aq2
                    3 -> R.string.aq3
                    4 -> R.string.aq4
                    else -> R.string.aq5
                }
            )
            airSubTv.text = if (a.usAqi >= 0) getString(R.string.aqi_line, a.usAqi) else "EU AQI"
            airBar.value = a.euAqi.toFloat()
            val pv = doubleArrayOf(a.pm25, a.pm10, a.no2, a.o3, a.so2)
            for (i in pv.indices) airChipVal[i].text = if (pv[i].isNaN()) "–" else Math.round(pv[i]).toString()
        }

        // pollen
        val pol = a?.pollen
        pollenList.removeAllViews()
        if (pol == null || pol.isEmpty()) {
            pollenCard.visibility = View.GONE
        } else {
            pollenCard.visibility = View.VISIBLE
            for (pair in pol) {
                val v = pair.second
                val li = when {
                    v < 10 -> 0
                    v < 50 -> 1
                    v < 200 -> 2
                    else -> 3
                }
                val col = when (li) {
                    0 -> 0xFF50CCAA.toInt()
                    1 -> 0xFFF0E641.toInt()
                    2 -> 0xFFFF9340.toInt()
                    else -> 0xFFFF5050.toInt()
                }
                val row = LinearLayout(this)
                row.orientation = LinearLayout.HORIZONTAL
                row.gravity = Gravity.CENTER_VERTICAL
                val dot = tv(14f)
                dot.text = "●"
                dot.setTextColor(col)
                row.addView(dot)
                val nm = tv(16f)
                nm.text = getString(
                    when (pair.first) {
                        "alder" -> R.string.pl_alder
                        "birch" -> R.string.pl_birch
                        "grass" -> R.string.pl_grass
                        "mugwort" -> R.string.pl_mugwort
                        "olive" -> R.string.pl_olive
                        else -> R.string.pl_ragweed
                    }
                )
                val nl = lp(0, WRAP)
                nl.weight = 1f
                nl.leftMargin = di(10f)
                row.addView(nm, nl)
                val vv = tv(14f)
                vv.alpha = 0.85f
                vv.text = lvl(li) + " · " + Math.round(v) + " " + getString(R.string.pollen_unit)
                row.addView(vv)
                val rl = lp(MATCH, WRAP)
                rl.topMargin = di(8f)
                pollenList.addView(row, rl)
            }
        }

        // tiles
        tWind.value.text = fmt.wind(w.wind) + " " + dirName(w.windDir)
        setSub(tWind, getString(R.string.gusts, fmt.wind(w.gust)))
        compass.dir = w.windDir
        tHum.value.text = w.humidity.toString() + "%"
        setSub(tHum, getString(R.string.dew, fmt.temp(w.dew)))
        tPress.value.text = fmt.pressure(w.pressure)
        setSub(tPress, "")
        if (w.uv.isNaN()) {
            tUv.value.text = "–"
            setSub(tUv, "")
            uvBar.value = -1f
        } else {
            tUv.value.text = Math.round(w.uv).toString()
            val ul = when {
                w.uv < 3 -> 0
                w.uv < 6 -> 1
                w.uv < 8 -> 2
                w.uv < 11 -> 3
                else -> 4
            }
            setSub(tUv, lvl(ul))
            uvBar.value = w.uv.toFloat()
        }
        tVis.value.text = fmt.dist(w.visibility)
        setSub(tVis, "")
        tCloud.value.text = w.cloud.toString() + "%"
        setSub(tCloud, "")
        if (d0 != null) {
            tPrec.value.text = fmt.precip(d0.precip)
            setSub(tPrec, getString(R.string.chance, d0.pop))
            val mins = minutesOf(d0.sunset) - minutesOf(d0.sunrise)
            tDay.value.text = if (mins > 0) getString(R.string.hm, mins / 60, mins % 60) else "–"
            setSub(tDay, getString(R.string.sunset_at, d0.sunset))
            sunArc.set(d0.sunrise, d0.sunset, w.time.substring(11, 16), getString(R.string.sunrise), getString(R.string.sunset))
        }

        // moon
        val age = moonAge()
        moonView.age = age
        val f = age / 29.530588853
        val phase = ((f * 8.0) + 0.5).toInt() % 8
        val ill = Math.round((1.0 - Math.cos(2.0 * Math.PI * f)) / 2.0 * 100.0).toInt()
        moonTv.text = resources.getStringArray(R.array.moon_phases)[phase]
        moonSub.text = getString(R.string.moon_ill, ill)

        val ts = SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date(if (lastLoad > 0) lastLoad else System.currentTimeMillis()))
        footTv.text = getString(R.string.attribution) + "\n" + getString(R.string.updated, ts)

        if (!entered) {
            entered = true
            animateIn()
        }
    }

    private fun animateIn() {
        val n = body.childCount
        for (i in 0 until n) {
            val v = body.getChildAt(i)
            v.alpha = 0f
            v.translationY = dp(28f)
            v.animate().alpha(1f).translationY(0f).setStartDelay(60L * i).setDuration(420).start()
        }
    }

    private fun setBusy(b: Boolean) {
        if (b) {
            if (spin == null) {
                val a = ObjectAnimator.ofFloat(refreshBtn, View.ROTATION, 0f, 360f)
                a.duration = 900
                a.repeatCount = ValueAnimator.INFINITE
                a.interpolator = LinearInterpolator()
                a.start()
                spin = a
            }
        } else {
            spin?.cancel()
            spin = null
            refreshBtn.rotation = 0f
        }
    }

    // ---------------------------------------------------------------- data

    private fun refresh() {
        val p = place ?: return
        val my = ++gen
        setBusy(true)
        Thread {
            var f: String? = null
            var a: String? = null
            var w: Weather? = null
            val at = Thread {
                try { a = Api.air(p.lat, p.lon) } catch (e: Exception) { a = null }
            }
            at.start()
            try {
                f = Api.forecast(p.lat, p.lon)
            } catch (e: Exception) {
                f = null
            }
            try { at.join() } catch (e: InterruptedException) { }
            val fs = f
            if (fs != null) {
                try { w = Parser.parse(fs, a) } catch (e: Exception) { w = null }
            }
            val ws = w
            val now = System.currentTimeMillis()
            if (ws != null && fs != null) {
                prefs.edit().putString("cf", fs).putString("ca", a).putString("cp", p.key()).putLong("ct", now).apply()
            }
            ui.post {
                if (my == gen) {
                    setBusy(false)
                    if (ws != null) {
                        weather = ws
                        weatherKey = p.key()
                        lastLoad = now
                        render()
                    } else {
                        Toast.makeText(this, R.string.err_net, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.start()
    }

    private fun selectPlace(p: Place) {
        place = p
        prefs.edit().putString("cur", p.toJson().toString()).apply()
        if (weatherKey != p.key()) {
            weather = null
            entered = false
        }
        render()
        refresh()
    }

    private fun loadSaved() {
        saved = ArrayList()
        val s = prefs.getString("saved", null) ?: return
        try {
            val arr = JSONArray(s)
            for (i in 0 until arr.length()) saved.add(Place.from(arr.getJSONObject(i)))
        } catch (e: Exception) {
            saved = ArrayList()
        }
    }

    private fun persistSaved() {
        val arr = JSONArray()
        for (p in saved) arr.put(p.toJson())
        prefs.edit().putString("saved", arr.toString()).apply()
    }

    private fun addSaved(p: Place) {
        val it = saved.iterator()
        while (it.hasNext()) if (it.next().key() == p.key()) it.remove()
        saved.add(0, p)
        while (saved.size > 12) saved.removeAt(saved.size - 1)
        persistSaved()
    }

    // ---------------------------------------------------------------- location

    private fun locate() {
        if (Build.VERSION.SDK_INT >= 23 &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1)
            return
        }
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var best: Location? = null
        try {
            for (pr in lm.getProviders(true)) {
                val l = lm.getLastKnownLocation(pr)
                val b = best
                if (l != null && (b == null || l.time > b.time)) best = l
            }
        } catch (e: SecurityException) {
            best = null
        }
        val got = best
        if (got != null) {
            onLocation(got)
            return
        }
        val prov = when {
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> null
        }
        if (prov == null) {
            locFailed()
            return
        }
        val holder = arrayOfNulls<LocationListener>(1)
        val timeout = Runnable {
            val l = holder[0]
            if (l != null) {
                try { lm.removeUpdates(l) } catch (e: SecurityException) { }
            }
            locFailed()
        }
        holder[0] = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                ui.removeCallbacks(timeout)
                onLocation(loc)
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        try {
            lm.requestSingleUpdate(prov, holder[0]!!, Looper.getMainLooper())
            ui.postDelayed(timeout, 20000)
        } catch (e: SecurityException) {
            locFailed()
        }
    }

    private fun locFailed() {
        Toast.makeText(this, R.string.err_loc, Toast.LENGTH_SHORT).show()
        if (place == null) showPlaces() else refresh()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) locate()
            else if (place == null) showPlaces() else refresh()
        }
    }

    private fun onLocation(l: Location) {
        val prev = place
        Thread {
            var name: String? = null
            try {
                if (Geocoder.isPresent()) {
                    val r = Geocoder(this, Locale.getDefault()).getFromLocation(l.latitude, l.longitude, 1)
                    if (r != null && r.isNotEmpty()) {
                        val ad = r[0]
                        name = ad.locality ?: ad.subAdminArea ?: ad.adminArea
                    }
                }
            } catch (e: Exception) {
                name = null
            }
            val nm = name ?: (if (prev != null && prev.gps) prev.name else getString(R.string.my_loc))
            ui.post { selectPlace(Place(nm, "", l.latitude, l.longitude, true)) }
        }.start()
    }

    // ---------------------------------------------------------------- dialogs

    private fun showUnits() {
        val items = arrayOf("°C · km/h · hPa", "°C · m/s · mmHg", "°F · mph · inHg")
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(R.string.units_title)
            .setSingleChoiceItems(items, preset) { dlg, which ->
                preset = which
                prefs.edit().putInt("preset", which).apply()
                fmt = Fmt(this, which)
                dlg.dismiss()
                render()
            }
            .show()
    }

    private fun showPlaces() {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(di(20f), di(8f), di(20f), 0)
        val input = EditText(this)
        input.hint = getString(R.string.search_hint)
        input.setSingleLine()
        input.imeOptions = EditorInfo.IME_ACTION_SEARCH
        box.addView(input, lp(MATCH, WRAP))
        val gps = Button(this)
        gps.text = getString(R.string.my_loc)
        box.addView(gps, lp(MATCH, WRAP))
        val list = ListView(this)
        box.addView(list, lp(MATCH, di(260f)))
        val hint = tv(12f)
        hint.alpha = 0.6f
        hint.text = getString(R.string.remove_hint)
        hint.setPadding(0, di(8f), 0, di(8f))
        box.addView(hint, lp(MATCH, WRAP))

        val dlg = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(R.string.places)
            .setView(box)
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        var items: List<Place> = ArrayList(saved)
        var fromSearch = false
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, ArrayList<String>())
        list.adapter = adapter

        fun fill() {
            adapter.clear()
            for (p in items) adapter.add(if (p.sub.isEmpty()) p.name else p.name + " · " + p.sub)
            adapter.notifyDataSetChanged()
            hint.visibility = if (!fromSearch && items.isNotEmpty()) View.VISIBLE else View.GONE
        }
        fill()

        var pending: Runnable? = null
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val q = s.toString().trim()
                val old = pending
                if (old != null) ui.removeCallbacks(old)
                if (q.length < 2) {
                    items = ArrayList(saved)
                    fromSearch = false
                    fill()
                } else {
                    val r = Runnable {
                        Thread {
                            try {
                                val res = Api.geocode(q, Locale.getDefault().language)
                                ui.post {
                                    if (input.text.toString().trim() == q) {
                                        items = res
                                        fromSearch = true
                                        fill()
                                        if (res.isEmpty()) Toast.makeText(this@MainActivity, R.string.no_results, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                ui.post { Toast.makeText(this@MainActivity, R.string.err_net, Toast.LENGTH_SHORT).show() }
                            }
                        }.start()
                    }
                    pending = r
                    ui.postDelayed(r, 350)
                }
            }
        })

        list.setOnItemClickListener { _, _, pos, _ ->
            if (pos < items.size) {
                val pick = items[pos]
                if (fromSearch) addSaved(pick)
                dlg.dismiss()
                selectPlace(pick)
            }
        }
        list.setOnItemLongClickListener { _, _, pos, _ ->
            if (!fromSearch && pos < saved.size) {
                saved.removeAt(pos)
                persistSaved()
                items = ArrayList(saved)
                fill()
                true
            } else {
                false
            }
        }
        gps.setOnClickListener {
            dlg.dismiss()
            locate()
        }

        val win = dlg.window
        if (win != null) {
            win.setSoftInputMode(
                if (place == null) WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                else WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            )
        }
        dlg.show()
    }
}
