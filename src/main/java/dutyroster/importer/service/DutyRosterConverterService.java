/*
 * $Id: JavaTemplates.xml 53870 2013-02-12 10:32:44Z tlangfeld $
 */
package dutyroster.importer.service;

import dutyroster.importer.domain.DutyRosterMonth;
import dutyroster.importer.domain.DutyRosterShift;
import dutyroster.importer.domain.Shift;
import dutyroster.importer.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

/**
 * Converts docx to {@link dutyroster.importer.domain.DutyRosterMonth} and those to comma separated csv.
 * <p>
 * Shifts are identified by the appropriate label of a {@link dutyroster.importer.domain.Shift}.
 * The docx is supposed to contain in the following format:
 * <pre>
 * <TABLE FRAME=VOID CELLSPACING=0 COLS=8 RULES=NONE BORDER=1>
 * <COLGROUP><COL WIDTH=31><COL WIDTH=71><COL WIDTH=71><COL WIDTH=65><COL WIDTH=71><COL WIDTH=65><COL WIDTH=108><COL WIDTH=108></COLGROUP>
 * <TBODY>
 * <TR>
 * <TD WIDTH=31 HEIGHT=17 ALIGN=LEFT><BR></TD>
 * <TD WIDTH=71 ALIGN=LEFT>Mo</TD>
 * <TD WIDTH=71 ALIGN=LEFT>Di</TD>
 * <TD WIDTH=65 ALIGN=LEFT>Mi</TD>
 * <TD WIDTH=71 ALIGN=LEFT>Do</TD>
 * <TD WIDTH=65 ALIGN=LEFT>Fr</TD>
 * <TD WIDTH=108 ALIGN=LEFT>Sa</TD>
 * <TD WIDTH=108 ALIGN=LEFT>So</TD>
 * </TR>
 * <TR>
 * <TD HEIGHT=17 ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT>01.03.15</TD>
 * </TR>
 * <TR>
 * <TD HEIGHT=17 ALIGN=LEFT>TD</TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT>Peter</TD>
 * </TR>
 * <TR>
 * <TD HEIGHT=17 ALIGN=LEFT>SD</TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * </TR>
 * <TR>
 * <TD HEIGHT=17 ALIGN=LEFT>ND</TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT>Paul</TD>
 * </TR>
 * <TR>
 * <TD HEIGHT=17 ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT>02.03.15</TD>
 * <TD ALIGN=LEFT>03.03.15</TD>
 * <TD ALIGN=LEFT>04.03.15</TD>
 * <TD ALIGN=LEFT>05.03.15</TD>
 * <TD ALIGN=LEFT>06.03.15</TD>
 * <TD ALIGN=LEFT>07.03.15</TD>
 * <TD ALIGN=LEFT>08.03.15</TD>
 * </TR>
 * <TR>
 * <TD HEIGHT=17 ALIGN=LEFT>TD</TD>
 * <TD ALIGN=LEFT>Judy</TD>
 * <TD ALIGN=LEFT>Judy</TD>
 * <TD ALIGN=LEFT>Judy</TD>
 * <TD ALIGN=LEFT>Judy</TD>
 * <TD ALIGN=LEFT>Judy</TD>
 * <TD ALIGN=LEFT>Judy</TD>
 * <TD ALIGN=LEFT>Tom</TD>
 * </TR>
 * <TR>
 * <TD HEIGHT=17 ALIGN=LEFT>SD</TD>
 * <TD ALIGN=LEFT>Peter</TD>
 * <TD ALIGN=LEFT>Jane</TD>
 * <TD ALIGN=LEFT>Jane</TD>
 * <TD ALIGN=LEFT>Jane</TD>
 * <TD ALIGN=LEFT>Jane</TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * </TR>
 * <TR>
 * <TD HEIGHT=17 ALIGN=LEFT>ND</TD>
 * <TD ALIGN=LEFT>Paul</TD>
 * <TD ALIGN=LEFT>Peter</TD>
 * <TD ALIGN=LEFT>Peter</TD>
 * <TD ALIGN=LEFT>James</TD>
 * <TD ALIGN=LEFT>Mary</TD>
 * <TD ALIGN=LEFT>Mary</TD>
 * <TD ALIGN=LEFT>Mary</TD>
 * </TR>
 * <TR>
 * <TD HEIGHT=17 ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT>09.03.15</TD>
 * <TD ALIGN=LEFT>10.03.15</TD>
 * <TD ALIGN=LEFT>11.03.15</TD>
 * <TD ALIGN=LEFT>12.03.15</TD>
 * <TD ALIGN=LEFT>13.03.15</TD>
 * <TD ALIGN=LEFT>14.03.15</TD>
 * <TD ALIGN=LEFT>15.03.15</TD>
 * </TR>
 * <TR>
 * <TD HEIGHT=17 ALIGN=LEFT>TD</TD>
 * <TD ALIGN=LEFT>Peter</TD>
 * <TD ALIGN=LEFT>Peter</TD>
 * <TD ALIGN=LEFT>Tom</TD>
 * <TD ALIGN=LEFT>Diane</TD>
 * <TD ALIGN=LEFT>Tom</TD>
 * <TD ALIGN=LEFT>Judy</TD>
 * <TD ALIGN=LEFT>Peter</TD>
 * </TR>
 * <TR>
 * <TD HEIGHT=17 ALIGN=LEFT>SD</TD>
 * <TD ALIGN=LEFT>Judy</TD>
 * <TD ALIGN=LEFT>Diane</TD>
 * <TD ALIGN=LEFT>Peter</TD>
 * <TD ALIGN=LEFT>Peter</TD>
 * <TD ALIGN=LEFT>Peter</TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * </TR>
 * <TR>
 * <TD HEIGHT=17 ALIGN=LEFT>ND</TD>
 * <TD ALIGN=LEFT>James</TD>
 * <TD ALIGN=LEFT>James</TD>
 * <TD ALIGN=LEFT>Paul</TD>
 * <TD ALIGN=LEFT>Paul</TD>
 * <TD ALIGN=LEFT>Paul</TD>
 * <TD ALIGN=LEFT>Mary</TD>
 * <TD ALIGN=LEFT>Mary</TD>
 * </TR>
 * <TR>
 * <TD HEIGHT=17 ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT>16.03.15</TD>
 * <TD ALIGN=LEFT>17.03.15</TD>
 * <TD ALIGN=LEFT>18.03.15</TD>
 * <TD ALIGN=LEFT>19.03.15</TD>
 * <TD ALIGN=LEFT>20.03.15</TD>
 * <TD ALIGN=LEFT>21.03.15</TD>
 * <TD ALIGN=LEFT>22.03.15</TD>
 * </TR>
 * <TR>
 * <TD HEIGHT=17 ALIGN=LEFT>TD</TD>
 * <TD ALIGN=LEFT>Judy</TD>
 * <TD ALIGN=LEFT>Judy</TD>
 * <TD ALIGN=LEFT>Tom</TD>
 * <TD ALIGN=LEFT>Tom</TD>
 * <TD ALIGN=LEFT>Tom</TD>
 * <TD ALIGN=LEFT>Jane</TD>
 * <TD ALIGN=LEFT>Jane</TD>
 * </TR>
 * <TR>
 * <TD HEIGHT=17 ALIGN=LEFT>SD</TD>
 * <TD ALIGN=LEFT>Tom</TD>
 * <TD ALIGN=LEFT>Mary</TD>
 * <TD ALIGN=LEFT>Judy</TD>
 * <TD ALIGN=LEFT>Judy</TD>
 * <TD ALIGN=LEFT>Tom</TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * </TR>
 * <TR>
 * <TD HEIGHT=17 ALIGN=LEFT>ND</TD>
 * <TD ALIGN=LEFT>Frank</TD>
 * <TD ALIGN=LEFT>Frank</TD>
 * <TD ALIGN=LEFT>Frank</TD>
 * <TD ALIGN=LEFT>James</TD>
 * <TD ALIGN=LEFT>Judy</TD>
 * <TD ALIGN=LEFT>Judy</TD>
 * <TD ALIGN=LEFT>Judy</TD>
 * </TR>
 * <TR>
 * <TD HEIGHT=17 ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT>23.03.15</TD>
 * <TD ALIGN=LEFT>24.03.15</TD>
 * <TD ALIGN=LEFT>25.03.15</TD>
 * <TD ALIGN=LEFT>26.03.15</TD>
 * <TD ALIGN=LEFT>27.03.15</TD>
 * <TD ALIGN=LEFT>28.03.15</TD>
 * <TD ALIGN=LEFT>29.03.15</TD>
 * </TR>
 * <TR>
 * <TD HEIGHT=17 ALIGN=LEFT>TD</TD>
 * <TD ALIGN=LEFT>Mary</TD>
 * <TD ALIGN=LEFT>Mary</TD>
 * <TD ALIGN=LEFT>Peter</TD>
 * <TD ALIGN=LEFT>Judy</TD>
 * <TD ALIGN=LEFT>Judy</TD>
 * <TD ALIGN=LEFT>Judy/Tom</TD>
 * <TD ALIGN=LEFT>Judy/Tom</TD>
 * </TR>
 * <TR>
 * <TD HEIGHT=17 ALIGN=LEFT>SD</TD>
 * <TD ALIGN=LEFT>Jane</TD>
 * <TD ALIGN=LEFT>Jane</TD>
 * <TD ALIGN=LEFT>Mary</TD>
 * <TD ALIGN=LEFT>Mary</TD>
 * <TD ALIGN=LEFT>Diane</TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * </TR>
 * <TR>
 * <TD HEIGHT=17 ALIGN=LEFT>ND</TD>
 * <TD ALIGN=LEFT>Frank</TD>
 * <TD ALIGN=LEFT>Frank</TD>
 * <TD ALIGN=LEFT>Frank</TD>
 * <TD ALIGN=LEFT>Frank</TD>
 * <TD ALIGN=LEFT>Paul</TD>
 * <TD ALIGN=LEFT>Peter</TD>
 * <TD ALIGN=LEFT>Peter</TD>
 * </TR>
 * <TR>
 * <TD HEIGHT=17 ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT>30.03.15</TD>
 * <TD ALIGN=LEFT>31.03.15</TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * </TR>
 * <TR>
 * <TD HEIGHT=17 ALIGN=LEFT>TD</TD>
 * <TD ALIGN=LEFT>Tom</TD>
 * <TD ALIGN=LEFT>Jane</TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * </TR>
 * <TR>
 * <TD HEIGHT=17 ALIGN=LEFT>SD</TD>
 * <TD ALIGN=LEFT>Judy</TD>
 * <TD ALIGN=LEFT>Judy</TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * </TR>
 * <TR>
 * <TD HEIGHT=17 ALIGN=LEFT>ND</TD>
 * <TD ALIGN=LEFT>Peter</TD>
 * <TD ALIGN=LEFT>Peter</TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * <TD ALIGN=LEFT><BR></TD>
 * </TR>
 * </TBODY>
 * </TABLE>
 *     </pre>
 * </p>
 *
 * @author apohl
 */
