package dev.lootradar.catalog;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity @Table(name = "offers")
public class Offer {
    @Id public String id;
    public String gameId;
    public String storeId;
    @Column(length = 512) public String dealId;
    @Column(precision = 12, scale = 2) public BigDecimal price;
    @Column(precision = 12, scale = 2) public BigDecimal regularPrice;
    public int discount;
    @Column(precision = 5, scale = 2) public BigDecimal dealRating;
    public boolean active;
    public Instant checkedAt;
}
