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

import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;

import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.bridge.event.WikiReadyEvent;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.properties.ConverterManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.CoreConfiguration;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xpn.xwiki.web.Utils;

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
@ReferenceComponentList
class WebSocketEventsManagerTest
{
    @MockComponent
    private ObservationManager observation;

    @MockComponent
    private CoreConfiguration CoreConfiguration;

    @MockComponent
    private ConverterManager converter;

    @InjectMockComponents
    private WebSocketEventsManager manager;

    @InjectComponentManager
    private ComponentManager componentManager;

    @Test
    void test() throws ClassNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException,
        InvocationTargetException, NoSuchMethodException, SecurityException
    {
        Utils.setComponentManager(this.componentManager);

        Session session = mock(Session.class);
        Map<String, Object> userProperties = new HashMap<>();
        when(session.getUserProperties()).thenReturn(userProperties);
        RemoteEndpoint.Async async = mock(RemoteEndpoint.Async.class);
        when(session.getAsyncRemote()).thenReturn(async);

        when(this.converter.convert(String.class, "wiki1")).thenReturn("wiki1");

        Map<String, Object> message = new HashMap<>();
        message.put("eventData", "custom data");
        Map<String, Object> eventType = new HashMap<>();
        eventType.put("id", WikiReadyEvent.class.getName());
        message.put("eventType", eventType);
        Map<String, Object> params = new HashMap<>();
        params.put("wikiId", "wiki1");
        eventType.put("params", params);

        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                EventListener listener = invocation.getArgument(0);

                assertEquals("websocket-1", listener.getName());
                assertSame(WikiReadyEvent.class, listener.getEvents().get(0).getClass());
                assertEquals("wiki1", ((WikiReadyEvent) listener.getEvents().get(0)).getWikiId());

                return null;
            }

        }).when(this.observation).addListener(any());

        this.manager.addEvent(message, session);

        Map<String, EventListener> listeners =
            (Map) session.getUserProperties().get(WebSocketEventsManager.KEY_SESSION_LISTENERS);

        assertNotNull(listeners);

        Map.Entry<String, EventListener> listenerEntry = listeners.entrySet().iterator().next();

        EventListener listener = listenerEntry.getValue();

        assertEquals(listener.getName(), listenerEntry.getKey());

        XWikiDocument document = new XWikiDocument(new DocumentReference("wiki", "space", "document"));
        document.setRCSVersion(new Version(42, 43));
        XWikiContext xcontext = new XWikiContext();
        xcontext.setWiki(new XWiki());
        listener.onEvent(new WikiReadyEvent("wiki1"), document, xcontext);

        this.manager.dispose(session);

        verify(this.observation).removeListener(listener.getName());
    }
}
