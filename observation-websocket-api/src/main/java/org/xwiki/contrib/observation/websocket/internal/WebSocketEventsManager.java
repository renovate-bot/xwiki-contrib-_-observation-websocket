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

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @version $Id$
 */
@Component(roles = WebSocketEventsManager.class)
@Singleton
public class WebSocketEventsManager
{
    private static final String KEY_SESSION_LISTENERS = "xwiki.observation.listeners";

    @Inject
    private ObservationManager observation;

    @Inject
    private Logger logger;

    private final AtomicLong idCounter = new AtomicLong();

    class WebSocketEventListener implements EventListener
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

    public void addEvent(Map<String, Object> message, Session session)
        throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException,
        InvocationTargetException, NoSuchMethodException, SecurityException
    {
        // Parse the message
        Map<String, Object> eventType = (Map) message.get("eventType");
        Class<?> eventClass =
            Class.forName((String) eventType.get("id"), true, Thread.currentThread().getContextClassLoader());
        // TODO: add support for event parameters
        Event event = (Event) eventClass.getConstructor().newInstance();

        // Get the custom date
        Object listenerData = message.get("eventData");

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

    public void dispose(Session session)
    {
        // Unregister the listener associated to the session
        EventListener listener = (EventListener) session.getUserProperties().get(KEY_SESSION_LISTENERS);

        if (listener != null) {
            this.observation.removeListener(listener.getName());
        }
    }

    private void onEvent(Event event, Object source, Object data, Object listenerData, Session session)
        throws JsonProcessingException
    {
        // Serialize the event and data as a message
        Map<String, Object> messageObject = new HashMap<>();
        messageObject.put("event", event);
        messageObject.put("source", source);
        messageObject.put("data", data);
        messageObject.put("eventData", listenerData);
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(messageObject);

        // Send the message
        session.getAsyncRemote().sendText(json);
    }
}
