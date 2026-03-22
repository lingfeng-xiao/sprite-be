package com.lingfeng.sprite.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Chat Page Controller
 *
 * Redirects /chat to the static chat page
 */
@Controller
public class ChatPageController {

    @GetMapping("/chat")
    public String chat() {
        return "forward:/chat/index.html";
    }
}
