package dutyroster.importer;

import dutyroster.importer.domain.DutyRosterDiff;
import dutyroster.importer.domain.DutyRosterMonth;
import dutyroster.importer.domain.DutyRosterShift;
import dutyroster.importer.service.DutyRosterConverterService;
import dutyroster.importer.service.DutyRosterDiffService;
import dutyroster.importer.service.DutyRosterShiftService;
import dutyroster.importer.service.GoogleCalendarService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * @author apohl
 */
public class DutyRosterStandaloneApplication {

    private static final Logger LOG = LoggerFactory.getLogger(DutyRosterStandaloneApplication.class);

    public static final String ARG_FILENAME = "-f";

    public static final String ARG_DRY_RUN = "-d";

    /**
     * This would be the main method. Rename it, to start the application from here. Spring boot get's confused if
     * there is another main method.
     *
     * @param args
     */
    public static void convertAndImportDutyRoster(String... args) {

        String filename;
        int year;
        int month;

        if (args.length > 0) {
            filename = parseFilenameFromArgs(args);

            if (!StringUtils.endsWith(filename, ".docx")) {
                LOG.error("filename [{}] not supported", filename);
                return;
            }

            String[] tokens = StringUtils.split(filename, "_");
            try {
                year = Integer.parseInt(tokens[0]);
                String strMonth = tokens[1];
                String[] monthTokens = StringUtils.split(strMonth, ".");
                month = Integer.parseInt(monthTokens[0]) - 1;
            } catch (NumberFormatException e) {
                LOG.error("can not parse year and month from [{}]", filename);
                return;
            }
        } else {
            Calendar cal = Calendar.getInstance();
            year = cal.get(Calendar.YEAR);
            // attention: this is current month + 1! JANUARY=0, so e.g. current month = NOVEMBER = 10
            // -> importing next month must be current + 2 (to get "2014_12") !!!
            month = cal.get(Calendar.MONTH) + 2;
            filename = year + "_" + month + ".docx";
        }

        boolean dryRun = isDryRun(args);
        LOG.info("importing [{}], year=[{}], month=[{}], dryRun=[{}]", filename, year, month, dryRun);
        InputStream is = DutyRosterStandaloneApplication.class
                .getResourceAsStream("/dutyrosterconverter/" + filename);

        DutyRosterShiftService dutyRosterShiftService = new DutyRosterShiftService();
        DutyRosterConverterService converterService = new DutyRosterConverterService(dutyRosterShiftService);
        try {
            DutyRosterMonth newRoster = converterService.extractDutyRoster(is, year);

            // for manual import:
            converterService.convert2Csv(newRoster, filename);

            // automatic import
            GoogleCalendarService calendarService = new GoogleCalendarService(dutyRosterShiftService);
            calendarService.setYear(year);
            calendarService.setMonth(month);
            calendarService.setDryRun(dryRun);
            DutyRosterMonth oldRoster = calendarService.readDutyRosterMonth();

            DutyRosterDiff dutyRosterDiff = buildDiff(oldRoster, newRoster);
            if (dutyRosterDiff.hasChanges()) {
                importDutyRosterIntoGoogleCalendar(calendarService, dutyRosterDiff);
            }
            sendUpdateEmail(dutyRosterDiff, dryRun);

        } catch (Exception e) {
            LOG.error("exception in main: ", e);
        }
    }

    public static String parseFilenameFromArgs(String... args) {
        if (args == null || args.length == 0) {
            return null;
        }

        for (String arg : args) {
            if (StringUtils.startsWith(arg, ARG_FILENAME)) {
                String filename = StringUtils.substring(arg, ARG_FILENAME.length());
                if (StringUtils.isEmpty(filename)) {
                    LOG.error("error parsing filename [{}] - no value", arg);
                    LOG.error("correct filename argument: '" + ARG_FILENAME + "thefilename.docx'");
                    return null;
                }
                return filename;
            }
        }
        return null;
    }

    public static boolean isDryRun(String... args) {
        if (args == null || args.length == 0) {
            return false;
        }

        for (String arg : args) {
            if (StringUtils.startsWith(arg, ARG_DRY_RUN)) {
                return true;
            }
        }
        return false;
    }

    private static void sendUpdateEmail(DutyRosterDiff dutyRosterDiff, boolean dryRun) throws EmailException {
        Email email = new SimpleEmail();
        email.setDebug(false);
        email.setAuthentication("apohl67@googlemail.com", "MeTw3636399");
        email.setHostName("smtp.googlemail.com");
        email.setSSLOnConnect(false);
        email.setSslSmtpPort("587");
        email.setStartTLSEnabled(true);
        email.setFrom("apohl67@googlemail.com");
        email.setSubject("Update des Dienstplans");

        StringBuilder sb = new StringBuilder();
        sb.append("Soeben wurde der Dienstplan aktualisiert.\n\n") //
                .append("Es wurden ") //
                .append(dutyRosterDiff.getNumberOfDeletions()) //
                .append(" Schichte(n) gelöscht, ") //
                .append(dutyRosterDiff.getNumberOfAddititions()) //
                .append(" Schichte(n) hinzugefügt und ") //
                .append(dutyRosterDiff.getNumberOfChanges()) //
                .append(" Schichte(n) geändert. \n") //
                .append(createReportForEmail(dutyRosterDiff));
        email.setMsg(sb.toString());

        email.addTo("ali@pohllaurien.de");
        email.addTo("conny@pohllaurien.de");
        if (!dryRun) {
            email.send();
        } else {
            LOG.info("Running in dryRun mode, email to send would be:\n{}", sb.toString());
        }
    }

