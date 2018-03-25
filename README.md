# statistics-heartbeat

Calculates​ ​real time​ ​statistic​s ​from​ ​the​ ​last​ ​60​ ​seconds (configurable)

## API
### Adding a record
Endpoint
```
POST​ ​/transactions
```
Data
```json
{
  ​​​​​​​​​​​​​"amount":​ ​12.3,
  ​"timestamp":​ ​1478192204000
}
```

Example:
```bash
http :8080/transactions amount:=5 timestamp:=1521956629750
HTTP/1.1 201 Created
Content-Length: 76
Content-Type: text/plain; charset=UTF-8
Date: Sun, 25 Mar 2018 05:43:57 GMT
Server: akka-http/10.1.0
```

### Getting statistics
Endpoint
```
GET​ ​/statistics
```
Returns
```json
{
  "sum":​ ​1000, ​
  "avg":​ ​100, ​
  "max":​ ​200, ​
  "min":​ ​50, ​
  "count":​ ​10
}
```

Example:
```bash
http :8080/statistics                                      
HTTP/1.1 200 OK
Content-Length: 54
Content-Type: application/json
Date: Sun, 25 Mar 2018 06:03:17 GMT
Server: akka-http/10.1.0

{
  "avg": 30.0,
  "count": 3,
  "max": 50.0,
  "min": 5.0,
  "sum": 90.0
}
```
## Setup and Requirements

To build the project, [SBT](https://github.com/sbt/sbt) should be used. You could inslall it with [Homebrew](https://github.com/Homebrew/brew) (if running MacOS):
```bash
brew install sbt
```
Or use `apt-get` (for Debian-based systems)
```bash
echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823
sudo apt-get update
sudo apt-get install sbt
```

### Building and running the project
##### To run the app (one main class so you won't need to specify any)
```bash
sbt run
```

##### To package a jar
```bash
sbt assembly
```

##### To run the tests:
```bash
sbt testOnly
```