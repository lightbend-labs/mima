package com.typesafe.tools.mima.core.ui

import scala.swing._
import Swing._
import java.awt.Color

import javax.swing._
import GridBagPanel._
import java.awt.GridBagConstraints._

trait WithConstraints extends GridBagPanel {
  /** Convenience method for creating and adding components to a GridBagPanel.
   *  It has reasonable defaults for all parameters.
   */
  def withConstraints[T](gridx: Int = RELATIVE, 
      gridy: Int = RELATIVE, 
      gridwidth: Int = 1,
      gridheight: Int = 1, 
      weightx: Double = 0.0,
      weighty: Double = 0.0, 
      anchor: Anchor.Value = Anchor.NorthWest,
      fill: Fill.Value = Fill.None,
      insets: Insets = new Insets(0, 0, 0, 0), 
      ipadx: Int = 0,
      ipady: Int = 0)(op: Constraints => T) = {
        val c = new Constraints(gridx, gridy, 
                                  gridwidth, gridheight,   
                                  weightx, weighty, 
                                  anchor.id, fill.id, insets, 
                                  ipadx, ipady)
        op(c)
  }
}
