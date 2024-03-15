package com.pwc.madison.core.models;

import java.util.List;

import com.adobe.cq.export.json.ComponentExporter;

public interface ColumnControlModel extends ComponentExporter {

    String getColumnStackClass();

    String getColumnClass();

    List<String> getNumberOfColumns();

}
