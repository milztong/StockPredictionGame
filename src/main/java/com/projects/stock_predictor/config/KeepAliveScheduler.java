package com.projects.stock_predictor.config;

import com.projects.stock_predictor.user.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class KeepAliveScheduler {

    private final UserRepository userRepository;

    public KeepAliveScheduler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Ping every 14 minutes to keep Render from spinning down
    @Scheduled(cron = "0 */14 * * * *")
    public void keepRendererAlive() {
        System.out.println("Keep-alive ping");
    }

    // Query DB every 5 days to keep Supabase from pausing
    @Scheduled(cron = "0 0 10 */5 * *")
    public void keepSupabaseAlive() {
        long count = userRepository.count();
        System.out.println("Supabase keep-alive: " + count + " users in DB");
    }
}