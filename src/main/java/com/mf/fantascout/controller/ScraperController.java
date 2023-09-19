package com.mf.fantascout.controller;

import com.mf.fantascout.controller.sse.SSEUtil;
import com.mf.fantascout.controller.sse.records.PercentageSSESignal;
import com.mf.fantascout.model.TO.PlayerTO;
import com.mf.fantascout.service.FileManipulatorService;
import com.mf.fantascout.service.ScraperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping(path = "/fantascout/scraper")
public class ScraperController {

    @Autowired
    private ScraperService scraperService;

    @Autowired
    private SSEUtil<PercentageSSESignal> sseUtil;

    List<String> squads = Arrays.asList(
            "Atalanta",
            "Bologna",
            "Cagliari",
            "Empoli",
            "Fiorentina",
            "Frosinone",
            "Genoa",
            "Inter",
            "Juventus",
            "Lazio",
            "Lecce",
            "Milan",
            "Monza",
            "Napoli",
            "Roma",
            "Salernitana",
            "Sassuolo",
            "Torino",
            "Udinese",
            "Verona"
    );

    @GetMapping(path="/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<PercentageSSESignal>> subscribeToSSE() {
        return sseUtil.subscribe();
    }

    @GetMapping("players/favourites")
    public ResponseEntity<Object> readFavoritesPlayers() {
        List<String> favs = scraperService.getPlayersFromFile();
        return new ResponseEntity<>(favs, HttpStatus.OK);
    }

    @PostMapping("players/favourites")
    public ResponseEntity<Object> addPlayerToFavorites(@RequestBody String playerName) {
        if(!scraperService.addPlayerToFile(playerName))
            return new ResponseEntity<>("Error adding player to favorites", HttpStatus.INTERNAL_SERVER_ERROR);
        return new ResponseEntity<>("Player added to favorites.", HttpStatus.OK);
    }

    @DeleteMapping("players/favourites")
    public ResponseEntity<Object> removePlayerFromFavorites(@RequestParam String playerName) {
        if(!scraperService.removePlayerFromFile(playerName))
            return new ResponseEntity<>("Error removing player in favorites", HttpStatus.INTERNAL_SERVER_ERROR);
        return new ResponseEntity<>("Player removed from favorites.", HttpStatus.OK);
    }

    @GetMapping("squads/{squad}/{season}")
    public ResponseEntity<Object> readSquadPlayers(@PathVariable String squad, @PathVariable String season){
        try{
            List<PlayerTO> playersTOs = scraperService.getScrapedPlayersFromFantacalcio(squad,season);
            return new ResponseEntity<>(playersTOs, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error occurred while scraping the website.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("squads/{season}")
    public ResponseEntity<Object> readAllPlayers(@PathVariable String season) {

        try {
            List<PlayerTO> playerTOs = new ArrayList<>();
            List<CompletableFuture<List<PlayerTO>>> futures = new ArrayList<>();

            for (String squad : squads) {
                CompletableFuture<List<PlayerTO>> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        return scraperService.getScrapedPlayersFromFantacalcio(squad, season);
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                        return new ArrayList<PlayerTO>(); // Handle the exception as needed
                    }
                });
                futures.add(future);
            }

            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

            // Wait for all CompletableFuture instances to complete
            allOf.join();

            // Retrieve the results from completed futures
            for (CompletableFuture<List<PlayerTO>> future : futures) {
                try {
                    List<PlayerTO> squadPlayers = future.get(); // Get the result of each CompletableFuture
                    playerTOs.addAll(squadPlayers);
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    // Handle exceptions that may occur during CompletableFuture execution
                }
            }
            return new ResponseEntity<>(playerTOs, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error occurred while scraping the website.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
