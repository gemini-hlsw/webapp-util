import sbt._
import sbt.Keys._
import com.jsuereth.sbtpgp.PgpKeys
import laika.sbt.LaikaPlugin
import laika.sbt.LaikaPlugin.autoImport._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import org.scalajs.jsdependencies.sbtplugin.JSDependenciesPlugin
import org.scalajs.jsdependencies.sbtplugin.JSDependenciesPlugin.autoImport._
import org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbtcrossproject.CrossPlugin.autoImport._
import scalajscrossproject.ScalaJSCrossPlugin.autoImport._

object Build {
  import Dependencies._
  import Lib._

  lazy val genArity = TaskKey[Unit]("genArity")

  private val ghProject = "webapp-util"

  private val publicationSettings =
    Lib.publicationSettings(ghProject)

  def scalacCommonFlags = Seq(
    "-deprecation",
    "-feature",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-language:postfixOps",
    "-unchecked",                                    // Enable additional warnings where generated code depends on assumptions.
    "-Wconf:msg=copyArrayToImmutableIndexedSeq:s",   // utest noise
    "-Wconf:msg=may.not.be.exhaustive:e",            // Make non-exhaustive matches errors instead of warnings
    "-Wconf:msg=Reference.to.uninitialized.value:e", // Make uninitialised value calls errors instead of warnings
  )

  def scalac2Flags = Seq(
    "-target:11",
    "-Wdead-code",                                   // Warn when dead code is identified.
    "-Wunused:explicits",                            // Warn if an explicit parameter is unused.
    "-Wunused:implicits",                            // Warn if an implicit parameter is unused.
    "-Wunused:imports",                              // Warn if an import selector is not referenced.
    "-Wunused:locals",                               // Warn if a local definition is unused.
    "-Wunused:nowarn",                               // Warn if a @nowarn annotation does not suppress any warnings.
    "-Wunused:patvars",                              // Warn if a variable bound in a pattern is unused.
    "-Wunused:privates",                             // Warn if a private member is unused.
    "-Xlint:adapted-args",                           // An argument list was modified to match the receiver.
    "-Xlint:constant",                               // Evaluation of a constant arithmetic expression resulted in an error.
    "-Xlint:delayedinit-select",                     // Selecting member of DelayedInit.
    "-Xlint:deprecation",                            // Enable -deprecation and also check @deprecated annotations.
    "-Xlint:eta-zero",                               // Usage `f` of parameterless `def f()` resulted in eta-expansion, not empty application `f()`.
    "-Xlint:implicit-not-found",                     // Check @implicitNotFound and @implicitAmbiguous messages.
    "-Xlint:inaccessible",                           // Warn about inaccessible types in method signatures.
    "-Xlint:infer-any",                              // A type argument was inferred as Any.
    "-Xlint:missing-interpolator",                   // A string literal appears to be missing an interpolator id.
    "-Xlint:nonlocal-return",                        // A return statement used an exception for flow control.
    "-Xlint:nullary-unit",                           // `def f: Unit` looks like an accessor; add parens to look side-effecting.
    "-Xlint:option-implicit",                        // Option.apply used an implicit view.
    "-Xlint:poly-implicit-overload",                 // Parameterized overloaded implicit methods are not visible as view bounds.
    "-Xlint:private-shadow",                         // A private field (or class parameter) shadows a superclass field.
    "-Xlint:stars-align",                            // In a pattern, a sequence wildcard `_*` should match all of a repeated parameter.
    "-Xlint:valpattern",                             // Enable pattern checks in val definitions.
    "-Xmixin-force-forwarders:false",                // Only generate mixin forwarders required for program correctness.
    "-Xno-forwarders",                               // Do not generate static forwarders in mirror classes.
    "-Yjar-compression-level", "9",                  // compression level to use when writing jar files
    "-Yno-generic-signatures",                       // Suppress generation of generic signatures for Java.
    "-Ypatmat-exhaust-depth", "off"
  )

  def scalac3Flags = Seq(
    "-source:3.0-migration",
    "-Wconf:msg=unused:s", // Scala 3.1 doesn't support @nowarn("cat=unused")
    "-Ykind-projector",
  )

