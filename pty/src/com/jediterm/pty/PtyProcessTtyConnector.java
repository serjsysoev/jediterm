package com.jediterm.pty;

import com.jediterm.terminal.ProcessTtyConnector;
import com.jediterm.terminal.model.TerminalTypeAheadManager;
import com.pty4j.PtyProcess;
import com.pty4j.WinSize;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * @author traff
 */
public class PtyProcessTtyConnector extends ProcessTtyConnector {
  private final PtyProcess myProcess;
  private final TerminalTypeAheadManager myTypeaheadManager;

  public PtyProcessTtyConnector(@NotNull PtyProcess process, @NotNull Charset charset, @NotNull TerminalTypeAheadManager typeAheadManager) {
    super(process, charset);
    myProcess = process;
    myTypeaheadManager = typeAheadManager;
  }

  @Override
  public int read(char[] buf, int offset, int length) throws IOException {
    char[] intermediateBuffer = new char[length];

    int bytesRead = super.read(intermediateBuffer, 0, length);
    myTypeaheadManager.onBeforeProcessData(intermediateBuffer, bytesRead);

    System.arraycopy(intermediateBuffer, 0, buf, offset, bytesRead);
    return bytesRead;
  }

  @Override
  public void resize(@NotNull Dimension termWinSize) {
    myTypeaheadManager.onResize();

    if (isConnected()) {
      myProcess.setWinSize(new WinSize(termWinSize.width, termWinSize.height));
    }
  }

  @Override
  public void write(byte[] bytes) throws IOException {
    myTypeaheadManager.onUserData(new String(bytes));

    super.write(bytes);
  }

  @Override
  public boolean isConnected() {
    return myProcess.isRunning();
  }

  @Override
  public String getName() {
    return "Local";
  }
}
