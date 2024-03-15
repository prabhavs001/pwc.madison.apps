package com.pwc.madison.core.services;

import java.io.PrintWriter;

/**
 *
 * The service generates records XML files explicitly.
 *
 */
public interface ExplicitRecordsGenerationService {

    /**
     * To start records XML creation explicitly i.e for manual trigger via servlet.
     * 
     * @param printWriter
     *            {@link PrintWriter} PrintWriter to print messages in response
     */
    public void generateRecordsXml(final PrintWriter printWriter);
}
