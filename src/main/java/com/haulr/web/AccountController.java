package com.haulr.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.haulr.common.web.BusinessRuleException;
import com.haulr.security.AccountService;
import com.haulr.security.UserAccount;

/** Public account page: choosing a password from an invite or reset link. */
@Controller
public class AccountController {

    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/account/set-password")
    public String setPasswordForm(@RequestParam(required = false) String token, Model model) {
        accounts.usableToken(token).ifPresentOrElse(t -> {
            model.addAttribute("token", token);
            model.addAttribute("person", t.getUser().getFullName());
            model.addAttribute("email", t.getUser().getEmail());
            model.addAttribute("invite", t.getUser().isInvitePending());
        }, () -> model.addAttribute("expired", true));
        model.addAttribute("minLength", AccountService.MIN_PASSWORD);
        return "account/set-password";
    }

    @PostMapping("/account/set-password")
    public String setPassword(@RequestParam String token, @RequestParam String password, @RequestParam String confirm,
                              Model model, RedirectAttributes ra) {
        try {
            UserAccount u = accounts.choosePassword(token, password, confirm);
            ra.addFlashAttribute("prefillEmail", u.getEmail());
            return "redirect:/login?passwordSet";
        } catch (BusinessRuleException e) {
            model.addAttribute("problem", e.getMessage());
            return setPasswordForm(token, model);
        }
    }
}
