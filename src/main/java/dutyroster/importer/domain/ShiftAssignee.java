package dutyroster.importer.domain;

import lombok.Data;

/**
 * @author apo
 * @since 24.08.2016 22:10
 */
@Data
public class ShiftAssignee implements Comparable<ShiftAssignee> {

    private String name;

    private int noOfEarlyShifts;

    private int noOfLateShifts;

    private int noOfNightShifts;

    private double assignedShiftsPercentage;

    public int getTotalNoOfShifts() {
        return noOfEarlyShifts + noOfLateShifts + noOfNightShifts;
    }

    public void increaseNoOfEarlyShifts() {
        noOfEarlyShifts++;
    }

    public void increaseNoOfLateShifts() {
        noOfLateShifts++;
    }

    public void increaseNoOfNightShifts() {
        noOfNightShifts++;
    }

    @Override
    public int compareTo(ShiftAssignee o) {
        return o.getTotalNoOfShifts() - this.getTotalNoOfShifts();
    }
}
