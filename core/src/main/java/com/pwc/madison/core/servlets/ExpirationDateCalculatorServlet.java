package com.pwc.madison.core.servlets;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import javax.servlet.Servlet;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pwc.madison.core.constants.MadisonConstants;

/**
 * Servlet that returns the calculated expiration date of a topic
 */
@Component(service = Servlet.class,
           property = { Constants.SERVICE_DESCRIPTION
                   + "=This servlet is called while changing the publish date/effective as of date metadata"
                   + "of a topic", "sling.servlet.methods=GET", "sling.servlet.paths=/bin/pwc/getExpirationDate" })
public class ExpirationDateCalculatorServlet extends SlingSafeMethodsServlet {
    private static final Logger LOG = LoggerFactory.getLogger(ExpirationDateCalculatorServlet.class);
    private static final long serialVersionUid = 1L;

    @Override
    protected void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws IOException {
        ResourceResolver resourceResolver = request.getResourceResolver();
        RequestParameterMap requestParameterMap = request.getRequestParameterMap();
        String contentType = StringUtils.EMPTY;
        String expiryDateString = StringUtils.EMPTY;
        Date inputDate = null;
        try {
            if (requestParameterMap.containsKey(MadisonConstants.CONTENT_TYPE)) {
                contentType = requestParameterMap.getValue(MadisonConstants.CONTENT_TYPE).getString();
            }
            if (requestParameterMap.containsKey(MadisonConstants.INPUT_DATE)) {
                String inputDateString = requestParameterMap.getValue(MadisonConstants.INPUT_DATE).getString();
                long milliSeconds = Long.parseLong(inputDateString);
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(milliSeconds);
                SimpleDateFormat format = new SimpleDateFormat(MadisonConstants.DATE_FORMAT, Locale.ENGLISH);
                inputDate = format.parse(String.valueOf(calendar.getTime()));
            }

            if (!contentType.isEmpty() && null != inputDate) {
                expiryDateString = getExpiryDate(contentType, inputDate, resourceResolver);
            }
            response.getWriter().print(expiryDateString);
        } catch (ParseException e) {
            LOG.error("ParseException in ExpirationDateCalculatorServlet : {}", e);
        }
    }

    private String getExpiryDate(String contentType, Date inputDate, ResourceResolver resourceResolver) {
        String expDate = StringUtils.EMPTY;

        Resource contentTypesRootResource = resourceResolver.getResource(MadisonConstants.CONTENT_TYPE_REF_DATA_PATH);
        if (contentType.equals(MadisonConstants.PN_DEFAULT)) {
            expDate = calCulateExpiry(null, inputDate);
        } else if (null != contentTypesRootResource) {
            Iterator<Resource> contTypeIterator = contentTypesRootResource.getChildren().iterator();
            while (contTypeIterator.hasNext()) {
                Resource contentTypeResource = contTypeIterator.next();
                ValueMap contentTypeProperties = contentTypeResource.getValueMap();
                if (contentTypeProperties.containsKey(MadisonConstants.VALUE_PROPERTY)) {
                    String currentContentType = contentTypeProperties.get(MadisonConstants.VALUE_PROPERTY).toString();
                    if (currentContentType.equals(contentType)) {
                        expDate = calCulateExpiry(contentTypeProperties, inputDate);
                    }
                }
            }
        }
        return expDate;
    }

    private String calCulateExpiry(ValueMap contentTypeNodeProperties, Date inputDate) {
        String formattedDate = StringUtils.EMPTY;
        int expYear = 2;
        int expMonths = 0;
        Calendar cal = Calendar.getInstance();
        if (null != contentTypeNodeProperties) {
            if (contentTypeNodeProperties.containsKey(MadisonConstants.EXPIRY_PERIOD)) {
                Double expPeriod = Double
                        .parseDouble(contentTypeNodeProperties.get(MadisonConstants.EXPIRY_PERIOD).toString());
                String text = Double.toString(Math.abs(expPeriod));
                int integerPlaces = text.indexOf('.');
                int decimalPlaces = text.length() - integerPlaces - 1;
                expYear = expPeriod.intValue();
                Double expMonthsDecimal = expPeriod - expYear;
                if (decimalPlaces == 1) {
                    expMonths = (int) (expMonthsDecimal * 10);
                } else if (decimalPlaces == 2) {
                    expMonths = (int) (expMonthsDecimal * 100);
                }
            }
        }
        cal.setTime(inputDate);
        cal.add(Calendar.YEAR, expYear);
        cal.add(Calendar.MONTH, expMonths);
        Date expiryDate = cal.getTime();
        SimpleDateFormat sdf = new SimpleDateFormat(MadisonConstants.OUTPUT_DATE_FORMAT);
        formattedDate = sdf.format(expiryDate);
        return formattedDate;
    }
}
