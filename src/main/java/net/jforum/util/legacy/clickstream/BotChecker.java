package net.jforum.util.legacy.clickstream;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import net.jforum.util.legacy.clickstream.config.ClickstreamConfig;
import net.jforum.util.legacy.clickstream.config.ConfigLoader;

/**
 * Determines if a request is actually a bot or spider.
 * 
 * @author <a href="plightbo@hotmail.com">Patrick Lightbody</a>
 * @author Rafael Steil (little hacks for JForum)
 */
public class BotChecker
{
	/**
	 * Checks if we have a bot
	 * @param request the request
	 * @return <code>null</code> if there is no bots in the current request, or the bot's name otherwise
	 */
	public static String isBot(HttpServletRequest request) 
	{
		if (request.getRequestURI().indexOf("robots.txt") != -1) {
			// there is a specific request for the robots.txt file, so we assume
			// it must be a robot (only robots request robots.txt)
			return "Unknown (asked for robots.txt)";
		}

		String userAgent = request.getHeader("User-Agent");

		ClickstreamConfig config = ConfigLoader.getInstance().getConfig();

		if (userAgent != null && config != null) {
			List<String> agents = config.getBotAgents();

			userAgent = userAgent.toLowerCase();

			for (String agent : agents) {
				if (agent == null) {
					continue;
				}

				if (userAgent.indexOf(agent) != -1) {
					return userAgent;
				}
			}
		}

		String remoteHost = request.getRemoteHost(); // requires a DNS lookup

		if (remoteHost != null && remoteHost.length() > 0 && remoteHost.charAt(remoteHost.length() - 1) > 64) {
			List<String> hosts = config.getBotHosts();

			remoteHost = remoteHost.toLowerCase();

			for (String host : hosts) {
				if (host == null) {
					continue;
				}

				if (remoteHost.indexOf(host) != -1) {
					return remoteHost;
				}
			}
		}

		return null;
	}
}
