/*
 * $Id: JavaTemplates.xml 53870 2013-02-12 10:32:44Z tlangfeld $
 */
package dutyroster.importer.util;

import java.util.Calendar;
import java.util.Date;

/**
 * @author apohl
 */
public class DateUtil {

    public static Calendar getCalendar(Date date) {
        Calendar instance = Calendar.getInstance();
        instance.setTime(date);
        return instance;
    }

    public static int getMonth(Date date) {
        return getCalendar(date).get(Calendar.MONTH);
    }

    public static int getYear(Date date) {
        return getCalendar(date).get(Calendar.YEAR);
    }

    public static int getDay(Date date) {
        return getCalendar(date).get(Calendar.DAY_OF_MONTH);
    }

    public static Date createDateForCurrentYear(int month) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.HOUR_OF_DAY, 12);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        return cal.getTime();
    }

    public static Date createDate(int year, int month) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        return cal.getTime();
    }
}