@Service
@Slf4j
public class DutyRosterConverterService {

    /**
     * Simple wrapper for a specific day and month.
     */
    public class DayAndMonth {

        /**
         * The one based day of month.
         */
        private int day;

        /**
         * The zero based month (0=January).
         */
        private int month;

        public DayAndMonth(int day, int month) {
            this.day = day;
            this.month = month;
        }

        public int getDay() {
            return day;
        }

        public int getMonth() {
            return month;
        }
    }

    private static final String NL = "\n";

    private static final String COMMA = ",";

    private DutyRosterShiftService dutyRosterShiftService;

    @Autowired
    public DutyRosterConverterService(DutyRosterShiftService dutyRosterShiftService) {
        this.dutyRosterShiftService = dutyRosterShiftService;
    }

    /**
     * Parses the given docx and extracts a {@link dutyroster.importer.domain.DutyRosterMonth} from it.
     *
     * @param is   the input stream associated with the docx
     * @param year the year of the roster
     * @return the {@link dutyroster.importer.domain.DutyRosterMonth}
     * @throws IOException
     * @throws XmlException
     * @throws OpenXML4JException
     */
    public DutyRosterMonth extractDutyRoster(InputStream is, int year) throws IOException, XmlException,
            OpenXML4JException {
        XWPFDocument docIn = new XWPFDocument(is);
        XWPFTable table = docIn.getTables().get(0);
        List<XWPFTableRow> rows = table.getRows();
        int rowCount = 0;
        DutyRosterMonth dutyRosterMonth = null;

        for (XWPFTableRow row : rows) {
            List<XWPFTableCell> tableCells = row.getTableCells();
            int cellCount = 0;
            for (XWPFTableCell tableCell : tableCells) {
                String cellText = tableCell.getText();

                // have we encountered a row with dates?
                if (isDate(cellText)) {

                    // yes -> parse the month and date of the following shifts in this column
                    DayAndMonth dayAndMonth = parseDate(cellText);

                    if (dutyRosterMonth == null) {
                        dutyRosterMonth = new DutyRosterMonth(year, dayAndMonth.getMonth());
                        log.info("importing for year/month [{}/{}]", year, dayAndMonth.getMonth());
                    }

                    // up to three rows may contain the shifts for the current date
                    findShiftInColumnAndAddToDutyRoster(dutyRosterMonth, Shift.EARLY_SHIFT, table, rowCount,
                            cellCount, dayAndMonth);
                    findShiftInColumnAndAddToDutyRoster(dutyRosterMonth, Shift.LATE_SHIFT, table, rowCount,
                            cellCount, dayAndMonth);
                    findShiftInColumnAndAddToDutyRoster(dutyRosterMonth, Shift.NIGHT_SHIFT, table, rowCount,
                            cellCount, dayAndMonth);
                }
                cellCount++;
            }
            rowCount++;
        }
        return dutyRosterMonth;
    }

