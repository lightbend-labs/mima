package sbt

// sbt 1 -> sbt 0.13 compatibility
// Exposes (a minimal part) of the sbt 1 API using sbt 0.13 API
// Inspired by macro-compat

object compat {
  val scalaModuleInfo = SettingKey[Option[librarymanagement.ScalaModuleInfo]]("ivyScala")
}

package librarymanagement {
  object `package` {
    final val UpdateLogging = sbt.UpdateLogging
    type ModuleDescriptor = IvySbt#Module
    type UpdateLogging = UpdateLogging.Value
    type UpdateConfiguration = sbt.UpdateConfiguration
    type ScalaModuleInfo = IvyScala

    implicit class UpdateConfigurationOps(val _uc: UpdateConfiguration) extends AnyVal {
      def withLogging(ul: UpdateLogging): UpdateConfiguration = _uc copy (logging = ul)
    }
  }

  object UpdateConfiguration {
    def apply() = new UpdateConfiguration(None, false, UpdateLogging.Default)
  }

  class DependencyResolution(ivy: IvySbt) {
    def wrapDependencyInModule(m: ModuleID): ModuleDescriptor = {
      val moduleSettings = InlineConfiguration(
        "dummy" % "test" % "version",
        ModuleInfo("dummy-test-project-for-resolving"),
        dependencies = Seq(m)
      )
      new ivy.Module(moduleSettings)
    }

    def update(
        module: ModuleDescriptor,
        configuration: UpdateConfiguration,
        uwconfig: UnresolvedWarningConfiguration,
        log: Logger
    ): Either[UnresolvedWarning, UpdateReport] =
      IvyActions.updateEither(
        module,
        new UpdateConfiguration(
          retrieve = None,
          missingOk = false,
          logging = UpdateLogging.DownloadOnly
        ),
        UnresolvedWarningConfiguration(),
        LogicalClock.unknown,
        None,
        log
      )
  }

  package ivy {
    object IvyDependencyResolution {
      def apply(configuration: IvyConfiguration): DependencyResolution =
        new DependencyResolution(new IvySbt(configuration))
    }
  }
}

package internal {
  package librarymanagement {
  }
}
