const express = require('express')
const path = require('path');
const yaml = require('js-yaml');
const fs = require('fs/promises');
const os = require('os');
const app = express()
const port = 3000

// Dont use this server in production!
// This server contains many major security risks for the system

app.use(express.static('static'));

app.get('/elkjs/:filename', (req, res) => {
    res.sendFile(path.join(__dirname, 'node_modules', 'elkjs', 'lib', req.params.filename));
});

app.get('/boards/:filename', async (req, res) => {
    const pathToFile = path.join(__dirname, '..', 'environment', 'boards', req.params.filename);
    let fileContent = await fs.readFile(pathToFile, 'utf-8');
    fileContent = fileContent.substring(fileContent.indexOf('\n') + 1);
    const json = yaml.load(fileContent);
    res.send(json);
});

app.get('/event-logs', async (req, res) => {
    const pathToFile = path.join(__dirname, '..', 'event-logs.log');
    let fileContent = await fs.readFile(pathToFile, 'utf-8');
    let lines = fileContent.split('\n');
    lines.pop();
    lines = lines.map(l => JSON.parse(l));
    res.send(lines);
});

app.listen(port, () => {
  console.log(`Example app listening on port ${port}`)
})
