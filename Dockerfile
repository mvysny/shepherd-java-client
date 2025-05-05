# Packages Shepherd-Java as a docker container.
# Requires the docker buildkit/buildx extension.
#
# This image:
# 1. Starts shepherd-java web service which listens on 8080
# 2. Provides shepherd-cli script which you can use via `docker exec`.
#
## Usage ##
#
# Install Shepherd-Traefik as per instructions at https://github.com/mvysny/shepherd-traefik .
# Once everything is configured, Shepherd-Web starts as part of running `docker-compose up -d`.
#
# To run shepherd-cli commands, exec commands via docker:
# $ docker exec -ti -w /opt/shepherd-cli/bin int_shepherd ./shepherd-cli stats
#
## Building ##
#
# Build the image with:
# $ docker build -t mvysny/shepherd-java:latest .

# The "builder" stage. Copies the entire project into the container, into the /app/ folder, and builds it.
FROM --platform=$BUILDPLATFORM openjdk:21-bookworm AS builder
COPY . /app/
WORKDIR /app/
RUN --mount=type=cache,target=/root/.gradle --mount=type=cache,target=/root/.vaadin ./gradlew clean build -Pvaadin.productionMode --no-daemon --info --stacktrace
WORKDIR /app/shepherd-web/build/distributions/
RUN tar xvf shepherd-web-*.tar && rm shepherd-web-*.tar shepherd-web-*.zip
WORKDIR /app/shepherd-cli/build/distributions/
RUN tar xvf shepherd-cli-*.tar && rm shepherd-cli-*.tar shepherd-cli-*.zip
# At this point, we have:
# 1. the webapp (executable bash scrip plus a bunch of jars) in the /app/shepherd-web/build/distributions/shepherd-web-VERSION/ folder.
# 2. the cli (executable bash scrip plus a bunch of jars) in the /app/shepherd-cli/build/distributions/shepherd-cli-VERSION/ folder.

# The "Run" stage. Start with a clean image, and copy over just the app itself, omitting gradle, npm and any intermediate build files.
FROM openjdk:21-bookworm
COPY --from=builder /app/shepherd-web/build/distributions/shepherd-web-* /opt/shepherd-web/
COPY --from=builder /app/shepherd-cli/build/distributions/shepherd-cli-* /opt/shepherd-cli/
WORKDIR /opt/shepherd-web/bin
EXPOSE 8080
CMD ["./shepherd-web"]

