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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.websocket.Session;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

/**
 * @version $Id$
 */
@Component(roles = WebSocketEventsManager.class)
@Singleton
public class WebSocketEventsManager
{
    private static final String KEY_SESSION_LISTENER = "xwiki.observation.listeners";

    @Inject
    private ObservationManager observation;

    private final AtomicLong idCounter = new AtomicLong();

    class WebSocketEventListener implements EventListener
    {
        private final String name;

        private final Session session;

        private WebSocketEventListener(Session session)
        {
            this.name = "websocket-" + idCounter.incrementAndGet();
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
            return Collections.emptyList();
        }

        @Override
        public void onEvent(Event event, Object source, Object data)
        {
            WebSocketEventsManager.this.onEvent(event, source, data, this.session);
        }
    }

    public void addEvent(String json, Session session)
    {
        // TODO: Parse the message
        Event event = null;

        EventListener listener;
        synchronized (session) {
            // Register a listener for the session if it's not already the case
            listener = (EventListener) session.getUserProperties().get(KEY_SESSION_LISTENER);
            if (listener == null) {
                listener = new WebSocketEventListener(session);
                session.getUserProperties().put(KEY_SESSION_LISTENER, listener);
            }
        }

        // Register the new event
        this.observation.addEvent(listener.getName(), event);
    }

    public void dispose(Session session)
    {
        // Unregister the listener associated to the session
        EventListener listener = (EventListener) session.getUserProperties().get(KEY_SESSION_LISTENER);

        if (listener != null) {
            this.observation.removeListener(listener.getName());
        }
    }

    private void onEvent(Event event, Object source, Object data, Session session)
    {
        // TODO: Serialize the event and data as a message
        String json = null;

        // Send the message
        session.getAsyncRemote().sendText(json);
    }
}
