const $ = id => document.getElementById(id);
const REFRESH_MS = 3000;
let latestMatch;

function renderCommentary(match) {
    const labels = {0: 'No run. Good length delivery defended back to the bowler.', 1: 'Worked into the leg side. The batters complete a single.', 2: 'Driven into the gap and they come back for two.', 4: 'FOUR! Crunched through the covers, excellent timing.', 6: 'SIX! Clean strike over the long-on boundary.', W: 'WICKET! The fielding side finally gets the breakthrough.'};
    $('commentary-list').innerHTML = match.recentBalls.slice().reverse().map((ball, index) => `<div class="commentary-item ${ball === '4' || ball === '6' ? 'boundary' : ''}"><time>${index + 1}m</time><b>${ball}</b><strong>${labels[ball] || labels[0]}</strong></div>`).join('');
}

async function loadMatch() {
    const response = await fetch('/api/matches/live');
    if (!response.ok) throw new Error('Unable to load match');
    const match = await response.json();
    latestMatch = match;
    const balls = match.overs * 6 + match.balls;
    const required = Math.max(0, match.target - match.runs);
    $('score').textContent = `${match.runs}/${match.wickets}`;
    $('status').textContent = match.status;
    $('match-title').textContent = match.title.toUpperCase();
    $('batting-heading').textContent = `${match.battingTeam} batting`;
    $('batting-name').textContent = match.battingTeam;
    $('bowling-name').textContent = match.bowlingTeam;
    $('batting-code').textContent = code(match.battingTeam);
    $('bowling-code').textContent = code(match.bowlingTeam);
    $('scorecard-subtitle').textContent = `${match.battingTeam} · Innings ${match.inningsNumber}`;
    $('batting-heading').textContent = `${match.battingTeam} batting · Innings ${match.inningsNumber}`;
    $('innings-label').firstChild.textContent = `INNINGS ${match.inningsNumber} `;
    $('striker').textContent = match.striker;
    $('non-striker').textContent = match.nonStriker;
    $('bowler').textContent = match.currentBowler;
    const batters = match.players.filter(player => player.role === 'Batter');
    const bowlers = match.players.filter(player => player.role === 'Bowler');
    $('striker-select').innerHTML = batters.map(player => `<option>${player.name}</option>`).join('');
    $('bowler-select').innerHTML = bowlers.map(player => `<option>${player.name}</option>`).join('');
    $('striker-select').value = match.striker;
    $('bowler-select').value = match.currentBowler;
    $('overs').textContent = `${match.overs}.${match.balls} overs`;
    $('venue').textContent = match.venue;
    $('required').textContent = required;
    $('target-number').textContent = match.target;
    $('rrr').textContent = balls ? (required / Math.max(1, (120 - balls) / 6)).toFixed(2) : '0.00';
    $('runRate').textContent = (match.runs / Math.max(1, balls / 6)).toFixed(2);
    $('extras').textContent = match.extras;
    $('fours').textContent = match.fours;
    $('sixes').textContent = match.sixes;
    $('progress').style.width = `${Math.min(100, match.runs / match.target * 100)}%`;
    $('updated').textContent = new Date(match.updatedAt).toLocaleTimeString([], {hour: '2-digit', minute: '2-digit'});
    $('over-label').textContent = `OVER ${match.overs}`;
    $('balls').innerHTML = match.recentBalls.map(ball => `<span class="${ball === 'W' ? 'wicket-ball' : ball === '4' || ball === '6' ? 'boundary' : ''}">${ball}</span>`).join('');
    document.querySelectorAll('.control-grid button, .extras button').forEach(button => {
        button.disabled = match.status !== 'LIVE';
    });
    document.querySelector('.live-pill').textContent = match.status;
    $('players-list').innerHTML = match.players.map(player => `<div class="player"><div><strong>${player.name}</strong><small>${player.role}${player.wickets ? ` · ${player.wickets} wickets` : ''}</small></div><b>${player.runs}</b><b>${player.balls}</b><b>${player.strikeRate}</b></div>`).join('');
    renderCommentary(match);
}

function code(team) {
    return team.split(/\s+/).map(word => word[0]).join('').slice(0, 3).toUpperCase();
}

async function record(url, action) {
    const response = await fetch(url, {method: 'POST'});
    if (!response.ok) throw new Error('Could not record delivery');
    $('last-action').textContent = `${action} recorded`;
    await loadMatch();
    $('toast').textContent = `${action} recorded successfully`;
    $('toast').classList.add('show');
    setTimeout(() => $('toast').classList.remove('show'), 1800);
}

function deliveryQuery(url) {
    return `${url}&striker=${encodeURIComponent($('striker-select').value)}&bowler=${encodeURIComponent($('bowler-select').value)}`;
}
document.querySelectorAll('[data-runs]').forEach(button => button.addEventListener('click', () => record(deliveryQuery(`/api/matches/live/runs?runs=${button.dataset.runs}`), button.firstChild.textContent.trim())));
$('wicket').addEventListener('click', () => record(deliveryQuery('/api/matches/live/wicket?'), 'Wicket'));
document.querySelectorAll('[data-extra]').forEach(button => button.addEventListener('click', () => record(deliveryQuery(`/api/matches/live/extras/${button.dataset.extra}?`), `${button.textContent} recorded`)));
$('undo').addEventListener('click', () => record('/api/matches/live/undo', 'Last delivery undone'));
$('reset').addEventListener('click', () => {
    if (confirm('Reset the innings to its starting score?')) record('/api/matches/live/reset', 'Innings reset');
});

$('match-form').addEventListener('submit', async event => {
    event.preventDefault();
    const params = new URLSearchParams({
        battingTeam: $('batting-team').value,
        bowlingTeam: $('bowling-team').value,
        venue: $('match-venue').value,
        maxOvers: $('match-overs').value,
        target: $('match-target').value
    });
    try {
        await record(`/api/matches/start?${params}`, 'New match started');
        await loadMatches();
    } catch (error) {
        $('last-action').textContent = error.message;
    }
});

async function loadMatches() {
    const response = await fetch('/api/matches');
    const matches = await response.json();
    $('match-list').innerHTML = matches.map(match => `<div class="match-item"><div><b>${match.title}</b><small>Match ${match.id}</small></div><strong class="${match.status.toLowerCase()}">${match.status}</strong><span>${match.score}</span></div>`).join('');
}

loadMatch().then(loadMatches).catch(() => $('last-action').textContent = 'Service unavailable');
setInterval(() => loadMatch().catch(() => {}), REFRESH_MS);
