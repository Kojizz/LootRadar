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
import dev.lootradar.auth.Identity;
import org.springframework.security.core.Authentication;

@RestController @RequestMapping("/api/wishlist")
public class WishlistController {
    private final WishlistRepository wishlist;
    private final CatalogService catalog;
    public WishlistController(WishlistRepository wishlist, CatalogService catalog) { this.wishlist = wishlist; this.catalog = catalog; }
    public record Wish(GameView game, BigDecimal targetPrice, Instant createdAt) {}
    public record Target(@DecimalMin("0") @DecimalMax("10000") @Digits(integer = 5, fraction = 2) BigDecimal targetPrice) {}
    @PostMapping("/import") @ResponseStatus(HttpStatus.NO_CONTENT)
    @org.springframework.transaction.annotation.Transactional
    public void importGuest(@RequestHeader("X-Profile-Id") String guest, Authentication auth) {
        if (!Identity.signedIn(auth)) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED);
        String source = Identity.profile(null, guest), destination = Identity.profile(auth, null);
        for (WishlistEntry old : wishlist.findByProfileIdOrderByCreatedAtDesc(source)) {
            String key = destination + ":" + old.gameId;
            if (wishlist.existsById(key)) continue;
            WishlistEntry entry = new WishlistEntry(); entry.id = key; entry.profileId = destination;
            entry.gameId = old.gameId; entry.targetPrice = old.targetPrice; entry.createdAt = old.createdAt;
            wishlist.save(entry);
        }
    }
    @GetMapping
    public List<Wish> list(@RequestHeader(value="X-Profile-Id", required=false) String guest, Authentication auth) {
        return wishlist.findByProfileIdOrderByCreatedAtDesc(Identity.profile(auth, guest)).stream()
            .map(w -> new Wish(catalog.gameView(w.gameId), w.targetPrice, w.createdAt)).toList();
    }
    @PutMapping("/{gameId:[a-zA-Z0-9-]+}")
    public Wish save(@RequestHeader(value="X-Profile-Id", required=false) String guest, Authentication auth, @PathVariable String gameId, @Valid @RequestBody Target target) {
        GameView game = catalog.gameView(gameId);
        String key = Identity.profile(auth, guest) + ":" + gameId;
        WishlistEntry entry = wishlist.findById(key).orElseGet(WishlistEntry::new);
        entry.id = key; entry.profileId = Identity.profile(auth, guest); entry.gameId = gameId;
        entry.targetPrice = target.targetPrice;
        if (entry.createdAt == null) entry.createdAt = Instant.now();
        wishlist.save(entry);
        return new Wish(game, entry.targetPrice, entry.createdAt);
    }
    @DeleteMapping("/{gameId:[a-zA-Z0-9-]+}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@RequestHeader(value="X-Profile-Id", required=false) String guest, Authentication auth, @PathVariable String gameId) {
        wishlist.deleteById(Identity.profile(auth, guest) + ":" + gameId);
    }
}
