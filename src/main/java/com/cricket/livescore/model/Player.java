package com.cricket.livescore.model;

public record Player(String name, String role, int runs, int balls, int wickets,
                     int fours, int sixes, boolean out, double strikeRate) {
}
