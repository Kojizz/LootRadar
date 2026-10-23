package dev.lootradar.catalog;
import jakarta.persistence.*;
@Entity @Table(name = "games")
public class Game {
    @Id public String id;
    public boolean upcoming;
    public String releaseDate;
    @Column(nullable = false, length = 512) public String title;
    public String steamAppId;
    @Column(length = 2048) public String thumbnail;
    public int reviewPercent;
    public int reviewCount;
    public String reviewText;
}
