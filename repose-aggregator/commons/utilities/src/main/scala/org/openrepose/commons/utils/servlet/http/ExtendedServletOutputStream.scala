package org.openrepose.commons.utils.servlet.http

import java.io.InputStream
import javax.servlet.ServletOutputStream

trait ExtendedServletOutputStream extends ServletOutputStream {
  def getOutputStreamAsInputStream: InputStream

  def setOutput(in: InputStream): Unit

  def commit: Unit
}
