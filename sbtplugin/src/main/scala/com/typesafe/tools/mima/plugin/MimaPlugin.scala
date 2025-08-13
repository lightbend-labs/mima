package com.typesafe.tools.mima
package plugin

import sbt.*, Keys.*
import core.*

/** MiMa's sbt plugin. */
object MimaPlugin extends AutoPlugin {
  override def trigger = allRequirements

  object autoImport extends MimaKeys
  import autoImport.*

  override def globalSettings: Seq[Def.Setting[?]] = Seq(
    mimaPreviousArtifacts := NoPreviousArtifacts,
    mimaExcludeAnnotations := Nil,
    mimaBinaryIssueFilters := Nil,
    mimaFailOnProblem := true,
    mimaFailOnNoPrevious := true,
    mimaReportSignatureProblems := false,
    mimaCheckDirection := "backward",
  )

  override def projectSettings: Seq[Def.Setting[?]] = Seq(
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
    mimaDependencyResolution := dependencyResolution.value,
    mimaPreviousClassfiles := {
      artifactsToClassfiles.value.toClassfiles(mimaPreviousArtifacts.value)
    },
    mimaCurrentClassfiles := (Compile / classDirectory).value,
    mimaFindBinaryIssues := binaryIssuesIterator.value.toMap,
    mimaFindBinaryIssues / fullClasspath := (Compile / fullClasspath).value,
    mimaBackwardIssueFilters := SbtMima.issueFiltersFromFiles(mimaFiltersDirectory.value, "\\.(?:backward[s]?|both)\\.excludes".r, streams.value),
    mimaForwardIssueFilters := SbtMima.issueFiltersFromFiles(mimaFiltersDirectory.value, "\\.(?:forward[s]?|both)\\.excludes".r, streams.value),
    mimaFiltersDirectory := (Compile / sourceDirectory).value / "mima-filters",
  )

  @deprecated("Switch to enablePlugins(MimaPlugin)", "0.7.0")
  def mimaDefaultSettings: Seq[Setting[?]] = globalSettings ++ buildSettings ++ projectSettings

  trait ArtifactsToClassfiles {
    def toClassfiles(previousArtifacts: Set[ModuleID]): Map[ModuleID, File]
  }

  trait BinaryIssuesFinder {
    def runMima(prevClassFiles: Map[ModuleID, File], checkDirection: String)
    : Iterator[(ModuleID, (List[Problem], List[Problem]))]
  }

  val artifactsToClassfiles: Def.Initialize[Task[ArtifactsToClassfiles]] = Def.task {
    val depRes = mimaDependencyResolution.value
    val taskStreams = streams.value
    val smi = scalaModuleInfo.value
    previousArtifacts => previousArtifacts match {
      case _: NoPreviousArtifacts.type => NoPreviousClassfiles
      case previousArtifacts =>
        previousArtifacts.iterator.map { m =>
          val moduleId = CrossVersion(m, smi) match {
            case Some(f) => m.withName(f(m.name)).withCrossVersion(CrossVersion.disabled)
            case None => m
          }
          moduleId -> SbtMima.getPreviousArtifact(moduleId, depRes, taskStreams)
        }.toMap
    }
  }

  val binaryIssuesFinder: Def.Initialize[Task[BinaryIssuesFinder]] = Def.task {
    val log = streams.value.log
    val currClassfiles = mimaCurrentClassfiles.value
    val cp = (mimaFindBinaryIssues / fullClasspath).value
    val sv = scalaVersion.value
    val excludeAnnots = mimaExcludeAnnotations.value.toList
    val failOnNoPrevious = mimaFailOnNoPrevious.value
    val projName = name.value

    (prevClassfiles, checkDirection) => {
      if (prevClassfiles eq NoPreviousClassfiles) {
        val msg = "mimaPreviousArtifacts not set, not analyzing binary compatibility"
        if (failOnNoPrevious) sys.error(msg) else log.info(s"$projName: $msg")
      } else if (prevClassfiles.isEmpty) {
        log.info(s"$projName: mimaPreviousArtifacts is empty, not analyzing binary compatibility.")
      }

      prevClassfiles.iterator.map { case (moduleId, prevClassfiles) =>
        moduleId -> SbtMima.runMima(prevClassfiles, currClassfiles, cp, checkDirection, sv, log, excludeAnnots)
      }
    }
  }

  private val binaryIssueFilters = Def.task {
    val noSigs = ProblemFilters.exclude[IncompatibleSignatureProblem]("*")
    mimaBinaryIssueFilters.value ++ (if (mimaReportSignatureProblems.value) Nil else Seq(noSigs))
  }

  // Allows reuse between mimaFindBinaryIssues and mimaReportBinaryIssues
  // without blowing up the Akka build's heap
  private val binaryIssuesIterator = Def.task {
    binaryIssuesFinder.value.runMima(mimaPreviousClassfiles.value, mimaCheckDirection.value)
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
