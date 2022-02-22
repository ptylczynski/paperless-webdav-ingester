package cloud.ptl.paperlesswebdavingester;

import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PaperlessWebdavIngesterApplication {
    @Value("${webdav.username}")
    private String username;
    @Value("${webdav.password}")
    private String password;

    public static void main(String[] args) {
        SpringApplication.run(PaperlessWebdavIngesterApplication.class, args);
    }

    @Bean
    public Sardine createClient() {
        return SardineFactory.begin(username, password);
    }
}
