package dutyroster.importer.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.SecurityUtils;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.google.api.services.calendar.model.Settings;
import dutyroster.importer.domain.DutyRosterMonth;
import dutyroster.importer.domain.DutyRosterShift;
import dutyroster.importer.domain.Shift;
import dutyroster.importer.util.DateUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.util.*;

/**
 * Reads and writes events from and to a Google calendar. The calendar must have been authorized for requests. See
 * <a href="https://developers.google.com/google-apps/calendar/auth">Authorizing Requests to the Google Calendar API</a>.
 *
 * @author apohl
 */
@Service
public class GoogleCalendarService {

    private final Logger log = LoggerFactory.getLogger(GoogleCalendarService.class);

    @Value(value = "${dutyroster.importer.service.calendar.calendarId}")
    private String calendarId;

    @Value(value = "${dutyroster.importer.service.calendar.accountId}")
    private String accountId;

    @Value(value = "${dutyroster.importer.service.calendar.p12}")
    private String p12ResourcePath;

    @Value(value = "${dutyroster.importer.service.calendar.application}")
    private String applicationId;

    private int year;

    private int month;

    private Calendar client;

    private boolean dryRun;

    private DutyRosterShiftService dutyRosterShiftService;

    @Autowired
    public GoogleCalendarService(DutyRosterShiftService dutyRosterShiftService) {
        this.dutyRosterShiftService = dutyRosterShiftService;
    }

    /**
     * Deletes an event from the Google calendar. The event is identified by the
     * {@link dutyroster.importer.domain.DutyRosterShift#getEventId()}.
     * <p>
     * If {@link #dryRun} is <code>true</code>, the
     * action is not carried out actually but only logged.
     * </p>
     *
     * @param shift the {@link dutyroster.importer.domain.DutyRosterShift} corresponding with the event
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public void deleteEvent(DutyRosterShift shift) throws IOException, GeneralSecurityException {
        log.debug("[dryRun=[{}] deleting event [{}]", dryRun, shift.getEventId());
        if (!dryRun) {
            getClient().events().delete(calendarId, shift.getEventId()).execute();
        }
    }

    /**
     * Adds an event to the Google calendar. The event is identified by the
     * {@link dutyroster.importer.domain.DutyRosterShift#getEventId()}.
     * <p>
     * If {@link #dryRun} is <code>true</code>, the
     * action is not carried out actually but only logged.
     * </p>
     *
     * @param shift the {@link dutyroster.importer.domain.DutyRosterShift} corresponding with the event
     * @return the outcome of the api operation or null if <code>dryRun</code>
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public Event addEvent(DutyRosterShift shift) throws IOException, GeneralSecurityException {
        Event event = createEvent(shift);
        log.debug("[dryRun=[{}], adding event [{}]", dryRun, event);
        if (!dryRun) {
            Event result = getClient().events().insert(calendarId, event).execute();
            return result;
        } else {
            return null;
        }
    }

    /**
     * /**
     * Updates an event of the Google calendar. The event is identified by the
     * {@link dutyroster.importer.domain.DutyRosterShift#getEventId()}.
     * <p>
     * If {@link #dryRun} is <code>true</code>, the
     * action is not carried out actually but only logged.
     * </p>
     *
     * @param shift the {@link dutyroster.importer.domain.DutyRosterShift} corresponding with the event
     * @return the outcome of the api operation or null if <code>dryRun</code>
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public Event updateEvent(DutyRosterShift shift) throws IOException, GeneralSecurityException {
        Event event = createEvent(shift);
        log.info("[dryRun=[{}] updating event [{}]", dryRun, event);
        if (!dryRun) {
            Event result = getClient().events().update(calendarId, shift.getEventId(), event).execute();
            return result;
        } else {
            return null;
        }
    }

    /**
     * Logs the calendar settings on debug level.
     *
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public void getSettings() throws IOException, GeneralSecurityException {
        Settings settings = getClient().settings().list().execute();
        log.debug("calendar settings: [{}]", settings);
    }

    /**
     * Log all calendar events of the given month (from the last day of the prior month at 12:00h
     * to the first day of the next month at 12:00h) console ordered by the start time on info level.
     *
     * @throws java.io.IOException
     */
    public void showEvents() throws IOException, GeneralSecurityException {

        for (Event item : listEvents()) {
            log.info(item.toPrettyString());
        }
    }

