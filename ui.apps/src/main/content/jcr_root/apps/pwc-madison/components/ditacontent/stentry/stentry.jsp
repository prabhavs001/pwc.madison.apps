<%--
********************************************************************
*
*
*
***********************************************************************
*
* ADOBE CONFIDENTIAL
*
* ___________________
*
* Copyright 2016 Adobe Systems Incorporated
* All Rights Reserved.
*
* NOTICE:  All information contained herein is, and remains
* the property of Adobe Systems Incorporated and its suppliers,
*if any.The intellectual and technical concepts contained
* herein are proprietary to Adobe Systems Incorporated and its
* suppliers and may be covered by U.S.and Foreign Patents,
*patents in process, and are protected by trade secret or copyright law.
* Dissemination of this information or reproduction of this material
* is strictly forbidden unless prior written permission is obtained
* from Adobe Systems Incorporated.
*********************************************************************
--%>

<%@page session="false"%>

<%@page import="java.util.Stack,
                java.util.Map,
                org.json.JSONObject,
                com.adobe.fmdita.common.NodeUtils,
                com.adobe.fmdita.common.MiscUtils"
%>
<%@page trimDirectiveWhitespaces="true"%>

<%@include file="/libs/fmdita/components/dita/common/localization.jsp"%>
<%@include file="/libs/foundation/global.jsp"%>
<%@include file="/libs/fmdita/components/common/tablestack.jsp" %>

<%@page import="org.w3c.dom.Element" %>

<%!
    int[] getSpanValues(ValueMap properties,JSONObject colSpecObject){
    try {
      Element element = MiscUtils.getRootElement(properties);
      if(element != null) {
        int start = 1, end = 1, morerows = 1;
        if(element.hasAttribute("namest") && element.hasAttribute("nameend")){
          String startVal = element.getAttribute("namest");
          String endVal = element.getAttribute("nameend");
          JSONObject startColObj = (JSONObject)colSpecObject.get(startVal);
          JSONObject endColObj = (JSONObject)colSpecObject.get(endVal);
          start = startColObj.getInt("colnum");
          end = endColObj.getInt("colnum");
        }
        if(element.hasAttribute("morerows")) {
            morerows = Integer.parseInt(element.getAttribute("morerows")) + 1;
        }
        return (new int[]{start, end, morerows});
      }
    } catch(Exception ex) {    }
    return (new int[0]);
  }

%>

<%

String value = "1" ,spanAttrs = "";
int colCount = 0;
boolean bUnderHead = value.equals(getValueFromTableMap("thead", request));


try {
  String tColCount = getValueFromTableMap("tcolCount", request);
  colCount = Integer.parseInt(tColCount);
  String colSpec = getValueFromTableMap("colspec", request);
  String colName = properties.get("colname", "");
  int colNum = 0, colspan = 1;
  if(colSpec != null && !colSpec.isEmpty())
  {
    JSONObject colSpecObject = new JSONObject(colSpec);
    int[] spanArrs = getSpanValues(properties, colSpecObject);
    if(spanArrs.length > 0) {
      colNum = spanArrs[0];
      colspan = spanArrs[1] - spanArrs[0] + 1;
      if(colspan>1)
        spanAttrs = " colspan=\""+colspan+"\"";
      if(spanArrs[2]>1)
        spanAttrs += " rowspan=\""+spanArrs[2]+"\"";
    }
    else {
      JSONObject curColObj = (JSONObject)colSpecObject.get(colName);
      colNum = curColObj.getInt("colnum");
    }
    for(int i=colCount;i<colNum;i++){
      if(bUnderHead) {
        %><th style="display:none"></th><%
      }
      else {
        %><td style="display:none"></td><%
      }
      colCount++;
    }
    if(spanArrs.length > 0)
      colCount = spanArrs[1];
  }
} catch(Exception ex) {
  
}
String classVal = NodeUtils.getOutputClass(properties), classAttr="";
String attrs = MiscUtils.GetOutputAttributes(properties, slingRequest);
String outputClass = properties.get("outputclass", "");
String rowSepClass = properties.get("rowsep", "0");
String colSepClass = properties.get("colsep", "0");
String valign = properties.get("valign", "");
String rowSepVal = "";
String colSepVal = "";
String valignVal = "";

if(rowSepClass.equals("1")) {
    rowSepVal = "border-bottom";
} else if(rowSepClass.equals("2")) {
    rowSepVal = "double-border-bottom";
} else if(rowSepClass.equals("3")) {
    rowSepVal = "thin-bottom-underline-only";
} else if(rowSepClass.equals("4")) {
    rowSepVal = "thick-bottom-underline-only";
}

if(colSepClass.equals("1")) {
    colSepVal = "border-right";
}

if(valign.equals("middle")){
    valignVal = "middle-align";
} else if(valign.equals("bottom")){
    valignVal = "bottom-align";
}

/* The below ordering of outputclass and classVal should me maintained based on logic of 
align attribute for TD element should take prefrence over other styles 
*/
String mergeClass = " class=\"" + outputClass + " " + rowSepVal + " " + colSepVal + " " + valignVal + " "+ classVal+"\"";
if(bUnderHead) {
  %><th <%= mergeClass %> <%= spanAttrs %> <%= getLocalizationAttributes(properties) %>><%
} else {
  String tCol = getValueFromTableMap("tcol", request);
  try {
    int col = Integer.parseInt(tCol);
    if(colCount == col)
      classAttr = " class=\"" + outputClass + classVal +  " relcol\"";
  } catch(Exception ex) {
    
  }
%><td <%= mergeClass %> <%= spanAttrs %> <%= getLocalizationAttributes(properties) %> ><% 
} 
%><cq:include script="/libs/fmdita/components/dita/delegator/delegator.jsp" /><%
if(bUnderHead) { 
  %></th><%
}
else {
	%></td><%
}
setKeyInTableMap("tcolCount", Integer.toString(++colCount), request);
%>