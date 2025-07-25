/*
 * Copyright (c) JForum Team
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, 
 * with or without modification, are permitted provided 
 * that the following conditions are met:
 * 
 * 1) Redistributions of source code must retain the above 
 * copyright notice, this list of conditions and the 
 * following disclaimer.
 * 2) Redistributions in binary form must reproduce the 
 * above copyright notice, this list of conditions and 
 * the following disclaimer in the documentation and/or 
 * other materials provided with the distribution.
 * 3) Neither the name of "Rafael Steil" nor 
 * the names of its contributors may be used to endorse 
 * or promote products derived from this software without 
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT 
 * HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, 
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL 
 * THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE 
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER 
 * IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN 
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 * 
 * Created on 03/03/2004 - 20:29:45
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.util.mail;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.mail.Address;
import jakarta.mail.Header;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.event.TransportEvent;
import jakarta.mail.event.TransportListener;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.simplejavamail.utils.mail.dkim.DkimMessage;
import org.simplejavamail.utils.mail.dkim.DkimSigner;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.jforum.JForumExecutionContext;
import net.jforum.entities.User;
import net.jforum.exceptions.MailException;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;
import net.jforum.util.stats.StatsEvent;

/**
 * Dispatch emails to the world. 
 * 
 * @author Rafael Steil
 */
public class Spammer
{
	private static final Logger LOGGER = Logger.getLogger(Spammer.class);

	private static final int MESSAGE_HTML = 0;
	private static final int MESSAGE_TEXT = 1;

	// the regex looks for a property in form "prop_key.name=propValue"
	private static final Pattern EXTRA_PROPS_PATTERN = Pattern.compile("([\\p{IsAlphabetic}0-9_\\-\\.]*)=(.*)$");

	private static String signingDomain = SystemGlobals.getValue(ConfigKeys.MAIL_DKIM_SIGNING_DOMAIN);
	private static String selector = SystemGlobals.getValue(ConfigKeys.MAIL_DKIM_SELECTOR);
	private static String derFile = SystemGlobals.getValue(ConfigKeys.MAIL_DKIM_DER_FILE);
	private static String identity = SystemGlobals.getValue(ConfigKeys.MAIL_DKIM_IDENTITY);

	private int messageFormat;
	private Session session;
	private String username;
	private String password;

	private Properties mailProps = new Properties();
	private MimeMessage message;
	private List<User> users = new ArrayList<>();
	private String messageId;
	private String inReplyTo;
	private boolean needCustomization;
	private SimpleHash templateParams;
	private Template template;

	protected Spammer() throws MailException
	{
		final boolean ssl = SystemGlobals.getBoolValue(ConfigKeys.MAIL_SMTP_SSL);

		final String hostProperty = this.hostProperty(ssl);
		final String portProperty = this.portProperty(ssl);
		final String authProperty = this.authProperty(ssl);
		final String localhostProperty = this.localhostProperty(ssl);

		mailProps.put(hostProperty, SystemGlobals.getValue(ConfigKeys.MAIL_SMTP_HOST));
		mailProps.put(portProperty, SystemGlobals.getValue(ConfigKeys.MAIL_SMTP_PORT));

		String localhost = SystemGlobals.getValue(ConfigKeys.MAIL_SMTP_LOCALHOST);
		
		if (StringUtils.isNotEmpty(localhost)) {
			LOGGER.debug("localhost="+localhost);
			mailProps.put(localhostProperty, localhost);
		}
		
		mailProps.put("mail.mime.charset", SystemGlobals.getValue(ConfigKeys.MAIL_CHARSET));
		mailProps.put(authProperty, SystemGlobals.getValue(ConfigKeys.MAIL_SMTP_AUTH));
		applyExtraMailProperties();

		username = SystemGlobals.getValue(ConfigKeys.MAIL_SMTP_USERNAME);
		password = SystemGlobals.getValue(ConfigKeys.MAIL_SMTP_PASSWORD);

		messageFormat = SystemGlobals.getValue(ConfigKeys.MAIL_MESSAGE_FORMAT).trim().equals("html") 
			? MESSAGE_HTML
			: MESSAGE_TEXT;

		this.session = Session.getInstance(mailProps);
	}

	/**
	 * Check if there any extra mail parameters to be applied to mailProps before attempting to
	 * connect to the mail server. Uses a regex matcher to avoid malformed strings.
	 */
	private void applyExtraMailProperties() {
		String mailProperties = SystemGlobals.getValue(ConfigKeys.MAIL_SMTP_ADDITIONAL_PROPERTIES);

		// if there are extra SMTP parameters to process
		if (!StringUtils.isEmpty(mailProperties)) {
			String[] propKeyVals = mailProperties.split(",");
			for (String keyVal : propKeyVals) {
				Matcher keyValMatcher = EXTRA_PROPS_PATTERN.matcher(keyVal);
				if (keyValMatcher.matches()) {
					String key = keyValMatcher.group(1).trim();
					String value = keyValMatcher.group(2).trim();
					LOGGER.debug("Additional SMTP property: " + key + "=" + value);
					mailProps.put(key, value);
				} else {
					LOGGER.warn("Property " + keyVal + " is not valid");
				}
			}
		}
	}