  val commonSettings = ConfigureBoth(
    _.settings(
      scalaVersion          := Ver.scala2,
      crossScalaVersions    := Seq(Ver.scala2, Ver.scala3),
      libraryDependencies  ++= Seq(Dep.betterMonadicFor, Dep.kindProjector).filter(_ => scalaVersion.value startsWith "2"),
      scalacOptions        ++= scalacCommonFlags,
      scalacOptions        ++= scalac2Flags.filter(_ => scalaVersion.value.startsWith("2")),
      scalacOptions        ++= scalac3Flags.filter(_ => scalaVersion.value.startsWith("3")),
      Test / scalacOptions --= Seq("-Ywarn-dead-code"),
      testFrameworks        := Nil,
      updateOptions         := updateOptions.value.withCachedResolution(true),
    ))

  def testSettings = ConfigureBoth(
    _.settings(
      testFrameworks += new TestFramework("utest.runner.Framework"),
      libraryDependencies ++= Seq(
        Dep.microlibsTestUtil.value % Test,
        Dep.nyayaGen.value % Test,
        Dep.nyayaProp.value % Test,
        Dep.nyayaTest.value % Test,
        Dep.utest.value % Test,
      ),
      parallelExecution := false,
    ))
    .jsConfigure(_.settings(
      libraryDependencies ++= Seq(
        Dep.scalaJsJavaTime.value % Test,
        Dep.scalaJsReactTest.value % Test,
        Dep.scalaJsSecureRandom.value % Test,
      ),
      Test / jsEnv := new AdvancedNodeJSEnv(
        AdvancedNodeJSEnv.Config().withEnv(Map(
          "CI"        -> (if (inCI) "1" else "0"),
          "SBT_ROOT"  -> (ThisBuild / baseDirectory).value.getAbsolutePath,
          "SCALA_VER" -> scalaVersion.value,
        ))
      ),
    ))

  lazy val root = project
    .in(file("."))
    .settings(
      name := "webapp-util",
      crossScalaVersions := Nil,
    )
    .configure(commonSettings.jvm, preventPublication)
    .aggregate(
      coreBoopickleJS,
      coreBoopickleJVM,
      coreCatsEffectJS,
      coreCatsEffectJVM,
      coreCirceJS,
      coreCirceJVM,
      coreJS,
      coreJVM,
      coreOkHttp4,
      dbPostgres,
      examplesJS,
      examplesJVM,
      ghpages,
      testBoopickleJS,
      testBoopickleJVM,
      testCatsEffectJS,
      testCatsEffectJVM,
      testCirceJS,
      testCirceJVM,
      testCoreJS,
      testCoreJVM,
      testDbPostgres,
      testNode,
    )

  // ===================================================================================================================

  lazy val coreJVM = core.jvm
  lazy val coreJS  = core.js
  lazy val core = crossProject(JSPlatform, JVMPlatform)
    .configureCross(commonSettings, publicationSettings, testSettings)
    .jsConfigure(_
      .enablePlugins(JSDependenciesPlugin)
      .dependsOn(testNode % "test->compile")
    )
    .settings(
      libraryDependencies ++= Seq(
        Dep.microlibsStdlibExt.value,
        Dep.microlibsUtils.value,
        Dep.univEq.value,
      ),
    )
    .jvmSettings(
      libraryDependencies ++= Seq(
        Dep.javaxWebsocketApi.value,
        Dep.scalaLogging.value,
      ),
    )
    .jsSettings(
      libraryDependencies ++= Seq(
        Dep.microlibsAdtMacros.value,
        Dep.scalaJsDom.value,
        Dep.scalaJsReactCore.value,
        Dep.scalaJsReactExtra.value,
      ),
      jsDependencies ++= Seq(
        Dep.base32768(Test).value,
      ),
    )

  lazy val testCoreJVM = testCore.jvm
  lazy val testCoreJS  = testCore.js
  lazy val testCore = crossProject(JSPlatform, JVMPlatform)
    .configureCross(commonSettings, publicationSettings, testSettings)
    .dependsOn(core)
    .settings(
      moduleName := "test",
      libraryDependencies ++= Seq(
        Dep.microlibsTestUtil.value,
        Dep.testStateCore.value,
      ),
    )

  // ===================================================================================================================

