package edu.mit.puzzle.cube.core.events;

import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Protocol;
import org.restlet.resource.ClientResource;

/**
 * Publishes an event by posting it to an HTTP route.
 */
public class EventPublisher<T extends Event> implements EventProcessor<T> {

    private final Client client;
    private final ClientResource clientResource;

    public EventPublisher(Context context, String route) {
        client = new Client(context, Protocol.HTTP);
        clientResource = new ClientResource(route);
        clientResource.setNext(client);
    }

    @Override
    public void process(T event) {
        clientResource.post(event, MediaType.APPLICATION_JSON);
    }
}
