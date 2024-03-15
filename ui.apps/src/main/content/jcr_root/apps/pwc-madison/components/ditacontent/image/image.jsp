<%--
  Copyright 1997-2009 Day Management AG
  Barfuesserplatz 6, 4001 Basel, Switzerland
  All Rights Reserved.

  This software is the confidential and proprietary information of
  Day Management AG, ("Confidential Information"). You shall not
  disclose such Confidential Information and shall use it only in
  accordance with the terms of the license agreement you entered into
  with Day.

  ==============================================================================
--%><%@page import="com.day.cq.commons.jcr.JcrUtil,
                javax.xml.parsers.DocumentBuilder,
                javax.xml.parsers.DocumentBuilderFactory,
                org.w3c.dom.Document,
                org.w3c.dom.Element,
                org.w3c.dom.Node,
                org.w3c.dom.NodeList, 
                org.w3c.dom.NamedNodeMap,
                org.w3c.dom.Attr,
                org.w3c.dom.ls.DOMImplementationLS,
                org.w3c.dom.ls.LSSerializer,
                java.io.InputStream,
                java.io.ByteArrayInputStream,
                org.apache.commons.lang.StringEscapeUtils,
                org.apache.commons.codec.binary.Base64,
                org.apache.commons.io.FilenameUtils,
                java.util.Map,
                java.util.HashMap,
                org.apache.sling.api.SlingHttpServletRequest,
				com.day.cq.i18n.I18n"
%>
<%@page import="java.util.Locale,java.util.ResourceBundle"%>
<%@page import="com.adobe.fmdita.common.PathUtils,
                com.adobe.fmdita.common.DomUtils,
                com.adobe.fmdita.common.NodeUtils"
%>
<%@page import="org.apache.sling.api.resource.Resource,
                org.apache.sling.api.resource.ResourceResolver,
                org.apache.sling.api.resource.ResourceUtil,
                com.day.cq.dam.api.Asset,
                com.day.cq.dam.api.DamConstants,
                com.pwc.madison.core.constants.MadisonConstants"%>
<%@include file="/libs/foundation/global.jsp"%>

<%@page import="com.adobe.fmdita.common.MiscUtils" %>

<% final Locale pageLocale = currentPage.getLanguage(false); 
final ResourceBundle resourceBundle = slingRequest.getResourceBundle(pageLocale); 
I18n i18n = new I18n(resourceBundle); 
%>

<%!
  public String addLinkDesc(String sourceFile, Element ref, Element linkDesc) {

  String result = null;
  try {
    Document doc = linkDesc.getOwnerDocument();

    String refPath = ref.getAttribute("href");
    if (refPath != null) {
      String bookmark = "";
      int bookmarkPos = refPath.indexOf("#");
      String format = ref.getAttribute("format");
      if (refPath != null && format.equalsIgnoreCase("dita")) {
        if (bookmarkPos != -1) {
          bookmark = refPath.substring(bookmarkPos);
          refPath = refPath.substring(0, bookmarkPos);
        }
        refPath = FilenameUtils.removeExtension(refPath) + ".html";
        refPath += bookmark;
      }
      refPath = PathUtils.getAbsolutePath(sourceFile, refPath);
    }
    Attr attr = doc.createAttribute("href");
    attr.setValue(refPath);
    linkDesc.setAttributeNode(attr);

    String scope = ref.getAttribute("scope");
    if(scope != null) {
      attr = doc.createAttribute("data-scope");
      attr.setValue(scope);
      linkDesc.setAttributeNode(attr);
    }

    String type = ref.getAttribute("type");
    if(type != null) {
      attr = doc.createAttribute("data-type");
      attr.setValue(type);
      linkDesc.setAttributeNode(attr);
    }

    String format = ref.getAttribute("format");
    if(format != null) {
      attr = doc.createAttribute("data-format");
      attr.setValue(format);
      linkDesc.setAttributeNode(attr);
    }

    result = DomUtils.serializeToHTML(linkDesc);

  } catch(Exception e) {
    result = "";
  }
  return result;
}


public int getImageDimension(String imagePath, SlingHttpServletRequest slingRequest) {
    int dimension = 0;
    try {
        ResourceResolver resourceResolver = slingRequest.getResourceResolver();
        Resource resource = resourceResolver.getResource(imagePath);
        if(null == resource){
            return -1;
        }
        Asset asset = resource.adaptTo(Asset.class);
        if(null == asset){
            return -1;
        }
        dimension = Integer.valueOf(asset.getMetadataValue(DamConstants.TIFF_IMAGEWIDTH));
    } catch (Exception e) {
        dimension = 0;
    }
    return dimension;
}


