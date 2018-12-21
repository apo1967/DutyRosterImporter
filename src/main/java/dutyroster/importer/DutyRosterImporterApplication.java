package dutyroster.importer;

import dutyroster.importer.service.EmailProperties;
import dutyroster.importer.storage.StorageProperties;
import dutyroster.importer.storage.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

/**
 * @author apohl
 */
@SpringBootApplication
@PropertySources({
        @PropertySource(value = "classpath:/calendar.properties", encoding = "UTF-8"),
        @PropertySource(value = "classpath:/email.properties", encoding = "UTF-8")})
@Slf4j
@EnableConfigurationProperties({StorageProperties.class, EmailProperties.class})
@RestController
@CrossOrigin
public class DutyRosterImporterApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(DutyRosterImporterApplication.class, args);

        log.info("Let's inspect the beans provided by Spring Boot:");

        String[] beanNames = ctx.getBeanDefinitionNames();
        Arrays.sort(beanNames);
        for (String beanName : beanNames) {
            log.info(beanName);
        }
    }

    @Bean
    CommandLineRunner init(StorageService storageService) {
        return (args) -> {
            storageService.deleteAll();
            storageService.init();
        };
    }

    @GetMapping({"/ping", "/", "/api/ping"})
    public String ping() {
        log.info("pong");
        return "pong";
    }
}
