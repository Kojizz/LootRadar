package dev.lootradar.catalog;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
public final class ApiModels {
    private ApiModels() {}
    public static final Map<String, String> STORES = Map.of("1", "Steam", "25", "Epic Games", "7", "GOG", "ms", "Microsoft / Xbox PC");
    public record Store(String id, String name) {}
    public record OfferView(String storeId, String storeName, BigDecimal price, BigDecimal regularPrice,
                            int discount, BigDecimal dealRating, String url, Instant checkedAt) {}
    public record GameView(String id, String title, String steamAppId, String image, String thumbnail, int reviewPercent,
                           int reviewCount, String reviewText, List<OfferView> offers, boolean upcoming, String releaseDate) {}
    public record Status(Instant lastSuccess, Instant lastAttempt, boolean syncing, boolean stale, String message) {}
    public record Catalog(List<GameView> games, List<Store> stores, Status status) {}
    public record PricePoint(Instant observedAt, BigDecimal price) {}
    public record HistoricLow(BigDecimal price, Instant date) {}
    public record Detail(GameView game, List<PricePoint> history, HistoricLow providerHistoricalLow,
                         boolean comparisonFresh, String note) {}
}
