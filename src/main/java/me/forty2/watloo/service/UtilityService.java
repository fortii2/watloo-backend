package me.forty2.watloo.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class UtilityService {

    public String startHandler() {
        return """
                Welcome!
                
                You can use /help to see all available commands.
                """;
    }

    public String helpHandler() {
        return """
                using /pick with arguments split by whitespace for random selection
                
                using /add_course to bind your courses based on different terms
                
                /help for help documents
                
                """;
    }

    public String pickHandler(String msg) {
        String args = msg.substring(5).trim();
        String[] split = args.split("\\s+");

        if (args.isBlank() || split.length == 0) {
            return "Please provide at least 1 option.";
        }

        int random = ThreadLocalRandom.current().nextInt(split.length);

        return split[random];
    }

    public String defaultHandler() {
        return """
                Please input valid commands.
                
                You can use /help to see all available commands.
                """;
    }
}
