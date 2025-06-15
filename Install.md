# Installation Instructions

## Agent
The project can be build using the default gradle flow.
We provide a gradle wrapper to prevent version problems.

Windows:
```bash
./gradlew.bat build
```

Linux:
```bash
./gradlew build
```

## Node.js Viewer
The Node.js game state viewer can be found in the website package.
To Install and start it, execute the following commands:

1. Change Directory
    ```bash
    cd website
    ```

2. Edit Configuration <br>
Configure the path to the directory with the board configuration files inside **server.js**.

3. Install Dependencies
    ```bash
    npm install
    ```
4. Start Server
    ```bash
    npm start
    ```

The server should now be available at http://loacalhost:3000

