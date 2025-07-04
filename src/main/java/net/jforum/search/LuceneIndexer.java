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
 * Created on 18/07/2007 17:18:41
 * 
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import net.jforum.dao.AttachmentDAO;
import net.jforum.dao.DataAccessDriver;
import net.jforum.entities.Attachment;
import net.jforum.entities.AttachmentInfo;
import net.jforum.entities.Post;
import net.jforum.exceptions.SearchException;
import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

/**
 * @author Rafael Steil
 */
public class LuceneIndexer
{
	private static final Logger LOGGER = Logger.getLogger(LuceneIndexer.class);

	private LuceneSettings settings;
	private Directory ramDirectory;
	private IndexWriter ramWriter;
	private int ramNumDocs;
	private List<NewDocumentAdded> newDocumentAddedList = new ArrayList<>();

	private AttachmentDAO attachDAO;
	private String attachDir = SystemGlobals.getValue(ConfigKeys.ATTACHMENTS_STORE_DIR);

	public LuceneIndexer(final LuceneSettings settings)
	{
		this.settings = settings;
		this.createRAMWriter();
		this.attachDAO = DataAccessDriver.getInstance().newAttachmentDAO();
	}

	public void watchNewDocuDocumentAdded(NewDocumentAdded newDoc)
	{
		this.newDocumentAddedList.add(newDoc);
	}

	public void batchCreate(final Post post)
	{
		synchronized (LOGGER) {
			try {
				final Document document = this.createDocument(post);
				if (document != null) {
					this.ramWriter.addDocument(document);
					this.flushRAMDirectoryIfNecessary();
				}
			}
			catch (IOException e) {
				throw new SearchException(e);
			}
		}
	}

	private void createRAMWriter()
	{
		try {
			if (this.ramWriter != null) {
				this.ramWriter.close();
			}

			this.ramDirectory = new ByteBuffersDirectory();
			final IndexWriterConfig conf = new IndexWriterConfig(this.settings.analyzer()).setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
			this.ramWriter = new IndexWriter(this.ramDirectory, conf);
			this.ramNumDocs = SystemGlobals.getIntValue(ConfigKeys.LUCENE_INDEXER_RAM_NUMDOCS);
		}
		catch (IOException e) {
			throw new SearchException(e);
		}
	}

	private void flushRAMDirectoryIfNecessary()
	{
		if (this.ramWriter.getDocStats().maxDoc >= this.ramNumDocs) {
			this.flushRAMDirectory();
		}
	}

	public void flushRAMDirectory()
	{
		synchronized (LOGGER) {
			IndexWriter writer = null;

			try {
				final IndexWriterConfig conf = new IndexWriterConfig(this.settings.analyzer()).setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
				writer = new IndexWriter(this.settings.directory(), conf);
				this.ramWriter.commit();
				this.ramWriter.close();
				writer.addIndexes(new Directory[] { this.ramDirectory });
				writer.forceMergeDeletes();

				this.createRAMWriter();
			}
			catch (IOException e) {
				throw new SearchException(e);
			}
			finally {
				if (writer != null) {
					try { 
						writer.commit(); 
						writer.close();

						this.notifyNewDocumentAdded();
					}
					catch (Exception e) {
						LOGGER.error(e.toString(), e);
					}
				}
			}
		}
	}

