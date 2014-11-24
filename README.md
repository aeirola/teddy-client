Teddy Client
============

Android client app for connecting to irssi instances running the [`teddy-nu.pl`](https://github.com/ailin-nemui/teddy/tree/teddy-nu) remote access plugin over WebSockets.

Currently based on the Autobahn|Android WebSocket client library and the Jackson JSON library.

[![Build Status](https://travis-ci.org/aeirola/teddy-client.svg)](https://travis-ci.org/aeirola/teddy-client)

TODO
----
- TravisCI
 - Upload to g-drive?

------- Field test -------

- Improve line rendering
 - Handle irssi escapes
 - TextView_autoLink
- Improved server event handling
 - Use of Loaders and Cursors for data updates?
 - Register only to required events
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