package edu.mit.puzzle.cube.core.serverresources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import edu.mit.puzzle.cube.core.events.Event;
import edu.mit.puzzle.cube.core.events.EventFactory;
import edu.mit.puzzle.cube.core.events.EventProcessor;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.ext.json.JsonRepresentation;

import java.io.IOException;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public class EventsResource extends AbstractCubeResource {

    private final EventFactory eventFactory;
    private final EventProcessor eventProcessor;

    public EventsResource(
            EventFactory eventFactory,
            EventProcessor eventProcessor
    ) {
        this.eventFactory = checkNotNull(eventFactory);
        this.eventProcessor = checkNotNull(eventProcessor);
    }


    @Override
    protected String handleGet() throws JsonProcessingException {
        return "";
    }

    @Override
    protected String handlePost(JsonRepresentation representation) throws JsonProcessingException {
        try {
            JSONObject obj = representation.getJsonObject();
            Optional<Event> event = eventFactory.generateEvent(obj.toString());

            if (event.isPresent()) {
                eventProcessor.process(event.get());
                return MAPPER.writeValueAsString(ImmutableMap.of("processed",true));
            } else {
                return MAPPER.writeValueAsString(ImmutableMap.of("processed",false));
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
