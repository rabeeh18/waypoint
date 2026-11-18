package com.parcelo.web;

import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.parcelo.common.web.NotFoundException;
import com.parcelo.config.AppMode;
import com.parcelo.security.Role;
import com.parcelo.security.SecurityConfig;
import com.parcelo.security.UserAccount;
import com.parcelo.security.UserAccountRepository;

@Controller
public class HomeController {

    private final AppMode mode;
    private final UserAccountRepository users;
    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    public HomeController(AppMode mode, UserAccountRepository users) {
        this.mode = mode;
        this.users = users;
    }

    /** Everyone lands on the screen for the role an admin gave them. */
    @GetMapping("/")
    public String home(Authentication auth) {
        boolean driver = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(Role.DRIVER.authority()));
        boolean dispatch = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(Role.DISPATCHER.authority()));
        return dispatch ? "redirect:/dashboard" : driver ? "redirect:/driver" : "redirect:/track";
    }

    /**
     * Opens on "Track a parcel" unless the URL or the outcome of a sign-in says staff. When the
     * server decides, the tab is fixed; otherwise login.js may restore the visitor's last choice.
     */
    @GetMapping("/login")
    public String login(Model model) {
        if (mode.demo()) {
            model.addAttribute("demoDrivers", users.findAll().stream().filter(u -> u.hasRole(Role.DRIVER))
                .sorted(java.util.Comparator.comparing(UserAccount::getFullName)).toList());
        }
        model.addAttribute("tab", mode.demo() ? "demo" : "track");
        model.addAttribute("tabFixed", false);
        return "login";
    }

    /** Demo mode only: one-click sign-in as a seeded user, for showing the project. */
    @PostMapping("/demo-login")
    public String demoLogin(@RequestParam("as") String username, HttpServletRequest request, HttpServletResponse response) {
        UserAccount account = users.findByUsername(username).orElse(null);
        if (!mode.demo() || account == null || !(username.equals("dispatcher") || account.hasRole(Role.DRIVER))) {
            throw new NotFoundException("Page", "/demo-login");
        }
        UserDetails user = SecurityConfig.toUserDetails(account);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
        SecurityContextHolder.setContext(context);
        request.getSession(true);
        request.changeSessionId();
        contextRepository.saveContext(context, request, response);
        return "redirect:/";
    }
}
