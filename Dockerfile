FROM openjdk:17-alpine

ADD build/libs/somfymqtt-0.0.1-SNAPSHOT.jar /srv/somfymqtt.jar

ENTRYPOINT ["java","-jar","/srv/somfymqtt.jar"]