package dev.lootradar.catalog;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.net.http.HttpClient;
import java.time.Duration;
@Component
public class CheapSharkClient {
    private final RestClient client;
    public CheapSharkClient(@Value("${lootradar.provider-url}") String baseUrl) {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build());
        factory.setReadTimeout(Duration.ofSeconds(20));
        client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory)
            .defaultHeader("User-Agent", "LootRadar/0.1 (portfolio game deal tracker)").build();
    }
    public JsonNode deals(String storeId) {
        return client.get().uri("/deals?storeID={store}&pageSize=60&sortBy=DealRating&onSale=1", storeId)
            .retrieve().body(JsonNode.class);
    }
    public JsonNode game(String gameId) {
        return client.get().uri("/games?id={id}", gameId).retrieve().body(JsonNode.class);
    }
}
