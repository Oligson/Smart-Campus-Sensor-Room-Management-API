# Smart Campus Sensor & Room Management API
**5COSC022W Client-Server Architectures — Coursework 2025/26** 
**Student Name — Oligson Fernandes** 
**StudentID — W2069935** 
**University of Westminster**

---

## Overview

For this coursework I was asked to build a RESTful API for a "Smart Campus" system that manages Rooms and Sensors across a university campus. The idea is that facilities managers can use the API to keep track of which sensors are in which rooms and monitor their readings over time.

I built the API using JAX-RS with Jersey as the implementation and deployed it on Apache Tomcat. All the data is stored in memory using HashMaps — no database was used as per the coursework requirements.

The API lets you:
- Create and manage rooms on campus
- Register sensors and link them to rooms
- Record sensor readings over time
- Get filtered lists of sensors by type
- Handle errors properly with meaningful JSON responses instead of raw error pages

The base URL for everything is `/api/v1` and the API returns JSON for all responses including errors.

### Project Structure

```
com.smartcampus
    config
        ApplicationConfig.java
    data
        DataStore.java
    model
        Room.java
        Sensor.java
        SensorReading.java
        ApiError.java
    resource
        DiscoveryResource.java
        RoomResource.java
        SensorResource.java
        SensorReadingResource.java
    exception
        RoomNotFoundException.java
        RoomNotEmptyException.java
        SensorNotFoundException.java
        LinkedResourceNotFoundException.java
        SensorUnavailableException.java
        mapper
            RoomNotFoundExceptionMapper.java
            RoomNotEmptyExceptionMapper.java
            SensorNotFoundExceptionMapper.java
            LinkedResourceNotFoundExceptionMapper.java
            SensorUnavailableExceptionMapper.java
            GlobalExceptionMapper.java
    filter
        LoggingFilter.java
```

---

## How to Build and Run the Project

### What You Need First

- Java JDK 17 — download from adoptium.net
- Apache Maven — download from maven.apache.org
- Apache Tomcat 10 or above — download from tomcat.apache.org
- NetBeans 18 (this is what I used to build it)

### Step 1 — Clone the repo

```bash
git clone https://github.com/Oligson/-Smart-Campus-Sensor-Room-Management-API.git
```

Then open the folder in NetBeans by going to File → Open Project and selecting the folder you just cloned.

### Step 2 — Build the project

Right-click the project in NetBeans and click **Clean and Build**.

Or if you prefer the terminal:

```bash
cd -Smart-Campus-Sensor-Room-Management-API
mvn clean package
```

You should see BUILD SUCCESS at the end.

### Step 3 — Run it

Right-click the project in NetBeans and click **Run**. NetBeans will deploy it to Tomcat automatically.

If you want to deploy manually, copy the WAR file from the `target/` folder into your Tomcat `webapps/` folder and start Tomcat:

```bash
copy target\smart-campus-api.war C:\path\to\tomcat\webapps\
C:\path\to\tomcat\bin\startup.bat
```

### Step 4 — Check it's working

Open your browser and go to:

```
http://localhost:8080/smart-campus-api/api/v1
```

You should see a JSON response like this:

```json
{
  "api": "Smart Campus Sensor & Room Management API",
  "version": "1.0.0",
  "status": "operational",
  "contact": "admin@smartcampus.ac.uk",
  "_links": {
    "self": "/api/v1",
    "rooms": "/api/v1/rooms",
    "sensors": "/api/v1/sensors"
  }
}
```

If you see that, everything is working.

---

## Sample curl Commands

Here are some example requests you can run to test the API. I tested all of these while building the project.

### 1. Get the discovery endpoint
```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1
```

### 2. Get all rooms
```bash
curl -X GET http://localhost:8080/smart-campus-api/api/v1/rooms
```

### 3. Create a new room
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d "{\"id\":\"CS-101\",\"name\":\"CS Lecture Hall\",\"capacity\":120}"
```

### 4. Register a new sensor linked to that room
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d "{\"id\":\"CO2-007\",\"type\":\"CO2\",\"status\":\"ACTIVE\",\"currentValue\":0,\"roomId\":\"CS-101\"}"
```

