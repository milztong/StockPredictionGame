package com.projects.stock_predictor.challenge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyChallengeRepository extends JpaRepository<DailyChallenge, UUID> {

    Optional<DailyChallenge> findByChallengeDate(LocalDate date);

    Optional<DailyChallenge> findFirstByResolvedTrueOrderByChallengeDateDesc();

    // Alle ungelösten Challenges deren Auflösungsdatum (challengeDate + 7 Tage) heute oder früher ist
    @Query("SELECT dc FROM DailyChallenge dc WHERE dc.resolved = false AND dc.challengeDate <= :cutoff")
    List<DailyChallenge> findUnresolvedBefore(LocalDate cutoff);

    // Letzte N verwendeten Stock-IDs — damit wir keine Aktie zu oft wiederholen
    @Query("SELECT dc.stock.id FROM DailyChallenge dc ORDER BY dc.challengeDate DESC LIMIT :limit")
    List<UUID> findRecentStockIds(int limit);
}
