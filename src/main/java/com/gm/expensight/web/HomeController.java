package com.gm.expensight.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("pageTitle", "Welcome");
        return "index";
    }

    @GetMapping("/home")
    public String home(@AuthenticationPrincipal OAuth2User principal, Model model) {
        model.addAttribute("pageTitle", "Dashboard");
        
        if (principal == null) {
            return "redirect:/login";
        }
        
        String name = principal.getAttribute("name");
        String email = principal.getAttribute("email");
        
        model.addAttribute("name", name);
        model.addAttribute("email", email);
        model.addAttribute("userEmail", email);
        
        return "home";
    }

    @GetMapping("/login")
    public String login(@AuthenticationPrincipal OAuth2User principal, Model model) {
        model.addAttribute("pageTitle", "Login");
        if (principal != null) {
            model.addAttribute("alreadyLoggedIn", true);
            model.addAttribute("name", principal.getAttribute("name"));
        }
        return "login";
    }

    @GetMapping("/force-login")
    public String forceLogin() {
        return "redirect:/logout?redirect=/oauth2/authorization/google";
    }
}


