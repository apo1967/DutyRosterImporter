/*
 * $Id: JavaTemplates.xml 53870 2013-02-12 10:32:44Z tlangfeld $
 */
package dutyroster.importer.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dutyroster.importer.domain.DutyRosterDiff;
import dutyroster.importer.domain.DutyRosterMonth;
import dutyroster.importer.domain.DutyRosterShift;
import dutyroster.importer.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTimeComparator;
import org.joda.time.DateTimeFieldType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Builds {@link dutyroster.importer.domain.DutyRosterDiff}s.
 *
 * @author apohl
 */
@Service
@Slf4j
public class DutyRosterDiffService {

    private DutyRosterShiftService dutyRosterShiftService;

    @Autowired
    public DutyRosterDiffService(DutyRosterShiftService dutyRosterShiftService) {
        this.dutyRosterShiftService = dutyRosterShiftService;
    }

    /**
     * @param dutyRoster1 the first (before) {@link dutyroster.importer.domain.DutyRosterMonth}
     * @param dutyRoster2 the second (after) {@link dutyroster.importer.domain.DutyRosterMonth}
     * @return the {@link dutyroster.importer.domain.DutyRosterDiff}
     */
    public DutyRosterDiff diff(DutyRosterMonth dutyRoster1, DutyRosterMonth dutyRoster2) {
        DutyRosterDiff diff = new DutyRosterDiff(DateUtil.getYear(dutyRoster1.getDate()),
                DateUtil.getMonth(dutyRoster1.getDate()));

        Collection onlyIn1 = CollectionUtils.subtract(dutyRoster1.getAllDutyRosterDays(),
                dutyRoster2.getAllDutyRosterDays());
        diff.setOnlyBefore(createDutyRosterMonth(dutyRoster1.getDate(), onlyIn1));

        Collection onlyIn2 = CollectionUtils.subtract(dutyRoster2.getAllDutyRosterDays(),
                dutyRoster1.getAllDutyRosterDays());
        diff.setOnlyAfter(createDutyRosterMonth(dutyRoster2.getDate(), onlyIn2));

        diff.setChanges(findChangesOfPersonnel(dutyRoster1.getAllDutyRosterDays(),
                dutyRoster2.getAllDutyRosterDays()));

        return diff;
    }

    /**
     * @param date             the source date determining year and month of the duty roster to be created
     * @param dutyRosterShifts the shifts of the duty roster to be created
     * @return the new {@link dutyroster.importer.domain.DutyRosterMonth}
     */
    private DutyRosterMonth createDutyRosterMonth(Date date,
                                                  Collection<DutyRosterShift> dutyRosterShifts) {

        DutyRosterMonth dutyRosterMonth = new DutyRosterMonth(DateUtil.getYear(date),
                DateUtil.getMonth(date));
        for (DutyRosterShift dutyRosterShift : dutyRosterShifts) {
            dutyRosterMonth.addDutyRosterShift(dutyRosterShift);
        }
        return dutyRosterMonth;
    }

    /**
     * @param set1 the set of old shifts
     * @param set2 the set of new shifts
     * @return the list of shifts which have changed personnel
     */
    private List<DutyRosterDiff.Change> findChangesOfPersonnel(Set<DutyRosterShift> set1,
                                                               Set<DutyRosterShift> set2) {

        List<DutyRosterDiff.Change> changes = new ArrayList<>();

        DateTimeComparator comparator = DateTimeComparator.getInstance(
                DateTimeFieldType.minuteOfHour());

        Collection intersection = CollectionUtils.intersection(set1, set2);
        for (Object o : intersection) {
            DutyRosterShift shift = (DutyRosterShift) o;
            DutyRosterShift shiftIn1 = dutyRosterShiftService.findDutyRosterShift(set1, shift);
            DutyRosterShift shiftIn2 = dutyRosterShiftService.findDutyRosterShift(set2, shift);

            // compare by name, start and end
            if (!StringUtils.equals(shiftIn1.getName(), shiftIn2.getName()) ||
                    comparator.compare(shiftIn1.getFrom().getTime(), shiftIn2.getFrom().getTime()) != 0 ||
                    comparator.compare(shiftIn1.getTo().getTime(), shiftIn2.getTo().getTime()) != 0) {
                log.debug("shift changed from [{}] to [{}]", shiftIn1, shiftIn2);
                shiftIn2.setEventId(shiftIn1.getEventId());
                shiftIn2.setSequence(shiftIn1.getSequence());
                changes.add(new DutyRosterDiff.Change(shiftIn1, shiftIn2));
            }
        }
        return changes;
    }

    /**
     * @param diff the {@link dutyroster.importer.domain.DutyRosterDiff}
     * @return a Json representation of the given diff
     */
    public static String toJson(DutyRosterDiff diff) {

        if (diff == null) {
            return "null";
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(diff);
    }
}