	public boolean dispatchMessages()
	{
        try
        {
            int sendDelay = SystemGlobals.getIntValue(ConfigKeys.MAIL_SMTP_DELAY);

			if (SystemGlobals.getBoolValue(ConfigKeys.MAIL_SMTP_AUTH)) {
				if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
                	boolean ssl = SystemGlobals.getBoolValue(ConfigKeys.MAIL_SMTP_SSL);

                    Transport transport = this.session.getTransport(ssl ? "smtps" : "smtp");
					/*
					transport.addTransportListener(new TransportListener() {
						@Override
						public void messagePartiallyDelivered (TransportEvent e) {
	                       	LOGGER.debug("messagePartiallyDelivered "+e.toString());
						}

						@Override
						public void messageNotDelivered (TransportEvent e) {
	                       	LOGGER.debug("messageNotDelivered "+e.toString());
						}

						@Override
						public void messageDelivered (TransportEvent e) {
	                       	LOGGER.debug("messageDelivered "+e.toString());
						}
					});
					*/

                    try {
	                    String host = SystemGlobals.getValue(ConfigKeys.MAIL_SMTP_HOST);

	                    transport.connect(host, username, password);

	                    if (transport.isConnected()) {
	                        for (User user : this.users) {
	                        	if (this.needCustomization) {
	                        		this.defineUserMessage(user);
	                        	}

	                        	if (StringUtils.isNotEmpty(user.getEmail())) {
	                        		Address address = new InternetAddress(user.getEmail());	                        	
	                        		LOGGER.debug("Sending mail to: " + user.getEmail());	                        	
	                        		this.message.setRecipient(Message.RecipientType.TO, address);	                            
									this.message.saveChanges();
									new StatsEvent("Sent email", user.getEmail()).record();

									//Enumeration headers = this.message.getAllHeaders();
									//while (headers.hasMoreElements()) {
									//	Header hdr = (Header) headers.nextElement();
									//	LOGGER.debug(hdr.getName()+"="+hdr.getValue());
									//}

									// no point in trying DKIM if the relevant properties have not been set
									MimeMessage msg = this.message;
									if (StringUtils.isNotEmpty(signingDomain) && StringUtils.isNotEmpty(derFile)) {
										try {
											DkimSigner dkimSigner = new DkimSigner(signingDomain, selector, new File(derFile));
											dkimSigner.setIdentity(identity);
											/*
											dkimSigner.setHeaderCanonicalization(Canonicalization.SIMPLE);
											dkimSigner.setBodyCanonicalization(Canonicalization.RELAXED);
											dkimSigner.setSigningAlgorithm(SigningAlgorithm.SHA256_WITH_RSA);
											dkimSigner.setLengthParam(true);
											dkimSigner.setCopyHeaderFields(false);
											*/
											msg = new DkimMessage(msg, dkimSigner);
										} catch (Exception ex) {
											LOGGER.warn("Can't use DKIM: "+ex.getMessage());
										}
									}

	                        		transport.sendMessage(msg, new Address[] { address });
	                        	}
	                        	if (sendDelay > 0) {
		                        	try {
		                            	Thread.sleep(sendDelay);
		                            } catch (InterruptedException ie) {
		                            	LOGGER.error("Error while Thread.sleep." + ie, ie);
		                            }
	                        	}
	                        }
	                    }
                    } catch (Exception e) {
                    	throw new MailException(e);
                    }
                    finally {
                    	try { transport.close(); } catch (Exception e) { LOGGER.error(e); }
                    }
                }
            } else {
            	for (User user : this.users) {
                	if (this.needCustomization) {
                		this.defineUserMessage(user);
                	}

                	if (StringUtils.isNotEmpty(user.getEmail())) {
                		Address address = new InternetAddress(user.getEmail());
                		LOGGER.debug("Sending mail to: " + user.getEmail());
                		this.message.setRecipient(Message.RecipientType.TO,address);
						this.message.saveChanges();
						new StatsEvent("Sent email", user.getEmail()).record();
                		Transport.send(this.message, new Address[] { address });
                	}
                    if (sendDelay > 0) {
	                    try {
	                    	Thread.sleep(sendDelay);
	                    } catch (InterruptedException ie) {
	                    	LOGGER.error("Error while Thread.sleep." + ie, ie);
	                    }
                    }
                }
            }
        }
        catch (MessagingException e) {
            LOGGER.error("Error while dispatching the message. " + e, e);
        }

