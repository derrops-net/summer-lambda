package net.derrops.util

import java.text.SimpleDateFormat

class DateUtil {

    static def sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'")
    static def tz = TimeZone.getTimeZone("Australia/Melbourne")

    static String nowAsISO() {
        return format(new Date())
    }
    static String format(Date date) {
        TimeZone tz = tz
        sdf.setTimeZone(tz)
        return sdf.format(date)
    }

}
