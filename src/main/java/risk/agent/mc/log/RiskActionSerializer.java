package risk.agent.mc.log;

import at.ac.tuwien.ifs.sge.game.risk.board.RiskAction;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class RiskActionSerializer extends StdSerializer<RiskAction> {
  public RiskActionSerializer() {
    this(null);
  }

  public RiskActionSerializer(Class<RiskAction> t) {
    super(t);
  }

  @Override
  public void serialize(
      RiskAction value, JsonGenerator jgen, SerializerProvider provider)
      throws IOException, JsonProcessingException {

    jgen.writeStartObject();
    jgen.writeStringField("action", value.toString());
    jgen.writeNumberField("selecting", value.selected());
    jgen.writeNumberField("reinforcedId", value.reinforcedId());
    jgen.writeNumberField("attackerCasualties", value.attackerCasualties());
    jgen.writeNumberField("defenderCasualties", value.defenderCasualties());
    jgen.writeNumberField("fortifiedId", value.fortifiedId());
    jgen.writeNumberField("bonus", value.getBonus());
    jgen.writeNumberField("troops", value.troops());
    jgen.writeBooleanField("isBonus", value.isBonus());
    jgen.writeBooleanField("isCardIds", value.isCardIds());
    jgen.writeBooleanField("isEndPhase", value.isEndPhase());
    jgen.writeEndObject();
  }
}
