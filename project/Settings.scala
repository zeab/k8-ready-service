
//Imports
import Common.seqBaseProjectTemplate
import Versions._
import sbt.Def

//Settings
object Settings {

  val rootSettings: Seq[Def.Setting[_]] = seqBaseProjectTemplate(rootVersion)

}
