package com.jediterm.terminal;

import com.jediterm.terminal.emulator.Emulator;

import java.io.IOException;

/**
 * @author traff
 */
public abstract class DataStreamIteratingEmulator implements Emulator {
  protected final TerminalDataStream myDataStream;
  protected final Terminal myTerminal;

  private boolean myEof = false;

  public DataStreamIteratingEmulator(TerminalDataStream dataStream, Terminal terminal) {
    myDataStream = dataStream;
    myTerminal = terminal;
  }

  @Override
  public boolean hasNext() {
    return !myEof;
  }

  @Override
  public void resetEof() {
    myEof = false;
  }

  @Override
  public void next() throws IOException {
    try {
      if (myDataStream instanceof ArrayTerminalDataStream) { // TODO: more permanent solution, probably change to the interface.
        ArrayTerminalDataStream terminalDataStream = (ArrayTerminalDataStream) myDataStream;
        int streamInitialOffset = terminalDataStream.myOffset;

        char b = myDataStream.getChar();
        processChar(b, myTerminal);

        int bytesRead = terminalDataStream.myOffset - streamInitialOffset;
        String processedCharacters = new String(terminalDataStream.myBuf, streamInitialOffset, bytesRead);

      } else {
        char b = myDataStream.getChar();
        processChar(b, myTerminal);
      }
      Integer streamInitialOffset = null;
      if () {
        streamInitialOffset = ((ArrayTerminalDataStream) myDataStream).myOffset;
      }


      if (streamInitialOffset != null) {
        int newStreamOffset = ((ArrayTerminalDataStream) myDataStream).myOffset;
        String processedCharacters = String(((ArrayTerminalDataStream) myDataStream).myBuf,
      }
      // TODO (Sergey): get myDataStream offset and by difference between offsets find characters that were read
      // then feed them to prediction engine.
      // Prediction engine will analyze input and match control sequences, then, if predictions were correct
      // it will remove them from prediction queue and reaaply all other predictions so that they will be visible.
      // If predictions were not correct it won't reapply them and they will disappear.
      // This needs to be synchronized only with UI thread that can add predictions to queue and force rerender?
      // I need to block rerendering while I'm processing from writer thread because I will rerender myself.
    }
    catch (TerminalDataStream.EOF e) {
      myEof = true;
    }
  }

  protected abstract void processChar(char ch, Terminal terminal) throws IOException;
}
