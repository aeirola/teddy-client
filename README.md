Teddy Client
============

Android client app for connecting to irssi instances running the `teddy.pl` remote access plugin over WebSockets.

Currently based on the Autobahn|Android WebSocket client library and the Jackson JSON library.

TODO
----
- Server event handling
 - Use of Loaders and Cursors for data updates?
- SSL support
- Visual tweaking
- Move all protocol handling to separate worker thread
 - Do network requests and JSON (de)serialization there
 - Communication using Handlers?
- Connection handling
 - Reconnection
 - Disconnection
- Redesign the WebSocket protocol
 - Limit to `bind` and `eval` operations?
 - Completely own fork? (JSON-RPC, WAMP?)