    /**
     * @param text a text from a cell in the docx table
     * @return true, if the text seems to be a date, e.g. "01.04."
     */
    private boolean isDate(String text) {
        return text.matches("([1-9]|0[1-9]|[12]\\d|3[01])\\.([1-9]|0[1-9]|1[0-2])\\.");
    }

    /**
     * Converts the given <code>dutyRosterMonth</code> to a CSV that may be imported manually into any Google calendar.
     *
     * @param dutyRosterMonth the {@link dutyroster.importer.domain.DutyRosterMonth}
     * @param filename        the name of the file to be created (without extension!)
     * @throws IOException
     */
    public void convert2Csv(DutyRosterMonth dutyRosterMonth, String filename) throws IOException {
        if (dutyRosterMonth == null || dutyRosterMonth.getDutyRosterDays().isEmpty()) {
            return;
        }

        File tempFile = File.createTempFile(filename + "_", ".csv");
        log.info("creating file [" + tempFile.getAbsolutePath() + "]");

        /**
         * Expected format (taken from Google docs): <br/>
         *
         * <pre>
         * Header: Subject,Start Date,Start Time,End Date,End Time,All Day Event,Description, Location, Private
         * Example values: Abschlussklausur,05/12/20,07:10:00 PM,05/12/20,10:00:00 PM,False,Zwei Aufsatzfragen zu Themen aus dem gesamten Semester,"Audimax, Gropius-Bau",True
         * </pre>
         */
        StringBuilder sb = new StringBuilder();
        sb.append(
                "Subject,Start Date,Start Time,End Date,End Time,All Day Event,Description,Location,Private")
                .append(NL);

        FastDateFormat fdf = FastDateFormat.getInstance("dd/MM/yy,KK:mm:ss a");
        Set<DutyRosterShift> allDutyRosterShifts = dutyRosterMonth.getAllDutyRosterDays();
        for (DutyRosterShift rosterDay : allDutyRosterShifts) {
            sb.append(dutyRosterShiftService.createEventSummary(rosterDay.getShift(), rosterDay.getName())) // Subject
                    .append(COMMA).append(fdf.format(rosterDay.getFrom())) // Start Date, Start Time
                    .append(COMMA).append(fdf.format(rosterDay.getTo())) // End Date, End Time
                    .append(COMMA).append("False") // All Day Event
                    .append(COMMA).append("") // Description
                    .append(COMMA).append("") // Location
                    .append(COMMA).append("True") // Private
                    .append(NL);
        }

        FileUtils.writeStringToFile(tempFile, sb.toString());
    }

