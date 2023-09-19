package com.mf.fantascout.model.TO;

import lombok.Data;

@Data
public class PlayerTO {
    private String name;
    private String team;
    private String role;
    private String imagePath;
    private Float standardMean;
    private Float fantaMean;
    private Integer quotationClassic;
    private Integer quotationMantra;
    private Integer FVM1000Classic;
    private Integer FVM1000Mantra;
    private Integer playedMatches;
    private Integer goals;
    private Integer goalsHome;
    private Integer goalsAway;
    private Integer scoredShotsPenalty;
    private Integer totalShotsPenalty;
    private Integer assists;
    private Integer autogoals;
    private Integer goalsConceded;
    private Integer goalsConcededHome;
    private Integer goalsConcededAway;
    private Integer yellowCards;
    private Integer redCards;
    private Integer starterTimes;
    private Integer starterPercentage;
    private Integer enteredTimes;
    private Integer enteredPercentage;
    private Integer disqualifiedTimes;
    private Integer disqualifiedPercentage;
    private Integer injuredTimes;
    private Integer injuredPercentage;
    private Integer unusedTimes;
    private Integer unusedPercentage;
}
