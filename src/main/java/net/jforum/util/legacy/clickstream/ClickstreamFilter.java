package net.jforum.util.legacy.clickstream;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import net.jforum.util.preferences.ConfigKeys;

/**
 * The filter that keeps track of a new entry in the clickstream for <b>every request</b>.
 * 
 * @author <a href="plightbo@hotmail.com">Patrick Lightbody</a>
 * @author Rafael Steil (little hacks for JForum)
 * @version $Id$
 */
public class ClickstreamFilter implements Filter
{
	private static final Logger LOGGER = Logger.getLogger(ClickstreamFilter.class);

	/**
	 * Attribute name indicating the filter has been applied to a given request.
	 */
	private static final String FILTER_APPLIED = "_clickstream_filter_applied";

	/**
	 * Processes the given request and/or response.
	 * 
	 * @param request The request
	 * @param response The response
	 * @param chain The processing chain
	 * @throws IOException If an error occurs
	 * @throws ServletException If an error occurs
	 */
	@Override public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException,
			ServletException
	{
		// Ensure that filter is only applied once per request.
		if (request.getAttribute(FILTER_APPLIED) == null) {
			request.setAttribute(FILTER_APPLIED, Boolean.TRUE);
			
			final String bot = BotChecker.isBot((HttpServletRequest)request);
			
			if (bot != null && LOGGER.isDebugEnabled()) {
				LOGGER.debug("Found a bot: " + bot);
			}
			
			request.setAttribute(ConfigKeys.IS_BOT, Boolean.valueOf(bot != null));
		}
		
		// Pass the request on
		chain.doFilter(request, response);
	}

	/**
	 * Initializes this filter.
	 * 
	 * @param filterConfig The filter configuration
	 * @throws ServletException If an error occurs
	 */
	@Override public void init(final FilterConfig filterConfig) throws ServletException {
		// Do nothing
	}

	/**
	 * Destroys this filter.
	 */
	@Override public void destroy() {
		// Do nothing
	}
}