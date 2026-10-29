package project2.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Fails startup with a clear message when the MySQL password has not been
 * supplied. This runs before beans are created, so Hibernate never attempts an
 * unauthenticated connection.
 */
@Configuration(proxyBeanMethods = false)
public class DatabaseCredentialValidator {

    private static final String URL_PROPERTY = "spring.datasource.url";
    private static final String PASSWORD_PROPERTY = "spring.datasource.password";

    @Bean
    static BeanFactoryPostProcessor databasePasswordValidator(Environment environment) {
        return beanFactory -> {
            String url = environment.getProperty(URL_PROPERTY, "");
            if (!url.startsWith("jdbc:mysql:")) {
                return;
            }

            String password;
            try {
                password = environment.getProperty(PASSWORD_PROPERTY);
            } catch (IllegalArgumentException ex) {
                throw new IllegalStateException(missingPasswordMessage(), ex);
            }
            if (password == null || password.isBlank() || password.startsWith("${")) {
                throw new IllegalStateException(missingPasswordMessage());
            }
        };
    }

    private static String missingPasswordMessage() {
        return "Database password is not configured. Set the persistent DB_PASSWORD environment variable "
                + "(Windows: [Environment]::SetEnvironmentVariable('DB_PASSWORD', '<password>', 'User')) "
                + "and open a new terminal.";
    }
}
