package com.pwc.madison.core.services.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jcr.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.mailer.MessageGateway;
import com.day.cq.mailer.MessageGatewayService;
import com.pwc.madison.core.services.MailSenderService;
import com.pwc.madison.core.userreg.Constants;
import com.pwc.madison.core.userreg.utils.UserRegUtil;
import com.pwc.madison.core.util.MadisonUtil;

@Component(service = MailSenderService.class)
public class MailSenderServiceImpl implements MailSenderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailSenderService.class);

    @Reference
    private ResourceResolverFactory resourceResolverFactory;
    @Reference
    private MessageGatewayService messageGatewayService;

    @Override
    public boolean sendMailWithEmailString(final String sendFrom, final String[] sendTo, final String subject,
            String body, final Map<String, String> emailParams) {
        boolean isMailSent = false;
        try {
            if (null != sendTo && sendTo.length > 0 && null != body) {
                MessageGateway<Email> messageGateway;
                HtmlEmail email = new HtmlEmail();

                // Setting Encoding to support all language characters
                email.setCharset(Constants.UTF_8_ENCODING);

                // set body
                if (null != emailParams)
                    body = StrSubstitutor.replace(body, emailParams);
                email.setHtmlMsg(body);

                // set sendTo
                List<InternetAddress> emailToList = new ArrayList<InternetAddress>();
                for (String contact : sendTo) {
                    InternetAddress internetAddress = new InternetAddress(contact);
                    emailToList.add(internetAddress);
                }
                email.setTo(emailToList);

                // set subject
                if (null != subject)
                    email.setSubject(subject);

                // set sendFrom
                if (null != sendFrom)
                    email.setFrom(sendFrom);

                messageGateway = messageGatewayService.getGateway(Email.class);
                messageGateway.send((Email) email);
                isMailSent = true;
            } else {
                LOGGER.info(
                        "MailSenderService : sendMailWithEmailString() : Email not sent! Either sendTo is null or body is null or sendTo list is empty");
            }
        } catch (AddressException addressException) {
            LOGGER.error(
                    "MailSenderService : sendMailWithEmailString() : Exception occured while creating address from sendTo Array : {}",
                    addressException);
        } catch (EmailException emailException) {
            LOGGER.error("MailSenderService : sendMailWithEmailString() : Exception occured while sending mail : {}",
                    emailException);
        }
        return isMailSent;
    }

    @Override
    public boolean sendMailWithEmailTemplate(final String sendFrom, final String[] sendTo, final String subject,
            final String emailTemplatePath, final Map<String, String> emailParams) {
        ResourceResolver resourceResolver = null;
        boolean isMailSent;
        try {
            resourceResolver = MadisonUtil.getResourceResolver(resourceResolverFactory, Constants.USERREG_ADMIN_SUB_SERVICE);
            String body = UserRegUtil.getStringFromNodePath(emailTemplatePath, resourceResolver.adaptTo(Session.class));
            isMailSent = sendMailWithEmailString(sendFrom, sendTo, subject, body, emailParams);
        } finally {
            if (null != resourceResolver) {
                resourceResolver.close();
            }
        }
        return isMailSent;
    }

}
