/*
 * $Id: JavaTemplates.xml 53870 2013-02-12 10:32:44Z tlangfeld $
 */
package dutyroster.importer.service;

import dutyroster.importer.domain.DutyRosterMonth;
import dutyroster.importer.domain.DutyRosterShift;
import dutyroster.importer.domain.Shift;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * A utility class providing common functionality for dealing with Google calendar events and
 * {@link dutyroster.importer.domain.DutyRosterShift}
 * s.
 *
 * @author apohl
 */
@Service
@Slf4j
public class DutyRosterShiftService {

    private static final String SEPARATOR_FOR_EVENT_SUMMARY = ": ";

    /**
     * @param set   the set of {@link dutyroster.importer.domain.DutyRosterShift} to search within
     * @param shift the shift to find
     * @return the shift, if it exists, or null, if no such shift exits
     */
    public DutyRosterShift findDutyRosterShift(Set<DutyRosterShift> set,
                                               DutyRosterShift shift) {

        if (set == null || set.isEmpty()) {
            return null;
        }

        for (DutyRosterShift dutyRosterShift : set) {
            if (dutyRosterShift.equals(shift)) {
                return dutyRosterShift;
            }
        }
        return null;
    }

    /**
     * @param shift the {@link dutyroster.importer.domain.Shift}
     * @param name  the name of the personnel
     * @return the event summary, e.g. "FD Jane"
     */
    public String createEventSummary(Shift shift, String name) {
        return shift.getLabel() + SEPARATOR_FOR_EVENT_SUMMARY + name;
    }

    /**
     * @param summary an event summary, e.g. "SD: Benny"
     * @return the tokens of the summary or null, if the summary label (e.g. "SD") is not parsable (e.g. "Krank! SD")
     */
    public String[] parseEventSummary(String summary) {
        if (StringUtils.isEmpty(summary) || !StringUtils.contains(summary, SEPARATOR_FOR_EVENT_SUMMARY)) {
            log.error("can not parse event [{}]", summary);
            return null;
        }

        String[] split = StringUtils.splitByWholeSeparator(summary, SEPARATOR_FOR_EVENT_SUMMARY);

        // throws IllegalArgumentException if fails
        try {
            Shift.parseLabel(split[0]);
        } catch (IllegalArgumentException e) {
            log.error("can not parse event [{}]", summary);
            return null;
        }

        return split;
    }

    /**
     * @param dutyRosterMonth the {@link dutyroster.importer.domain.DutyRosterMonth} to search within
     * @param shift           the {@link dutyroster.importer.domain.Shift} to find
     * @param day             the day of the shift to find
     * @return see {@link dutyroster.importer.service.DutyRosterShiftService#findDutyRosterShift(java.util.Set, DutyRosterShift)}
     */
    public DutyRosterShift findShift(DutyRosterMonth dutyRosterMonth, Shift shift, int day) {
        DutyRosterShift dutyRosterShift = dutyRosterMonth.createShift(shift, day, null);
        return findDutyRosterShift(dutyRosterMonth.getAllDutyRosterDays(), dutyRosterShift);
    }


}
