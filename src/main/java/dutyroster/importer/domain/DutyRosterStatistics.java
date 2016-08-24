package dutyroster.importer.domain;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author apo
 * @since 24.08.2016 22:05
 */
@Data
public class DutyRosterStatistics {

    private int noOfAssignedEarlyShifts;

    private int noOfPossibleEarlyShifts;

    private int noOfAssignedLateShifts;

    private int noOfPossibleLateShifts;

    private int noOfAssignedNightShifts;

    private int noOfPossibleNightShifts;

    private double percentageOfAssignedShifts;

    /**
     * The assignees of this month. Key: ShiftAssignee.name, value: ShiftAssignee instance.
     */
    private Map<String, ShiftAssignee> assigness = new HashMap<>();

    public int getTotalNoOfPossibleShifts() {
        return noOfPossibleEarlyShifts + noOfPossibleLateShifts + noOfPossibleNightShifts;
    }

    public int getTotalNoOfAssignedShifts() {
        return noOfAssignedEarlyShifts + noOfAssignedLateShifts + noOfAssignedNightShifts;
    }

    public void increaseNoOfPossibleEarlyShifts() {
        noOfPossibleEarlyShifts++;
    }

    public void increaseNoOfPossibleLateShifts() {
        noOfPossibleLateShifts++;
    }

    public void increaseNoOfPossibleNightShifts() {
        noOfPossibleNightShifts++;
    }

    public void increaseNoOfAssignedEarlyShifts() {
        noOfAssignedEarlyShifts++;
    }

    public void increaseNoOfAssignedLateShifts() {
        noOfAssignedLateShifts++;
    }

    public void increaseNoOfAssignedNightShifts() {
        noOfAssignedNightShifts++;
    }
}
