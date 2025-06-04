import { copy } from "./utils.mjs";
const canvas = document.getElementById('graph');

canvas.width  = window.innerWidth;
canvas.height = window.innerHeight;

const ctx = canvas.getContext("2d");

let lastGraphDrawn = null;
const canvasMeta = {
    offset: {
        x: 0,
        y: 0
    },
    scaling: 1
}

const nodePadding = {
    width: 20,
    height: 20
}

let mouseMove = null;

const graphTemplate = {
    id: "root",
    layoutOptions: {
        'elk.algorithm': 'layered',
        'elk.direction': 'DOWN',
        'elk.padding': '[top=25,left=25,bottom=25,right=25]'
    },
    children: [],
    edges: []
};

let lastTreeState = {};
const nodeChanges = new Set();

export async function init() {
    
}

export async function onEvent(event) {
    prepareTree(event);
    const graphObject = buildPrintableTree(event);
    await layoutAndDraw(graphObject);
}

canvas.onmousedown = (e) => {
    mouseMove = {x: e.screenX, y: e.screenY};
    canvas.style.cursor = "grabbing";
}

canvas.onmousemove = (e) => {
    if (!mouseMove) return;
    const newCoords = {x: e.screenX, y: e.screenY};
    const diffX = (newCoords.x - mouseMove.x) * 1.5 * calcMouseMovementMultiplier(canvasMeta.scaling);
    const diffY = (newCoords.y - mouseMove.y) * 1.5 * calcMouseMovementMultiplier(canvasMeta.scaling);
    mouseMove = newCoords;

    canvasMeta.offset.x += diffX;
    canvasMeta.offset.y += diffY;

    redraw();
}

function calcMouseMovementMultiplier(x) {
  return Math.max(0.1, 2 / (1 + Math.pow(x, 1.5)));
}

canvas.onmouseup = (e) => {
    mouseMove = null;
    canvas.style.cursor = "grab";
}

canvas.onmouseleave = (e) => {
    mouseMove = null;
    canvas.style.cursor = "grab";
}

window.addEventListener('keydown', (e) => {
    switch (e.key) {
        case '+':
            scaleUp();
            break;
        case '-':
            scaleDown();
            break;
    }
});

function scaleUp() {
    canvasMeta.scaling += 0.15;
    redraw();
}

function scaleDown() {
    canvasMeta.scaling -= 0.15;
    if (canvasMeta.scaling < 0.1) {
        canvasMeta.scaling = 0.1;
    }
    redraw();
}

function redraw() {
    if (!lastGraphDrawn) return;
    draw(lastGraphDrawn);
}

function buildPrintableTree(node) {
    const graphObject = copy(graphTemplate);
    buildPrintableTreeRecursive(graphObject, null, node);
    return graphObject;
}

function buildPrintableTreeRecursive(graph, parent, node) {
    if (!node) return;

    graph.children.push(buildGraphChildren(node));
    if (parent != null) {
        graph.edges.push(buildGraphEdge(parent, node))
    }

    node.children.forEach((c) => {
        buildPrintableTreeRecursive(graph, node, c);
    });
}

function buildGraphChildren(node) {
    const text = "v:" + node.visits + ",t:" + node.total + ",u:" + node.ucbValue;
    const {width, height} = measureText(text);
    return {
        id: node.nodeId,
        width: width + nodePadding.width,
        height: height + nodePadding.height,
        labels: [
            {
                text: text,
            }
        ],
    }
}

function buildGraphEdge(srcNode, targetNode) {
    return {
        id: srcNode.nodeId + "->" + targetNode.nodeId,
        labels: [
            {
                text: targetNode.riskAction.action,
                layoutOptions: {
                    "elk.edgeLabels.placement": "CENTER"
                }
            }
        ],
        sources: [srcNode.nodeId],
        targets: [targetNode.nodeId]
    };
}

function prepareTree(node) {
    assignNodeLabelsAndIds(node);
    const newState = extractStateMap(node);
    extractStateDifferences(newState);
}

