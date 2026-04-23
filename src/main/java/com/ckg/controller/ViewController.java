package com.ckg.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("title", "CKG | In-Memory Intelligence");
        model.addAttribute("status", "Running (In-Memory DSA)");
        return "index";
    }
}