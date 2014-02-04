/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.editor;

import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.Segment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Provides services for moving the caret and retrieving information about caret position.
 *
 * May support several carets existing simultaneously in a document. {@link #supportsMultipleCarets()} method can be used to find out
 * whether particular instance of CaretModel does it. If it does, query and update methods for caret position operate on a certain 'primary'
 * caret. There exists a way to perform the same operation(s) on each caret - see {@link #runForEachCaret(Runnable)} method. Within its
 * context, query and update methods operate on the current caret in that iteration.
 * How 'primary' caret is determined by the model is not dictated.
 * At all times at least one caret will exist in a document.
 * <p>
 * Update methods, and {@link #runForEachCaret(Runnable)} method should only be run from EDT. Query methods can be run from any thread, when
 * called not from EDT, those methods are 'not aware' of 'runForEachCaret' scope - they will always return information about primary caret.
 *
 * @see Editor#getCaretModel()
 */
public interface CaretModel {
  /**
   * Moves the caret by the specified number of lines and/or columns.
   *
   * @param columnShift    the number of columns to move the caret by.
   * @param lineShift      the number of lines to move the caret by.
   * @param withSelection  if true, the caret move should extend the range or block selection in the document.
   * @param blockSelection if true and <code>withSelection</code> is true, the caret move should extend
   *                       the block selection in the document. This parameter is ignored when multiple carets are supported by the model.
   * @param scrollToCaret  if true, the document should be scrolled so that the caret is visible after the move.
   */
  void moveCaretRelatively(int columnShift,
                           int lineShift,
                           boolean withSelection,
                           boolean blockSelection,
                           boolean scrollToCaret);

  /**
   * Moves the caret to the specified logical position.
   * If corresponding position is in the folded region currently, the region will be expanded.
   *
   * @param pos the position to move to.
   */
  void moveToLogicalPosition(@NotNull LogicalPosition pos);

  /**
   * Moves the caret to the specified visual position.
   *
   * @param pos the position to move to.
   */
  void moveToVisualPosition(@NotNull VisualPosition pos);

  /**
   * Short hand for calling {@link #moveToOffset(int, boolean)} with <code>'false'</code> as a second argument.
   *
   * @param offset      the offset to move to
   */
  void moveToOffset(int offset);

  /**
   * Moves the caret to the specified offset in the document.
   * If corresponding position is in the folded region currently, the region will be expanded.
   *
   * @param offset                  the offset to move to.
   * @param locateBeforeSoftWrap    there is a possible case that there is a soft wrap at the given offset, hence, the same offset
   *                                corresponds to two different visual positions - just before soft wrap and just after soft wrap.
   *                                We may want to clearly indicate where to put the caret then. Given parameter allows to do that.
   *                                <b>Note:</b> it's ignored if there is no soft wrap at the given offset
   */
  void moveToOffset(int offset, boolean locateBeforeSoftWrap);

  /**
   * Caret position may be updated on document change (e.g. consider that user updates from VCS that causes addition of text
   * before caret. Caret offset, visual and logical positions should be updated then). So, there is a possible case
   * that caret model in in the process of caret position update now.
   * <p/>
   * Current method allows to check that.
   *
   * @return    <code>true</code> if caret position is up-to-date for now; <code>false</code> otherwise
   */
  boolean isUpToDate();

  /**
   * Returns the logical position of the caret.
   *
   * @return the caret position.
   */
  @NotNull
  LogicalPosition getLogicalPosition();

  /**
   * Returns the visual position of the caret.
   *
   * @return the caret position.
   */
  @NotNull
  VisualPosition getVisualPosition();

  /**
   * Returns the offset of the caret in the document.
   *
   * @return the caret offset.
   */
  int getOffset();

  /**
   * Adds a listener for receiving notifications about caret movement.
   *
   * @param listener the listener instance.
   */
  void addCaretListener(@NotNull CaretListener listener);

  /**
   * Removes a listener for receiving notifications about caret movement.
   *
   * @param listener the listener instance.
   */
  void removeCaretListener(@NotNull CaretListener listener);

  /**
   * @return    document offset for the start of the logical line where caret is located
   */
  int getVisualLineStart();

  /**
   * @return    document offset that points to the first symbol shown at the next visual line after the one with caret on it
   */
  int getVisualLineEnd();

  /**
   * Returns visual representation of caret (e.g. background color).
   *
   * @return Caret attributes.
   */
  TextAttributes getTextAttributes();

  /**
   * Tells whether multiple coexisting carets are supported by this CaretModel instance.
   */
  boolean supportsMultipleCarets();

  /**
   * Returns current caret - the one, query and update methods in the model operate at the moment. This is either an iteration-current
   * caret within the context of {@link #runForEachCaret(Runnable)} method, or the 'primary' caret without that context.
   * <p>
   * If multiple carets are not supported, the behaviour is unspecified.
   *
   * @see #supportsMultipleCarets()
   */
  @NotNull
  Caret getCurrentCaret();

  /**
   * Returns the 'primary' caret.
   * <p>
   * If multiple carets are not supported, the behaviour is unspecified.
   *
   * @see #supportsMultipleCarets()
   */
  @NotNull
  Caret getPrimaryCaret();

  /**
   * Returns all carets currently existing in the document, ordered by their position in the document.
   * <p>
   * If multiple carets are not supported, the behaviour is unspecified.
   *
   * @see #supportsMultipleCarets()
   */
  @NotNull
  Collection<Caret> getAllCarets();

  /**
   * Returns a caret at the given position in the document, or <code>null</code>, if there's no caret there.
   * <p>
   * If multiple carets are not supported, the behaviour is unspecified.
   *
   * @see #supportsMultipleCarets()
   */
  @Nullable
  Caret getCaretAt(@NotNull VisualPosition pos);

  /**
   * Adds a new caret at the given position, and returns corresponding Caret instance. Locations outside of possible values for the given
   * document are trimmed automatically.
   * Does nothing if a caret already exists at specified location or selection of existing caret includes the specified location,
   * <code>null</code> is returned in this case.
   * <p>
   * If multiple carets are not supported, the behaviour is unspecified.
   *
   * @see #supportsMultipleCarets()
   */
  @Nullable
  Caret addCaret(@NotNull VisualPosition pos);

  /**
   * Removes a given caret if it's recognized by the model and is not the only existing caret in the document, returning <code>true</code>.
   * <code>false</code> is returned if any of the above condition doesn't hold, and the removal cannot happen.
   * <p>
   * If multiple carets are not supported, the behaviour is unspecified.
   *
   * @see #supportsMultipleCarets()
   */
  boolean removeCaret(@NotNull Caret caret);

  /**
   * Removes all carets except the 'primary' one from the document.
   * <p>
   * If multiple carets are not supported, does nothing.
   *
   * @see #supportsMultipleCarets()
   */
  void removeSecondaryCarets();

  /**
   * Sets the number of carets, their positions and selection ranges according to the provided parameters. Null values in any of the lists
   * will mean that corresponding caret's position and/or selection won't be changed.
   * <p>
   * If multiple carets are not supported, the behaviour is unspecified.
   *
   * @see #supportsMultipleCarets()
   */
  void setCarets(@NotNull List<LogicalPosition> caretPositions, @NotNull List<? extends Segment> selections);

  /**
   * Executes the given task for each existing caret. Carets are iterated in their position order. Set of carets to iterate over is
   * determined in the beginning and is not affected by the potential carets addition or removal by the task being executed.
   * At the end, merging of carets and selections is performed, so that no two carets will occur at the same logical position and
   * no two selection will overlap after this method is finished.
   * <p>
   * If multiple carets are not supported, the given task is just executed once.
   *
   * @see #supportsMultipleCarets()
   */
  void runForEachCaret(@NotNull Runnable runnable);
}