        return true;
	}

	private void defineUserMessage(final User user)
	{
		try {
			this.templateParams.put("user", user);

			String text = this.processTemplate();
			int oldMessageFormat = messageFormat;
			if (user.notifyText()) {
				messageFormat = MESSAGE_HTML;
			}
			this.defineMessageText(text);
			messageFormat = oldMessageFormat;
		}
		catch (Exception e) {
			throw new MailException(e);
		}
	}

	/**
	 * Prepares the mail message for sending.
	 * 
	 * @param subject the subject of the email
	 * @param messageFile the path to the mail message template
	 * @throws MailException
	 */
	protected void prepareMessage(final String subject, final String messageFile) throws MailException
	{
		if (this.messageId != null) {
			this.message = new IdentifiableMimeMessage(session);
			((IdentifiableMimeMessage)this.message).setMessageId(this.messageId);
		} else {
			this.message = new MimeMessage(session);
		}

		this.templateParams.put("forumName", SystemGlobals.getValue(ConfigKeys.FORUM_NAME));

		try {
			this.message.setSentDate(new Date());
			this.message.setFrom(new InternetAddress(SystemGlobals.getValue(ConfigKeys.MAIL_SENDER)));
			this.message.setSubject(subject, SystemGlobals.getValue(ConfigKeys.MAIL_CHARSET));

			if (this.inReplyTo != null) {
				this.message.addHeader("In-Reply-To", this.inReplyTo);
			}

			this.createTemplate(messageFile);
			this.needCustomization = this.isCustomizationNeeded();

			// If we don't need to customize any part of the message, 
			// then build the generic text right now
			if (!this.needCustomization) {
				String text = this.processTemplate();
				this.defineMessageText(text);
			}
		} catch (Exception e) {
			throw new MailException(e);
		}
	}
	
	/**
	 * Set the text contents of the email we're sending
	 * @param text the text to set
	 * @throws MessagingException
	 */
	private void defineMessageText(final String text) throws MessagingException
	{
		String charset = SystemGlobals.getValue(ConfigKeys.MAIL_CHARSET);

		if (messageFormat == MESSAGE_HTML) {
			this.message.setContent(text.replaceAll("\n", "<br>"), "text/html; charset=" + charset);
		} else {
			this.message.setText(text, charset, "text/plain");
		}
	}

	/**
	 * Gets the message text to send in the email.
	 * 
	 * @param messageFile The optional message file to load the text. 
	 * @throws Exception
	 */
	protected void createTemplate(final String messageFile) throws IOException
	{
		String templateEncoding = SystemGlobals.getValue(ConfigKeys.MAIL_TEMPLATE_ENCODING);

		if (StringUtils.isEmpty(templateEncoding)) {
			this.template = JForumExecutionContext.getTemplateConfig().getTemplate(messageFile);
		}
		else {
			this.template = JForumExecutionContext.getTemplateConfig().getTemplate(messageFile, templateEncoding);
		}
	}

	/**
	 * Merge the template data, creating the final content.
	 * This method should only be called after {@link #createTemplate(String)}
	 * and {@link #setTemplateParams(SimpleHash)}
	 * 
	 * @return the generated content
	 * @throws IOException 
	 * @throws TemplateException 
	 */
	protected String processTemplate() throws TemplateException, IOException
	{
		StringWriter writer = new StringWriter();
		this.template.process(this.templateParams, writer);
		return writer.toString();
	}
	
	/**
	 * Set the parameters for the template being processed
	 * @param params the parameters to the template
	 */
	protected void setTemplateParams(SimpleHash params)
	{
		this.templateParams = params;
	}
	
	/**
	 * Check if we have to send customized emails
	 * @return true if there is a need for customized emails
	 */
	private boolean isCustomizationNeeded()
	{
		boolean need = false;
		
		for (User user : this.users) {
			if (user.notifyText()) {
				need = true;
				break;
			}
		}
		
		return need;
	}
	
	protected void setMessageId(final String messageId)
	{
		this.messageId = messageId;
	}
	
	protected void setInReplyTo(final String inReplyTo)
	{
		this.inReplyTo = inReplyTo;
	}
	
	protected void setUsers(final List<User> users)
	{
		this.users = users;
	}

	private String localhostProperty(final boolean ssl)
	{
		return ssl 
			? ConfigKeys.MAIL_SMTP_SSL_LOCALHOST
			: ConfigKeys.MAIL_SMTP_LOCALHOST;
	}

	private String authProperty(final boolean ssl)
	{
		return ssl 
			? ConfigKeys.MAIL_SMTP_SSL_AUTH
			: ConfigKeys.MAIL_SMTP_AUTH;
	}

	private String portProperty(final boolean ssl)
	{
		return ssl 
			? ConfigKeys.MAIL_SMTP_SSL_PORT
			: ConfigKeys.MAIL_SMTP_PORT;
	}

	private String hostProperty(final boolean ssl)
	{
		return ssl 
			? ConfigKeys.MAIL_SMTP_SSL_HOST
			: ConfigKeys.MAIL_SMTP_HOST;
	}
}
