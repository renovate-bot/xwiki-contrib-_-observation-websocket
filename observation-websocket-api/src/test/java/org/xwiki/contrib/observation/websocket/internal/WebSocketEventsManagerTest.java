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
import java.util.HashMap;
import java.util.Map;

import javax.websocket.Session;

import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.bridge.event.WikiReadyEvent;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import liquibase.pro.packaged.k;
import liquibase.pro.packaged.v;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate {@link WebSocketEventsManager}.
 * 
 * @version $Id$
 */
@ComponentTest
class WebSocketEventsManagerTest
{
    @MockComponent
    private ObservationManager observation;

    @InjectMockComponents
    private WebSocketEventsManager manager;

    @Test
    void test() throws ClassNotFoundException, InstantiationException, IllegalAccessException,
        IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException
    {
        Session session = mock(Session.class);
        Map<String, Object> userProperties = new HashMap<>();
        when(session.getUserProperties()).thenReturn(userProperties);
        
        Map<String, Object> message = new HashMap<>();
        Map<String, Object> eventType = new HashMap<>();
        eventType.put("id", WikiReadyEvent.class.getName());
        message.put("eventType", eventType);
        Map<String, Object> params = new HashMap<>();
        params.put("wikiId", "wiki1");
        message.put("params", params);

        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                EventListener listener = invocation.getArgument(0);

                assertEquals("websocket-1", listener.getName());
                assertSame(WikiReadyEvent.class, listener.getEvents().get(0).getClass());
                //assertEquals("wiki1", ((WikiReadyEvent)listener.getEvents().get(0)).getWikiId());

                return null;
            }

        }).when(this.observation).addListener(any());

        this.manager.addEvent(message, session);

        Map<String, EventListener> listeners =
            (Map) session.getUserProperties().get(WebSocketEventsManager.KEY_SESSION_LISTENERS);

        assertNotNull(listeners);

        Map.Entry<String, EventListener> listenerEntry = listeners.entrySet().iterator().next();

        String listenerName = listenerEntry.getValue().getName();

        assertEquals(listenerName, listenerEntry.getKey());

        this.manager.dispose(session);

        verify(this.observation).removeListener(listenerName);
    }
}
