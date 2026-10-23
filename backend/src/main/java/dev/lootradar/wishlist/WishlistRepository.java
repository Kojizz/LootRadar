package dev.lootradar.wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface WishlistRepository extends JpaRepository<WishlistEntry, String> {
    List<WishlistEntry> findByProfileIdOrderByCreatedAtDesc(String profileId);
}
