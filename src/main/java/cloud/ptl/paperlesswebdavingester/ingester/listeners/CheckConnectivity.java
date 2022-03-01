package cloud.ptl.paperlesswebdavingester.ingester.listeners;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

@Component
@Slf4j
public class CheckConnectivity implements StartupAction {
    @Value("${paperless.host}")
    private String host;

    @Override
    public void run() {
        log.info("Checking connectivity to: " + host);
        WebClient webClient = WebClient.builder().baseUrl(host)
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create().followRedirect(true))).build();
        try {
            webClient.get().retrieve().toEntity(String.class).block();
            log.info("Connection to " + host + " successful");
        } catch (WebClientRequestException ex) {
            throw new RuntimeException("Cannot make request to " + host + " because of " + ex.getMessage());
        } catch (WebClientResponseException ex) {
            throw new RuntimeException("Cannot connect to " + host + ". Response was: " + ex.getStatusCode());
        }
    }
}
