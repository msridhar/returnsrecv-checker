Releasing
=========

NOTE: when creating a release here, also create a release for the object construction checker.

Steps:
 1. Change the version in `gradle.properties` to a non-SNAPSHOT version.
 2. `git commit -am "Prepare for release X.Y.Z."` (where X.Y.Z is the new version)
 3. `git tag -a vX.Y.Z -m "Version X.Y.Z"` (where X.Y.Z is the new version)
 4. `./gradlew clean uploadArchives`
 5. Update the `gradle.properties` to the next SNAPSHOT version.
 6. `git commit -am "Prepare next development version."`
 7. `git push && git push --tags`
 8. Visit [Sonatype Nexus](https://oss.sonatype.org/) and promote the artifact.
