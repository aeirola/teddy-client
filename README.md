Teddy Client
============

Android client app for connecting to irssi instances running the `teddy.pl` remote access plugin over WebSockets.

Currently based on the Autobahn|Android WebSocket client library and the Jackson JSON library.

TODO
----
- Improve line rendering
 - Filter ANSI escape codes from input
  - Generate android spans from them
 - Show nick and timestamp
 - TextView_autoLink
- Improved server event handling
 - Use of Loaders and Cursors for data updates?
 - Register only to required events
- SSL support
- Visual tweaking
 - Material design
 - Transitions
 - Fonts
- Move all protocol handling to separate worker thread
 - Loaders would do this
 - Do network requests and JSON (de)serialization there
 - Communication using Handlers?
- Improve connection handling
 - Timeout detection
  - No ping in 15 sec
 - Disconnection
  - On inactivity (~5min)
 - Network state detection
 - Line list resync on reconnect

----- Release? -----

- Share action
- Widgets?
- Autoregonized embedded content
 - based on url regonition
 - images, videos, spotifys?
- Redesign the WebSocket protocol
 - Limit to `bind` and `eval` operations?
 - Completely own fork? (JSON-RPC, WAMP?)