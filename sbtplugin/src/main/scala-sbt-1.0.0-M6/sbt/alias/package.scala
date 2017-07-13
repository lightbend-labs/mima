package sbt

import sbt.librarymanagement._
import sbt.internal.librarymanagement._

/**
 * Alias to inaccessbile methods
 */
package object alias {

  object IvyActions {
    def updateEither(
      module: IvySbt#Module,
      configuration: UpdateConfiguration,
      uwconfig: UnresolvedWarningConfiguration,
      logicalClock: LogicalClock,
      depDir: Option[File],
      log: Logger
    ): Either[UnresolvedWarning, UpdateReport] =
      IvyActions.updateEither(module, configuration, uwconfig, logicalClock, depDir, log)
  }
}
