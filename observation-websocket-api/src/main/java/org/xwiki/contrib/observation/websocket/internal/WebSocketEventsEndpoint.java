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

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.websocket.AbstractXWikiEndpoint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@Named("observation")
@Singleton
public class WebSocketEventsEndpoint extends AbstractXWikiEndpoint
{
    @Inject
    private DocumentAccessBridge bridge;

    @Override
    public void onOpen(Session session, EndpointConfig config)
    {
        this.context.run(session, () -> {
            if (this.bridge.getCurrentUserReference() == null) {
                close(session, CloseReason.CloseCodes.CANNOT_ACCEPT,
                    "We don't accept connections from guest users. Please login first.");
            } else {
                session.addMessageHandler(new MessageHandler.Whole<String>()
                {
                    @Override
                    public void onMessage(String message)
                    {
                        handleMessage(session, message);
                    }
                });
            }
        });

    }

    /**
     * Handles received messages.
     *
     * @param message the received message
     * @return the message to send back
     */
    @SuppressWarnings("unchecked")
    public String onMessage(String message)
    {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String, Object> result = (Map<String, Object>) objectMapper.readValue(message, Object.class);
            String type = (String) result.get("type");
            
            return type;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        
        return "To change";
    }

}
