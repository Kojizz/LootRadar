package dev.lootradar.catalog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
interface GameRepository extends JpaRepository<Game, String> {}
interface OfferRepository extends JpaRepository<Offer, String> {
    List<Offer> findByActiveTrue();
    List<Offer> findByGameIdAndActiveTrue(String gameId);
    List<Offer> findByStoreId(String storeId);
}
interface ObservationRepository extends JpaRepository<PriceObservation, Long> {
    List<PriceObservation> findByGameIdAndObservedAtAfterOrderByObservedAtAsc(String gameId, Instant since);
    void deleteByObservedAtBefore(Instant before);
}
interface SyncStateRepository extends JpaRepository<SyncState, String> {}
