# CricketPulse - Live Cricket Score

A small Spring Boot assignment demonstrating a live cricket scoring platform without a database or heavy framework setup.

## Features

- REST API for the current match, match summary, and manual score updates.
- Fully manual scoring: the score changes only when an operator records a delivery.
- Cricket-aware extras: wides and no-balls do not consume a legal ball; byes and leg-byes do.
- Undo the last delivery and reset the innings from the scoring console.
- Start a fresh 0/0 match with custom batting team, bowling team, venue, and target.
- Record delivery-level data including striker, bowler, bat runs, extras, wickets, commentary, and timestamps.
- Track active striker/non-striker, legal overs, player boundaries, and delivery history.
- Player batting/bowling statistics and a compact match summary.
- Responsive frontend dashboard served directly by Spring Boot.
- Lightweight polling keeps the dashboard current every three seconds.

## Run

Install Java 17+ and Maven, then run:

```bash
mvn spring-boot:run
```

Open `http://localhost:8081`. Useful endpoints:

- `GET /api/matches/live`
- `POST /api/matches/start?battingTeam=India&bowlingTeam=Australia&venue=Wankhede&maxOvers=20&target=200`
- `POST /api/matches/live/delivery?runs=4&striker=India%20player%201&bowler=Australia%20bowler%201`
- `GET /api/matches`
- `GET /api/matches/101/summary`
- `POST /api/matches/live/runs?runs=4`
- `POST /api/matches/live/wicket`
- `POST /api/matches/live/extras/{wide|no-ball|bye|leg-bye}`
- `POST /api/matches/live/undo`
- `POST /api/matches/live/reset`
