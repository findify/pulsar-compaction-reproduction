import sbt.addCompilerPlugin
lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .enablePlugins(DockerPlugin)
  .settings(
    name := "pulsar-compaction",
    organization := "io.findify",
    version := "0.1",
    updateOptions := updateOptions.value.withCachedResolution(false),
    libraryDependencies ++= Seq(
      "com.github.mpilquist"         %% "simulacrum"           % "0.19.0",
      "org.typelevel"                %% "cats-core"            % "2.0.0",
      "org.typelevel"                %% "cats-effect"          % "2.0.0",
      "org.typelevel"                %% "cats-mtl-core"        % "0.7.0",
      "dev.profunktor"               %% "console4cats"         % "0.8.1",
      "io.circe"                     %% "circe-core"           % "0.13.+",
      "io.circe"                     %% "circe-generic"        % "0.13.+",
      "io.circe"                     %% "circe-parser"         % "0.13.+",
      "io.circe"                     %% "circe-generic-extras" % "0.13.+",
      "com.softwaremill.sttp.client" %% "core"                 % "2.0.7",
      "com.softwaremill.sttp.client" %% "circe"                % "2.0.7",
      "io.monix"                     %% "monix-execution"      % "3.0.0",
      "io.monix"                     %% "monix"                % "3.0.0",
      "com.typesafe.akka"            %% "akka-http"            % "10.1.11",
      "com.typesafe.akka"            %% "akka-stream"          % "2.5.26",
      "io.skuber"                    %% "skuber"               % "2.4.0",
      "com.github.pureconfig"        %% "pureconfig"           % "0.12.0",
      "org.scalaj"                   %% "scalaj-http"          % "2.4.2",
      "software.amazon.awssdk"       % "costexplorer"          % "2.13.23",
      "com.evolutiongaming"          %% "prometheus-exporter"  % "2.0.0",
      "org.postgresql"               % "postgresql"            % "42.2.5",
      "org.tpolecat"                 %% "doobie-core"          % "0.9.0",
      "org.tpolecat"                 %% "doobie-scalatest"     % "0.9.0" % Test,
      "org.tpolecat"                 %% "doobie-hikari"        % "0.9.0",
      "org.tpolecat"                 %% "doobie-postgres"      % "0.9.0",
      "org.apache.pulsar"            % "pulsar-client"         % "2.8.0",
      "org.scalactic"                %% "scalactic"            % "3.0.8" % Test,
      "org.scalatest"                %% "scalatest"            % "3.0.+" % Test
    ),
    scalaVersion := "2.13.1",
    scalacOptions ++= List(
      "-unchecked",
      "-deprecation",
      "-Ymacro-annotations",
      "-language:higherKinds",
      "-language:implicitConversions"
    ),
    addCompilerPlugin("org.typelevel" % "kind-projector_2.13.1" % "0.11.0"),
    addCompilerPlugin("com.olegpy"    %% "better-monadic-for"   % "0.3.1"),
    assemblyJarName in assemblyPackageDependency := "deps.jar",
    assemblyJarName in assembly := "app.jar",
    assemblyOption in assembly := (assemblyOption in assembly).value
      .copy(includeScala = false, includeDependency = false),
    assemblyMergeStrategy in assembly := {
      case x if x.endsWith("io.netty.versions.properties")                      => MergeStrategy.discard
      case x if x.endsWith("logback.xml")                                       => MergeStrategy.last
      case x if x.endsWith("module-info.class")                                 => MergeStrategy.discard
      case x if x.endsWith(".proto")                                            => MergeStrategy.discard
      case x if x.endsWith("io.netty.versions.properties")                      => MergeStrategy.discard
      case x if x.endsWith("logback.xml")                                       => MergeStrategy.last
      case x if x.endsWith("findbugsExclude.xml")                               => MergeStrategy.last
      case x if x.endsWith("module-info.class")                                 => MergeStrategy.discard
      case x if x.endsWith("public-suffix-list.txt")                            => MergeStrategy.discard
      case x if x.endsWith("native-image.properties")                           => MergeStrategy.discard
      case x if x.endsWith("reflection-config.json")                            => MergeStrategy.discard
      case x if x.endsWith("mime.types")                                        => MergeStrategy.last
      case x if x.endsWith("codegen-resources/customization.config")            => MergeStrategy.last
      case x if x.endsWith("codegen-resources/paginators-1.json")               => MergeStrategy.last
      case x if x.endsWith("codegen-resources/service-2.json")                  => MergeStrategy.last
      case x if x.endsWith("codegen-resources/waiters-2.json")                  => MergeStrategy.last
      case x if x.endsWith("org/asynchttpclient/config/ahc-default.properties") => MergeStrategy.last
      case x if x.endsWith("examples-1.json")                                   => MergeStrategy.last
      case x if x.endsWith("application.conf") =>
        MergeStrategy.first // This was added to fix fp conf overridden by maestro
      // a fix for a 2.12.13 regression: https://github.com/scala/bug/issues/12315
      case x if x.endsWith("nowarn.class")  => MergeStrategy.last
      case x if x.endsWith("nowarn$.class") => MergeStrategy.last
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    }
  )
