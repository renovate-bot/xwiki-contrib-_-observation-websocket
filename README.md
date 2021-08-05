# WebSocket Observation

Listen to XWiki events from JavaScript. Here's how you can use the API:

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
