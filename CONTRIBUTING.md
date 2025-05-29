# Developing

Please feel free to open bug reports to discuss new features; PRs are welcome as well :)

# Running tests

To run all tests, simply run `./gradlew test`.

# Releasing

To release the library to Maven Central:

1. Edit `build.gradle.kts` and remove `-SNAPSHOT` in the `version=` stanza
2. Commit with the commit message of simply being the version being released, e.g. "0.2"
3. git tag the commit with the same tag name as the commit message above, e.g. `0.2`
4. `git push`, `git push --tags`
5. Run `./gradlew clean build publish closeAndReleaseStagingRepositories` with JDK 17+
6. (Optional) watch [Maven Central Publishing Deployments](https://central.sonatype.com/publishing/deployments) as the deployment is published.
7. Build and release `shepherd-web` to Docker Hub:
   - `docker build --platform linux/amd64,linux/arm64 -t mvysny/shepherd-java:0.2 .`
   - `docker push mvysny/shepherd-java:0.2`
8. Add the `-SNAPSHOT` back to the `version=` while increasing the version to something which will be released in the future,
   e.g. 0.3, then commit with the commit message "0.3-SNAPSHOT" and push.