  lazy val coreBoopickleJVM = coreBoopickle.jvm
  lazy val coreBoopickleJS  = coreBoopickle.js
  lazy val coreBoopickle = crossProject(JSPlatform, JVMPlatform)
    .configureCross(commonSettings, publicationSettings, testSettings)
    .dependsOn(core)
    .jsConfigure(_
      .enablePlugins(JSDependenciesPlugin)
      .dependsOn(testNode % "test->compile")
    )
    .settings(
      moduleName := "core-boopickle",
      libraryDependencies ++= Seq(
        Dep.boopickle.value,
        Dep.microlibsNonEmpty.value,
        Dep.microlibsRecursion.value,
      ),
    )
    .jsSettings(
      jsDependencies ++= Seq(
        Dep.base32768(Test).value,
        Dep.pako(Test).value,
      ),
    )

  lazy val testBoopickleJVM = testBoopickle.jvm
  lazy val testBoopickleJS  = testBoopickle.js
  lazy val testBoopickle = crossProject(JSPlatform, JVMPlatform)
    .configureCross(commonSettings, publicationSettings, testSettings)
    .dependsOn(coreBoopickle, testCore)
    .jsConfigure(_
      .enablePlugins(JSDependenciesPlugin)
      .dependsOn(testNode)
    )
    .settings(
      moduleName := "test-boopickle",
    )
    .jsSettings(
      jsDependencies += Dep.pako(Test).value,
    )

  // ===================================================================================================================

  lazy val coreCatsEffectJVM = coreCatsEffect.jvm
  lazy val coreCatsEffectJS  = coreCatsEffect.js
  lazy val coreCatsEffect = crossProject(JSPlatform, JVMPlatform)
    .configureCross(commonSettings, publicationSettings, testSettings)
    .dependsOn(core)
    .settings(
      moduleName := "core-cats-effect",
      libraryDependencies += Dep.catsEffect.value,
    )

  lazy val testCatsEffectJVM = testCatsEffect.jvm
  lazy val testCatsEffectJS  = testCatsEffect.js
  lazy val testCatsEffect = crossProject(JSPlatform, JVMPlatform)
    .configureCross(commonSettings, publicationSettings, testSettings)
    .dependsOn(coreCatsEffect, testCore)
    .settings(
      moduleName := "test-cats-effect",
    )

  // ===================================================================================================================

  lazy val coreCirceJVM = coreCirce.jvm.settings(
    genArity := GenJsonCodecs(baseDirectory.value / ".." / "shared" / "src" / "main" / "scala"),
  )

  lazy val coreCirceJS  = coreCirce.js
  lazy val coreCirce = crossProject(JSPlatform, JVMPlatform)
    .configureCross(commonSettings, publicationSettings, testSettings)
    .dependsOn(core)
    .settings(
      moduleName := "core-circe",
      libraryDependencies ++= Seq(
        Dep.circeCore.value,
        Dep.circeParser.value,
        Dep.microlibsAdtMacros.value,
        Dep.microlibsRecursion.value,
      ),
    )

  lazy val testCirceJVM = testCirce.jvm
  lazy val testCirceJS  = testCirce.js
  lazy val testCirce = crossProject(JSPlatform, JVMPlatform)
    .configureCross(commonSettings, publicationSettings, testSettings)
    .dependsOn(coreCirce, testCore)
    .settings(
      moduleName := "test-circe",
      libraryDependencies += Dep.nyayaGen.value,
    )

  // ===================================================================================================================

  lazy val coreOkHttp4 = project
    .configure(commonSettings.jvm, publicationSettings.jvm)
    .dependsOn(coreJVM)
    .settings(
      moduleName := "core-okhttp4",
      libraryDependencies += Dep.okHttp4.value,
    )

  // ===================================================================================================================

  lazy val dbPostgres = project
    .configure(commonSettings.jvm, publicationSettings.jvm)
    .dependsOn(coreCatsEffectJVM)
    .settings(
      moduleName := "db-postgres",
      libraryDependencies ++= Seq(
        Dep.catsRetry          .value,
        Dep.clearConfig        .value,
        Dep.doobieCore         .value,
        Dep.doobieHikari       .value,
        Dep.doobiePostgres     .value,
        Dep.doobiePostgresCirce.value,
        Dep.flyway             .value,
        Dep.hikariCP           .value,
        Dep.postgresql         .value,
        Dep.scalaLogging       .value,
      ),
    )

