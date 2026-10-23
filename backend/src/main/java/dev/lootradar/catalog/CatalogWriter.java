package dev.lootradar.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class CatalogWriter {
    private final GameRepository games;
    private final OfferRepository offers;
    private final ObservationRepository observations;
    public CatalogWriter(GameRepository games, OfferRepository offers, ObservationRepository observations) {
        this.games = games; this.offers = offers; this.observations = observations;
    }

    @Transactional
    public synchronized void importStore(String storeId, JsonNode rows, Instant now) {
        if (rows == null || !rows.isArray()) throw new IllegalArgumentException("Invalid provider response");
        // A failed import rolls back retirement of the previous snapshot as well.
        offers.findByStoreId(storeId).forEach(offer -> { offer.active = false; offers.save(offer); });
        for (JsonNode row : rows) {
            String id = row.path("gameID").asText();
            if (!id.matches("[0-9]{1,32}") || !storeId.equals(row.path("storeID").asText()))
                throw new IllegalArgumentException("Invalid game or store ID");
            Game game = games.findById(id).orElseGet(Game::new);
            game.id = id;
            game.title = row.path("title").asText();
            if (game.title.isBlank()) throw new IllegalArgumentException("Missing game title");
            game.steamAppId = row.path("steamAppID").asText("");
            game.thumbnail = row.path("thumb").asText("");
            game.reviewPercent = row.path("steamRatingPercent").asInt();
            game.reviewCount = row.path("steamRatingCount").asInt();
            game.reviewText = row.path("steamRatingText").asText("");
            games.save(game);
            saveOffer(id, storeId, row, "salePrice", "normalPrice", now);
        }
        observations.deleteByObservedAtBefore(now.minus(180, ChronoUnit.DAYS));
    }

    @Transactional
    public synchronized void importComparison(String gameId, JsonNode root, Instant now) {
        JsonNode rows = root == null ? null : root.get("deals");
        if (rows == null || !rows.isArray()) throw new IllegalArgumentException("Invalid comparison response");
        offers.findByGameIdAndActiveTrue(gameId).forEach(offer -> { offer.active = false; offers.save(offer); });
        for (JsonNode row : rows) {
            String storeId = row.path("storeID").asText();
            if (ApiModels.STORES.containsKey(storeId)) saveOffer(gameId, storeId, row, "price", "retailPrice", now);
        }
    }

    private void saveOffer(String gameId, String storeId, JsonNode row, String priceField, String regularField, Instant now) {
        BigDecimal price = new BigDecimal(row.path(priceField).asText()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal regular = new BigDecimal(row.path(regularField).asText()).setScale(2, RoundingMode.HALF_UP);
        if (price.signum() < 0 || regular.signum() < 0 || row.path("dealID").asText().isBlank())
            throw new IllegalArgumentException("Invalid offer price or link");
        String key = gameId + ":" + storeId;
        Offer offer = offers.findById(key).orElseGet(Offer::new);
        offer.id = key; offer.gameId = gameId; offer.storeId = storeId;
        offer.dealId = row.path("dealID").asText();
        offer.price = price; offer.regularPrice = regular;
        offer.discount = regular.signum() == 0 ? 0 : Math.max(0, regular.subtract(price)
            .multiply(BigDecimal.valueOf(100)).divide(regular, 0, RoundingMode.HALF_UP).intValue());
        if (row.hasNonNull("dealRating")) offer.dealRating = new BigDecimal(row.get("dealRating").asText());
        else if (offer.dealRating == null) offer.dealRating = BigDecimal.ZERO;
        offer.active = true; offer.checkedAt = now;
        offers.save(offer);
        PriceObservation observation = new PriceObservation();
        observation.gameId = gameId; observation.storeId = storeId;
        observation.price = price; observation.observedAt = now;
        observations.save(observation);
    }
}
