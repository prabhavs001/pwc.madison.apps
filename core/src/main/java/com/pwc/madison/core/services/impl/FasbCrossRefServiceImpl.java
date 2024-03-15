package com.pwc.madison.core.services.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.xss.XSSAPI;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.commons.datasource.poolservice.DataSourceNotFoundException;
import com.day.commons.datasource.poolservice.DataSourcePool;
import com.google.gson.Gson;
import com.pwc.madison.core.constants.DITAConstants;
import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.exception.NoDataFoundException;
import com.pwc.madison.core.models.CrossRefResultRowModel;
import com.pwc.madison.core.models.FasbCrossRefModel;
import com.pwc.madison.core.models.StandardNumberModel;
import com.pwc.madison.core.models.StandardTypeModel;
import com.pwc.madison.core.services.FasbCrossRefService;

/*
 * it is service class for providing the data related to FASB Cross reference tool , it provides the data for
 * 1. standard type request
 * 2. Standard number request based on standard type
 * 3. Search result for by standard type request
 * 4. Search result for by codification request
 */
@Component(
		service = { FasbCrossRefService.class},
		immediate = true)
public class FasbCrossRefServiceImpl implements FasbCrossRefService {
	private static final Logger LOGGER = LoggerFactory.getLogger(FasbCrossRefService.class);
	@Reference
	private DataSourcePool dataSourcePool;

	@Reference
	private XSSAPI xssAPI;

	PreparedStatement preparedStatement=null;
	int index=1;

	/*
	 * it is responsible for returning the list of unique standard type
	 *
	 * it fetch data form DB and returns the response in json string format
	 *
	 * @return String
	 */
	@Override
	public String getStandardTypes() throws SQLException, DataSourceNotFoundException {
		final String query ="select distinct(STANDARD_TYPE) from fasb_cross_reference order by STANDARD_TYPE";
		final ArrayList<String> standardTypeList =new ArrayList<String>();
		final StandardTypeModel standardTypeModel=new StandardTypeModel();
		String standardTypejson=null;
		final Connection connection=getConnection();
		try
		{
			if(null !=connection)
			{
				LOGGER.debug("Got connection ");
				preparedStatement =connection.prepareStatement(query);
				final ResultSet rs=preparedStatement.executeQuery();
				while(rs.next())
				{
					LOGGER.info(xssAPI.encodeForHTML(rs.getString(MadisonConstants.DBSTANDARD_TYPE)));
					if(!(rs.getString(MadisonConstants.DBSTANDARD_TYPE)).isEmpty())
						standardTypeList.add(rs.getString(MadisonConstants.DBSTANDARD_TYPE));
				}
				LOGGER.debug("FasbCrossRefServiceImpl : getStandardTpyes() : standardTypeList {}", xssAPI.encodeForHTML(standardTypeList.toString()));	
				standardTypeModel.setStandardType(standardTypeList);
				final Gson gson = new Gson();
				standardTypejson = gson.toJson(standardTypeModel);
				LOGGER.debug("output json"+standardTypejson);
			}
		}
		finally{
			if(null!=connection)
			{
				connection.close();
			}
			if(null!=preparedStatement)
			{
				preparedStatement.close();
			}
		}
		return standardTypejson;
	}

	/*
	 * it is responsible for returning the list of standard numbers corresponding to standard type
	 *
	 * @param request
             {@link SlingHttpServletRequest}
	 *
	 * it takes standard type from request suffix
	 * Fetch data corresponding to standard type from DB and returns the response in the json format
	 *
	 * @return String
	 */
	@Override
	public String getStandardNumbers(SlingHttpServletRequest request,SlingHttpServletResponse response) throws SQLException, DataSourceNotFoundException, NoDataFoundException {
		final String query ="select distinct(STANDARD_Number) from fasb_cross_reference where STANDARD_type=? order by STANDARD_Number";
		final ArrayList<String> standardNumberList =new ArrayList<String>();
		final StandardNumberModel standardNumberModel=new StandardNumberModel();
		String standardNumberjson=null;
		final String[] suffixes = getSuffixes(request);
		String standardType=normaliseEncoded(suffixes[1]);
		if(standardType.endsWith(MadisonConstants.HTML_EXTN))
		{
			standardType=standardType.substring(0, standardType.indexOf(MadisonConstants.HTML_EXTN));
		}
		final Connection	connection=getConnection();
		try
		{
			if(null !=connection)
			{
				LOGGER.debug("Got connection ");
				preparedStatement =connection.prepareStatement(query);
				preparedStatement.setString(1, standardType);
				final ResultSet rs=preparedStatement.executeQuery();
				while(rs.next())
				{
					if(!(rs.getString(MadisonConstants.DBSTANDARD_NUMBER)).isEmpty())
						standardNumberList.add(rs.getString(MadisonConstants.DBSTANDARD_NUMBER));
				}
				if(standardNumberList.isEmpty())
				{
					response.setHeader(MadisonConstants.DISPATCHER,MadisonConstants.NO_CACHE);
				}
				standardNumberModel.setStandardNumber(standardNumberList);
				final Gson gson = new Gson();
				standardNumberjson = gson.toJson(standardNumberModel);
				LOGGER.debug("output json"+standardNumberjson);
			}
		}
		finally{
			if(null!=connection)
			{
				connection.close();
			}
			if(null!=preparedStatement)
			{
				preparedStatement.close();
			}
		}
		return standardNumberjson;
	}

