FROM openjdk:8-jre
COPY jar_app /jar_app
EXPOSE 8080 9443
CMD /jar_app/bin/start -Dhttps.port=9443 -Dplay.crypto.secret=secret