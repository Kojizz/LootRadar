package dev.lootradar.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.lootradar.wishlist.WishlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "lootradar.sync-enabled=false", "spring.datasource.url=jdbc:h2:mem:integration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"})
class CatalogIntegrationTests {
    @Autowired TestRestTemplate http;
    @Autowired CatalogService catalog;
    @Autowired CatalogWriter writer;
    @Autowired GameRepository games;
    @Autowired OfferRepository offers;
    @Autowired ObservationRepository observations;
    @Autowired SyncStateRepository states;
    @Autowired WishlistRepository wishlist;
    @Autowired ObjectMapper mapper;
    @MockitoBean CheapSharkClient provider;

    JsonNode deal(String store, String game, String price) throws Exception {
        return mapper.readTree("""
          [{"gameID":"%s","storeID":"%s","title":"Test Adventure %s","steamAppID":"123",
          "salePrice":"%s","normalPrice":"59.99","dealID":"a%%2Bb%%2Fc%%3D",
          "steamRatingPercent":"92","steamRatingCount":"2500","dealRating":"9.5","thumb":"https://example.com/game.jpg"}]
          """.formatted(game, store, game, price));
    }

    @BeforeEach void setup() throws Exception {
        wishlist.deleteAll(); observations.deleteAll(); offers.deleteAll(); games.deleteAll(); states.deleteAll();
        reset(provider);
        when(provider.deals("1")).thenReturn(deal("1", "100", "19.99"));
        when(provider.deals("25")).thenReturn(deal("25", "100", "9.99"));
        when(provider.deals("7")).thenReturn(deal("7", "200", "0.00"));
        catalog.sync();
    }

    @Test void importsRealOffersGroupsGamesAndNormalizesDealLinks() {
        var body = http.getForEntity("/api/catalog", JsonNode.class);
        assertThat(body.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body.getBody().path("games").size()).isEqualTo(2);
        var game = catalog.gameView("100");
        assertThat(game.offers()).hasSize(2);
        assertThat(game.offers().getFirst().price()).isEqualByComparingTo("9.99");
        assertThat(game.offers().getFirst().discount()).isEqualTo(83);
        assertThat(game.offers().getFirst().url()).endsWith("dealID=a%2Bb%2Fc%3D");
        assertThat(catalog.gameView("200").offers().getFirst().price()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(catalog.catalog().status().stale()).isFalse();
    }

    @Test void providerFailurePreservesCachedPricesAndMarksThemStale() {
        when(provider.deals("1")).thenThrow(new IllegalStateException("upstream unavailable"));
        catalog.sync();
        assertThat(offers.findById("100:1")).isPresent();
        assertThat(offers.findById("100:1").orElseThrow().price).isEqualByComparingTo("19.99");
        assertThat(catalog.catalog().status().stale()).isTrue();
        assertThat(catalog.catalog().status().message()).contains("Steam");
    }

    @Test void invalidBatchRollsBackRetirementAndPartialWrites() throws Exception {
        var malformed = mapper.readTree("""
          [{"gameID":"100","storeID":"1","title":"Changed","salePrice":"-1","normalPrice":"59.99","dealID":"x"}]
          """);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> writer.importStore("1", malformed, java.time.Instant.now())).isInstanceOf(IllegalArgumentException.class);
        assertThat(offers.findById("100:1").orElseThrow().active).isTrue();
        assertThat(games.findById("100").orElseThrow().title).isEqualTo("Test Adventure 100");
    }

    @Test void detailShowsOnlyObservedHistoryAndFiltersUnsupportedStores() throws Exception {
        when(provider.game("100")).thenReturn(mapper.readTree("""
          {"deals":[{"storeID":"1","price":"19.99","retailPrice":"59.99","dealID":"steam"},
          {"storeID":"25","price":"9.99","retailPrice":"59.99","dealID":"epic"},
          {"storeID":"99","price":"1.99","retailPrice":"59.99","dealID":"unsupported"}],
          "cheapestPriceEver":{"price":"4.99","date":1700000000}}
          """));
        var detail = catalog.detail("100");
        assertThat(detail.game().offers()).hasSize(2);
        assertThat(detail.history()).hasSize(1);
        assertThat(detail.history().getFirst().price()).isEqualByComparingTo("9.99");
        assertThat(detail.providerHistoricalLow().price()).isEqualByComparingTo("4.99");
        assertThat(detail.comparisonFresh()).isTrue();
        catalog.detail("100");
        verify(provider, times(1)).game("100");
    }

    @Test void wishlistIsPersistentIsolatedValidatedAndRemovable() {
        String alice = UUID.randomUUID().toString(), bob = UUID.randomUUID().toString();
        var headers = new HttpHeaders(); headers.set("X-Profile-Id", alice); headers.setContentType(MediaType.APPLICATION_JSON);
        var saved = http.exchange("/api/wishlist/100", HttpMethod.PUT, new HttpEntity<>("{\"targetPrice\":5.50}", headers), JsonNode.class);
        assertThat(saved.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(wishlist.findByProfileIdOrderByCreatedAtDesc(alice)).hasSize(1);
        assertThat(http.exchange("/api/wishlist", HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class).getBody().size()).isEqualTo(1);
        headers.set("X-Profile-Id", bob);
        assertThat(http.exchange("/api/wishlist", HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class).getBody().size()).isZero();
        assertThat(http.exchange("/api/wishlist/100", HttpMethod.PUT, new HttpEntity<>("{\"targetPrice\":-1}", headers), String.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(http.getForEntity("/api/wishlist", String.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        headers.set("X-Profile-Id", alice);
        assertThat(http.exchange("/api/wishlist/100", HttpMethod.DELETE, new HttpEntity<>(headers), String.class).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(wishlist.findByProfileIdOrderByCreatedAtDesc(alice)).isEmpty();
    }

    @Test void unknownGameReturns404WithoutUpstreamRequest() {
        assertThat(http.getForEntity("/api/games/999999", String.class).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(provider, never()).game("999999");
    }
}
