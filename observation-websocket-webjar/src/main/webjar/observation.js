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
/*!
## Velocity code here.
#set ($observationWebSocketURL = $services.websocket.url('observation'))
#[[*/
// Start JavaScript-only code.
(function(observationWebSocketURL) {
  "use strict";

define(['jquery'], function($) {
  var webSocket = new WebSocket(observationWebSocketURL);

  var webSocketPromise = $.Deferred();
  webSocket.onopen = function() {
    console.log('Observation WebSocket opened!');
    webSocketPromise.resolve(webSocket);
  };

  webSocket.onclose = function(event) {
    console.log(`Observation WebSocket closed: ${event.code} ${event.reason}`);
  };

  webSocket.onerror = function(event) {
    console.log(`Observation WebSocket error: ${event.code} ${event.reason}`);
  };

  var listeners = {};
  webSocket.onmessage = function(message) {
    console.log(`Observation WebSocket message: ${message}`);
    message = JSON.parse(message);
    if (message.type === 'event') {
      // Call the event listener.
      var listener = listeners[message.data.eventData.listenerId];
      if (typeof listener === 'function') {
        listener(message.data.event, message.data.source, message.data.data, message.data.eventData.data);
      }
    }
  };

  var nextListenerId = 0;
  var registerEventListener = function(eventType, listener, data) {
    if (typeof eventType === 'string') {
      eventType = {id: eventType};
    }
    eventType.params = eventType.params || {};
    var eventData = {data, listenerId: nextListenerId++};
    // Add the event listener.
    var message = {
      type: 'addListener',
      data: {eventData, eventType}
    };
    listeners[eventData.listenerId] = listener;
    webSocket.send(JSON.stringify(message));
  };

  var on = function(eventTypes, listener, data) {
    webSocketPromise.done(function(webSocket) {
      if (!$.isArray(eventTypes)) {
        eventTypes = [eventTypes];
      }
      eventTypes.forEach(eventType => registerEventListener(eventType, listener, data));
    });
  };

  return {on};
});

// End JavaScript-only code.
}).apply(']]#', $jsontool.serialize([$observationWebSocketURL]));
