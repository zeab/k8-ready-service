
//Imports
import Settings._
import Dependencies._
import Docker._

//Add all the command alias's
CommandAlias.allCommandAlias

lazy val k8readyservice = (project in file("."))
  .settings(rootSettings: _*)
  .settings(libraryDependencies ++= rootDependencies)
  .settings(removeDependencies: _*)
  .settings(rootDockerSettings)
  .enablePlugins(AshScriptPlugin)
  .enablePlugins(AssemblyPlugin)


