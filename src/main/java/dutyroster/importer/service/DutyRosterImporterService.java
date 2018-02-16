package dutyroster.importer.service;

import dutyroster.importer.domain.DutyRosterDiff;
import dutyroster.importer.domain.DutyRosterMonth;
import dutyroster.importer.domain.DutyRosterShift;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;

/**
 * @author apohl
 */
@Service
@Slf4j
public class DutyRosterImporterService {

    private DutyRosterConverterService converterService;

    private GoogleCalendarService calendarService;

    private DutyRosterDiffService diffService;

    @Autowired
    public DutyRosterImporterService(DutyRosterConverterService converterService, GoogleCalendarService calendarService,
                                     DutyRosterDiffService diffService) {
        this.converterService = converterService;
        this.calendarService = calendarService;
        this.diffService = diffService;
    }

    /**
     * @param is        the input stream associated with the docx to be imported
     * @param filename  the original name of the input file
     * @param dryRun    if <code>true</code>, the import will be tested only. No modifications of the Google calendar
     *                  will be carried out, no emails will be sent. If <code>false</code>, the calendar will be
     *                  modified actually and the results will be sent via email to the configured recipients.
     * @param createCsv if <code>true</code>, a CSV file will be created from the input file. This file may be imported
     *                  manually to any Google calendar.
     * @return the {@link dutyroster.importer.domain.DutyRosterDiff} build from the input file and the current Google
     * calendar
     */
    public DutyRosterDiff convertAndImportDutyRoster(InputStream is, String filename, boolean dryRun,
                                                     boolean createCsv, Integer year, Integer month) {

        if (!StringUtils.endsWith(filename, ".docx")) {
            log.error("filename [{}] not supported", filename);
            throw new RuntimeException("other files than docx are not supported");
        }

        if (month == null || year == null) {
            // try to get month and year from filename
            String[] tokens = StringUtils.split(filename, "_");
            try {
                year = Integer.parseInt(tokens[0]);
                String strMonth = tokens[1];
                String[] monthTokens = StringUtils.split(strMonth, ".");
                month = Integer.parseInt(monthTokens[0]) - 1;
            } catch (NumberFormatException e) {
                log.error("can not parse year and month from [{}]", filename);
                throw new RuntimeException(e);
            }
        }

        log.info("importing [{}], year=[{}], month=[{}], dryRun=[{}], createCsv=[{}]", filename, year, month, dryRun,
                createCsv);

        try {
            DutyRosterMonth newRoster = converterService.extractDutyRoster(is, year);

            if (createCsv) {
                // for manual import:
                converterService.convert2Csv(newRoster, filename);
            }

            // automatic import
            calendarService.setMonth(month);
            calendarService.setYear(year);
            calendarService.setDryRun(dryRun);
            DutyRosterMonth oldRoster = calendarService.readDutyRosterMonth();

            DutyRosterDiff dutyRosterDiff = buildDiff(oldRoster, newRoster);
            if (dutyRosterDiff.hasDifferences()) {
                importDutyRosterIntoGoogleCalendar(dutyRosterDiff);
            }
            return dutyRosterDiff;

        } catch (Exception e) {
            log.error("exception in main: ", e);
            throw new RuntimeException(e);
        }

    }

    /**
     * Builds the before/after diff of the given {@link dutyroster.importer.domain.DutyRosterMonth}.
     *
     * @param oldRoster the old duty roster
     * @param newRoster the new duty roster
     * @return the diff
     */
    private DutyRosterDiff buildDiff(DutyRosterMonth oldRoster, DutyRosterMonth newRoster) {
        DutyRosterDiff diff = diffService.diff(oldRoster, newRoster);
        if (diff.hasDifferences()) {
            log.info("We got changes: [{}] new events, [{}] events to be deleted, [{}] changed events",
                    diff.getOnlyBefore().getAllDutyRosterDays().size(),
                    diff.getOnlyAfter().getAllDutyRosterDays().size(),
                    diff.getChanges().size());
            log.debug(DutyRosterDiffService.toJson(diff));
        } else {
            log.info("no changes");
        }
        return diff;
    }

    /**
     * Imports the changes from the given {@link dutyroster.importer.domain.DutyRosterDiff} into the configured Google
     * calendar.
     *
     * @param diff the diff to be imported. Contains new shifts, changed shifts and deleted shifts.
     * @throws IOException
     * @throws GeneralSecurityException
     */
    private void importDutyRosterIntoGoogleCalendar(DutyRosterDiff diff) throws IOException, GeneralSecurityException {

        DutyRosterMonth onlyBefore = diff.getOnlyBefore();
        for (DutyRosterShift dutyRosterShift : onlyBefore.getAllDutyRosterDays()) {
            calendarService.deleteEvent(dutyRosterShift);
        }

        DutyRosterMonth onlyAfter = diff.getOnlyAfter();
        for (DutyRosterShift dutyRosterShift : onlyAfter.getAllDutyRosterDays()) {
            calendarService.addEvent(dutyRosterShift);
        }

        for (DutyRosterDiff.Change change : diff.getChanges()) {
            calendarService.updateEvent(change.getAfter());
        }
    }

}
