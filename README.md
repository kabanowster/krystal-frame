# Krystal Frame

Java framework simplifying workflow with various applications. Includes JavaFx libraries and embedded Tomcat server.
Utilises Spring-Core module, is highly customisable, allowing for custom implementation of core modules (and offering standard implementations by default).
To begin setting-up, start with ***KrystalFramework*** static class and its documentation.

### Modules:

- **SQL Query Factory** - builder-like classes and abstractions to serve and ease the creation of complex database SQL queries *(see: **Query.class**)*;
- ***QueryExecutorInterface.class*** - backbone of database operations;
- ***PersistenceInterface.class*** - ORM serving interface, highly customisable through annotations, utilising Virtual Threads for non-blocking persistence actions;
- ***PropertiesInterface.class*** - convenient properties and cmdlnArgs processing;
- ***CommanderInterface.class*** - internal commands parser *(see: **BaseCommander.class** implementation)*;
- ***FlowControlInterface.class*** - convenient access to Phasers and ScheduledExecutor to synchronise parallel taks;
- ***LoggingInterface.class*** - interface extending Log4j *LoggingWrapper.class*;