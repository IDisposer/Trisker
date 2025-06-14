import { copy } from "./utils.mjs";
const boardContainer = document.getElementById('map');
const roundIndicator = document.getElementById('round-indicator');

let boardData = null;;
let territoryCoordinates = {};
let lastPlayerData = null;
let board2dTemplate = [];

const territoryBoarders = ["/","\\","-","+","|","_","`"];

const colorMap = {
    update: "green",
    0: "darkblue",
    1: "#fb2410"
};

export async function init() {
    boardData = await (await fetch('/boards/risk_default.yaml')).json();
    prepareBoard();
}

export async function onEvent(event) {
    simulateMap(event);
    updateRoundIndicator(event.round);
}

function updateRoundIndicator(round) {
    roundIndicator.innerText = round + "";
}

function simulateMap(playerData) {
    let changes = [];
    if (lastPlayerData !== null) {
        Object.entries(playerData.territoryMap).forEach(([id, value]) => {
            if (JSON.stringify(value) !== JSON.stringify(lastPlayerData.territoryMap[id])) {
                changes.push(id);
            }
        });
    }

    let newBoard2d = copy(board2dTemplate);
    Object.entries(playerData.territoryMap).forEach(([id, value]) => {
        if (value.occupantPlayerId === -1) {
            return;
        }
        newBoard2d = insertTerritoryString(newBoard2d, id, value);
    });
    printBoard(newBoard2d, playerData, changes);
    lastPlayerData = playerData;
}

function prepareBoard() {
    let mapString = boardData.map;

    mapString = insertWhitespaces(mapString);
    board2dTemplate = extractTerritoryMap2d(mapString);
    fixMap();
    defineTerritoryArea();
}

function insertWhitespaces(text) {
    let result = "";
    let bracesOpen = false;
    for (let i = 0; i < text.length; i++) {
        const char = text[i];

        if (char == ' ') continue;

        if (char == '[') {
            bracesOpen = true;
        }

        if (char == ']') {
            bracesOpen = false;
        }

        if (!bracesOpen && /[0-9a-z]/.test(char)) {
            const spaces = ' '.repeat(charToNumberRadix(char));
            result += spaces;
        } else {
            result += char;
        }
    }
    return result;
}

function extractTerritoryMap2d(text) {
    let result = [[]];
    let currentTerritory = "";
    let bracesOpen = false;
    let shortenedChars = 0;
    let x = 0;
    let y = 0;
    let maxLineLength = 0;
    for (let i = 0; i < text.length; i++) {
        const char = text[i];

        if (char === '\n') {
            result.push([]);
            if (x > maxLineLength) {
                maxLineLength = x;
            }
            x = -1;
            shortenedChars = 0;
            y++;
            continue;
        }
        x++;

        if (char == '[') {
            bracesOpen = true;
            currentTerritory = '';
            shortenedChars++;
            continue;
        }

        if (char == ']') {
            bracesOpen = false;
            territoryCoordinates[currentTerritory] = {
                x: x - shortenedChars,
                y: y
            }
            result[y].push('X');
            continue;
        }

        if (bracesOpen) {
            shortenedChars++;
            currentTerritory += char;
        } else {
            result[y].push(char);
        }
    }

    return result;
}

function fixMap() {
    board2dTemplate[8][80] = "+";
    board2dTemplate[31][123] = "+";
    board2dTemplate[9][208] = "+";
    board2dTemplate[26][36] = "+";
    board2dTemplate[27][38] = "+";
    board2dTemplate[7][50] = "_";
}

function defineTerritoryArea() {
    Object.entries(territoryCoordinates).forEach(([id, value]) => {
        value.area = [];
        expandTerritoryAreaRecursive(value.area, value.x, value.y, new Set(), 120)
    });
}

function expandTerritoryAreaRecursive(area, x, y, seen, limit) {
    if (limit == 0) {
        return;
    }

    if (territoryBoarders.includes(board2dTemplate[y][x])) {
        return;
    }

    let c = {x,y};
    let u = JSON.stringify(c);
    if (seen.has(u)) {
        return;
    }
    seen.add(u);
    area.push(c);
    limit--;
    expandTerritoryAreaRecursive(area, x + 1, y, seen, limit);
    expandTerritoryAreaRecursive(area, x, y + 1, seen, limit);
    expandTerritoryAreaRecursive(area, x - 1, y, seen, limit);
    expandTerritoryAreaRecursive(area, x, y - 1, seen, limit);
}

function printBoard(board2d, playerData, changes) {
    board2d = copy(board2d);

    Object.entries(territoryCoordinates).forEach(([id, value]) => {
        value.area.forEach(c => {
            if (board2d[c.y][c.x] !== ' ') {
                return;
            }

            const playerId = playerData.territoryMap[id].occupantPlayerId;
            if (playerId === -1) {
                return;
            }

            if (!colorMap[playerId]) {
                colorMap[playerId] = getRandomColor(playerId);
            }

            const span = document.createElement('span');
            span.style.backgroundColor = changes.includes(id) ? colorMap.update : colorMap[playerId];
            span.innerText = " ";
            span.dataset.x = c.x;
            span.dataset.y = c.y;
            board2d[c.y][c.x] = span.outerHTML;
        });
    });

    let boardString = "";
    for (let idx of board2d.keys()) {
        boardString += board2d[idx].join("");
        boardString += "\n";
    }
    boardContainer.innerHTML = boardString;
}

function insertTerritoryString(board2d, id, data) {
    const {x, y} = territoryCoordinates[id];
    const infoString = `${id}[${data.occupantPlayerId}:${data.troops}]`;

    let c = 1;
    let firstIndex = x;
    let lastIndex = x;
    while (c < infoString.length) {
        if (board2d[y][firstIndex - 1] === ' ') {
            firstIndex--;
            c++;
        }
        if (c < infoString.length && board2d[y][lastIndex + 1] === ' ') {
            lastIndex++;
            c++;
        }
    }
    replaceAt(board2d[y], firstIndex, Array.from(infoString))
    return board2d;
}

function getRandomColor(id) {
    return `rgb(${id * 20 % 255},${id * 33 % 255}, ${id * 80 % 255})`
}

function charToNumberRadix(char) {
    const charCode = char.charCodeAt(0);
  
    if (charCode >= 48 && charCode <= 57) {
      return charCode - 48;
    }

    return charCode - 97 + 10;
}

function replaceAt(array, startIdx, replacement) {
    for(let c = 0; c < replacement.length; c++) {
        array[startIdx + c] = replacement[c];
    }
}
