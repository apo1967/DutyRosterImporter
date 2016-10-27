package dutyroster.importer.service;

import dutyroster.importer.domain.DutyRosterDiff;
import dutyroster.importer.domain.DutyRosterShift;
import dutyroster.importer.domain.DutyRosterStatistics;
import dutyroster.importer.domain.ShiftAssignee;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author apohl
 */
@Service
public class EmailService {

    @Value(value = "${dutyroster.importer.service.email.username}")
    private String username;

    @Value(value = "${dutyroster.importer.service.email.password}")
    private String password;

    @Value(value = "${dutyroster.importer.service.email.host}")
    private String host;

    @Value(value = "${dutyroster.importer.service.email.sslSmtpPort}")
    private String sslSmtpPort;

    @Value(value = "${dutyroster.importer.service.email.sslOnConnect}")
    private boolean sslOnConnect;

    @Value(value = "${dutyroster.importer.service.email.startTlsEnabled}")
    private boolean startTlsEnabled;

    @Value(value = "${dutyroster.importer.service.email.from}")
    private String from;

    @Value(value = "${dutyroster.importer.service.email.subject}")
    private String subject;

    @Value(value = "${dutyroster.importer.service.email.to}")
    private String to;

    private DutyRosterShiftService dutyRosterShiftService;

    @Autowired
    public EmailService(DutyRosterShiftService dutyRosterShiftService) {
        this.dutyRosterShiftService = dutyRosterShiftService;
    }

    /**
     * Creates an update email informing about the import result of the duty roster. Does not send the email!
     *
     * @param dutyRosterDiff the {@link DutyRosterDiff}
     * @return the email to send
     * @throws EmailException
     */
    public Email createUpdateEmail(DutyRosterDiff dutyRosterDiff, DutyRosterStatistics dutyRosterStatistics) throws EmailException {
        Email email = new SimpleEmail();
        email.setDebug(false);
        email.setAuthentication(username, password);
        email.setHostName(host);
        email.setSSLOnConnect(sslOnConnect);
        email.setSslSmtpPort(sslSmtpPort);
        email.setStartTLSEnabled(startTlsEnabled);
        email.setFrom(from);
        email.setSubject(subject);
        email.addTo(StringUtils.split(to, ","));

        StringBuilder sb = new StringBuilder();
        sb.append("Soeben wurde der Dienstplan aktualisiert.\n\n") //
                .append("Es wurden ") //
                .append(dutyRosterDiff.getNumberOfDeletions()) //
                .append(" Schichte(n) gelöscht, ") //
                .append(dutyRosterDiff.getNumberOfAddititions()) //
                .append(" Schichte(n) hinzugefügt und ") //
                .append(dutyRosterDiff.getNumberOfChanges()) //
                .append(" Schichte(n) geändert. \n") //
                .append(createStatistics(dutyRosterStatistics)) //
                .append(createReportForEmail(dutyRosterDiff));
        email.setMsg(sb.toString());

        return email;
    }

    private String createStatistics(DutyRosterStatistics statistics) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n") //
                .append("\n-------------------------------------------") //
                .append("\nStatistik:") //
                .append("\nSchichten insgesamt : ").append(statistics.getTotalNoOfPossibleShifts()) //
                .append("\nDavon besetzt:        ").append(statistics.getTotalNoOfAssignedShifts()) //
                .append("\nIn %:                 ").append(statistics.getPercentageOfAssignedShifts()) //
                .append("\nFrüh:                 ").append(statistics.getNoOfPossibleEarlyShifts()).append(" / ").append(statistics.getNoOfAssignedEarlyShifts())
                .append("\nSpät:                 ").append(statistics.getNoOfPossibleLateShifts()).append(" / ").append(statistics.getNoOfAssignedLateShifts())
                .append("\nNacht:                ").append(statistics.getNoOfPossibleNightShifts()).append(" / ").append(statistics.getNoOfAssignedNightShifts())
                .append("\n\nPro Mitarbeiter gesamt / Prozent (Früh/Spät/Nacht):");

