Teddy Client
============

Android client app for connecting to irssi instances running the [`teddy-nu.pl`](https://github.com/ailin-nemui/teddy/tree/teddy-nu) remote access plugin over WebSockets.

Currently based on the Autobahn|Android WebSocket client library and the Jackson JSON library.

[![Build Status](https://travis-ci.org/aeirola/teddy-client.svg)](https://travis-ci.org/aeirola/teddy-client)

TODO
----

- SQLite indexes
- Database consistency
- Add scrollback fetching
- Hide input field when reading scrollback
- Improved server event handling
- Visual tweaking
 - Material design
 - Transitions
 - Fonts
- Improve connection handling
 - Network state detection

----- Release? -----

- Share action
- Widgets?
- Autoregonized embedded content
 - based on url regonition
 - images, videos, spotifys?
- Redesign the WebSocket protocol
 - Limit to `bind` and `eval` operations?
 - Completely own fork? (JSON-RPC, WAMP?)



Component architecture
----------------------

 - UI Layer
  - Shows data in lists and such
 - CursorLoader
  - For fetchign data from the content provider
 - ContentProvider
  - Fetches data from rest client, and caches it in data store
 - Data Store
  - SQL lite
  - Serves as cache
 - REST client
  - Maintains connection to server and fetches/listens for data

Duplicated for windows and lines

