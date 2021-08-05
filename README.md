# WebSocket Observation

Listen to XWiki events from JavaScript.

* Project Lead: [Marius Dumitru Florea](http://www.xwiki.org/xwiki/bin/view/XWiki/mflorea)
* Communication: [Mailing List](http://dev.xwiki.org/xwiki/bin/view/Community/MailingLists), [IRC]( http://dev.xwiki.org/xwiki/bin/view/Community/IRC)
* [Development Practices](http://dev.xwiki.org)
* Minimal XWiki version supported: XWiki 13.7RC1
* License: LGPL 2.1+
* Continuous Integration Status: [![Build Status](http://ci.xwiki.org/job/XWiki%20Contrib/job/observation-websocket/job/master/badge/icon)](http://ci.xwiki.org/view/Contrib/job/XWiki%20Contrib/job/observation-websocket/job/master/)

## Usage

Here's how you can use the API:

```javascript
require.config({
  paths: {
    'xwiki-observation': $jsontool.serialize($services.webjars.url(
      'org.xwiki.contrib.observation-websocket:observation-websocket-webjar',
      'observation.js',
      {'evaluate': true}
    ))
  }
});

require(['xwiki-observation'], function(observation) {
  observation.on([
    'org.xwiki.bridge.event.DocumentCreatedEvent',
    'org.xwiki.bridge.event.DocumentDeletedEvent',
  ], (event, source, sourceData) => {
    console.log(`Received event ${event} from ${source} with ${sourceData}.`);
    ...
  });

  observation.on({
    id: 'org.xwiki.component.event.ComponentDescriptorAddedEvent',
    params: {
      roleType: 'org.xwiki.uiextension.UIExtension'
    }
  }, (event, source, sourceData, eventData) => {
    console.log(`Received event ${event} from ${source} with ${sourceData} and ${eventData}.`);
    ...
  }, {custom: 'event data'});
});
```

## Communication Protocol

The following messages are exchanged between the client and the server:

* the client wants to register an event listener
```json
{
  "type": "addListener",
  "data": {
    "eventType": {
      "id": "org.xwiki.component.event.ComponentDescriptorAddedEvent",
      "params": {
        "roleType": "org.xwiki.uiextension.UIExtension"
      }
    },
    "eventData": {}
  }
}
```
* an event was triggered on the server
```json
{
  "type": "event",
  "data": {
    "event": {},
    "source": {},
    "data": {},
    "eventData": {}
  }
}
```

## TODO

* Handle security issues: who's allowed to register event listeners? This is important because events can leak private information from the server.
* Implement a ping-pong strategy in order to keep the WebSocket connection alive (Jetty closes the connection after 5 minutes of inactivity for instance, so we're forced to reconnect and re-add the event listeners).
* Find a better / cleaner way of serializing event data that is pushed to the client.
* Extend the API to support removing an event listener.