	/*
	 * it is responsible for returning search result for Standard type option
	 *
	 * @param request
             {@link SlingHttpServletRequest}
	 *
	 * it takes standard Type,standard Number,number of record to return ( limit),number of records to  skip (skiprecord) from request suffix
	 * Fetch data corresponding to input data (standard type,Standard number,limit,skiprecord) from DB and returns the response in the json format
	 *
	 *@return String
	 */
	@Override
	public String getSearchResultByStandard(SlingHttpServletRequest request,SlingHttpServletResponse response) throws SQLException, DataSourceNotFoundException {
		String searchResultJson=null;

		final String query ="select total.totalcount ,searchdata.st,searchdata.sn,searchdata.pl,searchdata.stp,searchdata.sc,searchdata.topic,searchdata.term,searchdata.ph,searchdata.page from"
				+ " (select count(STANDARD_TYPE) as totalcount from fasb_cross_reference where STANDARD_TYPE=? and STANDARD_NUMBER=? ) as total , "
				+ "(select STANDARD_TYPE as st , STANDARD_NUMBER as sn,PARAGRAPH_LABEL as pl, TOPIC as topic, SUBTOPIC as stp, TERM as term, SECTION as sc, PARAGRAPH as ph, PAGE_PATH as page, "
				+"ROW_NUMBER() OVER ( ORDER BY STANDARD_TYPE,STANDARD_NUMBER,PARAGRAPH_LABEL ) R"
				+ " from fasb_cross_reference where STANDARD_TYPE=? and STANDARD_NUMBER=? ) as searchdata where  R BETWEEN ? and ?  ;";
		LOGGER.debug("query"+query);
		final String[] suffixes = getSuffixes(request);
		final String standardType=normaliseEncoded(suffixes[1]);
		String standardNumber=normaliseEncoded(suffixes[2]);
		if(standardNumber.contains(MadisonConstants.TILT)) {
			standardNumber = standardNumber.replaceAll(MadisonConstants.TILT, MadisonConstants.FORWARD_SLASH);
		}

		final int limit =Integer.parseInt(suffixes[3]);
		String skiprecordStr=suffixes[4];
		final int skiprecord =Integer.parseInt(skiprecordStr);
		FasbCrossRefModel fasbCrossRefModel=null;
		//			LOGGER.debug("standardType "+standardType+" standardNumber "+standardNumber+" limit "+limit+" skiprecord "+skiprecord);

		final Connection	connection=getConnection();
		try
		{
			if(null !=connection)
			{
				preparedStatement =connection.prepareStatement(query);
				preparedStatement.setString(1, standardType);
				preparedStatement.setString(2, standardNumber);
				preparedStatement.setString(3, standardType);
				preparedStatement.setString(4, standardNumber);

				preparedStatement.setString(5, String.valueOf(skiprecord+1));
				preparedStatement.setString(6, String.valueOf(skiprecord+limit));
				LOGGER.debug("prepared statement final is :: "+ preparedStatement.toString());
				final ResultSet rs=preparedStatement.executeQuery();
				fasbCrossRefModel=getSearchResultObj(rs,response);
				final Gson gson = new Gson();
				searchResultJson = gson.toJson(fasbCrossRefModel);

				LOGGER.debug("output json"+searchResultJson);
			}
		}
		finally{
			if(null!=connection)
			{
				connection.close();
			}
			if(null!=preparedStatement)
			{
				preparedStatement.close();
			}
		}
		return searchResultJson;
	}
	/*
	 * it is responsible for returning search result for codification option
	 *
	 * @param request
             {@link SlingHttpServletRequest}
	 *
	 * it takes topic,subTopic,section, paragraph,number of record to return ( limit),number of records to  skip (skiprecord) from request suffix
	 * Fetch data corresponding to input data (topic,subTopic,section, paragraph,limit,skiprecord) from DB and returns the response in the json format
	 *@return String
	 */
	@Override
	public String getSearchResultByCodification(SlingHttpServletRequest request,SlingHttpServletResponse response) throws SQLException, DataSourceNotFoundException {

		StringBuffer whereclous= new StringBuffer("where ");
		final ArrayList<String> paramList=new ArrayList<String> ();
		String wherestr=StringUtils.EMPTY;
		final String[] suffixes = getSuffixes(request);
		String topic=normaliseEncoded(suffixes[1]);
		String subTopic=normaliseEncoded(suffixes[2]);
		String section=normaliseEncoded(suffixes[3]);
		String paragraph=normaliseEncoded(suffixes[4]);
		final int limit=Integer.parseInt(suffixes[5]);
		String skiprecordStr=suffixes[6];

		final int skiprecord=Integer.parseInt(skiprecordStr);

		LOGGER.debug("topic "+topic+" subtopic "+subTopic+" limit "+limit+" skiprecord "+skiprecord+" section "+section+" paragraph "+paragraph);
		FasbCrossRefModel fasbCrossRefModel=null;
		String searchResultJson=null;

		if(!topic.equalsIgnoreCase(MadisonConstants.NA))
		{
			whereclous=whereclous.append("TOPIC=? AND ");
			paramList.add(MadisonConstants.TOPIC);
		}
		if(!subTopic.equalsIgnoreCase(MadisonConstants.NA))
		{
			whereclous=whereclous.append("SUBTOPIC=? AND ");
			paramList.add(MadisonConstants.SUBTOPIC);
		}
		if(!section.equalsIgnoreCase(MadisonConstants.NA))
		{
			whereclous=whereclous.append("SECTION=? AND ");
			paramList.add(MadisonConstants.SECTION);
		}
		if(!paragraph.equalsIgnoreCase(MadisonConstants.NA))
		{
			whereclous=whereclous.append("PARAGRAPH=? ");
			paramList.add(MadisonConstants.PARAGRAPH);
		}
		wherestr=whereclous.toString();
		if(wherestr.trim().endsWith(MadisonConstants.AND))
		{
			wherestr=wherestr.substring(0, wherestr.lastIndexOf(MadisonConstants.AND));
		}

		final String query ="select total.totalcount ,searchdata.st,searchdata.sn,searchdata.pl,searchdata.stp,searchdata.sc,searchdata.topic,searchdata.term,searchdata.ph,searchdata.page from"
				+ " (select count(STANDARD_TYPE) as totalcount from fasb_cross_reference "+wherestr+" ) as total , "
				+ "(select STANDARD_TYPE as st , STANDARD_NUMBER as sn,PARAGRAPH_LABEL as pl, TOPIC as topic, SUBTOPIC as stp,TERM as term, SECTION as sc, PAGE_PATH as page,  "
				+ "PARAGRAPH as ph ,ROW_NUMBER() OVER ( ORDER BY STANDARD_TYPE,STANDARD_NUMBER,PARAGRAPH_LABEL ) R "
				+ " from fasb_cross_reference "+wherestr+" ) as searchdata where  R BETWEEN ? and ?  ;";
		LOGGER.debug("query"+query);

		LOGGER.debug("before  connection");

		final Connection	connection=getConnection();
		try{
			if(null !=connection)
			{
				LOGGER.debug("Got connection");
				preparedStatement =connection.prepareStatement(query);
				populatePreparedStatement( preparedStatement, paramList, topic, subTopic, section, paragraph);
				populatePreparedStatement( preparedStatement, paramList, topic, subTopic, section, paragraph);
				preparedStatement.setString(index, String.valueOf(skiprecord+1));
				index++;
				preparedStatement.setString(index, String.valueOf(skiprecord+limit));
				index=1;
				final ResultSet rs=preparedStatement.executeQuery();
				fasbCrossRefModel=getSearchResultObj(rs,response);
				final Gson gson = new Gson();
				searchResultJson = gson.toJson(fasbCrossRefModel);
				LOGGER.debug("output json "+searchResultJson);
			}
		}
		finally{
			if(null!=connection)
			{
				connection.close();
			}
			if(null!=preparedStatement)
			{
				preparedStatement.close();
			}
		}
		return searchResultJson;
	}

