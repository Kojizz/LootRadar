package dev.lootradar.wishlist;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity @Table(name = "wishlist")
public class WishlistEntry {
    @Id public String id;
    public String profileId;
    public String gameId;
    @Column(precision = 12, scale = 2) public BigDecimal targetPrice;
    public Instant createdAt;
}
