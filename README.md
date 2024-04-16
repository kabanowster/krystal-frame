# Krystal Frame

Java framework, simplifying workflow with small JavaFx and Tomcat server applications.
Utilises Spring-core and webflux packages, offers built-in modules (with standard implementations) for various basic functionalities:

- **SQL Query Factory** - builder-like classes and abstractions to serve and ease the creation of complex database SQL queries *(see: **Query.class**)*;
- ***PersistenceInterface.class*** - ORM serving interface, highly customisable through annotations, utilising Virtual Threads and/or Flux streams;
- ***PropertiesInterface.class*** - convenient properties and cmdlnArgs processing;
- ***CommanderInterface.class*** - internal commands parser *(see: **BaseCommander.class** implementation)*;
- ***FlowControlInterface.class*** - convenient access to Phasers and ScheduledExecutor to synchronise parallel taks;
- ***LoggingInterface.class*** - interface extending Log4j *LoggingWrapper.class*;