package risk.agent.mc.log;

import at.ac.tuwien.ifs.sge.game.risk.board.Risk;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import at.ac.tuwien.ifs.sge.game.risk.board.RiskTerritory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import risk.agent.mc.mcts.TreeNode;
import risk.agent.mc.mcts.UCBNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

/**
 * This class logs event logs needed for the debugging in the Node.js frontend
 *
 * Currently, there are two event log types: TREE, BOARD
 */
public class EventLogService {

  private static final Path EVENT_LOG_FILE = Path.of("./event-logs.log");
  private static final boolean ENABLED = false;


  private static final boolean TREE_EVENTS_ENABLED = false;
  private static final int TREE_EVENT_LIMIT_PER_BOARD = 100;
  private static final int TREE_START = 40;
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
    TREE
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
    private int round;

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

    public int getRound() {
      return round;
    }

    public BoardLog setRound(int round) {
      this.round = round;
      return this;
    }
  }

  /**
   * Creates a BOARD event log for the frontend application
   * @param player the player which changed the board last
   * @param game the game after the change of the player
   */
  public static void logBoard(String player, Risk game) {
    if (!ENABLED) {
      return;
    }
    boardCounter++;

    BoardLog boardLog = new BoardLog();
    boardLog.setTerritoryMap(game.getBoard().getTerritories());
    boardLog.setPlayer(player);
    boardLog.setRound(boardCounter);

    LogEntry logEntry = new LogEntry();
    logEntry.setType(Type.BOARD);
    logEntry.setData(boardLog);

    log(serialize(logEntry));
  }

  /**
   * Creates a TREE event log for the frontend application
   * @param root the root node of the tree
   */
  public static void logTree(TreeNode<?> root) {
    if (!ENABLED || !TREE_EVENTS_ENABLED) {
      return;
    }

    if (boardCounter >= TREE_START || treeEventCounter == TREE_EVENT_LIMIT_PER_BOARD) {
      return;
    }

    treeEventCounter++;

    LogEntry logEntry = new LogEntry();
    logEntry.setType(Type.TREE);
    logEntry.setData(root);

    if (root instanceof UCBNode) {
      EventLogUtils.calculateUCBRecursive((UCBNode) root);
    }

    log(serialize(logEntry));
  }

  /**
   * Appends a message to the event logs file
   * @param logString the log string to append
   */
  private static void log(String logString) {
    if (!ENABLED) {
      return;
    }
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

  /**
   * Deletes the event log file if it exists
   */
  public static void reset() {
    if (!ENABLED) {
      return;
    }
    try {
      Files.delete(EVENT_LOG_FILE);
    } catch (IOException e) {
      System.out.println(e);
    }
  }

  /**
   * Serializes an Java object to json
   * @param object the object to serialize
   * @return a json string
   */
  private static String serialize(Object object) {
    if (!ENABLED) {
      return null;
    }
    try {
      return mapper.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the current count of board actions logged
   * @return the number of board actions logged
   */
  public static int getBoardCounter() {
    return boardCounter;
  }
}
