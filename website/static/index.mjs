import * as Map from './map.mjs';
import * as Tree from './tree.mjs';
import { sleep } from './utils.mjs';

const mapContainer = document.getElementById("map-container");
const treeContainer = document.getElementById("tree-container");

let currentEventIdx = 0;
let eventLogs = [];

async function init() {
    await Map.init();
    await Tree.init();
    eventLogs = await (await fetch('/event-logs')).json();
    eventLogs = eventLogs.filter(x => x.type === 'BOARD');
}

async function start() {
    await init();

    simulateNextEvent();
}

window.addEventListener('keydown', (e) => {
    console.log(e.key);
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
            break;
        case 'TREE':
            if (eventLogs[currentEventIdx - 1].type === 'BOARD') {
                simulateNextEvent();
                return;
            }

            while(eventLogs[currentEventIdx]?.type === 'TREE') currentEventIdx++;
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
