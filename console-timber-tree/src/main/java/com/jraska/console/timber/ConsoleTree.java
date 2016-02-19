package com.jraska.console.timber;

import android.util.Log;
import com.jraska.console.Console;
import timber.log.Timber;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConsoleTree extends Timber.Tree {

  //region Constants

  private static final int CALL_STACK_INDEX = 6;
  private static final Pattern ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$");

  //endregion

  //region Tree impl

  @Override
  protected void log(int priority, String tag, String message, Throwable t) {
    if (tag == null) {
      tag = getTag();
    }

    String consoleMessage;
    if (tag == null) {
      consoleMessage = String.format("%s: %s", priorityString(priority), message);
    } else {
      consoleMessage = String.format("%s/%s: %s", priorityString(priority), tag, message);
    }

    writeToConsole(consoleMessage);
  }

  //endregion

  //region Methods

  protected void writeToConsole(String consoleMessage) {
    Console.writeLine(consoleMessage);
  }

  protected String createStackElementTag(StackTraceElement element) {
    String tag = element.getClassName();
    Matcher matcher = ANONYMOUS_CLASS.matcher(tag);
    if (matcher.find()) {
      tag = matcher.replaceAll("");
    }

    return tag.substring(tag.lastIndexOf('.') + 1);
  }

  protected String getTag() {
    StackTraceElement[] stackTrace = new Throwable().getStackTrace();
    if (stackTrace.length <= CALL_STACK_INDEX) {
      return null;
    }
    return createStackElementTag(stackTrace[CALL_STACK_INDEX]);
  }

  private static String priorityString(int priority) {
    switch (priority) {
      case Log.ASSERT:
        return "WTF";
      case Log.ERROR:
        return "E";
      case Log.WARN:
        return "W";
      case Log.INFO:
        return "I";
      case Log.DEBUG:
        return "D";
      case Log.VERBOSE:
        return "V";

      default:
        throw new IllegalArgumentException();
    }
  }

  //endregion

}