### 5. Get only CO2 sensors using the type filter
```bash
curl -X GET "http://localhost:8080/smart-campus-api/api/v1/sensors?type=CO2"
```

### 6. Post a reading to a sensor
```bash
curl -X POST http://localhost:8080/smart-campus-api/api/v1/sensors/CO2-007/readings \
  -H "Content-Type: application/json" \
  -d "{\"value\":523.4}"
```

### 7. Try to delete a room that still has sensors (should return 409 error)
```bash
curl -X DELETE http://localhost:8080/smart-campus-api/api/v1/rooms/CS-101
```

---

## Report — Answers to Coursework Questions

---

### Part 1 — Service Architecture & Setup

**Q: Explain the default lifecycle of a JAX-RS Resource class and how this impacts the way you manage in-memory data structures.**

JAX-RS by default creates a brand new instance of each resource class every single time a request comes in. So for example if someone calls GET /rooms, JAX-RS creates a new RoomResource object just for that request, uses it to handle the response, then throws it away. This is called request scoped lifecycle.

The problem this causes is that you cannot store data inside the resource class itself, because any data you save in an instance variable will be gone when the next request creates a fresh instance. To get around this I created a DataStore class that uses the Singleton pattern, there is only ever one instance of it no matter how many requests come in, and all the resource classes share that same instance.

I also had to think about thread safety because multiple requests can come in at the same time on different threads. If two requests both try to write to a normal HashMap at the same time it could corrupt the data. To fix this I used ConcurrentHashMap instead of a regular HashMap, which handles concurrent access safely without me having to write extra synchronisation code.

---

**Q: Why is HATEOAS beneficial for client developers compared to static documentation?**

HATEOAS basically means the API tells you where to go next from within the response itself, rather than making you go read a separate documentation page. So when a client calls GET /api/v1, the response includes links to /api/v1/rooms and /api/v1/sensors so the client immediately knows where the main resources are without having to guess or look anything up.

I think the main benefit compared to static docs is that the API becomes kind of self-explanatory. If the server ever changes a URL, clients that follow the links in responses will still work correctly, whereas clients that have the URLs hardcoded from static documentation would break. It also means a developer who has never used the API before can start at the root endpoint and just follow the links to explore everything, which makes it much easier to get started with.

---

### Part 2 — Room Management

**Q: What are the implications of returning only IDs versus returning full room objects when listing rooms?**

The advantage of this is the client gets everything they need in one request and doesn't have to make follow up calls. The downside is if there were thousands of rooms the response could get very large and slow.

If I only returned IDs, the response would be tiny and fast, but then the client would need to call GET /rooms/{id} separately for every single room they wanted details on. If there were 100 rooms that would be 101 requests total which would be much worse overall.

In a real system you would probably return a summarised version with just the key fields like id and name, and include a link to the full room resource. But for this coursework since the Room model is fairly small I just returned the full objects to keep things simple for whoever is using the API.

---

**Q: Is the DELETE operation idempotent in your implementation?**

In my implementation, the first time you DELETE /rooms/{roomId} it works and returns 204 No Content. If you call it again with the same ID it returns 404 Not Found because the room is already gone.

Strictly speaking the response codes are different so some people would say it's not truly idempotent. But the way I see it, the actual state of the server is the same after both calls, the room doesn't exist either way. The 404 is just being honest about the current state rather than pretending the deletion happened again when there was nothing to delete.

I think this is actually the more correct behaviour because it gives the client accurate information. Most REST APIs I looked at when researching this take the same approach.

---

### Part 3 — Sensor Operations & Filtering

**Q: What happens if a client sends data in the wrong format to the @Consumes(APPLICATION_JSON) endpoint?**