	public void create(final Post post)
	{
		synchronized (LOGGER) {
			IndexWriter writer = null;

			try {
				final IndexWriterConfig conf = new IndexWriterConfig(this.settings.analyzer()).setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
				writer = new IndexWriter(this.settings.directory(), conf);

				final Document document = this.createDocument(post);
				if (document != null) {
					writer.addDocument(document);

					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Indexed " + document);
					}
				}
			}
			catch (Exception e) {
				LOGGER.error(e.toString(), e);
			}
			finally {
				if (writer != null) {
					try {
						writer.commit();
						writer.close();

						this.notifyNewDocumentAdded();
					}
					catch (Exception e) {
						LOGGER.error(e.toString(), e);
					}
				}
			}
		}
	}

	public void update(final Post post)
	{
		if (this.performDelete(post)) {
			this.create(post);
		}
	}

	private Document createDocument(final Post post)
	{
		if (post.isModerate())
			return null;

		Document doc = new Document();

		doc.add(new TextField(SearchFields.Indexed.SUBJECT, post.getSubject(), Field.Store.NO));
		doc.add(new StringField(SearchFields.Keyword.POST_ID, String.valueOf(post.getId()), Field.Store.YES));
		doc.add(new StringField(SearchFields.Keyword.FORUM_ID, String.valueOf(post.getForumId()), Field.Store.YES));
		doc.add(new NumericDocValuesField(SearchFields.Keyword.FORUM_ID, post.getForumId()));
		doc.add(new StringField(SearchFields.Keyword.TOPIC_ID, String.valueOf(post.getTopicId()), Field.Store.YES));
		doc.add(new StringField(SearchFields.Keyword.USER_ID, String.valueOf(post.getUserId()), Field.Store.YES));
		doc.add(new NumericDocValuesField(SearchFields.Keyword.DATE, post.getTime().getTime()));
		doc.add(new StoredField(SearchFields.Keyword.DATE, post.getTime().getTime()));
		doc.add(new StringField(SearchFields.Keyword.TOPIC_TYPE, String.valueOf(post.getTopicType()), Field.Store.YES));

		// remove UBB tags so that searches for "quote" doesn't find posts that include a quote tag
		String text = post.getText();
		// remove [quote] and similar
		text = text.replaceAll("\\[[^\\]=/]+?\\]", "");
		// remove [/quote] and similar
		text = text.replaceAll("\\[/[^\\]]+?\\]", "");
		// replace [quote=foo bar] by "foo bar "
		text = text.replaceAll("\\[[^\\]=]+?=([^\\]]+?)\\]", "$1 ");
		// TODO: we should remove more (or all) BB tags
		doc.add(new TextField(SearchFields.Indexed.CONTENTS, text, Field.Store.NO));

		if (post.hasAttachments()) {
			for (Attachment att : attachDAO.selectAttachments(post.getId())) {
				AttachmentInfo info = att.getInfo();
				doc.add(new TextField(SearchFields.Indexed.CONTENTS, info.getComment(), Field.Store.NO));
				LOGGER.debug("indexing filename="+info.getPhysicalFilename()+", mimetype="+info.getMimetype());

				File f = new File(attachDir + File.separatorChar + info.getPhysicalFilename());
				try (InputStream is = new FileInputStream(f)) {
					if (info.getMimetype().startsWith("text")) {
						String contents = new BufferedReader(new InputStreamReader(is))
											.lines().collect(Collectors.joining("\n"));
						doc.add(new TextField(SearchFields.Indexed.CONTENTS, contents, Field.Store.NO));
					} else if (info.getMimetype().equals("application/pdf")) {
						PDDocument pdfDocument = Loader.loadPDF(f);
						StringWriter writer = new StringWriter();
						PDFTextStripper stripper = new PDFTextStripper();
						stripper.writeText(pdfDocument, writer);
						String contents = writer.getBuffer().toString();
						doc.add(new TextField(SearchFields.Indexed.CONTENTS, contents, Field.Store.NO));
						pdfDocument.close();
					}
				} catch (Exception ex) {
					LOGGER.error("indexing "+f.getName()+": " + ex.getMessage());
					ex.printStackTrace();
				}
			}
		}

		return doc;
	}

	private void notifyNewDocumentAdded()
	{
		for (NewDocumentAdded newDoc : newDocumentAddedList) {
			newDoc.newDocumentAdded();
		}
	}

	public void delete(final Post post)
	{
		this.performDelete(post);
	}

	private boolean performDelete(final Post post)
	{
		synchronized (LOGGER) {
			IndexWriter writer = null;
			boolean status = false;

			try {
				final IndexWriterConfig conf = new IndexWriterConfig(this.settings.analyzer()).setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
				writer = new IndexWriter(this.settings.directory(), conf);
				writer.deleteDocuments(new Term(SearchFields.Keyword.POST_ID, String.valueOf(post.getId())));
				status = true;
			}
			catch (IOException e) {
				LOGGER.error(e.toString(), e);
			}
			finally {
				if (writer != null) {
					try {
						writer.commit();
						writer.close();
						this.flushRAMDirectory();
					}
					catch (IOException e) {
						LOGGER.error(e.toString(), e);
					}
				}
			}

			return status;
		}
	}
}