	/*
	 * Creates and return the DB connection object using datasource configured in "DAY commons jdbc connections Pool" osgi service
	 *
	 *  @return Connection
	 */

	private Connection getConnection() throws SQLException, DataSourceNotFoundException
	{
		final DataSource dataSource = (DataSource) dataSourcePool.getDataSource("sqldatasource");
		final Connection connection = dataSource.getConnection();
		return connection;
	}
	/*
	 * @param request
             {@link SlingHttpServletRequest}
	 *
	 * Iterates over result set , creates   the FasbCrossRefModel needed to generating the json string.
	 * @return FasbCrossRefModel
	 */
	private FasbCrossRefModel getSearchResultObj(ResultSet rs,SlingHttpServletResponse response) throws SQLException
	{
		String totalCount="0";
		final FasbCrossRefModel resultObj=new FasbCrossRefModel();
		CrossRefResultRowModel crossRefResultRowModel=new CrossRefResultRowModel();
		final ArrayList<CrossRefResultRowModel> resultRowList=new ArrayList<CrossRefResultRowModel>();
		rs.getFetchSize();
		while(rs.next())
		{
			crossRefResultRowModel=new CrossRefResultRowModel();
			crossRefResultRowModel.setStandardType(rs.getString(MadisonConstants.QUERY_STANDARD_TYPE));
			crossRefResultRowModel.setStandardNumber(rs.getString(MadisonConstants.QUERY_STANDARD_NUMBER));
			crossRefResultRowModel.setParagraphLabel(rs.getString(MadisonConstants.QUERY_PARAGRAPH_LABEL));
			crossRefResultRowModel.setTopic(StringUtils.isNotEmpty(rs.getString(MadisonConstants.QUERY_TOPIC)) ? rs.getString(MadisonConstants.QUERY_TOPIC) : rs.getString(MadisonConstants.QUERY_TERM));
			crossRefResultRowModel.setSubtopic(rs.getString(MadisonConstants.QUERY_SUBTOPIC));
			crossRefResultRowModel.setSection(rs.getString(MadisonConstants.QUERY_SECTION));
			crossRefResultRowModel.setParagraph(StringUtils.isNotEmpty(rs.getString(MadisonConstants.QUERY_PARAGRAPH)) ? rs.getString(MadisonConstants.QUERY_PARAGRAPH) : MadisonConstants.QUERY_PARAGRAPH_VALUE);
			crossRefResultRowModel.setPath(!rs.getString(MadisonConstants.QUERY_PAGE_PATH).isEmpty()?rs.getString(MadisonConstants.QUERY_PAGE_PATH) : DITAConstants.HASH_STR);
			totalCount=rs.getString(MadisonConstants.QUERY_TOTAL_COUNT);
			resultRowList.add(crossRefResultRowModel);
		}
		if(resultRowList.isEmpty())
		{
			response.setHeader(MadisonConstants.DISPATCHER,MadisonConstants.NO_CACHE );
		}
		resultObj.setTotalCount(Integer.parseInt(totalCount));
		resultObj.setSearchResult(resultRowList);
		return resultObj;
	}
	/*
	 * @param  preparedStatement
             {@link PreparedStatement}
	 * ,@param  list :   List of valid input parameter (having non NA values)
	 *              {@link ArrayList<String>}
	 * @param  topic : value of topic received for request suffix
	 *               {@link String}
	 * @param  subTopic : value of subTopic received for request suffix
	 *                  {@link String}
	 * @param  section : value of section received for request suffix
	 *              {@link String}
	 * @param  paragraph : value of section received for request suffix
	 *                  {@link String}
	 *
	 * Sets values in place of placeholder in  prepared statement
	 */
	private void populatePreparedStatement(PreparedStatement preparedStatement, ArrayList<String> list,String topic,String subTopic,String section,String paragraph) throws NumberFormatException, SQLException
	{
		for( final String listItem : list)
		{
			if(listItem.equals(MadisonConstants.TOPIC))
			{
				preparedStatement.setString(index, topic) ;
			}
			else if(listItem.equals(MadisonConstants.SUBTOPIC))
			{
				preparedStatement.setString(index, subTopic) ;

			}
			else if(listItem.equals(MadisonConstants.SECTION))
			{
				preparedStatement.setString(index, section) ;
			}
			else if(listItem.equals(MadisonConstants.PARAGRAPH))
			{
				preparedStatement.setString(index, paragraph) ;
			}
			index++;

		}
	}
	/*
	 * @param  request
	 *                  {@link SlingHttpServletRequest}
	 * Creates return the string array of suffix available in request
	 */
	private String[] getSuffixes(SlingHttpServletRequest request)
	{
		final RequestPathInfo pathInfo=   request.getRequestPathInfo();
		final String[] suffixes = pathInfo.getSelectors();
		return suffixes;
	}

	/**
	 * Method to convert request params in original form
	 * 
	 * @param str{@link String}
	 * @return str{@link String}
	 */
	private String normaliseEncoded(String str) {
		if(str.contains("~"))
			str = str.replaceAll("[~]", ".");
		if(str.contains("$"))
			str = str.replaceAll("[$]", "/");
		return str;
	}
}