    /**
     * Starting from a cell that contains a day and month specifier, e.g. "31.02.", we are looking for the given
     * shift in the same column beneath the current row. The shift determines the offset by it's ordinal, i.e., the
     * {@link dutyroster.importer.domain.Shift#NIGHT_SHIFT} is supposed to be defined in the current column / row + 3.
     *
     * @param dutyRosterMonth the current {@link dutyroster.importer.domain.DutyRosterMonth} we're working on
     * @param shift           the shift we are looking for
     * @param table           the imported table from the docx
     * @param rowIndex        the current row (that contains a date specifier, e.g. "31.02.")
     * @param columnIndex     the current column number
     * @param dayAndMonth     the day and month parsed from the current cell we're starting at, e.g. "31.02."
     */
    private void findShiftInColumnAndAddToDutyRoster(DutyRosterMonth dutyRosterMonth, Shift shift,
                                                     XWPFTable table, int rowIndex, int columnIndex,
                                                     DayAndMonth dayAndMonth) {
        if (dutyRosterMonth == null) {
            return;
        }

        int shiftRowOffset = findShiftRowOffset(table, shift, rowIndex);
        if (shiftRowOffset == -1) {
            log.warn("warning: no shift [{}] in duty roster DOCX", shift);
            return;
        }

        // skip the whole column if it does not match the duty roster's month (the docx may
        // contain the last shift of the last month or the first shift of the next month - we do not care for those
        Calendar cal = DateUtil.getCalendar(dutyRosterMonth.getDate());
        int month = cal.get(Calendar.MONTH);
        if (month != dayAndMonth.getMonth()) {
            log.warn("warning: parsed month [{}] does not equal first month value [{}] - " +
                            "skipping shift [{}]", dayAndMonth.getMonth(),
                    month, shift);
            return;
        }

        try {
            String personnel = table.getRow(rowIndex + shiftRowOffset).getCell(columnIndex)
                    .getText();
            if (StringUtils.isNotBlank(personnel) && !StringUtils.equals(personnel, "-")) {
                log.info("found shift {}.{}.: [{}]/[{}]",
                        dayAndMonth.getDay(), dayAndMonth.getMonth() + 1, shift.getLabel(), personnel);
                dutyRosterMonth.addDutyRosterShift(shift, dayAndMonth.getDay(), personnel);
            }
        } catch (Exception e) {
            log.error("error finding early shift for date [{}]: [{}]", dayAndMonth,
                    e.getMessage());
        }
    }

