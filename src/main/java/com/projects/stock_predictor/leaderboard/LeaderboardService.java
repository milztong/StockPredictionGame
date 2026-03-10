package com.projects.stock_predictor.leaderboard;

import com.projects.stock_predictor.result.Result;
import com.projects.stock_predictor.result.ResultRepository;
import com.projects.stock_predictor.user.User;
import com.projects.stock_predictor.user.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class LeaderboardService {

    private final ResultRepository resultRepository;
    private final UserRepository userRepository;

    public LeaderboardService(ResultRepository resultRepository,
                              UserRepository userRepository) {
        this.resultRepository = resultRepository;
        this.userRepository = userRepository;
    }

    public List<LeaderboardEntry> getTopLeaderboard() {
        List<Result> allResults = resultRepository.findAllWithPredictions();

        Map<UUID, List<Result>> resultsByUser = allResults.stream()
                .collect(Collectors.groupingBy(r -> r.getPrediction().getUserId()));

        List<LeaderboardEntry> unsorted = resultsByUser.entrySet().stream()
                .map(entry -> {
                    UUID userId = entry.getKey();
                    List<Result> results = entry.getValue();

                    int totalScore = results.stream().mapToInt(Result::getTotalScore).sum();
                    int resolved = results.size();
                    double avgScore = resolved > 0 ? (double) totalScore / resolved : 0;

                    String username = userRepository.findById(userId)
                            .map(User::getUsername)
                            .orElse("Unbekannt");

                    return new LeaderboardEntry(0, username, totalScore, resolved, avgScore);
                })
                .sorted((a, b) -> Integer.compare(b.totalScore(), a.totalScore()))
                .collect(Collectors.toList());

        AtomicInteger rank = new AtomicInteger(1);
        return unsorted.stream()
                .limit(50)
                .map(e -> new LeaderboardEntry(
                        rank.getAndIncrement(),
                        e.username(),
                        e.totalScore(),
                        e.predictionsResolved(),
                        e.avgScore()
                ))
                .toList();
    }

    public record LeaderboardEntry(
            int rank,
            String username,
            int totalScore,
            int predictionsResolved,
            double avgScore
    ) {}
}