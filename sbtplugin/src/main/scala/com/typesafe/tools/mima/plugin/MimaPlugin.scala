package com.typesafe.tools.mima
package plugin

import sbt._
import sbt.Keys._

/** MiMa's sbt plugin. */
object MimaPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin
  override def trigger = allRequirements

  object autoImport extends MimaKeys
  import autoImport._

  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    mimaPreviousArtifacts := NoPreviousArtifacts,
    mimaBinaryIssueFilters := Nil,
    mimaFailOnProblem := true,
    mimaFailOnNoPrevious := true,
    mimaCheckDirection := "backward",
  )

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    mimaReportBinaryIssues := {
      val log = new SbtLogger(streams.value)
      binaryIssuesIterator.value.foreach { case (moduleId, problems) =>
        SbtMima.reportModuleErrors(
          moduleId,
          problems._1,
          problems._2,
          mimaFailOnProblem.value,
          mimaBinaryIssueFilters.value,
          mimaBackwardIssueFilters.value,
          mimaForwardIssueFilters.value,
          log,
          name.value,
        )
      }
    },
    mimaPreviousClassfiles := {
      val ivy = ivySbt.value
      val taskStreams = streams.value
      mimaPreviousArtifacts.value match {
        case _: NoPreviousArtifacts.type => NoPreviousClassfiles
        case previousArtifacts =>
          previousArtifacts.iterator.map { m =>
            val moduleId = CrossVersion(m, scalaModuleInfo.value) match {
              case Some(f) => m.withName(f(Project.normalizeModuleID(m.name)))
              case None    => m // no module id normalization if it's fully declaring it (using "%")
            }
            moduleId -> SbtMima.getPreviousArtifact(moduleId, ivy, taskStreams)
          }.toMap
      }
    },
    mimaCurrentClassfiles := (classDirectory in Compile).value,
    mimaFindBinaryIssues := binaryIssuesIterator.value.toMap,
    fullClasspath in mimaFindBinaryIssues := (fullClasspath in Compile).value,
    mimaBackwardIssueFilters := SbtMima.issueFiltersFromFiles(mimaFiltersDirectory.value, "\\.(?:backward[s]?|both)\\.excludes".r, streams.value),
    mimaForwardIssueFilters := SbtMima.issueFiltersFromFiles(mimaFiltersDirectory.value, "\\.(?:forward[s]?|both)\\.excludes".r, streams.value),
    mimaFiltersDirectory := (sourceDirectory in Compile).value / "mima-filters",
  )

  /** Setup mima with default settings, applicable for most projects. */
  def mimaDefaultSettings: Seq[Setting[_]] = globalSettings ++ buildSettings ++ projectSettings

  // Allows reuse between mimaFindBinaryIssues and mimaReportBinaryIssues
  // without blowing up the Akka build's heap
  private def binaryIssuesIterator = Def.task {
    val s = streams.value
    val projectName = name.value
    mimaPreviousClassfiles.value match {
      case _: NoPreviousClassfiles.type =>
        val msg = s"$projectName: mimaPreviousArtifacts not set, not analyzing binary compatibility."
        if (mimaFailOnNoPrevious.value) sys.error(msg)
        else s.log.info(msg)
        Iterator.empty
      case previousClassfiles if previousClassfiles.isEmpty =>
        s.log.info(s"$projectName: mimaPreviousArtifacts is empty, not analyzing binary compatibility.")
        Iterator.empty
      case previousClassfiles =>
        val currentClassfiles = mimaCurrentClassfiles.value
        val cp = (fullClasspath in mimaFindBinaryIssues).value
        val checkDirection = mimaCheckDirection.value
        val log = new SbtLogger(s)
        previousClassfiles.iterator.map { case (moduleId, file) =>
          val problems = SbtMima.runMima(file, currentClassfiles, cp, checkDirection, log)
          (moduleId, (problems._1, problems._2))
        }
    }
  }

  // Used to differentiate unset mimaPreviousArtifacts from empty mimaPreviousArtifacts
  private object NoPreviousArtifacts extends EmptySet[ModuleID]
  private object NoPreviousClassfiles extends EmptyMap[ModuleID, File]

  private sealed class EmptySet[A] extends Set[A] {
    def iterator          = Iterator.empty
    def contains(elem: A) = false
    def + (elem: A)       = Set(elem)
    def - (elem: A)       = this

    override def size                  = 0
    override def foreach[U](f: A => U) = ()
    override def toSet[B >: A]: Set[B] = this.asInstanceOf[Set[B]]
  }

  private sealed class EmptyMap[K, V] extends Map[K, V] {
    def get(key: K)              = None
    def iterator                 = Iterator.empty
    def + [V1 >: V](kv: (K, V1)) = updated(kv._1, kv._2)
    def - (key: K)               = this


    override def size                                       = 0
    override def contains(key: K)                           = false
    override def getOrElse[V1 >: V](key: K, default: => V1) = default
    override def updated[V1 >: V](key: K, value: V1)        = Map(key -> value)

    override def apply(key: K) = throw new NoSuchElementException(s"key not found: $key")
  }

}
