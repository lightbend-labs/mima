package ssol.tools.mima.analyze

private[analyze] trait ReporterFactory {
  def createReporter: Reporter
}

private[analyze] class DefaultReporterFactory extends ReporterFactory {
  def createReporter = new DefaultReporter
}