The @Consumes annotation basically tells JAX-RS that this method will only accept requests where the Content-Type header says application/json. If a client sends the request with Content-Type: text/plain or Content-Type: application/xml instead, JAX-RS will automatically reject it before my method code even runs and send back a 415 Unsupported Media Type response.

I didn't have to write any code to handle this myself which I thought was quite useful. It means my method never has to deal with data it can't understand, and the client gets a clear error message telling them what content type to use instead.

---

**Q: Why is using @QueryParam better than putting the type in the URL path for filtering?**

The path of a URL should identify which resource you're talking about, while query parameters are for adjusting or filtering what you get back. So /sensors is the sensors collection and /sensors/TEMP-001 is one specific sensor. The resource hasn't changed, you're still looking at the sensors collection, just with a filter applied.

If I put the type in the path like /sensors/type/CO2 it starts to look like CO2 is itself a resource or that type is a sub-collection, which doesn't really make sense. It would also make it harder to combine multiple filters, with query params you can just do ?type=CO2&status=ACTIVE, but with path segments you'd need a completely separate route for every combination which would get very messy very quickly.

---

### Part 4 — Sub-Resources

**Q: Discuss the architectural benefits of the Sub-Resource Locator pattern.**

Before I understood this pattern I was planning to just put the readings endpoints inside SensorResource alongside all the other sensor methods. But that would have made SensorResource quite long and hard to read with methods for managing sensors and methods for managing readings all mixed together.

The sub-resource locator pattern lets me keep SensorResource focused only on sensor CRUD and move all the readings logic into its own separate class called SensorReadingResource. SensorResource just has one method that says if the request is for readings, hand it off to SensorReadingResource — which is a much cleaner separation.

The other benefit I found is that the locator method is a good place to do the initial validation. Before handing off to SensorReadingResource, I check that the sensor ID actually exists. This means I don't have to repeat that check in every single readings method, it happens once at the gateway.

---

### Part 5 — Error Handling & Logging

**Q: Why is HTTP 422 more semantically accurate than 404 when a referenced resource is missing from the request body?**

A 404 means I couldn't find what you were looking for which normally refers to the endpoint URL itself. But in this case the URL /api/v1/sensors is perfectly valid and the server found it fine. The problem is inside the request body, the roomId field points to a room that doesn't exist.

So returning 404 would be misleading because the client might think they called the wrong URL when actually their URL was right. HTTP 422 Unprocessable Entity is specifically designed for situations where the request was received and understood correctly but the server can't process it because something in the content is logically wrong. That fits much better here because the JSON was valid, the endpoint exists, but the value of one field refers to something that isn't there.

---

**Q: What are the cybersecurity risks of exposing Java stack traces to API consumers?**

A stack trace shows the full path through your code including package names, class names, and line numbers. Someone trying to attack your API could use that to figure out exactly what libraries and frameworks you're using and then look up known security vulnerabilities for those specific versions.

Stack traces can also show file paths on your server which could help an attacker understand how your server is set up. If a database error gets exposed it might show table or column names which could help with a SQL injection attack. Basically a stack trace is like giving an attacker a map of your application internals.

My GlobalExceptionMapper catches any error that isn't handled by the specific mappers and returns a generic message to the client instead. The full error details get logged on the server side where only I can see them, so I still have the information I need to debug problems without exposing anything to the outside world.

---

**Q: Why is it better to use JAX-RS filters for logging instead of putting Logger.info() calls in every method?**

The honest answer is it would be really tedious and error prone to add logging to every single resource method manually. If I had 20 endpoints I'd have to write the same logging code 20 times, and if I wanted to change the format I'd have to find and update all 20 places.

With a JAX-RS filter I write the logging code once and it automatically runs for every single request and response without me having to do anything in the individual resource classes. It's registered using the @Provider annotation and JAX-RS takes care of the rest.

The other thing I noticed is that a filter runs even for requests that get rejected before reaching a resource method, like if someone sends the wrong content type and gets a 415 back. A Logger.info() inside my method would never even run in that case, so I'd have gaps in my logs. The filter catches everything regardless of what happens.

