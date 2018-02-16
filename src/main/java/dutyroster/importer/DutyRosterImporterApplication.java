package dutyroster.importer;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

import java.util.Arrays;

/**
 * @author apohl
 */
@SpringBootApplication
@PropertySources({
        @PropertySource("classpath:calendar.properties"),
        @PropertySource("classpath:email.properties")})
@Slf4j
public class DutyRosterImporterApplication extends SpringBootServletInitializer {

   @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(DutyRosterImporterApplication.class);
    }

    public static void main(String[] args) throws Exception {
        ApplicationContext ctx = SpringApplication.run(DutyRosterImporterApplication.class, args);

        log.info("Let's inspect the beans provided by Spring Boot:");

        String[] beanNames = ctx.getBeanDefinitionNames();
        Arrays.sort(beanNames);
        for (String beanName : beanNames) {
            log.info(beanName);
        }
    }
}
