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
 * Created on 23/07/2007 15:58:30
 * 
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.search;

import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.ByteBuffersDirectory;

/**
 * @author Rafael Steil
 */
public class LuceneSettings
{
	private static final Logger LOGGER = Logger.getLogger(LuceneSettings.class);

	private Class<?> clazz;
	private Directory directory;

	public LuceneSettings (final Class<?> clazz)
	{
		this.clazz = clazz;
	}

	public void useRAMDirectory() throws IOException
	{
		this.directory = new ByteBuffersDirectory();
		final IndexWriterConfig conf = new IndexWriterConfig(analyzer()).setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
		final IndexWriter writer = new IndexWriter(this.directory, conf);
		writer.close();
	}

	public void useFSDirectory (final String indexDirectory) throws IOException
	{
		if (!DirectoryReader.indexExists(FSDirectory.open(Paths.get(indexDirectory)))) {
			this.createIndexDirectory(indexDirectory);
		}

		this.directory = FSDirectory.open(Paths.get(indexDirectory));
	}

	public void createIndexDirectory (final String directoryPath) throws IOException 
	{
		final FSDirectory fsDir = FSDirectory.open(Paths.get(directoryPath));	    
		final IndexWriterConfig conf = new IndexWriterConfig(analyzer()).setOpenMode(IndexWriterConfig.OpenMode.CREATE);
		final IndexWriter writer = new IndexWriter(fsDir, conf);
		writer.close();
	}

	public Directory directory()
	{
		return this.directory;
	}

	public Analyzer analyzer()
	{
		try {
			return (Analyzer) clazz.getDeclaredConstructor().newInstance();
		} catch (Exception ex) {
			LOGGER.error(ex.getMessage());
			return null;
		}
	}

	public String formatDateTime (final Date date)
	{
		return new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(date);
	}
}
