package dutyroster.importer.domain;

import dutyroster.importer.util.DateUtil;

import java.util.Date;

/**
 * Implements a shift in a duty roster.
 *
 * @author apohl
 */
public class DutyRosterShift implements Comparable<DutyRosterShift> {

    /**
     * An unique id containing the year, month, day and shift ordinal, e.g. "2015_02_22_2" (a late shift at 2015/02/22).
     * Important to make the schedule updatable.
     */
    private String id;

    /**
     * The start date and time of the shift.
     */
    private Date from;

    /**
     * The end date and time of the shift.
     */
    private Date to;

    /**
     * The name of the personnel.
     */
    private String name;

    /**
     * The id of the google Event, this instance may refer to.
     */
    private String eventId;

    private int sequence;

    /**
     * The {@link Shift} this instance applies to.
     */
    private Shift shift;

    public DutyRosterShift(String id, Date from, Date to, String name, Shift shift) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.name = name;
        this.shift = shift;
    }

    public String getId() {
        return id;
    }

    public Date getFrom() {
        return from;
    }

    public Date getTo() {
        return to;
    }

    public String getName() {
        return name;
    }

    public Shift getShift() {
        return shift;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public int getDay() {
        if (from == null) {
            return -1;
        }

        return DateUtil.getDay(from);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        DutyRosterShift that = (DutyRosterShift) o;

        return !(id != null ? !id.equals(that.id) : that.id != null);

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public int compareTo(DutyRosterShift o) {
        return this.id.compareTo(o.getId());
    }

    @Override
    public String toString() {
        return "DutyRosterShift{" + "id='" + id + '\'' + ", from=" + from + ", to=" + to
                + ", name='" + name + '\'' + ", shift=" + shift + '}';
    }
}
