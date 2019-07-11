package com.typesafe.tools.mima
package plugin

import sbt._
import sbt.Keys._

/** Sbt plugin for using MiMa. */
object MimaPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin
  override def trigger = allRequirements

  object autoImport extends MimaKeys
  import autoImport._

  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    mimaFailOnNoPrevious := true,
  )

  override def projectSettings: Seq[Def.Setting[_]] = mimaDefaultSettings

  /** Just configures MiMa to compare previous/current classfiles.*/
  def mimaReportSettings: Seq[Setting[_]] = Seq(
    mimaCheckDirection := "backward",
    mimaFiltersDirectory := (sourceDirectory in Compile).value / "mima-filters",
    mimaBinaryIssueFilters := Nil,
    mimaBackwardIssueFilters := SbtMima.issueFiltersFromFiles(mimaFiltersDirectory.value, "\\.(?:backward[s]?|both)\\.excludes".r, streams.value),
    mimaForwardIssueFilters := SbtMima.issueFiltersFromFiles(mimaFiltersDirectory.value, "\\.(?:forward[s]?|both)\\.excludes".r, streams.value),
    mimaFindBinaryIssues := binaryIssuesIterator.value.toMap,
    mimaReportBinaryIssues := {
      val log = new SbtLogger(streams.value)
      val projectName = name.value
      val failOnProblem = mimaFailOnProblem.value
      val binaryIssueFilters = mimaBinaryIssueFilters.value
      val backwardIssueFilters = mimaBackwardIssueFilters.value
      val forwardIssueFilters = mimaForwardIssueFilters.value
      binaryIssuesIterator.value.foreach { case (moduleId, problems) =>
        SbtMima.reportModuleErrors(
          moduleId,
          problems._1,
          problems._2,
          failOnProblem,
          binaryIssueFilters,
          backwardIssueFilters,
          forwardIssueFilters,
          log,
          projectName,
        )
      }
    }
  )

  /** Setup mima with default settings, applicable for most projects. */
  def mimaDefaultSettings: Seq[Setting[_]] = Seq(
    mimaFailOnProblem := true,
    mimaPreviousArtifacts := NoPreviousArtifacts,
    mimaCurrentClassfiles := (classDirectory in Compile).value,
    mimaPreviousClassfiles := {
      val scalaModuleInfoV = scalaModuleInfo.value
      val ivy = ivySbt.value
      val taskStreams = streams.value
      mimaPreviousArtifacts.value match {
        case _: NoPreviousArtifacts.type => NoPreviousClassfiles
        case previousArtifacts =>
          previousArtifacts.iterator.map { m =>
            val id = CrossVersion(m, scalaModuleInfoV) match {
              case Some(f) => m.withName(f(Project.normalizeModuleID(m.name)))
              case None    => m // no module id normalization if it's fully declaring it (using "%")
            }
            id -> SbtMima.getPreviousArtifact(id, ivy, taskStreams)
          }.toMap
      }
    },
    fullClasspath in mimaFindBinaryIssues := (fullClasspath in Compile).value
  ) ++ mimaReportSettings

  // Allows reuse between mimaFindBinaryIssues and mimaReportBinaryIssues
  // without blowing up the Akka build's heap
  private def binaryIssuesIterator = Def.task {
    val s = streams.value
    val log = new SbtLogger(s)
    val projectName = name.value
    val currentClassfiles = mimaCurrentClassfiles.value
    val cp = (fullClasspath in mimaFindBinaryIssues).value
    val checkDirection = mimaCheckDirection.value
    mimaPreviousClassfiles.value match {
      case _: NoPreviousClassfiles.type =>
        sys.error(s"$projectName: mimaPreviousArtifacts not set, not analyzing binary compatibility.")
      case previousClassfiles if previousClassfiles.isEmpty =>
        s.log.info(s"$projectName: mimaPreviousArtifacts is empty, not analyzing binary compatibility.")
        Iterator.empty
      case previousClassfiles =>
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

  // internal un-deprecated version
  private[mima] final val mimaFailOnNoPrevious =
    settingKey[Boolean]("if true, fail the build if no previous artifacts are set.")

}
