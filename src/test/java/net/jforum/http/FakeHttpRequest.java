/*
 * Copyright (c) 2003, Rafael Steil
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, 
 * with or without modification, are permitted provided 
 * that the following conditions are met:
 * 
 * 1) Redistributions of source code must retain the above 
 * copyright notice, this list of conditions and the 
 * following  disclaimer.
 * 2)  Redistributions in binary form must reproduce the 
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
 * Created on 04/12/2004 15:57:26
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;

/**
 * @author Rafael Steil
 */
public class FakeHttpRequest implements HttpServletRequest {
	private HttpSession session = new FakeHttpSession();
	private ConcurrentHashMap<String, Object> params = new ConcurrentHashMap<String, Object>();
	private Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();

	public String getAuthType() {
		return null;
	}

	public String getContextPath() {
		return "";
	}

	public Cookie[] getCookies() {
		return new Cookie[0];
	}

	public long getDateHeader(String name) {
		return 0;
	}

	public String getHeader(String name) {
		return null;
	}

	public Enumeration<String> getHeaderNames() {
		return null;
	}

	public Enumeration<String> getHeaders(String name) {
		return null;
	}

	public int getIntHeader(String name) {
		return 0;
	}

	public String getMethod() {
		return "dummy-method";
	}

	public String getPathInfo() {
		return null;
	}

	public String getPathTranslated() {
		return null;
	}

	public String getQueryString() {
		return null;
	}

	public String getRemoteUser() {
		return null;
	}

	public String getRequestedSessionId() {
		return null;
	}

	public String getRequestURI() {
		return "";
	}

	public StringBuffer getRequestURL() {
		return null;
	}

	public String getRequestId() {
		return "jforum2-"+System.currentTimeMillis();
	}

	public String getProtocolRequestId() {
		return "";
	}

	public ServletConnection getServletConnection() {
		return null;
	}

	public String getServletPath() {
		return null;
	}

	public HttpSession getSession() {
		return this.session;
	}

	public HttpSession getSession(boolean create) {
		return this.getSession();
	}

	public Principal getUserPrincipal() {
		return null;
	}

	public boolean isRequestedSessionIdFromCookie() {
		return false;
	}

	@Deprecated
	public boolean isRequestedSessionIdFromUrl() {
		return false;
	}

	public boolean isRequestedSessionIdFromURL() {
		return false;
	}

	public boolean isRequestedSessionIdValid() {
		return false;
	}

	public boolean isUserInRole(String role) {
		return false;
	}

	public Object getAttribute(String name) {
		return this.attributes.get(name);
	}

	public Enumeration<String> getAttributeNames() {
		return null;
	}

	public String getCharacterEncoding() {
		return null;
	}

	public int getContentLength() {
		return 0;
	}

	public String getContentType() {
		return null;
	}

	public ServletInputStream getInputStream() throws IOException {
		return null;
	}

	public String getLocalAddr() {
		return null;
	}

	public Locale getLocale() {
		return null;
	}

	public Enumeration<Locale> getLocales() {
		return null;
	}

	public String getLocalName() {
		return null;
	}

	public int getLocalPort() {
		return 0;
	}

	public String getParameter(String name) {
		return null;
	}

	public Map<String, String[]> getParameterMap() {
		return null;
	}

	public Enumeration<String> getParameterNames() {
		return this.params.keys();
	}

	public String[] getParameterValues(String name) {
		return new String[0];
	}

	public String getProtocol() {
		return null;
	}

	public BufferedReader getReader() throws IOException {
		return null;
	}

	@Deprecated
	public String getRealPath(String path) {
		return null;
	}

	public String getRemoteAddr() {
		return null;
	}

	public String getRemoteHost() {
		return null;
	}

	public int getRemotePort() {
		return 0;
	}

	public RequestDispatcher getRequestDispatcher(String path) {
		return null;
	}

	public String getScheme() {
		return null;
	}

	public String getServerName() {
		return null;
	}

	public int getServerPort() {
		return 0;
	}

	public boolean isSecure() {
		return false;
	}

	public void removeAttribute(String name) {
		this.attributes.remove(name);
	}

	public void setAttribute(String name, Object o) {
		this.attributes.put(name, o);
	}

	public void setCharacterEncoding(String enc) throws UnsupportedEncodingException {
	}

	@Override
	public Collection<Part> getParts()
		  throws IOException, ServletException
	{
		return new ArrayList<Part>();
	}

	@Override
	public Part getPart (String name)
            throws IOException, ServletException
	{
		return (Part) null;
	}

	@Override
	public <T extends HttpUpgradeHandler> T upgrade (Class<T> handlerClass)
		throws IOException, ServletException
	{
		return null;
	}

	@Override
	public void login (String username, String password)
		throws ServletException
	{
	}

	@Override
	public void logout()
		throws ServletException
	{
	}

	@Override
	public boolean authenticate (HttpServletResponse response)
		throws IOException, ServletException
	{
		return true;
	}

	@Override
	public String changeSessionId()
	{
		return new String();
	}

	@Override
	public ServletContext getServletContext()
	{
		return (ServletContext) null;
	}

	@Override
	public AsyncContext startAsync()
		throws IllegalStateException
	{
		return (AsyncContext) null;
	}

	@Override
	public AsyncContext startAsync (ServletRequest servletRequest, ServletResponse servletResponse)
		throws IllegalStateException
	{
		return (AsyncContext) null;
	}

	@Override
	public boolean isAsyncStarted()
	{
		return false;
	}

	@Override
	public boolean isAsyncSupported()
	{
		return false;
	}

	@Override
	public AsyncContext getAsyncContext()
	{
		return (AsyncContext) null;
	}

	@Override
	public DispatcherType getDispatcherType()
	{
		return (DispatcherType) null;
	}

	@Override
	public long getContentLengthLong()
	{
		return 0;
	}
}
