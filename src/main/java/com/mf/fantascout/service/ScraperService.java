package com.mf.fantascout.service;

import com.mf.fantascout.controller.sse.SSEUtil;
import com.mf.fantascout.controller.sse.records.PercentageSSESignal;
import com.mf.fantascout.model.TO.PlayerTO;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.util.FileCopyUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@Service
public class ScraperService {

    @Autowired
    FileManipulatorService fileManipulatorService;

    @Autowired
    private SSEUtil<PercentageSSESignal> sseUtil;

    private static final String FILE_NAME = "favourite-players.txt";

    public List<String> getPlayersFromFile() {
        return fileManipulatorService.readFile(FILE_NAME);
    }

    public boolean addPlayerToFile(String playerName){
        return fileManipulatorService.writeFile(FILE_NAME, playerName, true);
    }

    public boolean removePlayerFromFile(String playerNameToDelete){
        List<String> playersName = fileManipulatorService.readFile(FILE_NAME);
        playersName.remove(playerNameToDelete);
        return fileManipulatorService.writeFile(FILE_NAME, playersName, false);
    }

    public List<PlayerTO> getScrapedPlayersFromFantacalcio(String squad, String season) throws IOException {
        List<String> playerHrefs = getScrapedTeamPlayers(squad);

        List<PlayerTO> playerTOs = new ArrayList<>();
        for(String playerHref : playerHrefs){
            playerTOs.add(getPlayerFromHref(playerHref, season));
        }

        PercentageSSESignal signal = new PercentageSSESignal(1);
        sseUtil.emitSignal(signal);
        return  playerTOs;
    }

    private List<String> getScrapedTeamPlayers(String squad) throws IOException {
        String squadUrl = "https://www.fantacalcio.it/serie-a/squadre/" + squad;
        Document document = Jsoup.connect(squadUrl).get();
        Elements playerLinks = document.select("td.name a.player-link");

        List<String> playerHrefs = new ArrayList<>();
        for (Element playerLink : playerLinks) {
            String href = playerLink.attr("href");
            playerHrefs.add(href);
        }

        return playerHrefs;
    }

    private PlayerTO getPlayerFromHref(String playerHref, String season) throws IOException {
        try {
            String playerUrl = playerHref.replace("2023-24", season);
            Document document = Jsoup.connect(playerUrl).get();

            // Get Properties
            List<String> infoProperties = extractPlayerInfoProperties(document);
            List<String> statsProperties = extractPlayerStatsProperties(document);
            List<String> summaryProperties = extractPlayerSummaryProperties(document);
            List<String> seasonPresencesProperties = extractSeasonPresencesProperties(document);

            // Convert Properties to PlayerTO
            return convertPropertiesToPlayerTO(
                    infoProperties,
                    statsProperties,
                    summaryProperties,
                    seasonPresencesProperties);

        } catch (NullPointerException e) {
            // Handle the exception here by returning a PlayerTO with the name "not in Serie A"
            PlayerTO notInSerieAPlayer = new PlayerTO();
            notInSerieAPlayer.setTeam("not in Serie A");
            return notInSerieAPlayer;
        }
    }

    private List<String> extractPlayerStatsProperties(Document document) {
        Element playerMainInfo = document.getElementById("player-main-info");
        Elements statsRows = playerMainInfo.select(".player-stats");

        List<String> statsProperties = new ArrayList<>();
        boolean isQuotationClassicSet = false;
        boolean isQuotationMantraSet = false;
        for (Element row : statsRows) {
            Elements liElements = row.select("li");
            for(Element li: liElements){
                String propertyName = li.select("span[class=small-label]").text();
                String propertyValue = li.select("span").text().split(" ")[0];

                // Add a prefix to propertyName based on whether it's related to Quotation or FVM
                if (propertyName.equals("Classic") && !isQuotationClassicSet) {
                    propertyName = "Quotation Classic";
                    isQuotationClassicSet = true;
                } else if (propertyName.equals("Classic") && isQuotationClassicSet) {
                    propertyName = "FVM Classic";
                } else if (propertyName.equals("Mantra") && !isQuotationMantraSet){
                    propertyName = "Quotation Mantra";
                    isQuotationMantraSet = true;
                } else if (propertyName.equals("Mantra") && isQuotationMantraSet){
                    propertyName = "FVM Mantra";
                }

                statsProperties.add(propertyName + "|" + propertyValue);
            }
        }

        return statsProperties;
    }

    private List<String> extractPlayerInfoProperties(Document document){
        Element playerMainInfo = document.getElementById("player-main-info");
        Elements playerInfoRows = playerMainInfo.select(".card-content");

        List<String> infoProperties = new ArrayList<>();
        for(Element row : playerInfoRows){
            String playerName = row.select("h1[itemprop=name]").text();
            String playerTeam = row.select("span[itemprop=memberOf]").text();
            String playerPosition = row.select("span").attr("data-value");
            String playerImagePath = row.select("img").attr("src");
            infoProperties.add(playerName + "|" + playerTeam + "|" + playerPosition + "|" + playerImagePath);
        }

        return infoProperties;
    }

    private List<String> extractPlayerSummaryProperties(Document document){
        Element playerSummaryStatsSection = document.getElementById("player-summary-stats");
        Elements summaryStatsRows= playerSummaryStatsSection.select("tbody tr");

        List<String> summaryStatsProperties =  new ArrayList<>();
        for (Element row : summaryStatsRows) {
            String propertyName = row.select("th[itemprop=name description]").text();
            String propertyValue = row.select("td[class=value]").text();
            summaryStatsProperties.add(propertyName + "|" + propertyValue);
        }
        return summaryStatsProperties;
    }

