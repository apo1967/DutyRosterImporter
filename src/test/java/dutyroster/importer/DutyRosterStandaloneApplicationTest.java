package dutyroster.importer;

import org.junit.Test;

import static org.junit.Assert.*;

public class DutyRosterStandaloneApplicationTest {

    @Test
    public void testParseFilenameFromArgs() throws Exception {
        assertNull(DutyRosterStandaloneApplication.parseFilenameFromArgs(null));
        assertNull(DutyRosterStandaloneApplication.parseFilenameFromArgs("-d"));
        assertNull(DutyRosterStandaloneApplication.parseFilenameFromArgs("-f"));

        assertEquals("2014_12.docx", DutyRosterStandaloneApplication.parseFilenameFromArgs("-d", "-f2014_12.docx"));
    }

    @Test
    public void testIsDryRun() throws Exception {
        assertTrue(DutyRosterStandaloneApplication.isDryRun("-f2014_12.docx", "-d"));
        assertTrue(DutyRosterStandaloneApplication.isDryRun("-d", "-f2014_12.docx"));

        assertFalse(DutyRosterStandaloneApplication.isDryRun(null));
        assertFalse(DutyRosterStandaloneApplication.isDryRun(""));

        assertFalse(DutyRosterStandaloneApplication.isDryRun("-f2014_12.docx"));
        assertFalse(DutyRosterStandaloneApplication.isDryRun("-f2014_12.docx", "-D"));
    }
}