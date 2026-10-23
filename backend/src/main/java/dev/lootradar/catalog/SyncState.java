package dev.lootradar.catalog;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name = "sync_state")
public class SyncState {
    @Id public String id = "catalog";
    public Instant lastSuccess;
    public Instant lastAttempt;
    @Column(length = 512) public String lastError;
}
