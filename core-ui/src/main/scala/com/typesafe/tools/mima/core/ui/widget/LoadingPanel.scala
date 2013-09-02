package com.typesafe.tools.mima.core.ui.widget

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;

/**
 * Copy/pasted from `http://www.jroller.com/gfx/date/20050215`.
 * FIXME: This has to be cleaned.
 */
class LoadingPanel extends JComponent with MouseListener {
  private var ticker: Array[Area] = null
  private var animation: Thread = null
  private var started = false
  private var alphaLevel = 0
  private var rampDelay = 300
  private var shield = 0.70f
  private var _text: String = ""
  private var barsCount = 14
  private var fps = 15.0f

  private var hints: RenderingHints = null

  def this(text: String = "", barsCount: Int = 14, shield: Float = 0.0f, fps: Float = 15.0f, rampDelay: Int = 300) =
    {
      this()
      this._text = text
      this.rampDelay = if (rampDelay >= 0) rampDelay else 0
      this.shield = if (shield >= 0.0f) shield else 0.0f
      this.fps = if (fps > 0.0f) fps else 15.0f
      this.barsCount = if (barsCount > 0) barsCount else 14

      this.hints = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
      this.hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      this.hints.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }

  def text_=(text: String) {
    repaint();
    _text = text;
  }

  def text = _text

  def start() {
    addMouseListener(this);
    setVisible(true);
    ticker = buildTicker();
    animation = new Thread(new Animator(true));
    animation.start();
  }

  def stop() {
    if (animation != null) {
      animation.interrupt();
      animation = null;
      animation = new Thread(new Animator(false));
      animation.start();
    }
  }

  def interrupt() {
    if (animation != null) {
      animation.interrupt();
      animation = null;

      removeMouseListener(this);
      setVisible(false);
    }
  }

  override def paintComponent(g: Graphics) {
    if (started) {
      val width = getWidth()
      val height = getHeight()

      var maxY = 0.0d

      val g2 = g.asInstanceOf[Graphics2D]
      g2.setRenderingHints(hints)

      g2.setColor(new Color(255, 255, 255, (alphaLevel * shield).asInstanceOf[Int]))
      g2.fillRect(0, 0, width, height)

      for (i <- 0 until ticker.length) {
        val channel = 224 - 128 / (i + 1);
        g2.setColor(new Color(channel, channel, channel, alphaLevel))
        g2.fill(ticker(i));

        val bounds = ticker(i).getBounds2D()
        if (bounds.getMaxY() > maxY)
          maxY = bounds.getMaxY()
      }

      if (text != null && text.length() > 0) {
        val context = g2.getFontRenderContext()
        val layout = new TextLayout(text, getFont(), context)
        val bounds = layout.getBounds()
        g2.setColor(getForeground())
        layout.draw(g2, (width - bounds.getWidth()).asInstanceOf[Float] / 2,
          (maxY + layout.getLeading() + 2 * layout.getAscent()).asInstanceOf[Float])
      }
    }
  }

  def buildTicker(): Array[Area] =
    {
      val width = getWidth()
      val height = getHeight()

      val ticker = Array.ofDim[Area](barsCount)
      val hwidth = width.asInstanceOf[Double] / 2
      val hheight = height.asInstanceOf[Double] / 2
      val center = new Point2D.Double(hwidth, hheight)
      val fixedAngle = 2.0 * math.Pi / (barsCount.asInstanceOf[Double])

      for (j <- 0 until barsCount) {
        val i = j.asInstanceOf[Double]
        val primitive = buildPrimitive()

        val toCenter = AffineTransform.getTranslateInstance(center.getX(), center.getY());
        val toBorder = AffineTransform.getTranslateInstance(45.0, -6.0);
        val toCircle = AffineTransform.getRotateInstance(-i * fixedAngle, center.getX(), center.getY());

        val toWheel = new AffineTransform();
        toWheel.concatenate(toCenter);
        toWheel.concatenate(toBorder);

        primitive.transform(toWheel);
        primitive.transform(toCircle);

        ticker(i.asInstanceOf[Int]) = primitive;
      }

      ticker
    }

  def buildPrimitive(): Area =
    {
      val body = new Rectangle2D.Double(6, 0, 30, 12);
      val head = new Ellipse2D.Double(0, 0, 12, 12);
      val tail = new Ellipse2D.Double(30, 0, 12, 12);

      val tick = new Area(body);
      tick.add(new Area(head));
      tick.add(new Area(tail));

      return tick;
    }

  private[LoadingPanel] class Animator(rampUp: Boolean = true) extends Runnable {

    override def run() {
      val width = getWidth()
      val height = getHeight()
      val center = new Point2D.Double(width.asInstanceOf[Double] / 2, height.asInstanceOf[Double] / 2);
      val fixedIncrement = 2.0 * math.Pi / (barsCount.asInstanceOf[Double])
      val toCircle = AffineTransform.getRotateInstance(fixedIncrement, center.getX(), center.getY());

      val start = System.currentTimeMillis();
      if (rampDelay == 0)
        alphaLevel = if (rampUp) 255 else 0

      started = true;
      var inRamp = rampUp;

      while (!Thread.interrupted()) {
        if (!inRamp) {
          for (i <- 0 until ticker.length)
            ticker(i).transform(toCircle);
        }

        repaint();

        var break = false

        if (rampUp) {
          if (alphaLevel < 255) {
            alphaLevel = (255 * (System.currentTimeMillis() - start) / rampDelay).asInstanceOf[Int]
            if (alphaLevel >= 255) {
              alphaLevel = 255;
              inRamp = false;
            }
          }
        } else if (alphaLevel > 0) {
          alphaLevel = (255 - (255 * (System.currentTimeMillis() - start) / rampDelay)).asInstanceOf[Int]
          if (alphaLevel <= 0) {
            alphaLevel = 0;
            break = true
          }
        }

        if (!break) {
          try {
            Thread.sleep(if (inRamp) 10 else (1000 / fps).asInstanceOf[Int]);
            Thread.`yield`
          } catch {
            case e: InterruptedException => ()
          }
        }
      }

      if (!rampUp) {
        started = false;
        repaint();

        setVisible(false);
        removeMouseListener(LoadingPanel.this);
      }
    }
  }

  def mouseClicked(e: MouseEvent) {
  }

  def mousePressed(e: MouseEvent) {
  }

  def mouseReleased(e: MouseEvent) {
  }

  def mouseEntered(e: MouseEvent) {
  }

  def mouseExited(e: MouseEvent) {
  }
}
