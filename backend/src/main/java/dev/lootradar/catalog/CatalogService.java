package dev.lootradar.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import static dev.lootradar.catalog.ApiModels.*;

@Service
public class CatalogService {
    private static final Logger log = LoggerFactory.getLogger(CatalogService.class);
    private final GameRepository games;
    private final OfferRepository offers;
    private final ObservationRepository observations;
    private final SyncStateRepository states;
    private final CatalogWriter writer;
    private final CheapSharkClient provider;
    private final boolean syncEnabled;
    private final ExternalCatalogClient external;
    private final boolean extraSyncEnabled;
    private final AtomicBoolean syncing = new AtomicBoolean();
    private record CachedLow(Instant fetchedAt, HistoricLow low) {}
    private final Map<String, CachedLow> comparisons = new ConcurrentHashMap<>();

    public CatalogService(GameRepository games, OfferRepository offers, ObservationRepository observations,
            SyncStateRepository states, CatalogWriter writer, CheapSharkClient provider,
            @Value("${lootradar.sync-enabled:true}") boolean syncEnabled, ExternalCatalogClient external,
            @Value("${lootradar.extra-sync-enabled:${lootradar.sync-enabled:true}}") boolean extraSyncEnabled) {
        this.games = games; this.offers = offers; this.observations = observations;
        this.states = states; this.writer = writer; this.provider = provider; this.syncEnabled = syncEnabled; this.external = external; this.extraSyncEnabled = extraSyncEnabled;
    }

    @Scheduled(initialDelay = 1500, fixedDelayString = "${lootradar.sync-delay-ms:3600000}")
    public void scheduledSync() { if (syncEnabled) sync(); }

    public void sync() {
        if (!syncing.compareAndSet(false, true)) return;
        try {
            SyncState state = states.findById("catalog").orElseGet(SyncState::new);
            state.lastAttempt = Instant.now(); states.save(state);
            List<String> failed = new ArrayList<>();
            for (String store : List.of("1", "25", "7")) {
                try { writer.importStore(store, provider.deals(store), Instant.now()); }
                catch (Exception e) { failed.add(STORES.get(store)); log.warn("Could not refresh {}: {}", STORES.get(store), e.getClass().getSimpleName()); }
            }
            if (extraSyncEnabled) {
                try { writer.importStore("ms", external.microsoft(), Instant.now()); } catch (Exception e) { failed.add("Microsoft / Xbox PC"); }
                try { writer.importUpcoming(external.upcoming()); } catch (Exception e) { failed.add("Steam upcoming games"); }
            }
            if (failed.isEmpty()) { state.lastSuccess = Instant.now(); state.lastError = null; }
            else state.lastError = "Refresh unavailable for " + String.join(", ", failed) + ". Showing available saved prices.";
            states.save(state);
            comparisons.entrySet().removeIf(entry -> entry.getValue().fetchedAt.isBefore(Instant.now().minus(15, ChronoUnit.MINUTES)));
            log.info("Catalog refresh completed; {} games stored", games.count());
        } finally { syncing.set(false); }
    }

    public Catalog catalog() {
        Map<String, List<Offer>> byGame = offers.findByActiveTrue().stream().collect(Collectors.groupingBy(o -> o.gameId));
        List<GameView> list = games.findAll().stream()
            .filter(g -> g.upcoming || byGame.getOrDefault(g.id, List.of()).stream().anyMatch(o -> o.price.compareTo(o.regularPrice) < 0))
            .map(g -> view(g, byGame.getOrDefault(g.id, List.of())))
            .sorted(Comparator.comparing(GameView::title, String.CASE_INSENSITIVE_ORDER)).toList();
        SyncState state = states.findById("catalog").orElseGet(SyncState::new);
        boolean stale = state.lastSuccess == null || state.lastSuccess.isBefore(Instant.now().minus(2, ChronoUnit.HOURS)) || state.lastError != null;
        return new Catalog(list, List.of(new Store("1", "Steam"), new Store("25", "Epic Games"), new Store("7", "GOG"), new Store("ms", "Microsoft / Xbox PC")),
            new Status(state.lastSuccess, state.lastAttempt, syncing.get(), stale, state.lastError));
    }

    public GameView gameView(String id) {
        Game game = games.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
        return view(game, offers.findByGameIdAndActiveTrue(id));
    }

    public synchronized Detail detail(String id) {
        gameView(id); // Reject unknown IDs before contacting the provider.
        boolean fresh = true;
        CachedLow cached = comparisons.get(id);
        if (id.matches("[0-9]+") && (cached == null || cached.fetchedAt.isBefore(Instant.now().minus(15, ChronoUnit.MINUTES)))) {
            try {
                var response = provider.game(id);
                writer.importComparison(id, response, Instant.now());
                var low = response.path("cheapestPriceEver");
                HistoricLow historical = low.hasNonNull("price") && low.path("date").asLong() > 0
                    ? new HistoricLow(new BigDecimal(low.get("price").asText()), Instant.ofEpochSecond(low.path("date").asLong())) : null;
                cached = new CachedLow(Instant.now(), historical); comparisons.put(id, cached);
            } catch (Exception e) { fresh = false; log.warn("Comparison refresh unavailable for {}: {}", id, e.getClass().getSimpleName()); }
        }
        TreeMap<Instant, BigDecimal> hourly = new TreeMap<>();
        for (PriceObservation point : observations.findByGameIdAndObservedAtAfterOrderByObservedAtAsc(id, Instant.now().minus(90, ChronoUnit.DAYS)))
            hourly.merge(point.observedAt.truncatedTo(ChronoUnit.HOURS), point.price, BigDecimal::min);
        var history = hourly.entrySet().stream().map(p -> new PricePoint(p.getKey(), p.getValue())).toList();
        return new Detail(gameView(id), history, cached == null ? null : cached.low, fresh,
            fresh ? "Prices in USD. Availability and final price are confirmed by the store."
                  : "Live comparison is temporarily unavailable. Showing previously saved offers; check the store before buying.");
    }

    static String dealUrl(String id) {
        // CheapShark sends encoded IDs; normalize before encoding exactly once.
        String decoded = id.contains("%") ? URLDecoder.decode(id, StandardCharsets.UTF_8) : id;
        return "https://www.cheapshark.com/redirect?dealID=" + URLEncoder.encode(decoded, StandardCharsets.UTF_8);
    }

    private GameView view(Game game, List<Offer> rows) {
        var views = rows.stream().sorted(Comparator.comparing(o -> o.price)).map(o -> new OfferView(o.storeId,
            STORES.get(o.storeId), o.price, o.regularPrice, o.discount, o.dealRating, o.storeId.equals("ms") ? "https://www.microsoft.com/store/productId/" + o.dealId : dealUrl(o.dealId), o.checkedAt)).toList();
        String image = game.thumbnail;
        if (image != null && image.contains("steamstatic.com/") && image.contains("capsule_231x87"))
            image = image.replace("capsule_231x87.jpg", "header.jpg");
        return new GameView(game.id, game.title, game.steamAppId, image, game.thumbnail, game.reviewPercent,
            game.reviewCount, game.reviewText, views, game.upcoming, game.releaseDate);
    }
}
