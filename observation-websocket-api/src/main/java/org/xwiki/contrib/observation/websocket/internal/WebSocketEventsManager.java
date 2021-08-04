/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.observation.websocket.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.websocket.Session;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.properties.ConverterManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @version $Id$
 */
@Component(roles = WebSocketEventsManager.class)
@Singleton
public class WebSocketEventsManager
{
    static final String KEY_SESSION_LISTENERS = "xwiki.observation.listeners";

    static final String EVENTDATA = "eventData";

    @Inject
    private ObservationManager observation;

    @Inject
    private ConverterManager converter;

    @Inject
    private Logger logger;

    private final AtomicLong idCounter = new AtomicLong();

    final class WebSocketEventListener implements EventListener
    {
        private final String name;

        private final List<Event> events;

        private final Object listenerData;

        private final Session session;

        private WebSocketEventListener(Event event, Object listenerData, Session session)
        {
            this.name = "websocket-" + idCounter.incrementAndGet();
            this.events = Collections.singletonList(event);

            this.listenerData = listenerData;

            this.session = session;
        }

        @Override
        public String getName()
        {
            return this.name;
        }

        @Override
        public List<Event> getEvents()
        {
            return this.events;
        }

        @Override
        public void onEvent(Event event, Object source, Object data)
        {
            try {
                WebSocketEventsManager.this.onEvent(event, source, data, this.listenerData, this.session);
            } catch (JsonProcessingException e) {
                logger.error("Failed to send the event asa websocket message", e);
            }
        }
    }

    /**
     * @param message the addEvent message received from the client
     * @param session the WebSocket sessions
     * @throws ClassNotFoundException when failing to resolve the event
     * @throws InstantiationException when failing to resolve the event
     * @throws IllegalAccessException when failing to resolve the event
     * @throws IllegalArgumentException when failing to resolve the event
     * @throws InvocationTargetException when failing to resolve the event
     * @throws NoSuchMethodException when failing to resolve the event
     * @throws SecurityException when failing to resolve the event
     */
    public void addEvent(Map<String, Object> message, Session session)
        throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException,
        InvocationTargetException, NoSuchMethodException, SecurityException
    {
        // Parse the message
        Map<String, Object> eventType = (Map) message.get("eventType");
        Class<?> eventClass =
            Class.forName((String) eventType.get("id"), true, Thread.currentThread().getContextClassLoader());
        Map<String, Object> params = (Map) eventType.get("params");
        Event event = null;
        for (Constructor constructor : eventClass.getConstructors()) {
            if (constructor.getParameterCount() == params.size()) {
                Object[] parameters = converterParameters(constructor, params);

                if (parameters != null) {
                    event = (Event) constructor.newInstance(parameters);
                }
            }
        }

        if (event == null) {
            throw new NoSuchMethodException("No constructor could be found for parameters " + params);
        }

        // Get the custom date
        Object listenerData = message.get(EVENTDATA);

        // Create the listener
        WebSocketEventListener listener = new WebSocketEventListener(event, listenerData, session);

        // Register the listener
        this.observation.addListener(listener);

        // Rembember the registered listener in the session
        Map<String, EventListener> listeners;
        synchronized (session) {
            listeners = (Map<String, EventListener>) session.getUserProperties().get(KEY_SESSION_LISTENERS);
            if (listeners == null) {
                listeners = new ConcurrentHashMap<>();
                session.getUserProperties().put(KEY_SESSION_LISTENERS, listeners);
            }
        }
        listeners.put(listener.getName(), listener);
    }

    private Object[] converterParameters(Constructor constructor, Map<String, Object> params)
    {
        Parameter[] constructorParameters = constructor.getParameters();

        Object[] parameters = new Object[constructorParameters.length];

        for (int i = 0; i < constructorParameters.length; ++i) {
            Parameter constructorParameter = constructorParameters[i];
            String name = constructorParameter.getName();

            // Return null if the provided parameters names are not the expected ones
            if (!params.containsKey(name)) {
                return null;
            }

            Object value = params.get(name);

            parameters[i] = this.converter.convert(constructorParameter.getParameterizedType(), value);
        }

        return parameters;
    }

    /**
     * @param session release any resource associated with this sessions
     */
    public void dispose(Session session)
    {
        // Unregister the listener associated to the session
        Map<String, EventListener> listeners = (Map) session.getUserProperties().get(KEY_SESSION_LISTENERS);

        if (listeners != null) {
            listeners.values().forEach(l -> this.observation.removeListener(l.getName()));
        }
    }

    private void onEvent(Event event, Object source, Object data, Object listenerData, Session session)
        throws JsonProcessingException
    {
        StringBuilder builder = new StringBuilder();

        builder.append('{');

        ObjectMapper mapper = new ObjectMapper();

        // Serialize Event
        builder.append("\"event\":");
        builder.append(mapper.writeValueAsString(event));

        // Serialize source
        try {
            builder.append(',');
            builder.append("\"source\":");
            builder.append(mapper.writeValueAsString(source));
        } catch (Exception e) {
            this.logger.warn("Failed to serialize the source of event [{}]", event.toString(), e);
        }

        // Serialize data
        try {
            builder.append(',');
            builder.append("\"data\":");
            builder.append(mapper.writeValueAsString(data));
        } catch (Exception e) {
            this.logger.warn("Failed to serialize the date of event [{}]", event.toString(), e);

        }

        // Serialize the listener data
        builder.append(',');
        builder.append('"');
        builder.append(EVENTDATA);
        builder.append('"');
        builder.append(':');
        builder.append(mapper.writeValueAsString(listenerData));

        builder.append('}');

        // Send the message
        session.getAsyncRemote().sendText(builder.toString());
    }
}