    /**
     * Find the position of the given {@link dutyroster.importer.domain.Shift} in the table. The shift label must
     * be in the first column of the table. The lookup starts at the next row from the given row index.
     *
     * @param table    the duty roster table from the docx
     * @param shift    the shift to look for
     * @param rowIndex the current row index, i.e. a row, that contains the date of the shift
     * @return the cell position of the shift or -1, if no appropriate shift label was found in the first column
     */
    private int findShiftRowOffset(XWPFTable table, Shift shift, int rowIndex) {

        XWPFTableRow row;
        int offset = 0;
        while ((row = table.getRow(rowIndex + ++offset)) != null) {

            String text = row.getCell(0).getText();
            if (StringUtils.isBlank(text) || isDate(text)) {
                // a blank cell indicates a date or any other row -> no shift for the current date found
                return -1;
            }

            Shift foundShift = Shift.parseLabel(text);
            if (shift == foundShift) {
                return offset;
            }
        }

        // end of table has been reached -> no such shift
        return -1;
    }

    /**
     * @param cellText a day and month text in german notation, e.g. "31.02."
     * @return the {@link dutyroster.importer.service.DutyRosterConverterService.DayAndMonth}
     */
    private DayAndMonth parseDate(String cellText) {
        String[] tokens = StringUtils.split(cellText, ".");
        if (tokens.length < 2) {
            return null;
        }

        try {
            int day = Integer.parseInt(tokens[0]);
            int month = Integer.parseInt(tokens[1]) - 1;
            return new DayAndMonth(day, month);
        } catch (NumberFormatException e) {
            log.error("error parsing date from [{}]", cellText);
            return null;
        }
    }

}
