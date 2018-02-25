package dutyroster.importer.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dutyroster.importer.domain.DutyRosterDiff;
import dutyroster.importer.domain.DutyRosterMonth;
import dutyroster.importer.domain.DutyRosterShift;
import dutyroster.importer.domain.Shift;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest
public class DutyRosterDiffServiceTest {

    private final Logger log = LoggerFactory.getLogger(DutyRosterDiffServiceTest.class);

    @Autowired
    private DutyRosterDiffService dutyRosterDiffService;

    @Autowired
    private DutyRosterShiftService dutyRosterShiftService;

    @Test
    public void testDiff() {

        // given
        DutyRosterMonth month1 = new DutyRosterMonth(2014, 8);
        month1.addDutyRosterShift(Shift.EARLY_SHIFT, 1, "alfred");
        month1.addDutyRosterShift(Shift.LATE_SHIFT, 1, "anne");
        month1.addDutyRosterShift(Shift.NIGHT_SHIFT, 1, "anabel");

        month1.addDutyRosterShift(Shift.EARLY_SHIFT, 2, "benny");
        month1.addDutyRosterShift(Shift.LATE_SHIFT, 2, "bertha");
        month1.addDutyRosterShift(Shift.NIGHT_SHIFT, 2, "barbara");

        DutyRosterMonth month2 = new DutyRosterMonth(2014, 8);

        DutyRosterShift shift = dutyRosterShiftService.findShift(month1, Shift.EARLY_SHIFT, 2);
        DutyRosterShift changedShift = new DutyRosterShift(shift.getId(), DateUtils.addHours(
                shift.getFrom(), 1), shift.getTo(), shift.getName(), shift.getShift());
        month2.addDutyRosterShift(changedShift);
        month2.addDutyRosterShift(Shift.LATE_SHIFT, 2, "barbara");
        month2.addDutyRosterShift(Shift.NIGHT_SHIFT, 2, "bertha");

        month2.addDutyRosterShift(Shift.EARLY_SHIFT, 3, "christian");
        month2.addDutyRosterShift(Shift.LATE_SHIFT, 3, "charlotte");
        month2.addDutyRosterShift(Shift.NIGHT_SHIFT, 3, "carl");

        // when
        DutyRosterDiff diff = dutyRosterDiffService.diff(month1, month2);

        // then
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        log.debug(gson.toJson(diff));

        assertTrue(diff.hasChanges());
        assertEquals(3, diff.getNumberOfAddititions());
        assertEquals(3, diff.getNumberOfChanges());
        assertEquals(3, diff.getNumberOfDeletions());
    }
}