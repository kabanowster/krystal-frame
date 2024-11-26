# Krystal Frame

KrystalFrame is a standalone Java framework designed to simplify the development of multithreaded applications and servers, whether they are window-based or console-like. It features a robust, original ORM implementation powered by Virtual Threads, enhancing scalability and thread management. The framework includes JavaFX libraries, an embedded Tomcat server, and original console-like interface. Built with Spring-Core module, it is highly customizable, allowing developers to provide custom implementations for core interfaces, while also offering default implementations out-of-the-box. KrystalFrame also comes with Krystal Tools, featuring the innovative VirtualPromise class, which facilitates pipeline-style synchronization of Virtual Threads, further streamlining concurrent processing.
To begin, start with ***KrystalFramework*** static class and its documentation.

### Modules:

- **SQL Query Factory** - builder-like classes and abstractions to serve and ease the creation of complex database SQL queries *(see: **Query.class**)*;
- ***QueryExecutorInterface.class*** - backbone of database operations;
- ***PersistenceInterface.class*** - ORM serving interface, highly customisable through annotations, utilising Virtual Threads for non-blocking persistence actions;
- ***PropertiesInterface.class*** - convenient properties and cmdlnArgs processing;
- ***CommanderInterface.class*** - internal commands parser *(see: **BaseCommander.class** implementation)*;
- ***FlowControlInterface.class*** - convenient access to Phasers and ScheduledExecutor to synchronise parallel taks;
- ***LoggingInterface.class*** - interface extending Log4j *LoggingWrapper.class*;