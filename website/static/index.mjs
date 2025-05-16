import * as Map from './map.mjs';
import * as Tree from './tree.mjs';

let currentEventIdx = 0;
let eventLogs = [];

async function init() {
    await Map.init();
    await Tree.init();
    eventLogs = await (await fetch('/event-logs')).json();
}

async function start() {
    await init();

    simulateEvents();
}

async function simulateEvents() {
    while(currentEventIdx < eventLogs.length) {
        simulateNextEvent();
        await sleep(1000);
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
            Map.onEvent(event.data);
            break;
        case 'TREE':
            Tree.onEvent(event.data);
    }
}

function sleep(millis) {
    return new Promise((resolve) => {
        setTimeout(() => resolve(), millis);
    });
}

start();