public String addImage(String sourceFile, String href, String rendition, String mapName, Element rootElement,ValueMap properties, SlingHttpServletRequest slingRequest,int imageDimension) {


  String result = null;
  try {

    NodeList longdescrefs = DomUtils.getElementsByClassName("topic/longdescref", rootElement);
    Element ref = longdescrefs.getLength() > 0 ? (Element)longdescrefs.item(0) : null;
    String height = rootElement.getAttribute("height"), width = rootElement.getAttribute("width"), altText = rootElement.getAttribute("alt");

    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    Document doc = dBuilder.newDocument();

    Element linkDesc = null;
    Element img = doc.createElement("img");
    if(ref != null) {
      linkDesc = doc.createElement("a");
      doc.appendChild(linkDesc);
      addLinkDesc(sourceFile, ref, linkDesc);
      linkDesc.appendChild(img);
    } else {
      doc.appendChild(img);
    }
    Attr attr = doc.createAttribute("src");
    if (href != null) {
      href = PathUtils.getAbsolutePath(sourceFile, href);
    }
    if(!rendition.isEmpty()){
    	if(imageDimension >= MadisonConstants.MOBILE_MAX_WIDTH){
    		href = href + "/jcr:content/renditions/" + rendition;
        }
    }

    attr.setValue(href);
    img.setAttributeNode(attr);

    boolean addScaleFitClass = true;
	String heightNWidth = "";

	if (height != null && !height.isEmpty()) {
		heightNWidth = heightNWidth + "height: " + height + ";";
		addScaleFitClass = false;
	}
	if (width != null && !width.isEmpty()) {
		heightNWidth = heightNWidth + "width: " + width + ";";
		addScaleFitClass = false;
	}

	if (!heightNWidth.isEmpty()) {
		attr = doc.createAttribute("style");
		attr.setValue(heightNWidth);
		img.setAttributeNode(attr);
	}

    String scaleFit = rootElement.getAttribute("scalefit");
    addScaleFitClass &= scaleFit != null && scaleFit.equalsIgnoreCase("yes");

    if (mapName != null) {
      attr = doc.createAttribute("usemap");
      attr.setValue("#" + mapName);
      img.setAttributeNode(attr);
    }


    if(altText != null) {
      attr = doc.createAttribute("alt");
      attr.setValue(altText);
      img.setAttributeNode(attr);
    }

    Map<String, String> map = new HashMap<String, String>();
    map = MiscUtils.GetOutputAttributesMap(properties, slingRequest);
    for (Map.Entry<String, String> entry : map.entrySet()) {
      attr = doc.createAttribute(entry.getKey());
      attr.setValue(entry.getValue());
      img.setAttributeNode(attr);
    }
    if(addScaleFitClass) {
      String classAttr = img.getAttribute("class");
      classAttr = classAttr + " scalefit";
      img.setAttribute("class", classAttr);
    }

    result = DomUtils.serializeToHTML(linkDesc != null ? linkDesc : img);

  } catch(Exception e) {
    result = "";
  }
  return result;
}

%>

<%
  String href = properties.get("fileReference", "");
  String rendition = properties.get("rendition", "");
  String sourceFile = (String)request.getAttribute("sourceFile");
  String mapName = (String)request.getAttribute("mapName");
  String classVal = NodeUtils.getOutputClass(properties);
  Element rootElement = MiscUtils.getRootElement(properties);
  int imageDimension = getImageDimension(href,slingRequest);
%>
<%
  String image = addImage(sourceFile, href, rendition, mapName, rootElement, properties, slingRequest,imageDimension);

  String placement = rootElement.getAttribute("placement");
  String align =  rootElement.getAttribute("align"), alignStyle = "";
  boolean blockTag = placement.equalsIgnoreCase("break");
  if(align != null && !align.isEmpty()) {
    alignStyle = "style=\"text-align: " + align + "\"";
  }
  if(blockTag){
    %><div <%= alignStyle %> ><%
  }
  if(Integer.valueOf(imageDimension) >= MadisonConstants.MOBILE_MAX_WIDTH){
  %><a class="figure-image-anchor" href="javascript:void(0);" data-toggle="modal" data-target="#responsive-image-modal" id="image-modal-link">
		<cq:text value="<%= image %>"/>
        <span class="inline-img">
            <span class="icon-view-image"></span>
        </span>
 		<%= i18n.get("Common_Document_Body_View_Image") %>
	</a>
<%
  } else {
      %><div class="image"><cq:text value="<%= image %>"/></div><%
    }
  if(blockTag){
    %></div><%
  }
%>

