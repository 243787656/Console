package com.jraska.console;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static com.jraska.console.ViewUtil.findViewByIdSafe;

/**
 * Console like output view, which allows writing via static console methods
 * from anywhere of application.
 * If you want to see the output, you should use console in any of your layouts,
 * all calls to console static write methods will affect all instantiated consoles.
 */
public class Console extends FrameLayout {
  //region Constants

  static final String END_LINE = "\n";
  static final String REMOVING_UNSUPPORTED_MESSAGE
      = "Removing of Views is unsupported in " + Console.class;

  //endregion

  //region Public Static API

  public static void writeLine() {
    write(END_LINE);
  }

  /**
   * Write provided object String representation to console and starts new line
   * "null" is written if the object is null
   *
   * @param o Object to write
   */
  public static void writeLine(Object o) {
    __buffer.append(o).append(END_LINE);
    scheduleBufferPrint();
  }

  /**
   * Write SpannableString to the console
   * "null" is written if the object is null
   *
   * @param spannableString SpannableString to write
   */
  public static void write(SpannableString spannableString) {
    __buffer.append(spannableString);
    scheduleBufferPrint();
  }

  /**
   * Write Spannable to console and starts new line
   * "null" is written if the object is null
   *
   * @param spannableString SpannableString to write
   */
  public static void writeLine(SpannableString spannableString) {
    __buffer.append(spannableString).append(END_LINE);
    scheduleBufferPrint();
  }

  /**
   * Write provided object String representation to console
   * "null" is written if the object is null
   *
   * @param o Object to write
   */
  public static void write(Object o) {
    __buffer.append(o);
    scheduleBufferPrint();
  }

  /**
   * Clears the console text
   */
  public static void clear() {
    __buffer.clear();
    scheduleBufferPrint();
  }

  public static int consoleViewsCount() {
    return __consoles.size();
  }

  //endregion

  //region Fields

  static List<WeakReference<Console>> __consoles = new ArrayList<>();
  static ConsoleBuffer __buffer = new ConsoleBuffer();

  // Handler for case writing is called from wrong thread
  private static volatile Handler __uiThreadHandler;
  private static final Object __lock = new Object();

  private TextView _text;
  private ScrollView _scrollView;

  // This will serve as flag for all view modifying methods
  // of Console to be suppressed from outside
  private boolean _privateLayoutInflated;

  // Fields are used to not schedule more than one runnable for scroll down
  private boolean _fullScrollScheduled;
  private final Runnable _scrollDownRunnable = new Runnable() {
    @Override public void run() {
      scrollFullDown();
    }
  };

  private UserTouchingListener _userTouchingListener;
  private FlingProperty _flingProperty;

  //endregion

  //region Constructors

  public Console(Context context) {
    super(context);
    init(context);
  }

  public Console(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public Console(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public Console(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    init(context);
  }

  private void init(Context context) {
    // Store myself as weak reference for static method calls
    __consoles.add(new WeakReference<>(this));

    LayoutInflater.from(context).inflate(R.layout.console_content, this);
    _privateLayoutInflated = true;

    _text = findViewByIdSafe(this, R.id.console_text);

    _scrollView = findViewByIdSafe(this, R.id.console_scroll_view);
    _flingProperty = FlingProperty.create(_scrollView);
    _userTouchingListener = new UserTouchingListener();
    _scrollView.setOnTouchListener(_userTouchingListener);

    printBuffer();
    // need to have extra post here, because scroll view is fully initialized after another frame
    post(new Runnable() {
      @Override public void run() {
        scrollDown();
      }
    });
  }

  //endregion

  //region Properties

  CharSequence getConsoleText() {
    return _text.getText().toString();
  }

  private boolean isUserInteracting() {
    return _userTouchingListener.isUserTouching() || _flingProperty.isFlinging();
  }

  private static Handler getUIThreadHandler() {
    synchronized (__lock) {
      if (__uiThreadHandler == null) {
        __uiThreadHandler = new Handler(Looper.getMainLooper());
      }

      return __uiThreadHandler;
    }
  }

  private static boolean isUIThread() {
    return Looper.myLooper() == Looper.getMainLooper();
  }

  //endregion

  //region FrameLayout overrides

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    _fullScrollScheduled = false;
  }

  @Override
  public void addView(View child, int index, ViewGroup.LayoutParams params) {
    // It is not possible to add views to Console, allow this only on initial layout creations
    if (!_privateLayoutInflated) {
      super.addView(child, index, params);
    } else {
      throw new UnsupportedOperationException("You cannot add views to " + Console.class);
    }
  }

  //region Suppress removing views

  @Override
  public final void removeView(View view) {
    throw new UnsupportedOperationException(REMOVING_UNSUPPORTED_MESSAGE);
  }

  @Override
  public final void removeViewInLayout(View view) {
    throw new UnsupportedOperationException(REMOVING_UNSUPPORTED_MESSAGE);
  }

  @Override
  public final void removeViewsInLayout(int start, int count) {
    throw new UnsupportedOperationException(REMOVING_UNSUPPORTED_MESSAGE);
  }

  @Override
  public final void removeViewAt(int index) {
    throw new UnsupportedOperationException(REMOVING_UNSUPPORTED_MESSAGE);
  }

  @Override
  public final void removeViews(int start, int count) {
    throw new UnsupportedOperationException(REMOVING_UNSUPPORTED_MESSAGE);
  }

  @Override
  public final void removeAllViews() {
    throw new UnsupportedOperationException(REMOVING_UNSUPPORTED_MESSAGE);
  }

  @Override
  public final void removeAllViewsInLayout() {
    throw new UnsupportedOperationException(REMOVING_UNSUPPORTED_MESSAGE);
  }

  //endregion

  //endregion

  //region Methods

  private void printScroll() {
    printBuffer();
    scrollDown();
  }

  private void printBuffer() {
    __buffer.printTo(_text);
  }

  private void scrollDown() {
    if (!isUserInteracting() && !_fullScrollScheduled) {
      post(_scrollDownRunnable);
      _fullScrollScheduled = true;
    }
  }

  private void scrollFullDown() {
    _scrollView.fullScroll(View.FOCUS_DOWN);
  }

  static void scheduleBufferPrint() {
    runBufferPrint();
  }

  private static void runBufferPrint() {
    if (!isUIThread()) {
      getUIThreadHandler().post(BufferPrintRunnable.INSTANCE);
      return;
    }

    // iteration from the end to allow in place removing
    for (int consoleIndex = __consoles.size() - 1; consoleIndex >= 0; consoleIndex--) {
      WeakReference<Console> consoleReference = __consoles.get(consoleIndex);
      Console console = consoleReference.get();
      if (console == null) {
        __consoles.remove(consoleIndex);
      } else {
        console.printScroll();
      }
    }
  }

  //endregion

  //region Nested classes

  static class BufferPrintRunnable implements Runnable {
    private static final BufferPrintRunnable INSTANCE = new BufferPrintRunnable();

    @Override
    public void run() {
      runBufferPrint();
    }
  }

  //endregion
}
