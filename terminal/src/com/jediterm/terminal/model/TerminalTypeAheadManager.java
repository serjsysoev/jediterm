package com.jediterm.terminal.model;

import com.jediterm.terminal.TextStyle;
import com.jediterm.terminal.ui.settings.SettingsProvider;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TerminalTypeAheadManager {
    private static final Logger LOG = Logger.getLogger(TerminalTypeAheadManager.class);
    private final SettingsProvider mySettingsProvider;
    private final TerminalTextBuffer myTerminalTextBuffer;
    private final JediTerminal myTerminal;
    private int typeaheadThreshold = 0; // TODO: pull from settings

    PredictionTimeline myTimeline;

    private final Object LOCK = new Object();
    private final List<TerminalModelListener> myListeners = new CopyOnWriteArrayList<>();
    private TextStyle myTypeAheadTextStyle;
    private boolean myOutOfSyncDetected;
    private long myLastTypedTime;

    final char ESC = 27;
    final String CSI = ESC + "[";
    final String SHOW_CURSOR = CSI + "?25h";
    final String HIDE_CURSOR = CSI + "?25l";
    final String DELETE_CHAR = CSI + "X";
    final String DELETE_REST_OF_LINE = CSI + "K";
    final Pattern CSI_STYLE_RE = Pattern.compile("^\\x1b\\[[0-9;]*m"); // TODO: test regexes
    final Pattern CSI_MOVE_RE = Pattern.compile("^\\x1b\\[?([0-9]*)(;[35])?O?([DC])");
    final Pattern NOT_WORD_RE = Pattern.compile("[^a-z0-9]", Pattern.CASE_INSENSITIVE);

    public TerminalTypeAheadManager(@NotNull TerminalTextBuffer terminalTextBuffer,
                                    @NotNull JediTerminal terminal,
                                    @NotNull SettingsProvider settingsProvider) {
        myTerminalTextBuffer = terminalTextBuffer;
        myTerminal = terminal;
        mySettingsProvider = settingsProvider;

        myTimeline = new PredictionTimeline();

        timeline.setShowPredictions(this._typeaheadThreshold == 0);
    }

    private void deferClearingPredictions() {
        // TODO
    }

    // TODO: debounce like in js https://stackoverflow.com/questions/4742210/implementing-debounce-in-java/38296055
    void reevaluatePredictorState() {
        reevaluatePredictorStateNow();
    }

    void reevaluatePredictorStateNow() {
        if (false) { // TODO: this._excludeProgramRe.test(this._terminalTitle)
            myTimeline.setShowPredictions(false);
        } else if (typeaheadThreshold < 0) {
            myTimeline.setShowPredictions(false);
        } else if (typeaheadThreshold == 0) {
            myTimeline.setShowPredictions(true);
        } // TODO: else latency calculatation
    }

    private void sendLatencyStats() {
        // TODO: maybe we can do something with those stats
    }

    public void onBeforeProcessData(char[] data, int length) {
        // TODO

        System.out.print("OnBeforeProcessChar: ");
        for (int i = 0; i < length; ++i) {
            if (data[i] >= 32 && data[i] < 127) {
                System.out.print(data[i]);
            } else if (data[i] == 27) {
                System.out.print("<ESC>");
            } else if (data[i] == 13) {
                System.out.print("<CR>");
            } else if (data[i] == 10) {
                System.out.print("<LF>");
            } else {
                System.out.println("\nUnknown char! " + (int) data[i]);
            }
        }
        System.out.println();
    }

    public void onUserData(String data) {
        // TODO: if (this._timeline?.terminal.buffer.active.type !== 'normal') { return; }

        // TODO: Detect programs like git log/less that use the normal buffer but don't take input by default (fixes #109541)

        // TODO: terminal prompt guard

        TypeaheadStringReader reader = new TypeaheadStringReader(data);
        while (reader.remaining() > 0) {
            if (reader.eatCharCode(127) != null) { // backspace
                IPrediction previous = myTimeline.peekEnd();
                if (previous != null && previous instanceof CharacterPrediction) {
                    myTimeline.addBoundary();
                }

                // backspace must be able to read the previously-written character in
                // the event that it needs to undo it
                if (myTimeline.isShowingPredictions()) {
                    // flushOutput(this._timeline.terminal);
                    // TODO: understand this && fix
                }

                if (true) { // TODO: this._timeline.tentativeCursor(buffer).x <= this._lastRow!.startingX
                    myTimeline.addBoundary(buffer, new BackspacePrediction(myTimeline.terminal));
                } else {
                    // Backspace decrements our ability to go right.
                    // TODO: this._lastRow.endingX--;
                    myTimeline.addPrediction(buffer, new BackspacePrediction(myTimeline.terminal));
                }

                continue;
            }

            if (reader.eatCharCode(32, 126) != null) { // alphanum
                // TODO
                System.out.println("Alphanum is not yet implemented");
                continue;
            }

            String cursorMv = reader.eatRe(CSI_MOVE_RE);
            if (cursorMv != null) {
                // TODO
                System.out.println("Cursor move is not yet implemented");
                continue;
            }

            // TODO: whole word navigation (like shift + arrows)

            // TODO: \r

            // TODO: hard boundary if nothing matched
            break;
        }

    /* TODO:
    		if (this._timeline.length === 1) {
			this._deferClearingPredictions();
			this._typeaheadStyle!.startTracking();
		}
     */

        System.out.println("onUserData: `" + data + "`");
    }

    public void onResize() {
        myTimeline.setShowPredictions(false);
        myTimeline.clearCursor();
        reevaluatePredictorState();

        System.out.println("Terminal resized!");
    }

    enum MatchResult {
        Success,
        Failure,
        Buffer,
    }

    // TODO: const compileExcludeRegexp = (programs = DEFAULT_LOCAL_ECHO_EXCLUDE) =>
    //	            new RegExp(`\\b(${programs.map(escapeRegExpCharacters).join('|')})\\b`, 'i');

    enum CharPredictState {
        Unknown,
        HasPendingChar,
        Validated,
    }

    enum CursorMoveDirection {
        Back('D'),
        Forwards('C');

        private char myDirection;

        CursorMoveDirection(char direction) {
            myDirection = direction;
        }

        char getDirection() {
            return myDirection;
        }
    }

    class ICoordinate {
        int myX;
        int myY;
        int myBaseY;

        ICoordinate(int _x, int _y, int _baseY) {
            myX = _x;
            myY = _y;
            myBaseY = _baseY;
        }
    }

    class Cursor extends ICoordinate {
        private JediTerminal myTerminal;
        private TerminalTextBuffer myTerminalTextBuffer;
        private int myRows;
        private int myCols;

        Cursor(int rows, int cols, JediTerminal terminal, TerminalTextBuffer terminalTextBuffer) {
            super(terminal.getCursorX(), terminal.getCursorY(), buffer.baseY); // TODO: baseY??

            myRows = rows;
            myCols = cols;
            myTerminal = terminal;
            myTerminalTextBuffer = terminalTextBuffer;
        }

        TerminalLine getLine() {
            return myTerminalTextBuffer.getLine(myY + myBaseY);
        }

        /* TODO:
        getCell(loadInto?: IBufferCell) {
		    return this.getLine()?.getCell(this._x, loadInto);
	    }
         */

        String moveTo(ICoordinate coordinate) {
            myX = coordinate.myX;
            myY = (coordinate.myY + coordinate.myBaseY) - myBaseY;

            return moveInstruction();
        }

        Cursor cloneCursor() {
            Cursor newCursor = new Cursor(myRows, myCols, myTerminal, myTerminalTextBuffer);
            newCursor.moveTo(this);
            return newCursor;
        }

        String move(int x, int y) {
            myX = x;
            myY = y;

            return moveInstruction();
        }

        String shift(int x, int y) {
            myX = x;
            myY = y;

            return moveInstruction();
        }

        String moveInstruction() {
            if (myY >= myRows) {
                myBaseY += myY - (myRows - 1);
                myY = myRows - 1;
            } else if (myY < 0) {
                myBaseY -= myY;
                myY = 0;
            }

            return CSI + (myY + 1) + ";" + (myX + 1) + "H";
        }
    }

    interface IPrediction {
        boolean getAffectsStyle();

        boolean getClearAfterTimeout();

        String apply(IBuffer buffer, Cursor cursor);

        String rollback(Cursor cursor);

        String rollForwards(Cursor cursor, String withInput);

        MatchResult matches(TypeaheadStringReader input, IPrediction lookBehind);
    }

    class HardBoundary implements IPrediction {

        @Override
        public boolean getAffectsStyle() {
            return false; // TODO: this wasn't defined in original HardBoundary, needs double check.
        }

        @Override
        public boolean getClearAfterTimeout() {
            return false;
        }

        @Override
        public String apply(IBuffer buffer, Cursor cursor) {
            return "";
        }

        @Override
        public String rollback(Cursor cursor) {
            return "";
        }

        @Override
        public String rollForwards(Cursor cursor, String withInput) {
            return "";
        }

        @Override
        public MatchResult matches(TypeaheadStringReader input, IPrediction lookBehind) {
            return MatchResult.Failure;
        }
    }

    class TentativeBoundary implements IPrediction {
        private Cursor myAppliedCursor;
        private IPrediction myInner;

        TentativeBoundary(IPrediction inner) {
            myInner = inner;
        }

        @Override
        public boolean getAffectsStyle() {
            return false; // TODO: was not defined, double check
        }

        @Override
        public boolean getClearAfterTimeout() {
            return true;
        }

        @Override
        public String apply(IBuffer buffer, Cursor cursor) {
            myAppliedCursor = cursor.cloneCursor();
            myInner.apply(buffer, myAppliedCursor);
            return "";
        }

        @Override
        public String rollback(Cursor cursor) {
            myInner.rollback(cursor.cloneCursor());
            return "";
        }

        @Override
        public String rollForwards(Cursor cursor, String withInput) {
            if (myAppliedCursor != null) {
                cursor.moveTo(myAppliedCursor);
            }

            return withInput;
        }

        @Override
        public MatchResult matches(TypeaheadStringReader input, IPrediction lookBehind) {
            return myInner.matches(input, lookBehind);
        }
    }

    // TODO: export const isTenativeCharacterPrediction = (p: unknown): p is (TentativeBoundary & { inner: CharacterPrediction }) =>
    //	                p instanceof TentativeBoundary && p.inner instanceof CharacterPrediction;

    class CharacterPrediction implements IPrediction {

        private class AppliedAt { // FIXME: how to properly create structures just for one variable?
            ICoordinate myPos;
            String myOldAttributes;
            String myOldChar; // FIXME: string character???

            AppliedAt(ICoordinate pos, String oldAttributes, String oldChar) {
                myPos = pos;
                myOldAttributes = oldAttributes;
                myOldChar = oldChar;
            }
        }

        AppliedAt myAppliedAt;
        TypeaheadStyle myStyle;
        char myChar;

        CharacterPrediction(TypeaheadStyle style, char character) {
            myStyle = style;
            myChar = character;
        }

        @Override
        public boolean getAffectsStyle() {
            return true;
        }

        @Override
        public boolean getClearAfterTimeout() {
            return true;
        }

        @Override
        public String apply(IBuffer buffer, Cursor cursor) {
            // TODO: cell = cursor.getCell();
            if (cell != null) {
                myAppliedAt = new AppliedAt(cursor, attributesToSeq(cell), cell.getChars());
            } else {
                myAppliedAt = new AppliedAt(cursor, "", "");
            }

            cursor.shift(1, 0);

            return myStyle.apply + myChar + myStyle.undo;
        }

        @Override
        public String rollback(Cursor cursor) {
            if (myAppliedAt == null) {
                return "";
            }

            if (!myAppliedAt.myOldChar.isBlank()) {
                return cursor.moveTo(myAppliedAt.myPos)
                        + myAppliedAt.myOldAttributes
                        + myAppliedAt.myOldChar
                        + cursor.moveTo(myAppliedAt.myPos);
            } else {
                return cursor.moveTo(myAppliedAt.myPos) + DELETE_CHAR;
            }
        }

        @Override
        public String rollForwards(Cursor cursor, String withInput) {
            if (myAppliedAt == null) {
                return "";
            }

            return cursor.cloneCursor().moveTo(myAppliedAt.myPos) + withInput;
        }

        @Override
        public MatchResult matches(TypeaheadStringReader input, IPrediction lookBehind) {
            int startIndex = input.myIndex;

            // remove any styling CSI before checking the char
            String eaten;
            do {
                eaten = input.eatRe(CSI_STYLE_RE);
            } while (eaten != null && !eaten.isEmpty());

            if (input.eof()) {
                return MatchResult.Buffer;
            }

            if (input.eatChar(myChar) != null) {
                return MatchResult.Success;
            }

            if (lookBehind instanceof CharacterPrediction) {
                // vscode #112842
                MatchResult sillyZshOutcome = input.eatGradually("\b" + ((CharacterPrediction) lookBehind).myChar + myChar);
                if (sillyZshOutcome != MatchResult.Failure) {
                    return sillyZshOutcome;
                }
            }

            input.myIndex = startIndex;
            return MatchResult.Failure;
        }
    }

    class BackspacePrediction implements IPrediction {

        private class AppliedAt { // FIXME: how to properly create structures just for one variable?
            ICoordinate myPos;
            String myOldAttributes;
            String myOldChar; // FIXME: string character???
            boolean myIsLastChar;

            AppliedAt(ICoordinate pos, String oldAttributes, String oldChar, boolean isLastChar) {
                myPos = pos;
                myOldAttributes = oldAttributes;
                myOldChar = oldChar;
                myIsLastChar = isLastChar;
            }
        }

        AppliedAt myAppliedAt;
        VsCodeTerminal myTerminal;

        BackspacePrediction(VsCodeTerminal terminal) {
            myTerminal = terminal;
        }

        @Override
        public boolean getAffectsStyle() {
            return false; // TODO: double check
        }

        @Override
        public boolean getClearAfterTimeout() {
            return true;
        }

        @Override
        public String apply(IBuffer buffer, Cursor cursor) {
            // at eol if everything to the right is whitespace (zsh will emit a "clear line" code in this case)

            boolean isLastChar = cursor.getLine().getText().trim().isBlank(); // TODO: .getLine()?.translateToString(undefined, cursor.x).trim();
            ICoordinate pos = cursor;
            String move = cursor.shift(-1, 0);
            Cell cell = cursor.getCell();
            myAppliedAt = cell != null
                    ? new AppliedAt(pos, attributesToSeq(cell), cell.getChars(), isLastChar)
                    : new AppliedAt(pos, "", "", isLastChar);

            return move + DELETE_CHAR;
        }

        @Override
        public String rollback(Cursor cursor) {
            if (myAppliedAt == null) {
                return "";
            }

            if (myAppliedAt.myOldChar.isEmpty()) {
                return cursor.moveTo(myAppliedAt.myPos) + DELETE_CHAR;
            }

            return myAppliedAt.myOldAttributes
                    + myAppliedAt.myOldChar
                    + cursor.moveTo(myAppliedAt.myPos);
                    // TODO:  + attributesToSeq(core(this._terminal)._inputHandler._curAttrData);
        }

        @Override
        public String rollForwards(Cursor cursor, String withInput) {
            return "";
        }

        @Override
        public MatchResult matches(TypeaheadStringReader input, IPrediction lookBehind) {
            if (myAppliedAt != null && myAppliedAt.myIsLastChar) {
                MatchResult r1 = input.eatGradually("\b" + CSI + "K");
                if (r1 != MatchResult.Failure) {
                    return r1;
                }

                MatchResult r2 = input.eatGradually("\b \b");
                if (r2 != MatchResult.Failure) {
                    return r2;
                }
            }

            return MatchResult.Failure;
        }
    }

    class NewlinePrediction implements IPrediction {
        ICoordinate prevPosition;

        @Override
        public boolean getAffectsStyle() {
            return false; // TODO: double check
        }

        @Override
        public boolean getClearAfterTimeout() {
            return true;
        }

        @Override
        public String apply(IBuffer buffer, Cursor cursor) {
            prevPosition = cursor;
            cursor.move(0, cursor.myY + 1);
            return "\r\n";
        }

        @Override
        public String rollback(Cursor cursor) {
            return prevPosition != null ? cursor.moveTo(prevPosition) : "";
        }

        @Override
        public String rollForwards(Cursor cursor, String withInput) {
            return "";
        }

        @Override
        public MatchResult matches(TypeaheadStringReader input, IPrediction lookBehind) {
            return input.eatGradually("\r\n");
        }
    }

    class LinewrapPrediction extends NewlinePrediction implements IPrediction {
        @Override
        public String apply(IBuffer buffer, Cursor cursor) {
            prevPosition = cursor;
            cursor.move(0, cursor.myY + 1);
            return " \r";
        }

        @Override
        public MatchResult matches(TypeaheadStringReader input, IPrediction lookBehind) {
            // bash and zshell add a space which wraps in the terminal, then a CR
            MatchResult r = input.eatGradually(" \r");
            if (r != MatchResult.Failure) {
                // zshell additionally adds a clear line after wrapping to be safe -- eat it
                MatchResult r2 = input.eatGradually(DELETE_REST_OF_LINE);
                return r2 == MatchResult.Buffer ? MatchResult.Buffer : r;
            }

            return input.eatGradually("\r\n");
        }
    }

    class CursorMovePrediction implements IPrediction {
        private class Applied {
            String myRollForward;
            int myPrevPosition;
            String myPrevAttrs;
            int myAmount;

            Applied(String rollForward, int prevPosition, String prevAttrs, int amount) {
                myRollForward = rollForward;
                myPrevPosition = prevPosition;
                myPrevAttrs = prevAttrs;
                myAmount = amount;
            }
        }

        Applied myApplied;
        private CursorMoveDirection myDirection;
        private boolean myMoveByWords;
        private int myAmount;

        CursorMovePrediction(CursorMoveDirection direction, boolean moveByWords, int amount) {
            myDirection = direction;
            myMoveByWords = moveByWords;
            myAmount = amount;
        }

        // TODO: implement
    }

    static class TypeaheadStringReader {
        private final String myString;
        private int myIndex = 0;

        TypeaheadStringReader(String string) {
            myString = string;
        }

        int remaining() {
            return myString.length() - myIndex;
        }

        boolean eof() {
            return myString.length() == myIndex;
        }

        String rest() {
            return myString.substring(myIndex);
        }

        Character eatChar(char character) {
            if (myString.charAt(myIndex) != character) {
                return null;
            }

            myIndex++;
            return character;
        }

        String eatStr(String substr) {
            if (!myString.substring(myIndex, substr.length()).equals(substr)) {
                return null;
            }

            myIndex += substr.length();
            return substr;
        }

        MatchResult eatGradually(String substr) {
            int prevIndex = myIndex;

            for (int i = 0; i < substr.length(); ++i) {
                if (i > 0 && eof()) {
                    return MatchResult.Buffer;
                }

                if (eatChar(substr.charAt(i)) == null) {
                    this.myIndex = prevIndex;
                    return MatchResult.Failure;
                }
            }

            return MatchResult.Success;
        }

        String eatRe(Pattern pattern) {
            // TODO: verify correctness
            Matcher matcher = pattern.matcher(myString.substring(myIndex));
            if (!matcher.matches()) {
                return null;
            }

            java.util.regex.MatchResult match = matcher.toMatchResult();


            myIndex += matcher.end();
            return match.group();
        }

        Integer eatCharCode(int min) {
            return eatCharCode(min, min + 1);
        }

        Integer eatCharCode(int min, int max) {
            int code = myString.charAt(this.myIndex);
            if (code < min || code >= max) {
                return null;
            }

            this.myIndex++;
            return code;
        }
    }

    static class PredictionTimeline {
        class PredictionWithGeneration {
            int gen;
            IPrediction p;
        }

        private ArrayList<PredictionWithGeneration> myExpected; // TODO
        private int myCurrentGen = 0;
        private Cursor myPhysicalCursor;
        private Cursor myTenativeCursor;
        private String myInputBuffer;
        private boolean myShowPredictions = false;
        private IPrediction myLookBehind;

        PredictionTimeline() {
            // TODO
        }

        ArrayList<IPrediction> currentGenerationPredictions() {
            // TODO
        }

        boolean isShowingPredictions() {
            return myShowPredictions;
        }

        boolean length() {
            return myExpected.length();
        }

        void setShowPredictions(boolean show) {
            if (show == myShowPredictions) {
                return;
            }

            myShowPredictions = show;

            /* TODO: ???
            const buffer = this._getActiveBuffer();
            if (!buffer) {
                return;
            }
             */

            ArrayList<IPrediction> toApply = currentGenerationPredictions();
            if (show) {
                clearCursor();
                // TODO
            } else {
                // TODO
            }
        }

        void undoAllPredictions() {
            ???buffer = getActiveBuffer();
            /* TODO
            if (this._showPredictions && buffer) {
                this.terminal.write(this._currentGenerationPredictions.reverse()
                    .map(p => p.rollback(this.physicalCursor(buffer))).join(''));
            }

            this._expected = [];
             */
        }

        String beforeServerInput(String input) {
            // TODO
        }

        private void clearPredictionState() {
            myExpected.clear();
            clearCursor();
            myLookBehind = null;
        }

        boolean addPrediction(IBuffer buffer, IPrediction prediction) {
            /* TODO:
            this._expected.push({ gen: this._currentGen, p: prediction });
            this._addedEmitter.fire(prediction);

            if (this._currentGen !== this._expected[0].gen) {
                prediction.apply(buffer, this.tentativeCursor(buffer));
                return false;
            }

            const text = prediction.apply(buffer, this.physicalCursor(buffer));
            this._tenativeCursor = undefined; // next read will get or clone the physical cursor

            if (this._showPredictions && text) {
                if (prediction.affectsStyle) {
                    this._style.expectIncomingStyle();
                }
                // console.log('predict:', JSON.stringify(text));
                this.terminal.write(text);
            }

    		return true;
             */
        }

        // TODO: addBoundary nonsense

        IPrediction peekEnd() {
            return myExpected.get(myExpected.size() - 1).p;
        }

        IPrediction peekStart() {
            return myExpected.get(0).p;
        }

        Cursor physicalCursor(IBuffer buffer) {
            if (myPhysicalCursor == null) {
                if (myShowPredictions) {
                    // TODO: flushOutput(this.terminal);
                }
                myPhysicalCursor = // new Cursor(this.terminal.rows, this.terminal.cols, buffer);
            }

            return myPhysicalCursor;
        }

        Cursor tentativeCursor(IBuffer buffer) {
            if (myTenativeCursor == null) {
                myTenativeCursor = physicalCursor(buffer).cloneCursor();
            }

            return myTenativeCursor;
        }

        void clearCursor() {
            myPhysicalCursor = null;
            myTenativeCursor = null;
        }

        private IBuffer getActiveBuffer() {
            /* TODO
            const buffer = this.terminal.buffer.active;
	    	return buffer.type === 'normal' ? buffer : undefined;
             */
        }
    }
}
