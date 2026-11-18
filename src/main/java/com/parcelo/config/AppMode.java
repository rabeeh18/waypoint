package com.parcelo.config;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Demo or production. Demo (the default profile) seeds synthetic India data and
 * starts the clock at a working hour. Production ({@code prod}) starts empty, runs on real time and has no shortcuts.
 */
@Component
public class AppMode {

    private final boolean demo;

    public AppMode(Environment env) {
        this.demo = env.acceptsProfiles(Profiles.of("demo"));
    }

    public boolean demo() {
        return demo;
    }
}
