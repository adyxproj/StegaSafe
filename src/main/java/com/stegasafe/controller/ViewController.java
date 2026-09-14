package com.stegasafe.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("activeTab", "dashboard");
        return "index";
    }

    @GetMapping("/encode")
    public String encode(Model model) {
        model.addAttribute("activeTab", "encode");
        return "index";
    }

    @GetMapping("/decode")
    public String decode(Model model) {
        model.addAttribute("activeTab", "decode");
        return "index";
    }

    @GetMapping("/capacity")
    public String capacity(Model model) {
        model.addAttribute("activeTab", "capacity");
        return "index";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("activeTab", "about");
        return "index";
    }
}
