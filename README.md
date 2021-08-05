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
