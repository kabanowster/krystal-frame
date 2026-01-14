# Krystal Frame

*click for video summary:*

[![KrystalFrame - A Direct Approach](https://img.youtube.com/vi/3QmOlU5Q4_A/0.jpg)](https://youtu.be/3QmOlU5Q4_A)

**Krystal Frame** is a comprehensive Java framework designed to streamline application development through high-level **persistence**, programmatic **query abstraction**, and efficient **flow management**. It bridges the gap between raw database operations and business logic, offering a suite of tools for rapid, type-safe development.

![Krystal Frame Infographic](/static/infographic1.png)

The **Persistence API** automates CRUD operations for objects implementing the `Entity` interface. It utilizes a streamlined annotation system, including `@Table`, `@Column`, and `@Key`, to map Java fields directly to database structures.

Beyond standard relational mapping, the framework supports **Vertical Persistence** models. This allows for flexible data structures using `@Vertical.PivotColumn` and `@Vertical.ValuesColumn` to handle attribute-value pairs within a single table. To ensure high performance, the `PersistenceMemory` layer caches loaded objects, providing rapid access and reducing redundant database queries.

**Krystal Frame** also features a robust **Query Factory** for building SQL statements programmatically, ensuring safety and readability without manual string concatenation. With integrated support for embedded **Tomcat** servers and non-blocking `KrystalServlet` implementations, it provides a complete ecosystem for building scalable web services and enterprise applications.

**Krystal Frame** includes a robust command system powered by the `CommanderInterface`, which supports advanced argument parsing for quoted or bracketed values. You can interact with your application via the `NativeConsoleReader` using standard System.in, or deploy a built-in Swing console through `KrystalFramework.startConsole()` for an integrated UI-based command and logging experience.

For more complex automation, the framework allows reading c ommands directly from text files via `readCommandsFromTextFile()`, which can be scheduled within your application flow. Additionally, `BaseCommander` provides a ready-to-use implementation for handling standard framework commands out of the box.

# Tutorial
***Related files are under `/tutorial` folder. See `/docs` for Javadoc.***

![Krystal Frame Tutorial Case Infographic](/static/infographic2.png)

### Building an API Server with the Krystal Frame

##### Understanding the apiserver architecture
 
We will deconstruct the application's core layers, starting from the application entry point (Main), moving through its data source providers (Providers) and data model (Entities), and finally examining how data is exposed through API endpoints (Servlets). Before diving into the application's code, it is essential to first understand the foundational framework upon which it is built.

##### 1. Application Entry Point: Deconstructing Main.java

Let's analyze the main method step by step to see how the application comes to life.

* **Database Provider Setup** The first step is to define the available database connections. The application registers both a primary and secondary provider and designates the primary one as the default for any operations that do not specify a provider. [More in the following section.](#2-defining-data-sources-the-providersjava-enum)
* **Connection Pooling** To manage database connections efficiently, the application configures a Hikari connection pool. This block sets crucial parameters that are vital for application performance and stability. `setMaxLifetime`, for instance, prevents connections from becoming stale when network hardware like firewalls silently drops long-lived connections, while `setIdleTimeout` ensures that unused connections are reclaimed to conserve resources. See official Hikari config reference for details.
* **In-Memory Caching** Next, the persistence memory cache is configured. This component stores frequently accessed database objects in memory to reduce database load and improve response times. `setDefaultMonitorInterval` determines how often the cache performs maintenance, while `setDefaultIntervalsCount` defines how many of these intervals must pass before a full maintenance cycle is completed.
* **Framework Initialization** This line is where the Krystal Frame officially boots up. It initializes the Spring application context, scans for components, and makes the framework's core services available for use. The later usage of the Spring is optional. Krystal Frame utilizes only its core module. Launch templates are prefixed with `frame(...)` and are combination of `primaryInitialization` and `start(...)` methods. 
* **Web Server Launch** With the backend configured, the embedded **Tomcat** web server is started. The `TomcatProperties` builder is used to define server settings, like a global connection timeout, and most importantly, register the servlets defined in the `Servlets` class. These servlets will handle incoming HTTP requests.
* **Keeping the Application Alive** Finally, this line prevents the application's main thread from exiting immediately after startup. `Flows.core.await(0)` effectively parks the thread, ensuring the web server and other background processes continue running indefinitely.

Since the database providers were the first components to be configured, it is logical to examine their definition next.

##### 2. Defining Data Sources: The Providers.java Enum

A key architectural pattern for building scalable applications is the abstraction of data sources. Instead of hard-coding connection details throughout the codebase, the *apiserver* centralizes them into "Providers." This design allows the application to easily manage multiple database connections and even switch between different database systems (e.g., SQL Server and PostgreSQL) with minimal code changes.  

The Providers.java enum is a clean and effective implementation of this pattern.

```java
@Getter  
public enum Providers implements ProviderInterface {  
    primary(DBCDrivers.jdbcSQLServer),  
    secondary(DBCDrivers.jdbcPostgresql);

    private final DBCDriverInterface driver;  
      
    Providers(DBCDriverInterface driver) {  
        this.driver = driver;  
    }  
}
```

* It implements the ProviderInterface from the Krystal framework, making it compatible with Krystal's data access layer.
* It defines two distinct data sources:
  * primary: Configured to use the jdbcSQLServer driver.
  * secondary: Configured to use the jdbcPostgresql driver.
* Using a Java enum to implement ProviderInterface provides compile-time safety and a clean, self-documenting way to manage a fixed set of data sources, preventing runtime errors that can arise from typos in string-based identifiers.
* This simple enum makes the application multi-database capable, a powerful feature that is leveraged later in the custom servlet implementation.With the data sources defined, the next logical step is to explore the data model these providers will interact with—the Entities.

*Tip: Usage of enums in the framework is optional, but it works really well with provided abstractions.*

The settings for each provider are coming out from `provider_name.properties` files, and directly correspond to props used by JDBC drivers. These files would be stored in *exposed directory* by default which is either at the root level of jar or within app's `resources`. You can overwrite these paths with methods in the example.

##### 3. The Data Model: A Deep Dive into Entities

Entities are the cornerstone of the application's data model. They are Java classes that directly map to database tables, serving as the critical bridge between the application's object-oriented logic and the underlying relational database. The Krystal Frame uses a combination of a marker interface and annotations to manage the persistence of these objects.

###### *3.1. The Entity Interface and Core Annotations*

At its most basic, any class that needs to be persisted by Krystal Frame must implement the `Entity` interface and provide no-arguments constructor. This signals to the framework that the class is part of the managed data model. Behavior is further refined using a set of powerful annotations. **Lombok** comes handy in this as well. 

*\* = mandatory*

| Annotation   | Purpose                                                                                                                                                                                                                     |
|--------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| @Table       | * Specifies the database table the entity maps to (e.g., `@Table("statistics.options")`). For a more sophisticated table definition (i.e. using `TableInterface` - @Overwrite the interface's `getTable()` method directly. |
| @Key         | Marks a field as part of the primary key for database ***writing*** operations.                                                                                                                                             |
| @Incremental | Indicates that a field is an auto-incrementing column in the database.                                                                                                                                                      |
| @Reader      | * Marks a constructor to be used when creating an object from database results, mapping database types (`java.sql`) with class.                                                                                             |
| @Memorized   | Instructs `PersistenceMemory` to cache instances of this entity ***infinetely*** for faster access.                                                                                                                         |
| @Fresh       | The opposite of `@Memorized`; instructs the framework to always fetch this entity from the database.                                                                                                                        |
| @Flattison   | Provides seamless and recursive support for JSON serialization and deserailization.                                                                                                                                         |

*Tip1: For ***any*** class to be just read from database (like with `Persistence.promiseAll()`) - only `Reader` must it declare and even does not have to implement `Entity`. Although, it won't be processed by `PersistenceMemory` or allow writing to db.* 

*Tip2: `Entities` are stored in `PersistenceMemory` by default and for set amount of time (refreshed upon use).*

*Tip3: There can be many `@Reader` constructors, for different fetch results based on custom condition logic - returning different filters, tables, providers, etc. All in a single class.*

###### *3.2. Example 1: A Simple Entity (Option.java)*

The `Option.class` is a perfect example of a basic entity. It represents a simple lookup table with an ID and a name.

```java
@Memorized  
@Flattison
@Table("statistics.options")  
@AllArgsConstructor(onConstructor_ = {@Reader})  
@NoArgsConstructor  
@Getter  
public class Option implements Entity {  
  private @Incremental @Key Integer id;  
  private String name;

  public Option(int id) {  
      this.id = id;  
      load();  
  }  
}
```

Its structure is straightforward:

* `@Memorized`: Instances of Option will be cached in memory for high performance.
* `@Table("statistics.options")`: Maps this class to the `statistics.options` table.
* `@Incremental` and `@Key`: Define the id field as an auto-incrementing primary key.
* `@AllArgsConstructor(onConstructor_ = {@Reader})`: This is a clever integration with Lombok. It generates a constructor that accepts all fields (id, name) and simultaneously annotates that constructor with @Reader. This tells Krystal Frame to use this constructor when hydrating Option objects from a database query result.
* The `public Option(int id)` constructor provides a convenient way to fetch an Option from the database by its ID, using the load() method inherited from the Entity interface.

###### *3.3. Example 2: A Complex Entity with Custom Logic (Stat.java)*

The `Stat.class` demonstrates a more advanced use case. It manages a many-to-many relationship with the Option entity, which requires custom logic to handle the linking table (StatsOption). Krystal Frame facilitates this through methods annotated with @Reader, @Writer, and @Remover.

* `@Reader public void readers()`  This method is invoked by the framework immediately  *after*  a Stat object has been loaded from the database. Its purpose is to resolve the many-to-many relationship by fetching all associated `Option` objects. It does this by querying the StatsOption linking table for records matching the current Stat `id` and then populating the `options` collection.
  * `@Writer private void writers()`  This method is called  *before*  a `Stat` object is saved or updated. It contains crucial logic to synchronize the state of the `options` collection with the database. The method intelligently compares the current list of options with what's already in the `STATS_OPTIONS` table, deleting links that are no longer needed and creating new ones for any added options. This ensures data integrity.
* `@Remover private void removers()`  Invoked when a `Stat` object is deleted, this method performs a cascade delete. It explicitly queries for and deletes all corresponding records in the `StatsOption` linking table, preventing orphaned records and maintaining a clean databas``e state.

###### *3.4. Example 3: The Linking Entity (StatsOption.java)*

`StatsOption.class` is the "join" or "linking" entity that enables the many-to-many relationship between `Stat` and `Option`. It directly maps to the `statistics.stats_options` table.A key feature here is the `@Fresh` annotation. Because this entity represents a transactional link rather than a stable piece of data, caching it could lead to stale information. By marking it as `@Fresh`, the application ensures it always fetches the current state of relationships directly from the database. Furthermore, this entity demonstrates how to handle potential SQL keyword conflicts: 

```java
@ColumnsMapping  
@Getter  
public enum Columns implements ColumnInterface {  
  stat("stat"),  
  option("[option]");  
  // ...  
}
```

The `@ColumnsMapping` annotation, combined with the nested Columns enum, provides an explicit mapping for the `option` field. It tells Krystal Frame to refer to this field as "[option]" in generated SQL queries. This annotation is a crucial escape hatch, allowing the ORM to coexist peacefully with legacy database schemas or naming conventions that might conflict with language keywords like `option`. 

*Tip: `@Column` annotations can be used to create mappings instead. Declaring columns enum (`ColumnInterface`) can be handy as a part of creating other queries, and in this case they simultaneously serve as mapping.*

With a clear understanding of the data model, we can now explore the final component: how this model is exposed to the outside world through the API layer.

##### 4. The API Layer: Exposing Data with Servlets

Servlets are the public-facing entry point to the application's logic. In the context of an API server, they are responsible for receiving HTTP requests, interacting with the data model to perform business logic, and sending back formatted responses (typically JSON). The Krystal Frame provides two powerful and distinct strategies for creating servlets, both of which are effectively utilized in the `apiserver` application.

###### *4.1. Approach 1: The Auto-Generated Persistence Servlet*

For standard CRUD operations, Krystal provides a highly efficient factory method, `KrystalServlet.getPersistenceServlet(...)`, that can generate a complete set of API endpoints with a single line of code. This method automatically creates a servlet that exposes `GET`, `POST`, and `DELETE` endpoints for every entity defined in its configuration, dramatically accelerating development for standard data management tasks. In our application, this call has been refactored into a reusable helper method, `defaultServlet`, which is a good architectural practice.

```java
public KrystalServlet statisticsServlet() {  
  return defaultServlet("Statistics", "/statistics", StatMappings.values());  
}

public KrystalServlet defaultServlet(String name, String context, PersistenceMappingInterface mappings) {  
  return KrystalServlet.getPersistenceServlet(CONTEXT_BASE + context, name, Stream.of(mappings).collect(Collectors.toSet()), "*");  
}
```

The configuration itself is supplied by the `StatMappings` enum:
```java
@Getter  
public enum StatMappings implements PersistenceMappingInterface {  
  stat(Stat.class),  
  option(Option.class),  
  statsOption(StatsOption.class);

  private final Class<? extends Entity> persistenceClass;  
    
  StatMappings(Class<? extends Entity> persistenceClass) {  
      this.persistenceClass = persistenceClass;  
  }  
}
```

This enum implements `PersistenceMappingInterface` and acts as a routing map. It tells the auto-generated servlet that requests to the `/api/v1/statistics/stat` path should operate on the `Stat` entity, `/api/v1/statistics/option` on the `Option` entity, and so on.

###### *4.2. Approach 2: The Custom-Built Servlet*

Not all API endpoints fit the simple CRUD pattern. For operations that require complex business logic, direct database calls, or fine-grained control over the HTTP response, Krystal Frame provides a flexible servlet builder. The `customCommentsServlet()` is a perfect illustration of this, constructed using `KrystalServlet.builder()`.

* **Explicit Method Handling**  Logic is attached to specific HTTP methods using handlers like `.servePost(...)` and `.serveDelete(...)`. This provides complete, granular control over what happens for each type of request.
* **Manual CORS Headers**  The servlet manually defines and sets Cross-Origin Resource Sharing (CORS) headers, which is essential for enabling access from web applications hosted on different domains. This demonstrates full control over the HTTP response.
* **Direct SQL Execution**  Instead of relying on the ORM, the servlet uses the `Q.q(...)` utility to execute raw SQL. In this case, it calls stored procedures, a common requirement for performance-critical operations or integration with existing database logic.
* **Multi-Provider Interaction**  The servlet strategically uses different data sources for different tasks. It sends write operations (new comments) to the secondary PostgreSQL database and delete operations to the primary SQL Server database, showcasing a practical application of the multi-provider architecture defined in `Providers`.This dual-approach to servlet creation—combining rapid, convention-based generation with the flexibility of a custom builder—is a significant strength of the framework.

##### 5. Conclusion: Key Architectural Takeaways

The `apiserver` application, though concise, serves as an excellent case study in modern API server design using the Krystal Frame framework. Its architecture is built on a set of sound software engineering principles that promote maintainability, scalability, and developer productivity.The most important architectural principles demonstrated are:

1. **Configuration-Driven Initialization:**  The application's lifecycle begins in `Main.java` with a clear, declarative setup. Database providers, connection pools, caching, and the web server are all configured and launched in a predictable sequence, making the system easy to understand and debug.
2. **Clear Separation of Concerns:**  The architecture is cleanly partitioned into a data model layer (Entities), a data access layer (Providers), and an API presentation layer (Servlets). This separation is a hallmark of maintainable software, as it allows each component to evolve independently.
3. **Hybrid Persistence Strategy:**  The application masterfully blends persistence techniques. It uses Krystal's high-level ORM features, including custom `@Reader`/`@Writer` methods, for managing complex object relationships, while seamlessly dropping down to direct SQL execution (`Q.q`) for specific, high-performance tasks like calling stored procedures.
4. **Leveraging Framework Strengths:**  The architecture demonstrates a "best of both worlds" philosophy. It leverages the framework's high-productivity features (`getPersistenceServlet`) for standard CRUD endpoints, which accelerates development, while retaining the full flexibility to build completely custom, logic-heavy endpoints (`KrystalServlet.builder`) when business requirements demand it.