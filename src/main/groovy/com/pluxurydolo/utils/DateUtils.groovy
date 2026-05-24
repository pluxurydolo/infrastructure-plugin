package com.pluxurydolo.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DateUtils {
    static String getCurrentDate() {
        Date now = new Date()
        return now.format('yyyy-MM-dd')
    }

    static boolean isDateAfter(String date1, String date2) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern('yyyy-MM-dd')
        LocalDate localDate1 = LocalDate.parse(date1, formatter)
        LocalDate localDate2 = LocalDate.parse(date2, formatter)
        return localDate1.isAfter(localDate2)
    }
}
