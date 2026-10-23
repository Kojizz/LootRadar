package dev.lootradar.wishlist;

import dev.lootradar.catalog.ApiModels.GameView;
import dev.lootradar.catalog.CatalogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/api/wishlist")
public class WishlistController {
    private final WishlistRepository wishlist;
    private final CatalogService catalog;
    public WishlistController(WishlistRepository wishlist, CatalogService catalog) { this.wishlist = wishlist; this.catalog = catalog; }
    public record Wish(GameView game, BigDecimal targetPrice, Instant createdAt) {}
    public record Target(@DecimalMin("0") @DecimalMax("10000") @Digits(integer = 5, fraction = 2) BigDecimal targetPrice) {}
    @GetMapping
    public List<Wish> list(@RequestHeader("X-Profile-Id") UUID profile) {
        return wishlist.findByProfileIdOrderByCreatedAtDesc(profile.toString()).stream()
            .map(w -> new Wish(catalog.gameView(w.gameId), w.targetPrice, w.createdAt)).toList();
    }
    @PutMapping("/{gameId:[0-9]+}")
    public Wish save(@RequestHeader("X-Profile-Id") UUID profile, @PathVariable String gameId, @Valid @RequestBody Target target) {
        GameView game = catalog.gameView(gameId);
        String key = profile + ":" + gameId;
        WishlistEntry entry = wishlist.findById(key).orElseGet(WishlistEntry::new);
        entry.id = key; entry.profileId = profile.toString(); entry.gameId = gameId;
        entry.targetPrice = target.targetPrice;
        if (entry.createdAt == null) entry.createdAt = Instant.now();
        wishlist.save(entry);
        return new Wish(game, entry.targetPrice, entry.createdAt);
    }
    @DeleteMapping("/{gameId:[0-9]+}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@RequestHeader("X-Profile-Id") UUID profile, @PathVariable String gameId) {
        wishlist.deleteById(profile + ":" + gameId);
    }
}
