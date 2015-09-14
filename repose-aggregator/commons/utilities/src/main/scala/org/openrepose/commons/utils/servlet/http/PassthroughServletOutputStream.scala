package org.openrepose.commons.utils.servlet.http

import java.io.InputStream
import javax.servlet.ServletOutputStream

class PassthroughServletOutputStream(servletOutputStream: ServletOutputStream) extends ExtendedServletOutputStream {

  override def write(b: Int): Unit = servletOutputStream.write(b)

  override def getOutputStreamAsInputStream: InputStream =
    throw new IllegalStateException("method should not be called if the ResponseMode is set to PASSTHROUGH")

  override def setOutput(in: InputStream): Unit =
    throw new IllegalStateException("method should not be called if the ResponseMode is not set to MUTABLE")

  override def commit: Unit =
    throw new IllegalStateException("method should not be called if the ResponseMode is not set to MUTABLE")
}
