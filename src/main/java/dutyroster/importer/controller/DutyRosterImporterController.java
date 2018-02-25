package dutyroster.importer.controller;

import dutyroster.importer.domain.DutyRosterDiff;
import dutyroster.importer.domain.DutyRosterStatistics;
import dutyroster.importer.service.DutyRosterImporterService;
import dutyroster.importer.service.DutyRosterStatisticsService;
import dutyroster.importer.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by apo on 05.03.2015.
 */
@RestController
@Slf4j
@CrossOrigin
public class DutyRosterImporterController {

    private DutyRosterImporterService dutyRosterImporterService;

    private EmailService emailService;

    private DutyRosterStatisticsService dutyRosterStatisticsService;

    @Autowired
    public DutyRosterImporterController(DutyRosterImporterService dutyRosterImporterService, EmailService emailService, DutyRosterStatisticsService dutyRosterStatisticsService) {
        this.dutyRosterImporterService = dutyRosterImporterService;
        this.emailService = emailService;
        this.dutyRosterStatisticsService = dutyRosterStatisticsService;
    }

    @PostMapping("/api/convertAndImport/{originalFilename}/{year}/{month}/{dryRun}/{createCsv}")
    public String handleConvertAndImport(@RequestBody String filename,
                                  @PathVariable("originalFilename") String originalFilename,
                                  @PathVariable("dryRun") boolean dryRun,
                                  @PathVariable("createCsv") boolean createCsv,
                                  @PathVariable("year") int year,
                                  @PathVariable("month") int month) throws EmailException, IOException, MessagingException {

        DutyRosterDiff dutyRosterDiff = dutyRosterImporterService.convertAndImportDutyRoster(new FileInputStream(filename),
                originalFilename, dryRun, createCsv, year, month);
        if (!dutyRosterDiff.hasDifferences()) {
            return "no changes in duty roster";
        }

        DutyRosterStatistics dutyRosterStatistics = dutyRosterStatisticsService.createDutyRosterStatistics(dutyRosterDiff.getOnlyAfter());
        log.info("[handleConvertAndImport] statistics: [{}]", dutyRosterStatistics);

        Email email = emailService.createUpdateEmail(dutyRosterDiff, dutyRosterStatistics);
        if (!dryRun) {
            email.send();
        } else {
            email.buildMimeMessage();
        }

        MimeMessage mimeMessage = email.getMimeMessage();
        String emailContent = (String) mimeMessage.getContent();
        log.info("email {}: [{}]", (dryRun ? "(not sent)" : "sent"), emailContent);
        return emailContent;
    }

}