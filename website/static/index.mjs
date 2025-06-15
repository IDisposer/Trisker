import * as Map from './map.mjs';
import * as Tree from './tree.mjs';

const mapContainer = document.getElementById("map-container");
const treeContainer = document.getElementById("tree-container");

let currentEventIdx = 0;
const eventStepSize = extractEventStepSizeFromUrl();
const treeEventsEnabled = enableTreeEventFromUrl();
let eventLogs = [];

async function init() {
    await Map.init();
    await Tree.init();
    eventLogs = await (await fetch('/event-logs')).json();
    if (!treeEventsEnabled) {
        eventLogs = eventLogs.filter(x => x.type !== 'TREE');
    }
    currentEventIdx = extractEventIdxFromUrl();
}

function extractEventIdxFromUrl() {
    let params = new URLSearchParams(document.location.search);
    let idx = params.get("idx");
    if (idx === null || idx === "") {
        return 0;
    }
    idx = parseInt(idx, 10) - 1; // -1 to align it with the round indicator starting from 1
    return isNaN(idx) || idx < 0 ? 0 : idx >= eventLogs.length ? eventLogs.length - 1 : idx;
}

function enableTreeEventFromUrl() {
    let params = new URLSearchParams(document.location.search);
    return params.get("enableTree") === "false";
}

function extractEventStepSizeFromUrl() {
    let params = new URLSearchParams(document.location.search);
    let stepSize = parseInt(params.get("stepSize"), 10);
    return isNaN(stepSize) || stepSize < 1 ? 20 : stepSize;
}

async function start() {
    await init();

    simulateNextEvent();
}

window.addEventListener('keydown', (e) => {
    switch (e.key) {
        case 'F10':
            e.preventDefault();
            finishEventTypeOrJumpToNext();
            break;
        case 'F11':
            e.preventDefault();
            simulateNextEvent();
            break;
    }
})

function finishEventTypeOrJumpToNext() {
    switch (eventLogs[currentEventIdx].type) {
        case 'BOARD':
            simulateNextEvent();
            if (!treeEventsEnabled) {
                currentEventIdx += eventStepSize - 1;
                if (currentEventIdx >= eventLogs.length) {
                    currentEventIdx = eventLogs.length - 1;
                }
            }
            break;
        case 'TREE':
            if (eventLogs[currentEventIdx - 1].type === 'BOARD') {
                simulateNextEvent();
                return;
            }

            while(eventLogs[currentEventIdx]?.type === 'TREE' && currentEventIdx < eventLogs.length) {
                currentEventIdx++;
            }
            currentEventIdx--;
            if (currentEventIdx >= 1) {
                currentEventIdx--;
                simulateNextEvent();
            }
            
            simulateNextEvent();
            break;
    }
}

function simulateNextEvent() {
    if (currentEventIdx >= eventLogs.length) {
        return;
    }

    const event = eventLogs[currentEventIdx++];
    console.log(event.type, event.data);
    switch (event.type) {
        case 'BOARD':
            showMapContainer();
            Map.onEvent(event.data);
            break;
        case 'TREE':
            showTreeContainer();
            Tree.onEvent(event.data);
    }
}

function showMapContainer() {
    mapContainer.style.display = 'block';
    treeContainer.style.display = 'none';
}

function showTreeContainer() {
    mapContainer.style.display = 'none';
    treeContainer.style.display = 'block';
}

start();
