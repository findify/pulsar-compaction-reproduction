### Pulsar Compaction Issue



#### Build

App was build using:
* jdk11
* sbt: 1.3.6
* scala: 2.13.6

Build app.jar:
`sbt assembly`

Build deps jar:
`sbt assemblyPackageDependency`


#### Metrics 

Key metrics are exposed via `localhost:8080/metrics`

![Metrics](rsc/metrics.png)