    private List<String> extractSeasonPresencesProperties(Document document) {
        Element playerSeasonPresencesSection = document.getElementById("meta-dataset-status-percent");
        Elements seasonPresencesRows = playerSeasonPresencesSection.select(".donut-summary");

        List<String> seasonPresencesProperties = new ArrayList<>();
        for (Element row : seasonPresencesRows) {
            Elements liElements = row.select("li");
            for(Element li: liElements){
                String propertyName = li.select("span[itemprop=name description]").text();
                String propertyValue = li.select("span[itemprop=value]").text();
                seasonPresencesProperties.add(propertyName + "|" + propertyValue);
            }
        }
        return seasonPresencesProperties;
    }

    private PlayerTO convertPropertiesToPlayerTO(
            List<String> infoProperties,
            List<String> statsProperties,
            List<String> summaryProperties,
            List<String> seasonPresencesProperties
    ) {
        PlayerTO player = new PlayerTO();

        // Convert and set values from the infoProperties list
        if (!infoProperties.isEmpty()) {
            String infoData = infoProperties.get(0);
            String[] infoParts = infoData.split("\\|");

            if (infoParts.length == 4) {
                player.setName(infoParts[0].trim());
                player.setTeam(infoParts[1].trim());
                player.setRole(infoParts[2].trim());
                player.setImagePath(infoParts[3].trim());
            }
        }

        // Convert and set values from the statsProperties list
        for (String entry : statsProperties) {
            String[] parts = entry.split("\\|");

            if (parts.length == 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();

                if (value.equals("-")) {
                    value = "1";
                }

                switch (key) {
                    case "MV" -> player.setStandardMean(Float.parseFloat(value.replace(',', '.')));
                    case "FM" -> player.setFantaMean(Float.parseFloat(value.replace(',', '.')));
                    case "Quotation Classic" -> player.setQuotationClassic(Integer.parseInt(value));
                    case "Quotation Mantra" -> player.setQuotationMantra(Integer.parseInt(value));
                    case "FVM Classic" -> player.setFVM1000Classic(Integer.parseInt(value));
                    case "FVM Mantra" -> player.setFVM1000Mantra(Integer.parseInt(value));
                    default -> {}
                }
            }
        }

        // Convert and set values from the summaryProperties list
        for (String entry : summaryProperties) {
            String[] parts = entry.split("\\|");

            if (parts.length == 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();

                switch (key) {
                    case "Partite a voto" -> player.setPlayedMatches(Integer.parseInt(value));
                    case "Gol" -> player.setGoals(Integer.parseInt(value));
                    case "Assist" -> player.setAssists(Integer.parseInt(value));
                    case "Gol casa/trasferta" -> {
                        String[] goalSplit = value.split("/");
                        if (goalSplit.length == 2) {
                            player.setGoalsHome(Integer.parseInt(goalSplit[0]));
                            player.setGoalsAway(Integer.parseInt(goalSplit[1]));
                        }
                    }
                    case "Ammonizioni" -> player.setYellowCards(Integer.parseInt(value));
                    case "Rigori segnati/totali" -> {
                        String[] penaltySplit = value.split("/");
                        if (penaltySplit.length == 2) {
                            player.setScoredShotsPenalty(Integer.parseInt(penaltySplit[0]));
                            player.setTotalShotsPenalty(Integer.parseInt(penaltySplit[1]));
                        }
                    }
                    case "Gol subiti" -> player.setGoalsConceded(Integer.parseInt(value));
                    case "Gol subiti casa/trasferta" -> {
                        String[] concededSplit = value.split("/");
                        if (concededSplit.length == 2) {
                            player.setGoalsConcededHome(Integer.parseInt(concededSplit[0]));
                            player.setGoalsConcededAway(Integer.parseInt(concededSplit[1]));
                        }
                    }
                    case "Espulsioni" -> player.setRedCards(Integer.parseInt(value));
                    case "Autoreti" -> player.setAutogoals(Integer.parseInt(value));
                    default -> {
                    }
                    // Handle any other keys or skip unknown keys
                }
            }
        }

        // Convert and set values from the seasonPresencesProperties list
        if (!seasonPresencesProperties.isEmpty()) {

            for (String data : seasonPresencesProperties) {
                String[] parts = data.split("\\|");

                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String[] values = parts[1].trim().split(" - ");

                    // Clean the values by removing non-numeric characters
                    values[0] = values[0].replaceAll("[^0-9]", "");
                    values[1] = values[1].replaceAll("[^0-9]", "");

                    switch (key) {
                        case "Titolare" -> {
                            player.setStarterTimes(Integer.parseInt(values[0]));
                            player.setStarterPercentage(Integer.parseInt(values[1]));
                        }
                        case "Entrato" -> {
                            player.setEnteredTimes(Integer.parseInt(values[0]));
                            player.setEnteredPercentage(Integer.parseInt(values[1]));
                        }
                        case "Squalificato" -> {
                            player.setDisqualifiedTimes(Integer.parseInt(values[0]));
                            player.setDisqualifiedPercentage(Integer.parseInt(values[1]));
                        }
                        case "Infortunato" -> {
                            player.setInjuredTimes(Integer.parseInt(values[0]));
                            player.setInjuredPercentage(Integer.parseInt(values[1]));
                        }
                        case "Inutilizzato" -> {
                            player.setUnusedTimes(Integer.parseInt(values[0]));
                            player.setUnusedPercentage(Integer.parseInt(values[1]));
                        }
                        // Handle other keys or skip unknown keys
                        default -> {}
                    }
                }
            }
        }



        return player;
    }


}
