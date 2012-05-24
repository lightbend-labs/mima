package com.typesafe.tools.mima.lib.ui

import com.typesafe.tools.mima.core.ui.MimaFrame

private[ui] class LibFrame extends MimaFrame {
  startWizard(new LibWizard)
}