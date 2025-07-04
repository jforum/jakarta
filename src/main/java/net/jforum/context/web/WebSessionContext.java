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
 * The JForum Project
 * http://www.jforum.net
 * 20.08.2006 18:42:11 
 */
package net.jforum.context.web;

import java.util.Enumeration;

import jakarta.servlet.http.HttpSession;

import net.jforum.context.SessionContext;

/**
 * @author SergeMaslyukov 
 * @version $Id$
 */
public class WebSessionContext implements SessionContext
{
	private transient final HttpSession httpSession;

	public WebSessionContext(final HttpSession httpSession)
	{
		this.httpSession = httpSession;
	}

	@Override public void setAttribute(final String name, final Object value)
	{
		httpSession.setAttribute(name, value);
	}

	@Override public void removeAttribute(final String name)
	{
		httpSession.removeAttribute(name);
	}

	@Override public Object getAttribute(final String name)
	{
		return httpSession.getAttribute(name);
	}

	@Override public String getId()
	{
		return httpSession.getId();
	}

	@Override public Enumeration<String> getAttributeNames()
	{
		return httpSession.getAttributeNames();
	}

	@Override public void invalidate()
	{
		httpSession.invalidate();
	}
}
