Java Native Web Server
===

Use `com.sun.net.httpserver.HttpServer.create`.

It is simple and efficient, suitable for most cases such as API server, web server, and RPC server.

Components
---

* **Config files** - jackson-databind
* **A lot of timeouts** - HashedWheelTimer (netty-common)
* **Cron jobs** - ThreadPoolTaskScheduler (spring-context)
* **Tasks aggregation and batch execution** - SmartBatcher
* **Persistent Queue** - FQueue
* **URL Router** - RouteKit
* **Fault tolerance** - Resilience4j
* **Database** - JdbcTemplate (spring-jdbc)
* **HTML**
  - Rendering - Java Template Engine
  - Styling - Bootstrap 5
* **Markdown processing** - commonmark
* **Documentation** - MkDocs

Threading Model
---

* sun.net.httpserver.ServerImpl.Dispatcher
  - accept new connection
  - process incoming requests
* VirtualThreadPerTaskExecutor
  - read request
  - write response
