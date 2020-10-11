# co2-monitor

Application for CO2 level measurement gathering from sensors.

- Gather measurements from multiple sensors
- Average CO2 level for the last 30 days 
- Maximum CO2 Level in the last 30 days
- Create alert when CO2 level exceeds

## Technical details

Application works as a single node, there is no cluster awareness for now. 
All sensors state is kept in the memory and will be lost during application crash or restart.
Each sensor has dedicated Akka Actor that takes care of keeping the state, message ordering and deleting old samples.
`SensorCoordinator` is used to forward messages to specific child actors and create new ones if doesn't exist. 

Single application running on a VM with around 1GB memory can store sample data for around 300 sensors (just sample data without alert logs). 
```
Requirements: 
- rate of measurement from a single sensor: 1req/minute
- data must be stored for 30 days to provide avg and max

60 x 24 x 30 -> 43200 number of samples per month for single sensor
(8 bytes (long) + 4 bytes (int) + 8 bytes for object) 20 bytes per sample
20 x 43200 ~ 864 kilobytes for single senor per month, we could round it to 2MB
```

To scale the solution and provide high availability we would need to distribute the system and replicate/persist the state. 
Akka Cluster extension would be a perfect solution for data sharding and even distribution of load across a cluster (actors can be moved between cluster nodes without loosing data). 
Akka Persistence could be used to persist data on disk / some proper storage (e.g nosql database). 
Akka Distributed data could be used to replicate state of the sensor to multiple nodes in cluster.

There is a scheduled job that deletes outdated samples for each sensor and runs every hour.

## Configuration

Server port and CO2 emission threshold can be configured by setting appropariete env variable (`THRESHOLD_LEVEL` and `PORT`) or updating `src/main/resources/sensor.conf` file.

## Build, test and run

`sbt-extras` script is provided in the `bin` directory to simplify building application without installing sbt before.

Compile and test code 
```
./bin/sbt test
```

Build an uberjar
 ```
./bin/sbt assembly
```

Run application
 ```
java -jar target/scala-2.13/co2-monitor-assembly-0.1.0-SNAPSHOT.jar
```

## Web API

Documentation for the Web API is generated by `tapir` library and exposed under `/docs` path of the application. It follows an OpenAPI standard and is in YAML format displayed in Swagger UI.

### Example requests

```
curl -X GET -H 'Content-Type: application/json' localhost:8080/api/v1/sensors/123e4567-e89b-12d3-a456-556642440000       
{"status":"OK"}

curl -X POST -H 'Content-Type: application/json' localhost:8080/api/v1/sensors/123e4567-e89b-12d3-a456-556642440000/measurements -d '{"co2":100,"time":"2020-10-10T18:55:47+00:00"}' 
curl -X POST -H 'Content-Type: application/json' localhost:8080/api/v1/sensors/123e4567-e89b-12d3-a456-556642440000/measurements -d '{"co2":100,"time":"2020-10-10T18:55:47+00:00"}'

curl -X GET -H 'Content-Type: application/json' localhost:8080/api/v1/sensors/123e4567-e89b-12d3-a456-556642440000                                                                  
{"status":"OK"}%

curl -X GET -H 'Content-Type: application/json' localhost:8080/api/v1/sensors/123e4567-e89b-12d3-a456-556642440000/metrics                                                          
{"maxLast30Days":100,"avgLast30Days":100}

curl -X GET -H 'Content-Type: application/json' localhost:8080/api/v1/sensors/123e4567-e89b-12d3-a456-556642440000/alerts 
[]

curl -X POST -H 'Content-Type: application/json' localhost:8080/api/v1/sensors/123e4567-e89b-12d3-a456-556642440000/measurements -d '{"co2":5000,"time":"2020-10-10T18:55:47+00:00"}'
curl -X POST -H 'Content-Type: application/json' localhost:8080/api/v1/sensors/123e4567-e89b-12d3-a456-556642440000/measurements -d '{"co2":5000,"time":"2020-10-10T18:55:47+00:00"}'

curl -X GET -H 'Content-Type: application/json' localhost:8080/api/v1/sensors/123e4567-e89b-12d3-a456-556642440000                                                                  
{"status":"WARN"}

curl -X POST -H 'Content-Type: application/json' localhost:8080/api/v1/sensors/123e4567-e89b-12d3-a456-556642440000/measurements -d '{"co2":5000,"time":"2020-10-10T18:55:47+00:00"}'

curl -X GET -H 'Content-Type: application/json' localhost:8080/api/v1/sensors/123e4567-e89b-12d3-a456-556642440000                                                                   
{"status":"ALERT"}

curl -X GET -H 'Content-Type: application/json' localhost:8080/api/v1/sensors/123e4567-e89b-12d3-a456-556642440000/alerts                                                            
[{"startTime":"2020-10-10T18:55:47Z[UTC]","endTime":null,"measurement1":5000,"measurement2":5000,"measurement3":5000}]

curl -X POST -H 'Content-Type: application/json' localhost:8080/api/v1/sensors/123e4567-e89b-12d3-a456-556642440000/measurements -d '{"co2":1000,"time":"2020-10-10T18:55:47+00:00"}'   
curl -X POST -H 'Content-Type: application/json' localhost:8080/api/v1/sensors/123e4567-e89b-12d3-a456-556642440000/measurements -d '{"co2":1000,"time":"2020-10-10T18:55:47+00:00"}'
curl -X POST -H 'Content-Type: application/json' localhost:8080/api/v1/sensors/123e4567-e89b-12d3-a456-556642440000/measurements -d '{"co2":1000,"time":"2020-10-10T18:55:47+00:00"}'

curl -X GET -H 'Content-Type: application/json' localhost:8080/api/v1/sensors/123e4567-e89b-12d3-a456-556642440000
{"status":"OK"}

curl -X GET -H 'Content-Type: application/json' localhost:8080/api/v1/sensors/123e4567-e89b-12d3-a456-556642440000/alerts                                                            
[{"startTime":"2020-10-10T18:55:47Z[UTC]","endTime":"2020-10-10T18:55:47Z[UTC]","measurement1":5000,"measurement2":5000,"measurement3":5000}] 
```