function extractStateDifferences(newState) {
    nodeChanges.clear();

    Object.entries(newState).forEach(([nodeId, value]) => {
        if (value !== lastTreeState[nodeId]) {
            nodeChanges.add(nodeId);
        }
    });

    lastTreeState = newState;
}

function extractStateMap(node) {
    const state = {};
    extractStateMapRecursive(state, node)
    return state;
}

function extractStateMapRecursive(state, node) {
    if (!node) return;
    const text = "v:" + node.visits + ",t:" + node.total + ",u:" + node.ucbValue;
    state[node.nodeId] = text;
    
    node.children.forEach((c) => {
        extractStateMapRecursive(state, c);
    });
}

function assignNodeLabelsAndIds(node) {
    node.nodeId = "n" + 1;
    assignNodeLabelsAndIdsRecursive(node, 1);
}

function assignNodeLabelsAndIdsRecursive(node, level) {
    node.level = level;
    const nextLevel = level + 1;
    node.children.forEach((c, idx) => {
        c.nodeId = node.nodeId + "|" + "n" + nextLevel + "_" + idx;
        assignNodeLabelsAndIdsRecursive(c, level + 1);
    });
}

const elk = new ELK({
    workerUrl: './elkjs/elk-worker.js'
})

async function layoutAndDraw(graph) {
    addTextMeasurements(graph);
    const layouted = await layout(graph);
    lastGraphDrawn = layouted;
    draw(layouted);
}

function addTextMeasurements(graph) {
     graph.children.forEach(e => {
        e.labels?.forEach(l => {
            const metrics = measureText(l.text);
            l.width = metrics.width;
            l.height = metrics.height;
        });
    });

    graph.edges.forEach(edge => {
        edge.labels?.forEach(l => {
            const metrics = measureText(l.text);
            l.width = metrics.width;
            l.height = metrics.height;
        });
    });
}

async function draw(graph) {
    clearCanvas();

    ctx.save();
    ctx.scale(canvasMeta.scaling, canvasMeta.scaling);
    ctx.translate(canvasMeta.offset.x, canvasMeta.offset.y);

    // draw other coorindates
    drawRectangle(graph.x, graph.y, graph.width, graph.height, 'whitesmoke');
    graph.children.forEach(e => {
        const hasChanges = nodeChanges.has(e.id);
        drawRectangle(e.x, e.y, e.width, e.height, hasChanges ? 'lightgreen' : 'darkblue');
        if (!hasChanges) {
            drawRectangle(e.x + 1, e.y + 1, e.width - 2, e.height - 2, 'white');
        }
        e.labels?.forEach(l => {
            const metrics = measureText(e.label);
            drawText(l.text, e.x + nodePadding.width / 2, e.y + metrics.height + nodePadding.height / 2, 'black');
        });
    });
    graph.edges.forEach(edge => {
        edge.sections.forEach(s => {
            drawLine(s.startPoint.x, s.startPoint.y, s.endPoint.x, s.endPoint.y);
        });
        edge.labels?.forEach(l => {
            const metrics = measureText(l.text);
            // drawText(l.text, l.x - metrics.width / 2, l.y + metrics.height / 2);
            drawText(l.text, l.x, l.y);
        });
    });
    ctx.restore();
}

async function layout(graph) {
    return await elk.layout(graph);
}

function measureText(text) {
    const metrics = ctx.measureText(text);
    metrics.height = metrics.emHeightAscent;
    return metrics;
}

function clearCanvas() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
}

function drawRectangle(x, y, h, w, color = 'orange') {
    ctx.save();
    ctx.fillStyle = color;
    ctx.beginPath();
    ctx.fillRect(x, y, h, w);
    ctx.stroke();
    ctx.restore();
}

function drawText(text, x, y, color = 'black') {
    ctx.save();
    ctx.fillStyle = color;
    ctx.fillText(text, x, y);
    ctx.restore();
}

function drawLine(x1, y1, x2, y2, color = 'black') {
    ctx.save();
    ctx.fillStyle = color;
    ctx.beginPath();
    ctx.moveTo(x1, y1);
    ctx.lineTo(x2, y2);
    ctx.stroke();
    ctx.restore();
}