package com.projects.stock_predictor.leaderboard;

import com.projects.stock_predictor.prediction.Prediction;
import com.projects.stock_predictor.result.Result;
import com.projects.stock_predictor.result.ResultRepository;
import com.projects.stock_predictor.user.User;
import com.projects.stock_predictor.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    @Mock ResultRepository resultRepository;
    @Mock UserRepository userRepository;

    @InjectMocks LeaderboardService leaderboardService;

    @Test
    void getTopLeaderboard_emptyResults_returnsEmptyList() {
        when(resultRepository.findAllWithPredictions()).thenReturn(List.of());

        List<LeaderboardService.LeaderboardEntry> result = leaderboardService.getTopLeaderboard();

        assertThat(result).isEmpty();
    }

    @Test
    void getTopLeaderboard_ranksByTotalScoreDescending() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        when(resultRepository.findAllWithPredictions()).thenReturn(List.of(
                result(userA, 80),
                result(userA, 60),  // userA total = 140
                result(userB, 150)  // userB total = 150
        ));
        when(userRepository.findById(userA)).thenReturn(Optional.of(user(userA, "Alice")));
        when(userRepository.findById(userB)).thenReturn(Optional.of(user(userB, "Bob")));

        List<LeaderboardService.LeaderboardEntry> board = leaderboardService.getTopLeaderboard();

        assertThat(board).hasSize(2);
        assertThat(board.get(0).username()).isEqualTo("Bob");
        assertThat(board.get(0).totalScore()).isEqualTo(150);
        assertThat(board.get(0).rank()).isEqualTo(1);
        assertThat(board.get(1).username()).isEqualTo("Alice");
        assertThat(board.get(1).totalScore()).isEqualTo(140);
        assertThat(board.get(1).rank()).isEqualTo(2);
    }

    @Test
    void getTopLeaderboard_calculatesAvgScoreCorrectly() {
        UUID userId = UUID.randomUUID();

        when(resultRepository.findAllWithPredictions()).thenReturn(List.of(
                result(userId, 100),
                result(userId, 50)
        ));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, "Alice")));

        List<LeaderboardService.LeaderboardEntry> board = leaderboardService.getTopLeaderboard();

        assertThat(board.get(0).avgScore()).isEqualTo(75.0);
        assertThat(board.get(0).predictionsResolved()).isEqualTo(2);
    }

    @Test
    void getTopLeaderboard_unknownUser_showsUnbekannt() {
        UUID userId = UUID.randomUUID();

        when(resultRepository.findAllWithPredictions()).thenReturn(List.of(result(userId, 100)));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        List<LeaderboardService.LeaderboardEntry> board = leaderboardService.getTopLeaderboard();

        assertThat(board.get(0).username()).isEqualTo("Unbekannt");
    }

    @Test
    void getTopLeaderboard_limitsToTop50() {
        List<Result> manyResults = new java.util.ArrayList<>();
        for (int i = 0; i < 60; i++) {
            UUID uid = UUID.randomUUID();
            manyResults.add(result(uid, i));
            when(userRepository.findById(uid)).thenReturn(Optional.of(user(uid, "user" + i)));
        }
        when(resultRepository.findAllWithPredictions()).thenReturn(manyResults);

        List<LeaderboardService.LeaderboardEntry> board = leaderboardService.getTopLeaderboard();

        assertThat(board).hasSize(50);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private Result result(UUID userId, int totalScore) {
        Prediction prediction = new Prediction();
        prediction.setUserId(userId);

        Result result = new Result();
        result.setPrediction(prediction);
        result.setTotalScore(totalScore);
        return result;
    }

    private User user(UUID id, String username) {
        User u = new User(username);
        return u;
    }
}
