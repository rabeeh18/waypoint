package com.Parcelo.web;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.Parcelo.common.time.OperationalClock;
import com.Parcelo.config.AppMode;
import com.Parcelo.security.UserAccountRepository;

/** Values every page header needs: who is signed in and the clock. */
@ControllerAdvice(basePackages = "com.Parcelo.web")
public class GlobalModel {

    private static final Map<String, Object> NOBODY = Map.of("name", "", "role", "");

    private final UserAccountRepository users;
    private final OperationalClock clock;
    private final AppMode mode;

    public GlobalModel(UserAccountRepository users, OperationalClock clock, AppMode mode) {
        this.users = users;
        this.clock = clock;
        this.mode = mode;
    }

    @ModelAttribute("demoMode")
    public boolean demoMode() {
        return mode.demo();
    }

    @ModelAttribute("me")
    public Map<String, Object> me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return NOBODY;
        }
        return users.findByUsername(auth.getName()).<Map<String, Object>>map(u ->
            Map.<String, Object>of("name", u.getFullName(), "role", u.primaryRole().label())).orElse(NOBODY);
    }

    @ModelAttribute("clockState")
    public Map<String, Object> clockState() {
        return Map.of("now", clock.now().toEpochMilli());
    }
}
