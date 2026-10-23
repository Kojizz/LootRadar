package dev.lootradar.catalog;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;

@Component
public class ExternalCatalogClient {
    private final ObjectMapper mapper;
    private final RestClient http;
    public ExternalCatalogClient(ObjectMapper mapper) {
        this.mapper = mapper;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build());
        factory.setReadTimeout(Duration.ofSeconds(30));
        http = RestClient.builder().requestFactory(factory).defaultHeader("User-Agent", "LootRadar/0.2 (PC game discovery)").build();
    }
    public ArrayNode microsoft() throws Exception {
        String html = http.get().uri("https://www.microsoft.com/en-us/store/deals/games/pc").retrieve().body(String.class);
        var cards = microsoftCards(html);
        var ids = new ArrayList<String>();
        cards.forEach(c -> {if (c.path("productId").asText().matches("[a-zA-Z0-9]{12}")) ids.add(c.path("productId").asText());});
        if (ids.isEmpty()) throw new IllegalStateException("Microsoft catalog format changed");
        JsonNode products = http.get().uri("https://displaycatalog.mp.microsoft.com/v7.0/products?bigIds={ids}&market=US&languages=en-us", String.join(",", ids)).retrieve().body(JsonNode.class);
        Set<String> pcIds = new HashSet<>();
        if (products == null || !products.path("Products").isArray()) throw new IllegalStateException("Microsoft compatibility unavailable");
        for (var product : products.path("Products"))
            for (var sku : product.path("DisplaySkuAvailabilities"))
                for (var availability : sku.path("Availabilities"))
                    for (var platform : availability.path("Conditions").path("ClientConditions").path("AllowedPlatforms"))
                        if ("Windows.Desktop".equals(platform.path("PlatformName").asText())) pcIds.add(product.path("ProductId").asText().toLowerCase(Locale.ROOT));
        return normalizeMicrosoft(cards, pcIds);
    }
    JsonNode microsoftCards(String html) throws Exception {
        for (var script : Jsoup.parse(html).select("script")) {
            String body = script.data().trim();
            if (body.startsWith("window.__List__=")) return mapper.readTree(body.substring("window.__List__=".length()).replaceFirst(";\\s*$", "")).path("cards");
        }
        throw new IllegalStateException("Microsoft catalog data missing");
    }
    ArrayNode normalizeMicrosoft(JsonNode cards, Set<String> pcIds) {
        ArrayNode rows = mapper.createArrayNode();
        for (var card : cards) {
            String id = card.path("productId").asText().toLowerCase(Locale.ROOT);
            String sale = card.path("price").path("currentPrice").asText(), regular = card.path("price").path("originalPrice").asText();
            if (!pcIds.contains(id) || !card.path("inStock").asBoolean() || card.path("preOrder").asBoolean()
                || !sale.matches("\\$[0-9,]+\\.[0-9]{2}") || !regular.matches("\\$[0-9,]+\\.[0-9]{2}")
                || card.path("price").toString().toLowerCase(Locale.ROOT).contains("game pass")) continue;
            var row = rows.addObject(); row.put("gameID", "ms-" + id); row.put("storeID", "ms");
            row.put("title", card.path("title").asText()); row.put("thumb", card.path("image").path("uri").asText());
            row.put("dealID", id); row.put("salePrice", sale.replaceAll("[$,]", "")); row.put("normalPrice", regular.replaceAll("[$,]", ""));
        }
        return rows;
    }
    public ArrayNode upcoming() {
        JsonNode response = http.get().uri("https://store.steampowered.com/search/results/?filter=comingsoon&os=win&supportedlang=english&start=0&count=30&json=1&cc=us&l=english")
            .retrieve().body(JsonNode.class);
        if (response == null || !response.has("results_html")) throw new IllegalStateException("Steam upcoming feed unavailable");
        return parseUpcoming(response.path("results_html").asText());
    }
    ArrayNode parseUpcoming(String html) {
        ArrayNode rows = mapper.createArrayNode();
        for (var card : Jsoup.parse(html).select("a.search_result_row[data-ds-appid]")) {
            String id = card.attr("data-ds-appid");
            if (!id.matches("[0-9]+")) continue;
            var row = rows.addObject(); row.put("id", "steam-" + id); row.put("steamAppId", id);
            row.put("title", card.select(".title").text()); row.put("releaseDate", card.select(".search_released").text());
            row.put("image", card.select("img").attr("src"));
        }
        if (rows.isEmpty()) throw new IllegalStateException("Steam upcoming feed is empty or changed");
        return rows;
    }
}
