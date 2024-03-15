package com.pwc.madison.core.services;

import java.io.PrintWriter;

/**
 * 
 * Service provides method to generate records XML.
 *
 */
public interface RecordsGenerationService {

    /**
     * Start records XML creation.
     * 
     * @param recordsFileName
     *            {@link String} name of the records XML file
     * @param printWriter
     *            {@link PrintWriter} if not null, relevant logs can be added to write during the procrss of creation of
     *            records XML
     */
    void generateRecordsXml(final String recordsFileName, final PrintWriter printWriter);

}
