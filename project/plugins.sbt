// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.13")

// For removing warts, maintain code quality
addSbtPlugin("org.wartremover" %% "sbt-wartremover" % "2.4.17")

// The sbt native packager - needed for docker builds
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.7")

// For code coverage test
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.9.3")

// For checkstyle
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

// For formatting Scala source code
addSbtPlugin("com.lucidchart" % "sbt-scalafmt" % "1.16")

// Build fat JAR file
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "1.1.1")

// Check vulnerabilities in JAR's
addSbtPlugin("net.vonbuchholtz" % "sbt-dependency-check" % "3.4.0")