    private static String createReportForEmail(DutyRosterDiff diff) {
        DutyRosterShiftService dutyRosterShiftService = new DutyRosterShiftService();

        SimpleDateFormat sdfDate = new SimpleDateFormat("dd.MM.");
        SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm");
        StringBuilder sb = new StringBuilder();

        if (diff.hasDeletions()) {
            sb.append("\n-------------------------------------------\n");
            sb.append("Folgende Schichten wurden gelöscht:\n\n");
            for (DutyRosterShift dutyRosterShift : diff.getDeletions()) {
                sb.append("Datum/Schicht: ") //
                        .append(sdfDate.format(dutyRosterShift.getFrom())) //
                        .append(" ")
                        .append(dutyRosterShiftService.createEventSummary(dutyRosterShift.getShift(), dutyRosterShift.getName())) //
                        .append(" von ") //
                        .append(sdfTime.format(dutyRosterShift.getFrom())) //
                        .append(" bis ") //
                        .append(sdfTime.format(dutyRosterShift.getTo())) //
                        .append("\n");
            }
        }

        if (diff.hasAdditions()) {
            sb.append("\n-------------------------------------------\n");
            sb.append("Folgende Schichten wurden hinzugefügt:\n\n");
            for (DutyRosterShift dutyRosterShift : diff.getAdditions()) {
                sb.append("Datum/Schicht: ") //
                        .append(sdfDate.format(dutyRosterShift.getFrom())) //
                        .append(" ")
                        .append(dutyRosterShiftService.createEventSummary(dutyRosterShift.getShift(), dutyRosterShift.getName())) //
                        .append(" von ") //
                        .append(sdfTime.format(dutyRosterShift.getFrom())) //
                        .append(" bis ") //
                        .append(sdfTime.format(dutyRosterShift.getTo())) //
                        .append("\n");
            }
        }

        if (diff.hasChanges()) {
            sb.append("\n-------------------------------------------\n");
            sb.append("Folgende Schichten wurden geändert:\n\n");
            for (DutyRosterDiff.Change change : diff.getChanges()) {
                sb.append("Datum/Schicht vorher:  ") //
                        .append(sdfDate.format(change.getBefore().getFrom())) //
                        .append(" ") //
                        .append(dutyRosterShiftService.createEventSummary(change.getBefore().getShift(), change.getBefore().getName())) //
                        .append(" von ") //
                        .append(sdfTime.format(change.getBefore().getFrom())) //
                        .append(" bis ") //
                        .append(sdfTime.format(change.getBefore().getTo())) //
                        .append("\n");

                sb.append("Datum/Schicht nachher: ") //
                        .append(sdfDate.format(change.getAfter().getFrom())) //
                        .append(" ") //
                        .append(dutyRosterShiftService.createEventSummary(change.getAfter().getShift(), change.getAfter().getName())) //
                        .append(" von ") //
                        .append(sdfTime.format(change.getAfter().getFrom())) //
                        .append(" bis ") //
                        .append(sdfTime.format(change.getAfter().getTo())) //
                        .append("\n\n");

            }
        }

        return sb.toString();
    }

    private static DutyRosterDiff buildDiff(DutyRosterMonth oldRoster, DutyRosterMonth newRoster) {
        DutyRosterDiffService diffService = new DutyRosterDiffService(new DutyRosterShiftService());
        DutyRosterDiff diff = diffService.diff(oldRoster, newRoster);
        if (diff.hasDifferences()) {
            LOG.info("We got changes: [{}] new events, [{}] events to be deleted, [{}] changed events",
                    diff.getOnlyBefore().getAllDutyRosterDays().size(),
                    diff.getOnlyAfter().getAllDutyRosterDays().size(),
                    diff.getChanges().size());
            LOG.debug(DutyRosterDiffService.toJson(diff));
        } else {
            LOG.info("no changes");
        }
        return diff;
    }

    private static void importDutyRosterIntoGoogleCalendar(GoogleCalendarService service, DutyRosterDiff
            diff) throws IOException, GeneralSecurityException {

        DutyRosterMonth onlyBefore = diff.getOnlyBefore();
        for (DutyRosterShift dutyRosterShift : onlyBefore.getAllDutyRosterDays()) {
            service.deleteEvent(dutyRosterShift);
        }

        DutyRosterMonth onlyAfter = diff.getOnlyAfter();
        for (DutyRosterShift dutyRosterShift : onlyAfter.getAllDutyRosterDays()) {
            service.addEvent(dutyRosterShift);
        }

        for (DutyRosterDiff.Change change : diff.getChanges()) {
            service.updateEvent(change.getAfter());
        }
    }

}
