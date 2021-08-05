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
  // The map of registered event listeners.
  var listeners = {};

  // The promise used to access the WebSocket after the connection is established.
  var webSocketPromise = $.Deferred();

  // We need to be able to reconnect the WebSocket in case the connection is closed (e.g. when the connection times out
  // or when the server restarts).
  var connectTimeout;
  var reconnect = function() {
    // We cannot change the promise state once it was resolved / rejected so we need to create another one.
    if (webSocketPromise.state() !== 'pending') {
      webSocketPromise = $.Deferred();
    }

    // Wait 5 seconds before trying to reconnect.
    clearTimeout(connectTimeout);
    connectTimeout = setTimeout(() => {
      connect();

      // Re-add the event listeners that were registered in the previous WebSocket connection (session).
      var listenersArray = Object.values(listeners);
      listeners = {};
      listenersArray.forEach(listener => on(listener.eventType, listener.listener, listener.data));
    }, 5000);
  };

  var connect = function() {
    var webSocket = new WebSocket(observationWebSocketURL);

    webSocket.onopen = function() {
      console.log('Observation WebSocket opened!');
      // We can now use the WebSocket to register event listeners.
      webSocketPromise.resolve(webSocket);
    };

    webSocket.onclose = function(event) {
      console.log(`Observation WebSocket closed: ${event.code} ${event.reason}`);
      reconnect();
    };

    webSocket.onerror = function(event) {
      console.log(`Observation WebSocket error: ${event.code} ${event.reason}`);
      reconnect();
    };

    webSocket.onmessage = function(event) {
      console.log(`Observation WebSocket message: ${event.data}`);
      var message = JSON.parse(event.data);
      if (message.type === 'event') {
        // An event happened on the server side. Call the event listener on the client side.
        /* jshint ignore:start */
        // JSHint doesn't like pptional chaining (https://github.com/jshint/jshint/issues/3448)
        var listener = listeners[message.data?.eventData?.listenerId]?.listener;
        /* jshint ignore:end */
        if (typeof listener === 'function') {
          listener(message.data.event, message.data.source, message.data.data, message.data.eventData.data);
        }
      }
    };

    // TODO: We need to implement a ping-pong strategy in order to keep the WebSocket connection alive.
  };

  // We need to know which listener function to call when an event message is received so we need to associate a unique
  // id (in the scope of this JavaScript module) to the listener function. We do this using a counter that always
  // increments because in the future we may want to also remove event listeners.
  var nextListenerId = 0;

  var registerEventListener = function(webSocket, eventType, listener, data) {
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
    listeners[eventData.listenerId] = {eventType, listener, data};
    webSocket.send(JSON.stringify(message));
  };

  /**
   * Registers an event listener for a server-side event.
   *
   * @param {Array|Object|String} eventTypes the list of server-side events to listen to; each event is specified either
   *          using the full name of the Java event class (e.g. 'org.xwiki.refactoring.event.DocumentRenamedEvent') or
   *          using an object with this format: {id: '<eventClassName>', params: {...}}; the event parameters can be
   *          used to filter the events and are passed to the event constructor on the server-side so their name needs
   *          to match the name of the event constructor parameters
   * @param {Function} listener the listener function to call when the specified event is triggered on the server-side;
   *          the listener receives 4 arguments: event, source, sourceData and eventData; source holds information about
   *          the code that triggered the event; event data is the data you passed when registering the event listener
   * @param {Object} data some data to associate to the event; it will be passed as is to the event listener when the
   *          event is triggered
   */
  var on = function(eventTypes, listener, data) {
    webSocketPromise.done(function(webSocket) {
      if (!$.isArray(eventTypes)) {
        eventTypes = [eventTypes];
      }
      eventTypes.forEach(eventType => registerEventListener(webSocket, eventType, listener, data));
    });
  };

  // Connect to the observation end-point on the server side in order to be able to register event listeners.
  connect();

  return {on};
});

// End JavaScript-only code.
}).apply(']]#', $jsontool.serialize([$observationWebSocketURL]));
