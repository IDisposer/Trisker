const canvas = document.getElementById('graph');
const ctx = canvas.getContext("2d");

export async function init() {
    await layoutAndDraw();
}

export async function onEvent(event) {
    
}

const graph = {
    id: "root",
    layoutOptions: {
        'elk.algorithm': 'layered',
        'elk.direction': 'DOWN',
        // 'elk.direction': 'RIGHT',
        'elk.padding': '[top=25,left=25,bottom=25,right=25]'
    },
    children: [
        {
            id: "n1",
            width: 30,
            height: 30
        },
        {
            id: "n2",
            width: 30,
            height: 30
        },
        {
            id: "n3",
            width: 30,
            height: 30
        }
    ],
    edges: [
        {
            id: "e1",
            labels: [
                {
                    text: "Check",
                    layoutOptions: {
                        "elk.edgeLabels.placement": "CENTER"
                    }
                }
            ],
            sources: ["n1"],
            targets: ["n2"]
        },
        {
            id: "e2",
            sources: ["n1"],
            targets: ["n3"]
        }
    ]
}

const elk = new ELK({
    workerUrl: './elkjs/elk-worker.js'
})

async function layoutAndDraw() {
    addTextMeasurements(graph);
    const layouted = await layout(graph);
    draw(layouted);
}

function addTextMeasurements(graph) {
    graph.edges.forEach(edge => {
        edge.labels?.forEach(l => {
            const metrics = measureText(l.text);
            l.width = metrics.width;
            l.height = metrics.height;
        });
    });
}

async function draw(graph) {
    console.log(graph);

    drawRectangle(graph.x, graph.y, graph.width, graph.height);
    graph.children.forEach(element => {
        drawRectangle(element.x, element.y, element.width, element.height, 'red');
    });
    graph.edges.forEach(edge => {
        edge.sections.forEach(s => {
            drawLine(s.startPoint.x, s.startPoint.y, s.endPoint.x, s.endPoint.y);
        });
        edge.labels?.forEach(l => {
            const metrics = measureText(l.text);
            drawText(l.text, l.x - metrics.width / 2, l.y + metrics.height / 2);
        });
    });
}

async function layout(graph) {
    return await elk.layout(graph);
}

function measureText(text) {
    const metrics = ctx.measureText(text);
    metrics.height = metrics.emHeightAscent;
    return metrics;
}

function drawRectangle(x, y, h, w, color = 'orange') {
    ctx.save();
    ctx.fillStyle = color;
    ctx.beginPath();
    ctx.fillRect(x, y, h, w);
    ctx.stroke();
    ctx.restore();
}

function drawText(text, x, y) {
    ctx.save();
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