        List<ShiftAssignee> values = new ArrayList<>();
        values.addAll(statistics.getAssigness().values());
        Collections.sort(values);

        for (ShiftAssignee shiftAssignee : values) {
            sb.append("\n").append(shiftAssignee.getName()).append(": \t");
            if (shiftAssignee.getName().length() < 7) {
                sb.append("\t");
            }
            sb
                    .append(shiftAssignee.getTotalNoOfShifts()).append(" / ")
                    .append(shiftAssignee.getAssignedShiftsPercentage())
                    .append(" (")
                    .append(shiftAssignee.getNoOfEarlyShifts()).append("/")
                    .append(shiftAssignee.getNoOfLateShifts()).append("/")
                    .append(shiftAssignee.getNoOfNightShifts())
                    .append(")");
        }
        return sb.toString();
    }

    /**
     * Creates a Strong from the given diff of the duty roster for the email body.
     *
     * @param diff the [@link DutyRosterDiff}
     * @return the string representation of the diff
     */
    private String createReportForEmail(DutyRosterDiff diff) {

        // TODO: use i18nized texts from bundle

        SimpleDateFormat sdfDate = new SimpleDateFormat("dd.MM.");
        SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm");
        StringBuilder sb = new StringBuilder();

        if (diff.hasDeletions()) {
            sb.append("\n-------------------------------------------\n");
            sb.append("Folgende Schichten wurden gelöscht:\n\n");
            for (DutyRosterShift dutyRosterShift : diff.getDeletions()) {
                sb.append("Datum/Schicht: ") //
                        .append(sdfDate.format(dutyRosterShift.getFrom())) //
                        .append(" ")
                        .append(dutyRosterShiftService.createEventSummary(dutyRosterShift.getShift(), dutyRosterShift.getName())) //
                        .append(" von ") //
                        .append(sdfTime.format(dutyRosterShift.getFrom())) //
                        .append(" bis ") //
                        .append(sdfTime.format(dutyRosterShift.getTo())) //
                        .append("\n");
            }
        }

        if (diff.hasAdditions()) {
            sb.append("\n-------------------------------------------\n");
            sb.append("Folgende Schichten wurden hinzugefügt:\n\n");
            for (DutyRosterShift dutyRosterShift : diff.getAdditions()) {
                sb.append("Datum/Schicht: ") //
                        .append(sdfDate.format(dutyRosterShift.getFrom())) //
                        .append(" ")
                        .append(dutyRosterShiftService.createEventSummary(dutyRosterShift.getShift(), dutyRosterShift.getName())) //
                        .append(" von ") //
                        .append(sdfTime.format(dutyRosterShift.getFrom())) //
                        .append(" bis ") //
                        .append(sdfTime.format(dutyRosterShift.getTo())) //
                        .append("\n");
            }
        }

        if (diff.hasChanges()) {
            sb.append("\n-------------------------------------------\n");
            sb.append("Folgende Schichten wurden geändert:\n\n");
            for (DutyRosterDiff.Change change : diff.getChanges()) {
                sb.append("Datum/Schicht vorher:  ") //
                        .append(sdfDate.format(change.getBefore().getFrom())) //
                        .append(" ") //
                        .append(dutyRosterShiftService.createEventSummary(change.getBefore().getShift(), change.getBefore().getName())) //
                        .append(" von ") //
                        .append(sdfTime.format(change.getBefore().getFrom())) //
                        .append(" bis ") //
                        .append(sdfTime.format(change.getBefore().getTo())) //
                        .append("\n");

                sb.append("Datum/Schicht nachher: ") //
                        .append(sdfDate.format(change.getAfter().getFrom())) //
                        .append(" ") //
                        .append(dutyRosterShiftService.createEventSummary(change.getAfter().getShift(), change.getAfter().getName())) //
                        .append(" von ") //
                        .append(sdfTime.format(change.getAfter().getFrom())) //
                        .append(" bis ") //
                        .append(sdfTime.format(change.getAfter().getTo())) //
                        .append("\n\n");

            }
        }

        return sb.toString();
    }

}