    /**
     * Reads all events within the current month from the Google calendar.
     *
     * @return the list of events of the current {@link #year} and {@link #month}
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public List<Event> listEvents() throws IOException, GeneralSecurityException {
        Date date = DateUtil.createDate(year, month);
        DateTime timeMin = new DateTime(date);
        DateTime timeMax = new DateTime(DateUtils.addMonths(date, 1));

        Events events = getClient().events() //
                .list(calendarId) //
                .setSingleEvents(true) //
                .setOrderBy("startTime") // requires setSingleEvents(true)
                .setTimeMin(timeMin) //
                .setTimeMax(timeMax) //
                .execute();
        List<Event> items = events.getItems();

        List<Event> filteredEvents = new ArrayList<>();
        for (Event item : items) {
            EventDateTime start = item.getStart();
            DateTime startDate = start.getDateTime();
            if (startDate.getValue() >= timeMin.getValue()) {
                filteredEvents.add(item);
            } else {
                log.info("ignoring event [{}] - start time [{}] is before [{}]", item, startDate, timeMin);
            }
        }
        return filteredEvents;
    }

    /**
     * Reads all events of the current {@link #year} and {@link #month} and converts them into a
     * {@link dutyroster.importer.domain.DutyRosterMonth}. Events not having a parseable event summary (i.e. not
     * appearing to be a duty roster shift) will be ignored.
     *
     * @return
     * @throws IOException
     * @throws GeneralSecurityException
     */
    public DutyRosterMonth readDutyRosterMonth() throws IOException, GeneralSecurityException {
        log.info("reading events of year/month [{}/{}]", year, month);
        List<Event> events = listEvents();
        DutyRosterMonth dutyRosterMonth = new DutyRosterMonth(year, month);
        for (Event event : events) {
            Date start = new Date(event.getStart().getDateTime().getValue());
            Date end = new Date(event.getEnd().getDateTime().getValue());
            String summary = event.getSummary();
            String[] tokens = dutyRosterShiftService.parseEventSummary(summary);
            if (tokens != null) {
                Shift shift = Shift.parseLabel(tokens[0]);
                String id = DutyRosterMonth.createId(shift, start);
                DutyRosterShift dutyRosterShift = new DutyRosterShift(id, start, end, tokens[1], shift);
                dutyRosterShift.setEventId(event.getId());
                dutyRosterShift.setSequence(event.getSequence());
                dutyRosterMonth.addDutyRosterShift(dutyRosterShift);
            } else {
                log.info("ignoring event with summary [{}]", summary);
            }
        }
        return dutyRosterMonth;
    }

    /**
     * Initializes the connection to the Google calendar.
     *
     * @throws IOException
     * @throws GeneralSecurityException
     */
    private void init() throws IOException, GeneralSecurityException {

        Validate.notNull(year, "year is not set");
        Validate.notNull(month, "month is not set");

        PrivateKey privateKey = SecurityUtils.loadPrivateKeyFromKeyStore(SecurityUtils
                        .getPkcs12KeyStore(), GoogleCalendarService.class
                        .getResourceAsStream(p12ResourcePath),
                "notasecret", "privatekey", "notasecret");

        GoogleCredential credentials = new GoogleCredential.Builder()
                .setTransport(GoogleNetHttpTransport.newTrustedTransport())
                .setJsonFactory(new GsonFactory())
                        // calendar must be visible for email:
                .setServiceAccountId(accountId)
                        // api "calendar" must be enabled for project (see
                        // https://console.developers.google.com/project?authuser=0)
                .setServiceAccountScopes(Arrays.asList("https://www.googleapis.com/auth/calendar"))
                        // private P12 key has been generated in the google developer console
                .setServiceAccountPrivateKey(privateKey).build();

        this.client = new Calendar.Builder(GoogleNetHttpTransport.newTrustedTransport(),
                new GsonFactory(), credentials).setApplicationName(applicationId).build();

        getSettings();
    }

    /**
     * @return the {@link com.google.api.services.calendar.Calendar} client
     * @throws IOException
     * @throws GeneralSecurityException
     */
    private Calendar getClient() throws IOException, GeneralSecurityException {
        if (client == null) {
            init();
        }
        return client;
    }

    /**
     * Converts the given shift into a Google calendar event.
     *
     * @param shift the {@link dutyroster.importer.domain.DutyRosterShift}
     * @return the Google calendar event
     */
    private Event createEvent(DutyRosterShift shift) {
        Event event = new Event();
        event.setSummary(dutyRosterShiftService.createEventSummary(shift.getShift(), shift.getName()));
        DateTime start = new DateTime(shift.getFrom(), TimeZone.getTimeZone("GMT"));
        event.setStart(new EventDateTime().setDateTime(start));
        DateTime end = new DateTime(shift.getTo(), TimeZone.getTimeZone("GMT"));
        event.setEnd(new EventDateTime().setDateTime(end));
        if (StringUtils.isNotEmpty(shift.getEventId())) {
            event.setId(shift.getEventId());
        }
        if (shift.getSequence() > 0) {
            event.setSequence(shift.getSequence() + 1);
        }
        return event;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public void setMonth(int month) {
        this.month = month;
    }
}
