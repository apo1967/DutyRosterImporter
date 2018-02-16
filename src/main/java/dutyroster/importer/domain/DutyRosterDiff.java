/*
 * $Id: JavaTemplates.xml 53870 2013-02-12 10:32:44Z tlangfeld $
 */
package dutyroster.importer.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author apohl
 */
@Getter
@Setter
public class DutyRosterDiff {

    /**
     * Simple wrapper for a change of a shift.
     */
    @Getter
    @AllArgsConstructor
    public static class Change {
        private DutyRosterShift before;
        private DutyRosterShift after;
    }

    /**
     * The roster containing the shifts that are present in the current roster only.
     */
    DutyRosterMonth onlyBefore;

    /**
     * The roster containing the shifts that are present in the new roster only.
     */
    DutyRosterMonth onlyAfter;

    /**
     * The list of changes of existing shifts.
     */
    List<Change> changes = new ArrayList<>();

    public DutyRosterDiff(int year, int month) {
        this.onlyBefore = new DutyRosterMonth(year, month);
        this.onlyAfter = new DutyRosterMonth(year, month);
    }

    public boolean hasDeletions() {
        return !onlyBefore.getAllDutyRosterDays().isEmpty();
    }

    public int getNumberOfDeletions() {
        return onlyBefore.getAllDutyRosterDays().size();
    }

    public boolean hasAdditions() {
        return !onlyAfter.getAllDutyRosterDays().isEmpty();
    }

    public int getNumberOfAddititions() {
        return onlyAfter.getAllDutyRosterDays().size();
    }

    public boolean hasChanges() {
        return !changes.isEmpty();
    }

    public int getNumberOfChanges() {
        return changes.size();
    }

    public Set<DutyRosterShift> getDeletions() {
        return onlyBefore.getAllDutyRosterDays();
    }

    public Set<DutyRosterShift> getAdditions() {
        return onlyAfter.getAllDutyRosterDays();
    }

    public boolean hasDifferences() {
        return !(onlyBefore.getAllDutyRosterDays().isEmpty()
                && onlyAfter.getAllDutyRosterDays().isEmpty()
                && changes.isEmpty());
    }

    @Override
    public String toString() {
        return "DutyRosterDiff{" + "onlyBefore=" + onlyBefore + ", onlyAfter=" + onlyAfter + ", changes="
                + changes + '}';
    }
}
