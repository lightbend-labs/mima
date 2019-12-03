package com.typesafe.tools.mima
package plugin

import java.nio.charset.Charset

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
    mimaCreateExclusionFile := createExclusionFile.value
  )

  /** Setup MiMa with default settings, applicable for most projects. */
  def mimaDefaultSettings: Seq[Setting[_]] = globalSettings ++ buildSettings ++ projectSettings

  // Allows reuse between mimaFindBinaryIssues and mimaReportBinaryIssues
  // without blowing up the Akka build's heap
  private def binaryIssuesIterator = Def.task {
    val s = streams.value
    val previousClassfiles = mimaPreviousClassfiles.value

    if (previousClassfiles.isEmpty) {
      val projectName = name.value
      var msg = s"$projectName: mimaPreviousArtifacts is empty, not analyzing binary compatibility."
      if (previousClassfiles eq NoPreviousClassfiles) {
        msg = s"$projectName: mimaPreviousArtifacts not set, not analyzing binary compatibility."
        if (mimaFailOnNoPrevious.value)
          sys.error(msg)
      }
      s.log.info(msg)
    }

    val currentClassfiles = mimaCurrentClassfiles.value
    val cp = (fullClasspath in mimaFindBinaryIssues).value
    val log = new SbtLogger(s)

    previousClassfiles.iterator.map { case (moduleId, prevClassfiles) =>
      moduleId -> SbtMima.runMima(prevClassfiles, currentClassfiles, cp, mimaCheckDirection.value, log)
    }
  }

  private def createExclusionFile = Def.task {
    import com.typesafe.tools.mima.core.ProblemReporting.versionOrdering

    val log = streams.value.log
    val highestVersion = mimaPreviousArtifacts.value.toSeq.maxBy(_.revision)(versionOrdering).revision
    val prefix = sys.props.getOrElse("akka.http.mima.exclusion-template-prefix", "_generated")

    val allExcludes =
      mimaPreviousClassfiles.value.toSeq.flatMap {
        case (module, file) =>
          val (backward, forward) = SbtMima.runMima(
            file,
            mimaCurrentClassfiles.value,
            (fullClasspath in mimaFindBinaryIssues).value,
            mimaCheckDirection.value,
            new SbtLogger(streams.value))

          val filters = mimaBinaryIssueFilters.value
          val backwardFilters = mimaBackwardIssueFilters.value
          val forwardFilters = mimaForwardIssueFilters.value

          def isReported(module: ModuleID, verionedFilters: Map[String, Seq[core.ProblemFilter]])(
            problem: core.Problem) =
            (verionedFilters.collect {
              // get all filters that apply to given module version or any version after it
              case f @ (version, filters) if versionOrdering.gteq(version, module.revision) => filters
            }.flatten ++ filters).forall { f =>
              f(problem)
            }

          val backErrors = backward.filter(isReported(module, backwardFilters))
          val forwErrors = forward.filter(isReported(module, forwardFilters))

          val filteredCount = backward.size + forward.size - backErrors.size - forwErrors.size
          val filteredNote = if (filteredCount > 0) " (filtered " + filteredCount + ")" else ""

          if (backErrors.size + forwErrors.size > 0) backErrors.flatMap(p => p.howToFilter.map((_, module.revision)))
          else Nil
      }

    if (allExcludes.nonEmpty) {
      val lines: List[String] =
        "# Autogenerated MiMa filters, please pull branch and check validity, and add comments why they are needed etc." ::
          "# Don't merge like this." :: "" ::
          allExcludes
            .groupBy(_._1)
            .mapValues(_.map(_._2).toSet)
            .groupBy(_._2)
            .flatMap {
              case (versionsAffected, exclusions) =>
                val versions = versionsAffected.toSeq.sorted(versionOrdering)
                s"# Incompatibilities against ${versions.mkString(", ")}" +: exclusions.keys.toVector.sorted :+ ""
            }
            .toList

      val targetFile = new File(mimaFiltersDirectory.value, s"$highestVersion.backwards.excludes/$prefix.excludes")
      targetFile.getParentFile.mkdirs()
      IO.writeLines(targetFile, lines, utf8, false)
      log.info(
        s"Created ${targetFile.getPath} as a template to ignore pending mima issues. Remove `.template` suffix to activate.")
      targetFile :: Nil
    } else
      Nil
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

  private val utf8 = Charset.forName("utf-8")
}
