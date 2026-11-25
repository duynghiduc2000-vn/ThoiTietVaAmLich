package com.mrwind.lunar.data

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.floor

object LunarCalendar {
    private val CAN = arrayOf("Giáp", "Ất", "Bính", "Đinh", "Mậu", "Kỷ", "Canh", "Tân", "Nhâm", "Quý")
    private val CHI = arrayOf("Tý", "Sửu", "Dần", "Mão", "Thìn", "Tỵ", "Ngọ", "Mùi", "Thân", "Dậu", "Tuất", "Hợi")
    private val WEEKDAYS = arrayOf("Chủ Nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy")

    private fun jdFromDate(dd: Int, mm: Int, yy: Int): Long {
        val a = (14 - mm) / 12
        val y = yy + 4800 - a
        val m = mm + 12 * a - 3
        var jd = dd.toLong() + (153L * m + 2) / 5 + 365L * y + y / 4 - y / 100 + y / 400 - 32045
        if (jd < 2299161) {
            jd = dd.toLong() + (153L * m + 2) / 5 + 365L * y + y / 4 - 32083
        }
        return jd
    }

    private fun getNewMoonDay(k: Long, timeZone: Double): Long {
        val t = k / 1236.85
        val t2 = t * t
        val t3 = t2 * t
        val dr = Math.PI / 180.0
        var jd1 = 2415020.75933 + 29.53058868 * k + 0.0001178 * t2 - 0.000000155 * t3
        jd1 += 0.00033 * Math.sin((166.56 + 132.87 * t - 0.009173 * t2) * dr)
        val m = 359.2242 + 29.10535608 * k - 0.0000333 * t2 - 0.00000347 * t3
        val mpr = 306.0253 + 385.81691806 * k + 0.0107306 * t2 + 0.00001236 * t3
        val f = 21.2964 + 390.67050646 * k - 0.0016528 * t2 - 0.00000239 * t3
        var c1 = (0.1734 - 0.000393 * t) * Math.sin(m * dr) + 0.0021 * Math.sin(2 * m * dr)
        c1 = c1 - 0.4068 * Math.sin(mpr * dr) + 0.0161 * Math.sin(2 * mpr * dr)
        c1 -= 0.0004 * Math.sin(3 * mpr * dr)
        c1 = c1 + 0.0104 * Math.sin(2 * f * dr) - 0.0051 * Math.sin((m + mpr) * dr)
        c1 -= 0.0074 * Math.sin((m - mpr) * dr)
        c1 = c1 + 0.0004 * Math.sin((2 * f + m) * dr) - 0.0004 * Math.sin((2 * f - m) * dr)
        c1 = c1 - 0.0006 * Math.sin((2 * f + mpr) * dr) + 0.0010 * Math.sin((2 * f - mpr) * dr)
        c1 += 0.0005 * Math.sin((m + 2 * mpr) * dr)
        val deltat = if (t < -11) {
            0.001 + 0.000839 * t + 0.0002261 * t2 - 0.00000845 * t3 - 0.000000081 * t3 * t
        } else {
            -0.000278 + 0.000265 * t + 0.000262 * t2
        }
        val jdNew = jd1 + c1 - deltat
        return floor(jdNew + 0.5 + timeZone / 24.0).toLong()
    }

    private fun getSunLongitude(jdn: Long, timeZone: Double): Int {
        val t = (jdn - 0.5 - timeZone / 24.0 - 2451545.0) / 36525.0
        val t2 = t * t
        val dr = Math.PI / 180.0
        val m = 357.52910 + 35999.05030 * t - 0.0001559 * t2 - 0.00000048 * t * t2
        val l0 = 280.46645 + 36000.76983 * t + 0.0003032 * t2
        var dl = (1.914600 - 0.004817 * t - 0.000014 * t2) * Math.sin(dr * m)
        dl = dl + (0.019993 - 0.000101 * t) * Math.sin(dr * 2 * m) + 0.000290 * Math.sin(dr * 3 * m)
        var l = l0 + dl
        l *= dr
        l -= Math.PI * 2 * floor(l / (Math.PI * 2))
        return floor(l / Math.PI * 6).toInt()
    }

