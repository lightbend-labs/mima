/* NSC -- new Scala compiler
 * Copyright 2005-2007 LAMP/EPFL
 * @author  Martin Odersky
 */
// $Id: Settings.scala 12480 2007-08-06 17:39:10Z spoon $

package com.typesafe.tools.mima.core

import scala.tools.nsc

class Settings(specificOptions: String*) extends nsc.Settings(Console.println) {

  val interactive = BooleanSetting ("-i", "Interactive mode")
  val mimaOutDir = StringSetting("-d", "directory", "Specify where to place generated class files", "(change existing)")
  val fixall = BooleanSetting("-fixall", "Fix without asking")

  allSettings += (interactive, mimaOutDir)

  def visibleNames = 
    List(
      "-bootclasspath", 
      "-classpath", 
      "-extdirs", 
      "-javaextdirs", 
      "-usejavacp", 
      "-version", 
      "-help", 
      "-Ydebug",
      "-verbose", 
      "-d", 
      "-i") ++ specificOptions

  override def visibleSettings = super.visibleSettings.filter (visibleNames contains _.name)
}

    
  
