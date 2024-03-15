package com.pwc.madison.core.beans;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 *Bean for postprocessing workflow
 */
public class PostProcessing {

    /**
     * Post processing failure
     */
    private boolean failure;

    /**
     * list of exceptions
     */
    private StringBuffer exception;

    public PostProcessing(){
        failure = false;
        exception = new StringBuffer("");
    }

    /**
     * returns failure
     * @return failure
     */
    public boolean isFailure() {
        return failure;
    }


    /**
     * Sets failure
     * @param failure
     */
    public void setFailure(boolean failure) {
        this.failure = failure;
    }

    /**
     * returns exceptions
     * @return
     */
    public StringBuffer getException() {
        return exception;
    }

    /**
     * Appends exception-string with the provided exceptions with new line
     * @param s
     */
    public void appendException(String s){
        exception.append(s);
        exception.append(System.lineSeparator());
    }

    /**
     * Appends exception-string with the provided string
     * @param s
     */
    public void appendString(String s){
        exception.append(s);
    }


    /**
     * gets error stack-trace and appends it to exception-string
     * @param th
     */
    public void appendLog(Throwable th){
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        th.printStackTrace(printWriter);
        exception.append(writer.toString());
        exception.append(System.lineSeparator());
    }

}
