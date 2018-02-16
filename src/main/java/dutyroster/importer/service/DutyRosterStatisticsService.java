package dutyroster.importer.service;

import dutyroster.importer.domain.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author apo
 * @since 24.08.2016 22:04
 */
@Service
@Slf4j
public class DutyRosterStatisticsService {

    private Pattern pattern = Pattern.compile("^[\\w]+[ \\w\\.]*");

    public DutyRosterStatistics createDutyRosterStatistics(DutyRosterMonth dutyRosterMonth) {
        DutyRosterStatistics statistics = new DutyRosterStatistics();

        Instant instant = Instant.ofEpochMilli(dutyRosterMonth.getDate().getTime());
        LocalDate date = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalDate();

        int lastDay = LocalDate.of(date.getYear(), date.getMonth().plus(1), 1).minusDays(1).getDayOfMonth();
        log.info("[findNoOfShifts] lastDay of month [{}]: [{}]", date.getMonth(), lastDay);

        for (int i = 1; i <= lastDay; i++) {
            LocalDate day = LocalDate.of(date.getYear(), date.getMonth(), i);

            statistics.increaseNoOfPossibleEarlyShifts();
            statistics.increaseNoOfPossibleNightShifts();

            if (!day.getDayOfWeek().equals(DayOfWeek.SATURDAY) && !day.getDayOfWeek().equals(DayOfWeek.SATURDAY)) {
                statistics.increaseNoOfPossibleLateShifts();
            }

            doStatistics(i, dutyRosterMonth, statistics);
        }

        // calculate percentages of assigned shifts per assignee
        for (ShiftAssignee shiftAssignee : statistics.getAssigness().values()) {
            double percentageOfTotalShifts = shiftAssignee.getTotalNoOfShifts() * 100 / statistics.getTotalNoOfAssignedShifts();
            shiftAssignee.setAssignedShiftsPercentage(percentageOfTotalShifts);
        }

        statistics.setPercentageOfAssignedShifts(statistics.getTotalNoOfAssignedShifts() * 100 / statistics.getTotalNoOfPossibleShifts());

        return statistics;
    }

    private void doStatistics(int day, DutyRosterMonth month, DutyRosterStatistics statistics) {
        Set<DutyRosterShift> dutyRosterShifts = month.getDutyRosterDays().get(day);
        if (dutyRosterShifts == null) {
            log.info("[doStatistics] no shifts in day [{}] in month [{}]", day, month);
            return;
        }

        for (DutyRosterShift dutyRosterShift : dutyRosterShifts) {

            Shift shift = dutyRosterShift.getShift();

            String realName = findRealName(dutyRosterShift.getName());
            ShiftAssignee shiftAssignee = statistics.getAssigness().get(realName);

            if (shiftAssignee == null) {
                shiftAssignee = new ShiftAssignee();
                shiftAssignee.setName(realName);
                statistics.getAssigness().put(realName, shiftAssignee);
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
    }

    private String findRealName(String name) {
        Matcher m = pattern.matcher(name.trim());
        if (m.find()) {
            String realName = m.group(0).trim();
            if (!name.equals(realName)) {
                log.info("[findRealName] real name of [{}] is [{}]", name, realName);
            }
            return realName;
        }
        return name;
    }
}