  lazy val testDbPostgres = project
    .configure(commonSettings.jvm, publicationSettings.jvm)
    .dependsOn(dbPostgres)
    .settings(
      moduleName := "test-db-postgres",
      libraryDependencies ++= Seq(
        Dep.izumiReflect     .value,
        Dep.microlibsTestUtil.value,
        Dep.univEq           .value,
      ),
    )

  // ===================================================================================================================

  lazy val testNode = project
    .enablePlugins(ScalaJSPlugin)
    .configure(commonSettings.js, publicationSettings.js)
    .settings(
      moduleName := "test-node",
      libraryDependencies ++= Seq(
        Dep.scalaJsDom.value,
        Dep.scalaJsReactCore.value,
        Dep.microlibsTestUtil.value,
      ),
    )

  // ===================================================================================================================

  lazy val examplesJVM = examples.jvm
  lazy val examplesJS  = examples.js
  lazy val examples = crossProject(JSPlatform, JVMPlatform)
    .configureCross(commonSettings, testSettings)
    .configure(preventPublication)
    .dependsOn(
      core,
      coreBoopickle,
      coreCatsEffect,
      coreCirce,
      testBoopickle % "compile->test",
      testCatsEffect % "compile->test",
      testCirce % "compile->test",
      testCore % "compile->test",
    )
    .jvmConfigure(_.dependsOn(
      coreOkHttp4,
      dbPostgres,
      testDbPostgres,
    ))
    .jsConfigure(addReactJsDependencies(Test))
    .jsConfigure(_.dependsOn(
      testNode,
    ))

  lazy val ghpages = project
    .enablePlugins(LaikaPlugin)
    .configure(commonSettings.jvm, preventPublication, GhPages.settings)

  object GhPages {
    import cats.syntax.all._
    import laika.ast._
    import laika.directive._
    import laika.markdown.github.GitHubFlavor
    import laika.parse.code.languages._
    import laika.parse.code.SyntaxHighlighting

    private def allSourceFiles() = {
      val cmd = "find examples/*/src -name '*.scala'"
      val out = sys.process.Process(List("bash", "-c", cmd)).!!
      out
        .linesIterator
        .map(_.trim)
        .filter(_.nonEmpty)
        .toVector
    }

    private val sourceDirective = Blocks.create("sourceFile") {
      import Blocks.dsl._

      attribute(0).as[String].map { filename =>
        val ext = filename.reverse.takeWhile(_ != '.').reverse

        val syntax = ext match {
          case "scala" => ScalaSyntax
        }

        // Notice we re-load all source files on-demand so that we don't have to restart
        // sbt to detect changes at the file-system level.
        val files = allSourceFiles()

        val candidates = files.filter(_.endsWith("/" + filename))

        val path =
          candidates.length match {
            case 0 => throw new RuntimeException("File not found: " + filename)
            case 1 => candidates.head
            case n => throw new RuntimeException(s"Ambiguous filename: $filename\nCandidates:\n${candidates.map("  " + ).sorted.mkString("\n")}")
          }

        var source    = IO.read(file(path))
     // val content   = source.replaceFirst("^package .+", "").trim + "\n" // good enough lol
        val content   = source.trim + "\n"
        val parsed    = syntax.rootParser.parse(content).toEither.fold(sys.error, identity)
        val codeBlock = CodeBlock(language = ext, content = parsed)
        val name      = path.replace("src/test/scala/japgolly/webapputil/examples", "...")
        val nameSpan  = Seq(InlineCode("text", Seq(CodeSpan(name))), Text(":"))

        // BlockSequence(Paragraph(nameSpan), codeBlock)
        codeBlock
      }
    }

    private object CustomDirectives extends DirectiveRegistry {
      override val spanDirectives = Seq()
      override val blockDirectives = Seq(sourceDirective)
      override val templateDirectives = Seq()
      override val linkDirectives = Seq()
    }

    def settings: Project => Project = _.settings(
      laikaExtensions := Seq(
        GitHubFlavor,
        SyntaxHighlighting,
        CustomDirectives,
      )
    )
  }

}