    private fun getLunarMonth11(yy: Int, timeZone: Double): Long {
        val off = jdFromDate(31, 12, yy) - 2415021
        val k = floor(off / 29.530588853).toLong()
        var nm = getNewMoonDay(k, timeZone)
        val sunLong = getSunLongitude(nm, timeZone)
        if (sunLong >= 9) {
            nm = getNewMoonDay(k - 1, timeZone)
        }
        return nm
    }

    private fun getLeapMonthOffset(a11: Long, timeZone: Double): Int {
        val k = floor((a11 - 2415021.076998695) / 29.530588853 + 0.5).toLong()
        var last = 0
        var i = 1
        var arc = getSunLongitude(getNewMoonDay(k + i, timeZone), timeZone)
        do {
            last = arc
            i++
            arc = getSunLongitude(getNewMoonDay(k + i, timeZone), timeZone)
        } while (arc != last && i < 14)
        return i - 1
    }

    fun convertSolar2Lunar(dd: Int, mm: Int, yy: Int, timeZone: Double = 7.0): Triple<Int, Int, Int> {
        val dayNumber = jdFromDate(dd, mm, yy)
        val k = floor((dayNumber - 2415021.076998695) / 29.530588853).toLong()
        var monthStart = getNewMoonDay(k + 1, timeZone)
        if (monthStart > dayNumber) {
            monthStart = getNewMoonDay(k, timeZone)
        }
        var a11 = getLunarMonth11(yy, timeZone)
        var b11 = a11
        var lunarYear: Int
        if (a11 >= monthStart) {
            lunarYear = yy
            a11 = getLunarMonth11(yy - 1, timeZone)
        } else {
            lunarYear = yy + 1
            b11 = getLunarMonth11(yy + 1, timeZone)
        }
        val lunarDay = (dayNumber - monthStart + 1).toInt()
        val diff = floor((monthStart - a11) / 29.0).toInt()
        var lunarLeap = 0
        var lunarMonth = diff + 11
        if (b11 - a11 > 365) {
            val leapMonthDiff = getLeapMonthOffset(a11, timeZone)
            if (diff >= leapMonthDiff) {
                lunarMonth = diff + 10
                if (diff == leapMonthDiff) {
                    lunarLeap = 1
                }
            }
        }
        if (lunarMonth > 12) {
            lunarMonth -= 12
        }
        if (lunarMonth >= 11 && diff < 4) {
            lunarYear -= 1
        }
        return Triple(lunarDay, lunarMonth, lunarLeap)
    }

    fun getCanChiDay(dd: Int, mm: Int, yy: Int): String {
        val jd = jdFromDate(dd, mm, yy)
        val can = CAN[((jd + 9) % 10).toInt()]
        val chi = CHI[((jd + 1) % 12).toInt()]
        return "$can $chi"
    }

    fun getCanChiYear(yy: Int): String {
        val can = CAN[(yy + 6) % 10]
        val chi = CHI[(yy + 8) % 12]
        return "$can $chi"
    }

    fun formatSolarLine(cal: Calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"))): String {
        val dow = WEEKDAYS[(cal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)]
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val m = cal.get(Calendar.MONTH) + 1
        val y = cal.get(Calendar.YEAR)
        return "$dow • %02d/%02d/%04d".format(d, m, y)
    }

    fun formatLunarLine(cal: Calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"))): String {
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val m = cal.get(Calendar.MONTH) + 1
        val y = cal.get(Calendar.YEAR)
        val (lDay, lMonth, lLeap) = convertSolar2Lunar(d, m, y, 7.0)
        val dayCanChi = getCanChiDay(d, m, y)
        val leapText = if (lLeap == 1) " (Nhuận)" else ""
        return "Âm: %02d/%02d$leapText • Ngày $dayCanChi".format(lDay, lMonth)
    }
}
