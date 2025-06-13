package org.example.log;

import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskTerritory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.example.mcts.TreeNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

public class EventLogService {

  private static final Path EVENT_LOG_FILE = Path.of("./event-logs.log");
  private static final boolean ENABLED = false;
  private static final int TREE_EVENT_LIMIT_PER_BOARD = 100;
  private static final int TREE_START = 40;
  private static final int TREE_END = 42;
  private static int boardCounter = 0;
  private static int treeEventCounter = 0;

  private static final ObjectMapper mapper;

  static {
    mapper = new ObjectMapper();

    SimpleModule module = new SimpleModule();
    module.addSerializer(RiskAction.class, new RiskActionSerializer());
    mapper.registerModule(module);
  }

  public enum Type {
    BOARD,
    TREE // {Node: Id, N: 1, Children: [{Node: Id, N: 1 Children: []}]}
  }

  private static class LogEntry {
    private Type type;
    private Object data;

    public Type getType() {
      return type;
    }

    public LogEntry setType(Type type) {
      this.type = type;
      return this;
    }

    public Object getData() {
      return data;
    }

    public LogEntry setData(Object data) {
      this.data = data;
      return this;
    }
  }

  private static class BoardLog {
    private Map<Integer, RiskTerritory> territoryMap = new HashMap<>();
    private String player;

    public Map<Integer, RiskTerritory> getTerritoryMap() {
      return territoryMap;
    }

    public BoardLog setTerritoryMap(
        Map<Integer, RiskTerritory> territoryMap) {
      this.territoryMap = territoryMap;
      return this;
    }

    public String getPlayer() {
      return player;
    }

    public BoardLog setPlayer(String type) {
      this.player = type;
      return this;
    }
  }

  public static void logBoard(String player, Risk game) {
    if (!ENABLED) {
      return;
    }
    boardCounter++;
    treeEventCounter = 0;

    BoardLog boardLog = new BoardLog();
    boardLog.setTerritoryMap(game.getBoard().getTerritories());
    boardLog.setPlayer(player);

    LogEntry logEntry = new LogEntry();
    logEntry.setType(Type.BOARD);
    logEntry.setData(boardLog);

    log(serialize(logEntry));
  }

  public static void logTree(TreeNode<?> root) {
    if (!ENABLED) {
      return;
    }

    if (boardCounter >= TREE_START || treeEventCounter == TREE_EVENT_LIMIT_PER_BOARD) {
      return;
    }

    treeEventCounter++;

    LogEntry logEntry = new LogEntry();
    logEntry.setType(Type.TREE);
    logEntry.setData(root);

    log(serialize(logEntry));
  }

  public static void log(String logString) {
    logString += System.lineSeparator();
    try {
      if (Files.notExists(EVENT_LOG_FILE)) {
        Files.createFile(EVENT_LOG_FILE);
      }
      Files.write(EVENT_LOG_FILE, logString.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
    } catch (IOException e) {
      System.out.println(e);
    }
  }

  public static void reset() {
    try {
      Files.delete(EVENT_LOG_FILE);
    } catch (IOException e) {
      System.out.println(e);
    }
  }

  private static String serialize(Object object) {
    try {
      return mapper.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
