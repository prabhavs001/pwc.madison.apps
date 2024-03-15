package com.pwc.madison.core.models;

import com.pwc.madison.core.constants.MadisonConstants;
import com.pwc.madison.core.util.MadisonUtil;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Topic FAQ Tile Component's composite multifield content model represents a FAQ tile containing FAQ object's question, answer, image, related links path
 * and multiple guidance links.
 **/
@Model(adaptables = {Resource.class})
public class TopicFaqTileContent {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopicFaqTile.class);

    private static final String FAQ_OBJECT_PATH_FROM_PAGE = "/jcr:content/root/maincontainer/readerrow/bodycontainer/docreader/contentbody/topicbody/pwc-faq/faq-body/faq-bodydiv";
    private static final String QUESTION_PATH_FROM_FAQ_OBJECT = "question-text";
    private static final String ANSWER_PATH_FROM_FAQ_OBJECT = "answer-text";
    private static final String IMAGE_PATH_FROM_FAQ_ANSWER = "fig";
    private static final String RELATED_LINKS_PATH_FROM_FAQ_OBJECT = "related-links";

    @OSGiService
    private ResourceResolverFactory resolverFactory;

    @ValueMapValue
    private String faqPath;

    private String questionPath, answerPath, imagePath, relatedLinksPath;

    @ChildResource(injectionStrategy = InjectionStrategy.OPTIONAL)
    private Resource guidanceLinks;

    private List<LinkField> guidanceLinkList = new ArrayList<>();

    /**
     * Initialize question, answer, image, related links path and guidance links,
     */
    @PostConstruct
    protected void init() {
        String temp = faqPath;
        faqPath += FAQ_OBJECT_PATH_FROM_PAGE;

        ResourceResolver resourceResolver = MadisonUtil.getResourceResolver(resolverFactory, MadisonConstants.MADISON_READ_SUB_SERVICE);
        if (Objects.nonNull(resourceResolver)) {
            Resource faqResource = resourceResolver.getResource(faqPath);
            if (Objects.nonNull(faqResource)) {

                Resource questionResource = resourceResolver.getResource(faqResource, QUESTION_PATH_FROM_FAQ_OBJECT);
                if (Objects.nonNull(questionResource)) {
                    questionPath = questionResource.getPath();
                }

                Resource answerResource = resourceResolver.getResource(faqResource, ANSWER_PATH_FROM_FAQ_OBJECT);
                if (Objects.nonNull(answerResource)) {
                    answerPath = answerResource.getPath();

                    Resource imageResource = resourceResolver.getResource(answerResource, IMAGE_PATH_FROM_FAQ_ANSWER);
                    if (Objects.nonNull(imageResource)) {
                        imagePath = imageResource.getPath();
                    }
                }

                Resource relatedLinksResource = resourceResolver.getResource(faqResource, RELATED_LINKS_PATH_FROM_FAQ_OBJECT);
                if (Objects.nonNull(relatedLinksResource)) {
                    relatedLinksPath = relatedLinksResource.getPath();
                }

            }
        }

        LOGGER.debug("FAQ object's path: {}, Question path: {}, Answer path: {}, Image path: {}, Related links path: {}", new Object[]{faqPath, questionPath, answerPath, imagePath, relatedLinksPath});

        if (Objects.nonNull(guidanceLinks)) {
            for (Resource guidanceLinkResource : guidanceLinks.getChildren()) {
                LinkField guidanceLink = guidanceLinkResource.adaptTo(LinkField.class);
                if (Objects.nonNull(guidanceLink)) {
                    guidanceLinkList.add(guidanceLink);
                }
            }
        }

        resourceResolver.close();
        faqPath = temp + ".html";
    }

    /**
     * @return question resource's path, null if question node doesn't exist
     */
    public String getQuestionPath() {
        return questionPath;
    }

    /**
     * @return answer resource's path, null if answer node doesn't exist
     */
    public String getAnswerPath() {
        return answerPath;
    }

    /**
     * @return image resource's path, null if image node doesn't exist
     */
    public String getImagePath() {
        return imagePath;
    }

    /**
     * @return relatedLinks resource's path, null if relatedLinks node doesn't exist
     */
    public String getRelatedLinksPath() {
        return relatedLinksPath;
    }

    /**
     * @return List of {@link LinkField} representing guidance links, empty list if nothing is authored
     */
    public List<LinkField> getGuidanceLinkList() {
        return guidanceLinkList;
    }

    public String getFaqPath() { return faqPath; }

}
