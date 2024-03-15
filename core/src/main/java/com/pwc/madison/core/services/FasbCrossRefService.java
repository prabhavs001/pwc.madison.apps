/**
 *
 */
package com.pwc.madison.core.services;

import java.sql.SQLException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

import com.day.commons.datasource.poolservice.DataSourceNotFoundException;
import com.pwc.madison.core.exception.NoDataFoundException;

/**
 * The Interface FasbCrossRefService Provides Standard type , Standard number and search result
 *
 */
public interface FasbCrossRefService {

	public String getStandardTypes() throws SQLException, DataSourceNotFoundException;
	public String getStandardNumbers(SlingHttpServletRequest request,SlingHttpServletResponse response) throws SQLException, DataSourceNotFoundException, NoDataFoundException;
	public String getSearchResultByStandard(SlingHttpServletRequest request, SlingHttpServletResponse response)throws SQLException, DataSourceNotFoundException, NoDataFoundException;
	public String getSearchResultByCodification(SlingHttpServletRequest request,SlingHttpServletResponse response)throws SQLException, DataSourceNotFoundException, NoDataFoundException;
}
