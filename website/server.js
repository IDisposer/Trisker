const express = require('express')
const path = require('path');
const yaml = require('js-yaml');
const fs = require('fs/promises');
const app = express()

// Dont use this server in production!
// This server contains many major security risks for the system

const PORT = 3000;
const PATH_TO_BOARDS_DIRECTORY = '../environment/boards';

// DO NOT CHANGE THE LINES AFTERWARDS
const eventLogsCache = {
    lastModified: 0,
    data: {}
}

app.use(express.static('static'));

app.get('/elkjs/:filename', (req, res) => {
    res.sendFile(path.join(__dirname, 'node_modules', 'elkjs', 'lib', req.params.filename));
});

app.get('/boards/:filename', async (req, res) => {
    const pathToFile = path.join(__dirname, PATH_TO_BOARDS_DIRECTORY, req.params.filename);
    let fileContent = await fs.readFile(pathToFile, 'utf-8');
    fileContent = fileContent.substring(fileContent.indexOf('\n') + 1);
    const json = yaml.load(fileContent);
    res.send(json);
});

app.get('/event-logs', async (req, res) => {
    const pathToFile = path.join(__dirname, '..', 'event-logs.log');

    const stats = await fs.stat(pathToFile);
    if (stats.mtimeMs === eventLogsCache.lastModified) {
        res.send(eventLogsCache.data);
        return;
    }
    eventLogsCache.lastModified = stats.mtimeMs;

    let fileContent = await fs.readFile(pathToFile, 'utf-8');
    let lines = fileContent.split('\n');
    lines.pop();
    lines = lines.map(l => JSON.parse(l));
    eventLogsCache.data = lines;
    res.send(lines);
});

app.listen(PORT, () => {
  console.log(`Debugging app is listening on port ${PORT}`);
  console.log(`http://localhost:${PORT}`);
})
