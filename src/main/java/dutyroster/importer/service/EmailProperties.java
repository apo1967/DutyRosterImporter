package dutyroster.importer.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;

/**
 * @author apo
 * @since 26.04.2018 14:24
 */
@PropertySource("classpath:email.properties")
@ConfigurationProperties(prefix = "mail")
@Getter
@Setter
public class EmailProperties {

    private String username;
    private String password;
    private String host;
    private String sslSmtpPort = "587";
    private boolean sslOnConnect = false;
    private boolean startTlsEnabled = true;
    private String from;
    private String subject = "Update des Dienstplans";
    private String to;
}
