package com.pwc.madison.core.services;

import java.util.Map;

/**
 * The Interface MailSenderService allows to send mail.
 */
public interface MailSenderService {
    
    /**
     * Send mail to the given sentTo Email IDs with the given mail body.
     *
     * @param sendFrom {@link String} Name/email to in the from section of the mail. If the sendFrom is null, email ID configured in mailing
     *            service is used for from section by default
     * @param sendTo Array of email IDs to which the mail is to be sent
     * @param subject {@link String} the subject of the mail. If the subject is null, no subject is set for the mail
     * @param body {@link String} the body of the mail
     * @param emailParams {@link Map} Map of keys to value pair that are to be replaced in the given body. Ex: if the body contains the
     *            place holder like ${fname}, the ${fname} will be replaced with value of the 'fname' key in map. Null in case no place
     *            holders are to be replaced
     * @return true, if successful. If the body or sendTo Array is null or the sendTo is empty, mails are not sent and false is returned.
     */
    public boolean sendMailWithEmailString(final String sendFrom, final String[] sendTo, final String subject, String body,
            final Map<String, String> emailParams);
    
    /**
     * Send mail to the given sentTo Email IDs with the email template present in the given emailTemplatePath.
     *
     * @param sendFrom {@link String} Name/email to in the from section of the mail. If the sendFrom is null, email ID configured in mailing
     *            service is used for from section by default
     * @param sendTo Array of email IDs to which the mail is to be sent
     * @param subject {@link String} the subject of the mail. If the subject is null, no subject is set for the mail
     * @param emailTemplatePath {@link String} the path where the mail template is present
     * @param emailParams {@link Map} Map of keys to value pair that are to be replaced in the given body. Ex: if the email template
     *            contains the place holder like ${fname}, the ${fname} will be replaced with value of the 'fname' key in map.
     * @return true, if successful. If the emailTemplatePath or sendTo Array is null or the sendTo is empty or emailTemplatePath is not.
     *         Null in case no place holders are to be replaced found, mails are not sent and false is returned.
     */
    public boolean sendMailWithEmailTemplate(final String sendFrom, final String[] sendTo, final String subject,
            final String emailTemplatePath, final Map<String, String> emailParams);
    
}
