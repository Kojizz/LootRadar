package dev.lootradar.auth;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

@Component
public class SteamOpenIdClient {
    private final RestClient client;
    public SteamOpenIdClient() {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build());
        factory.setReadTimeout(Duration.ofSeconds(15)); client = RestClient.builder().requestFactory(factory).build();
    }
    public boolean verify(Map<String,String> params) {
        var form = new LinkedMultiValueMap<String,String>();
        params.forEach((key,value) -> {if (key.startsWith("openid.")) form.add(key,value);});
        form.set("openid.mode", "check_authentication");
        String result = client.post().uri("https://steamcommunity.com/openid/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(String.class);
        return result != null && result.lines().anyMatch("is_valid:true"::equals);
    }
}
