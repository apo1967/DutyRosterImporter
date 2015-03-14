package dutyroster.importer.domain;

import dutyroster.importer.util.DateUtil;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.util.*;

/**
 * A duty roster covering a whole month. Contains n shifts per dutyRosterShift of the month.
 *
 * @author apohl
 */
public class DutyRosterMonth {

    /**
     * Determines the year and month of the duty roster.
     */
    private Date date;

    /**
     * The duty roster - the list of {@link dutyroster.importer.domain.DutyRosterShift}s as values grouped by the
     * day as key.
     */
    private Map<Integer, Set<DutyRosterShift>> dutyRosterDays = new HashMap<>();

    public DutyRosterMonth(int year, int month) {
        this.date = DateUtil.createDate(year, month);
    }

    /**
     * Creates a dutyRosterShift with the given params and adds it to this duty roster.
     *
     * @param shift  the {@link dutyroster.importer.domain.Shift}
     * @param day    the dutyRosterShift of the dutyRosterShift
     * @param person the name of the personnel who's in charge
     */
    public void addDutyRosterShift(Shift shift, int day, String person) {
        addDutyRosterShift(createShift(shift, day, person));
    }

    /**
     * Creates a {@link dutyroster.importer.domain.DutyRosterShift} with the given params.
     *
     * @param shift  the {@link dutyroster.importer.domain.Shift}
     * @param day    the dutyRosterShift of the dutyRosterShift
     * @param person the name of the personnel
     * @return the {@link dutyroster.importer.domain.DutyRosterShift}
     */
    public DutyRosterShift createShift(Shift shift, int day, String person) {
        Date from = createFrom(shift, day);
        Date to = createTo(shift, day);
        String id = createId(shift, from);
        return new DutyRosterShift(id, from, to, person, shift);
    }

    /**
     * Adds the given shift to the duty roster of this month.
     *
     * @param dutyRosterShift The shift
     */
    public void addDutyRosterShift(DutyRosterShift dutyRosterShift) {
        Set<DutyRosterShift> days = dutyRosterDays.get(dutyRosterShift.getDay());
        if (days == null) {
            days = new HashSet<>();
        }
        days.add(dutyRosterShift);
        dutyRosterDays.put(dutyRosterShift.getDay(), days);
    }

    /**
     * Creates the "FROM" timestamp for the Google calendar event. Rules are:
     * <ul>
     * <li>{@link dutyroster.importer.domain.Shift#EARLY_SHIFT} is from 07:00h</li>
     * <li>{@link dutyroster.importer.domain.Shift#LATE_SHIFT} is from 14:00h</li>
     * <li>{@link dutyroster.importer.domain.Shift#NIGHT_SHIFT} is from 21:00h on Fridays or Saturdays,
     * else from 22:00h</li>
     * </ul>
     *
     * @param shift the {@link dutyroster.importer.domain.Shift}
     * @param day   the day of the shift
     * @return the FROM timestamp based on the {@link #date}
     */
    private Date createFrom(Shift shift, int day) {
        Calendar cal = DateUtil.getCalendar(this.date);
        cal.set(Calendar.DAY_OF_MONTH, day);

        switch (shift) {
            case EARLY_SHIFT:
                cal.set(Calendar.HOUR_OF_DAY, 7);
                break;
            case LATE_SHIFT:
                cal.set(Calendar.HOUR_OF_DAY, 14);
                break;
            case NIGHT_SHIFT:
                int weekday = cal.get(Calendar.DAY_OF_WEEK);
                if (weekday == Calendar.FRIDAY || weekday == Calendar.SATURDAY) {
                    cal.set(Calendar.HOUR_OF_DAY, 21);
                } else {
                    cal.set(Calendar.HOUR_OF_DAY, 22);
                }
                break;
        }
        return cal.getTime();
    }

    /**
     * Creates the "TO" timestamp for the Google calendar event. Rules are:
     * <ul>
     * <li>{@link dutyroster.importer.domain.Shift#EARLY_SHIFT} is until 14:00h</li>
     * <li>{@link dutyroster.importer.domain.Shift#LATE_SHIFT} is until 20:00h</li>
     * <li>{@link dutyroster.importer.domain.Shift#NIGHT_SHIFT} is until 07:00h of the next day</li>
     * </ul>
     *
     * @param shift the {@link dutyroster.importer.domain.Shift}
     * @param day   the day of the shift
     * @return the "TO" timestamp based on the {@link #date}
     */
    private Date createTo(Shift shift, int day) {
        Calendar cal = DateUtil.getCalendar(this.date);
        cal.set(Calendar.DAY_OF_MONTH, day);

        switch (shift) {
            case EARLY_SHIFT:
                cal.set(Calendar.HOUR_OF_DAY, 14);
                break;
            case LATE_SHIFT:
                cal.set(Calendar.HOUR_OF_DAY, 20);
                break;
            case NIGHT_SHIFT:
                cal.set(Calendar.HOUR_OF_DAY, 7);
                cal.setTime(DateUtils.addDays(cal.getTime(), 1));
                break;
        }
        return cal.getTime();
    }

    /**
     * @return the {@link #date}
     */
    public Date getDate() {
        return date;
    }

    /**
     * @return the {@link #dutyRosterDays}
     */
    public Map<Integer, Set<DutyRosterShift>> getDutyRosterDays() {
        return dutyRosterDays;
    }

    /**
     * @return all {@link dutyroster.importer.domain.DutyRosterShift}s of this month in a single set
     * (contrary to {@link #dutyRosterDays}, which is grouped by day)
     */
    public Set<DutyRosterShift> getAllDutyRosterDays() {
        Set<DutyRosterShift> allDutyRosterShifts = new TreeSet<>();
        for (Set<DutyRosterShift> rosterDays : dutyRosterDays.values()) {
            allDutyRosterShifts.addAll(rosterDays);
        }
        return allDutyRosterShifts;
    }

    /**
     * Creates a unique shift id.
     *
     * @param shift the {@link dutyroster.importer.domain.Shift}
     * @param from  the start date and time
     * @return the id, e.g. "2015_02_22_2"
     */
    public static String createId(Shift shift, Date from) {
        FastDateFormat fdf = FastDateFormat.getInstance("yyyy_MM_dd_");
        String id = fdf.format(from);
        id += shift.ordinal();
        return id;
    }


}
