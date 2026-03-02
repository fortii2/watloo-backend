package me.forty2.watloo.controller;

import me.forty2.watloo.entity.Term;
import me.forty2.watloo.repository.TermRepository;
import me.forty2.watloo.service.CommandCorrectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/demo")
@CrossOrigin(origins = "*")
public class DemoController {

    @Autowired
    private CommandCorrectionService commandCorrectionService;

    @Autowired
    private TermRepository termRepository;

    // -------------------------------------------------------
    // Feature 1: System status
    // GET /api/demo/status
    // -------------------------------------------------------
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("app", "Watloo Backend");
        result.put("status", "running");
        result.put("time", LocalDateTime.now().toString());

        long termCount = termRepository.count();
        result.put("synced_terms", termCount);

        List<Term> upcoming = termRepository
                .findTop4ByTermEndDateAfterOrderByTermBeginDateAsc(LocalDateTime.now());
        result.put("upcoming_terms", upcoming.stream().map(Term::getName).toList());

        return ResponseEntity.ok(result);
    }

    // -------------------------------------------------------
    // Feature 2: AI command correction
    // GET /api/demo/check?input=/stars
    // GET /api/demo/check?input=/halp
    // GET /api/demo/check?input=/pick+Jan18+Feb19
    // -------------------------------------------------------
    @GetMapping("/check")
    public ResponseEntity<Map<String, String>> check(@RequestParam String input) {
        String corrected = commandCorrectionService.check(input);
        Map<String, String> result = new LinkedHashMap<>();
        result.put("original", input);
        result.put("corrected", corrected);
        result.put("changed", String.valueOf(!input.equals(corrected)));
        return ResponseEntity.ok(result);
    }

    // -------------------------------------------------------
    // Feature 3: Known commands reference
    // GET /api/demo/commands
    // -------------------------------------------------------
    @GetMapping("/commands")
    public ResponseEntity<Map<String, Object>> commands() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("known_commands", Set.of(
                "/start",
                "/help",
                "/pick <options...>",
                "/add_course",
                "/restaurant_reviews"
        ));
        result.put("ai_correction", "enabled - typos and format errors are auto-corrected before routing");
        return ResponseEntity.ok(result);
    }

    // -------------------------------------------------------
    // Feature 4: Upcoming terms from Waterloo Open Data
    // GET /api/demo/terms
    // -------------------------------------------------------
    @GetMapping("/terms")
    public ResponseEntity<List<Term>> terms() {
        List<Term> terms = termRepository
                .findTop4ByTermEndDateAfterOrderByTermBeginDateAsc(LocalDateTime.now());
        return ResponseEntity.ok(terms);
    }
}
