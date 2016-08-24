package dutyroster.importer.service;

import dutyroster.importer.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.Set;

/**
 * @author apo
 * @since 24.08.2016 22:04
 */
@Service
public class DutyRosterStatisticsService {

    private final Logger log = LoggerFactory.getLogger(DutyRosterStatisticsService.class);

    public DutyRosterStatistics createDutyRosterStatistics(DutyRosterMonth dutyRosterMonth) {
        DutyRosterStatistics statistics = new DutyRosterStatistics();

        Instant instant = Instant.ofEpochMilli(dutyRosterMonth.getDate().getTime());
        LocalDate date = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalDate();

        int noOfAssignableEarlyShifts, noOfAssignableLateShifts, noOfAssignableNightShifts = 0;

        int lastDay = LocalDate.of(date.getYear(), date.getMonth().plus(1), 1).minusDays(1).getDayOfMonth();
        log.info("[findNoOfShifts] lastDay of month [{}]: [{}]", date.getMonth(), lastDay);

        for (int i = 1; i <= lastDay; i++) {
            LocalDate day = LocalDate.of(date.getYear(), date.getMonth(), i);

            statistics.increaseNoOfPossibleEarlyShifts();
            statistics.increaseNoOfPossibleNightShifts();

            if (!day.getDayOfWeek().equals(DayOfWeek.SATURDAY) && !day.getDayOfWeek().equals(DayOfWeek.SATURDAY)) {
                statistics.increaseNoOfPossibleLateShifts();
            }

            DutyRosterShift earlyShift = addAssignedDutyRosterShiftOfDay(i, dutyRosterMonth, statistics);
            if (earlyShift != null) {
                statistics.getNoOfAssignedEarlyShifts();
                ShiftAssignee shiftAssignee = statistics.getAssigness().get(earlyShift.getName());
                if (shiftAssignee == null) {
                    shiftAssignee = new ShiftAssignee();
                    shiftAssignee.setName(earlyShift.getName());
                    statistics.getAssigness().put(shiftAssignee.getName(), shiftAssignee);
                }
                shiftAssignee.increaseNoOfEarlyShifts();
            }
        }

        // calculate percentages of assigned shifts per assignee
        for (ShiftAssignee shiftAssignee : statistics.getAssigness().values()) {
            double percentageOfTotalShifts = shiftAssignee.getTotalNoOfShifts() * 100 / statistics.getTotalNoOfAssignedShifts();
            shiftAssignee.setAssignedShiftsPercentage(percentageOfTotalShifts);
        }

        statistics.setPercentageOfAssignedShifts(statistics.getTotalNoOfAssignedShifts() * 100 / statistics.getTotalNoOfPossibleShifts());

        return statistics;
    }

    private DutyRosterShift addAssignedDutyRosterShiftOfDay(int day, DutyRosterMonth month, DutyRosterStatistics statistics) {
        Set<DutyRosterShift> dutyRosterShifts = month.getDutyRosterDays().get(day);
        for (DutyRosterShift dutyRosterShift : dutyRosterShifts) {

            Shift shift = dutyRosterShift.getShift();

            ShiftAssignee shiftAssignee = statistics.getAssigness().get(dutyRosterShift.getName());

            if (shiftAssignee == null) {
                shiftAssignee = new ShiftAssignee();
                shiftAssignee.setName(dutyRosterShift.getName());
                statistics.getAssigness().put(shiftAssignee.getName(), shiftAssignee);
            }

            switch (shift) {
                case EARLY_SHIFT:
                    shiftAssignee.increaseNoOfEarlyShifts();
                    statistics.increaseNoOfAssignedEarlyShifts();
                    break;
                case LATE_SHIFT:
                    shiftAssignee.increaseNoOfLateShifts();
                    statistics.increaseNoOfAssignedLateShifts();
                    break;
                case NIGHT_SHIFT:
                    shiftAssignee.increaseNoOfNightShifts();
                    statistics.increaseNoOfAssignedNightShifts();
                    break;
                default:
                    throw new UnsupportedOperationException("unknown shift " + shift);
            }
        }
        return null;
    }
}
