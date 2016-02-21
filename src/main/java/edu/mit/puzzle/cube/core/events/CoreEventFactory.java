package edu.mit.puzzle.cube.core.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Optional;

public class CoreEventFactory implements EventFactory {

    protected static ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public Optional<Event> generateEvent(String json) throws IOException {

        JsonNode node = MAPPER.readTree(json);
        JsonNode eventTypeNode = node.get("eventType");
        String eventType = eventTypeNode.asText();

        if ("HuntStart".equals(eventType)) {
            String runId = node.get("runId").asText();
            return Optional.of(new HuntStartEvent(runId));
        } else if ("FullRelease".equals(eventType)) {
            String runId = node.get("runId").asText();
            String puzzleId = node.get("puzzleId").asText();
            return Optional.of(new FullReleaseEvent(runId, puzzleId));
        } else if ("TimingUpdate".equals(eventType)) {
            String runId = node.get("runId").asText();
            return Optional.of(new TimingUpdateEvent(runId));
        }

        return Optional.empty();
    }

}
