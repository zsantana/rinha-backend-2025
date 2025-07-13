#!/bin/bash

JAVA_OPTS="
  -XX:+AlwaysPreTouch
  -XX:MaxRAMPercentage=75
  -Dquarkus.vertx.prefer-native-transport=true
  -Dio.netty.buffer.checkBounds=false
  -Dmutiny.disableCallBackDecorators=true
  -Xss512k 
  -XX:MetaspaceSize=32M 
  -XX:MaxMetaspaceSize=64M 
  -XX:+ExitOnOutOfMemoryError
"

java $JAVA_OPTS -jar target/quarkus-app/quarkus-run.jar
