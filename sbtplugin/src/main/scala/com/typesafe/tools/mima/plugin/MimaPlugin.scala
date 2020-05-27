package com.typesafe.tools.mima
package plugin

import sbt._
import sbt.Keys._
import com.typesafe.tools.mima.core.{ IncompatibleSignatureProblem, ProblemFilters }
import lmcoursier.CoursierDependencyResolution

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
    mimaReportSignatureProblems := false,
    mimaCheckDirection := "backward",
  )

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    mimaReportBinaryIssues := {
      binaryIssuesIterator.value.foreach { case (moduleId, problems) =>
        SbtMima.reportModuleErrors(
          moduleId,
          problems._1,
          problems._2,
          mimaFailOnProblem.value,
          binaryIssueFilters.value,
          mimaBackwardIssueFilters.value,
          mimaForwardIssueFilters.value,
          streams.value.log,
          name.value,
        )
      }
    },
    mimaPreviousClassfiles := {
      val depRes = CoursierDependencyResolution(
        csrConfiguration.value
          .withInterProjectDependencies(Vector.empty)
          .withFallbackDependencies(Vector.empty)
          .withSbtScalaOrganization(None)
          .withSbtScalaVersion(None)
          .withSbtScalaJars(Vector.empty)
          .withExcludeDependencies(Vector.empty)
          .withForceVersions(Vector.empty)
          .withReconciliation(Vector.empty)
      )
      val taskStreams = streams.value
      mimaPreviousArtifacts.value match {
        case _: NoPreviousArtifacts.type => NoPreviousClassfiles
        case previousArtifacts =>
          previousArtifacts.iterator.map { m =>
            val moduleId = CrossVersion(m, scalaModuleInfo.value) match {
              case Some(f) => m.withName(f(m.name))
              case None    => m
            }
            moduleId -> SbtMima.getPreviousArtifact(moduleId, depRes, taskStreams)
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

  @deprecated("Switch to enablePlugins(MimaPlugin)", "0.7.0")
  def mimaDefaultSettings: Seq[Setting[_]] = globalSettings ++ buildSettings ++ projectSettings

  private def binaryIssueFilters = Def.task {
    val noSigs = ProblemFilters.exclude[IncompatibleSignatureProblem]("*")
    mimaBinaryIssueFilters.value ++ (if (mimaReportSignatureProblems.value) Nil else Seq(noSigs))
  }

  // Allows reuse between mimaFindBinaryIssues and mimaReportBinaryIssues
  // without blowing up the Akka build's heap
  private def binaryIssuesIterator = Def.task {
    val s = streams.value
    val previousClassfiles = mimaPreviousClassfiles.value
    val currentClassfiles = mimaCurrentClassfiles.value
    val cp = (fullClasspath in mimaFindBinaryIssues).value
    val sv = scalaVersion.value

    if (previousClassfiles eq NoPreviousClassfiles) {
      val msg = "mimaPreviousArtifacts not set, not analyzing binary compatibility"
      if (mimaFailOnNoPrevious.value) sys.error(msg) else s.log.info(s"${name.value}: $msg")
    } else if (previousClassfiles.isEmpty) {
      s.log.info(s"${name.value}: mimaPreviousArtifacts is empty, not analyzing binary compatibility.")
    }

    previousClassfiles.iterator.map { case (moduleId, prevClassfiles) =>
      moduleId -> SbtMima.runMima(prevClassfiles, currentClassfiles, cp, mimaCheckDirection.value, sv, s.log)
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
