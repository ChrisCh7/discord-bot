package com.chrisch.discordbot

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class BotController {
    @GetMapping("/")
    fun root(): ResponseEntity<String> = ResponseEntity.ok("OK")
}
