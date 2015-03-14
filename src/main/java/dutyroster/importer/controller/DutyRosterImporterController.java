package dutyroster.importer.controller;

import dutyroster.importer.domain.DutyRosterDiff;
import dutyroster.importer.service.DutyRosterImporterService;
import dutyroster.importer.service.EmailService;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by apo on 05.03.2015.
 */
@RestController
@RequestMapping("/api")
public class DutyRosterImporterController {

    private final Logger log = org.slf4j.LoggerFactory.getLogger(DutyRosterImporterController.class);

    @Autowired
    private DutyRosterImporterService dutyRosterImporterService;

    @Autowired
    private EmailService emailService;

    @RequestMapping("/ping")
    public
    @ResponseBody
    String ping() {
        log.info("pong");
        return "pong";
    }

    @RequestMapping(value = "/convertAndImport/{originalFilename}/{year}/{month}/{dryRun}/{createCsv}", method = RequestMethod.POST)
    public
    @ResponseBody
    String handleConvertAndImport(@RequestBody String filename,
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

        Email email = emailService.createUpdateEmail(dutyRosterDiff);
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