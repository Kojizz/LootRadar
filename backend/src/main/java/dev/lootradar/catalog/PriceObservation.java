package dev.lootradar.catalog;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity @Table(name = "price_observations")
public class PriceObservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String gameId;
    public String storeId;
    @Column(precision = 12, scale = 2) public BigDecimal price;
    public Instant observedAt;
}
