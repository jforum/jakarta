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
 * Created on 27/07/2007 15:10:51
 * 
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.search;

import net.jforum.dao.DataAccessDriver;
import net.jforum.entities.Post;
import net.jforum.exceptions.ForumException;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;

import org.apache.log4j.Logger;

/**
 * @author Rafael Steil
 */
public class LuceneContentCollector 
{
	private static final Logger LOGGER = Logger.getLogger(LuceneContentCollector.class);

	private LuceneSettings settings;

	public LuceneContentCollector (LuceneSettings settings)
	{
		this.settings = settings;
	}

	public List<Post> collect (SearchArgs args, ScoreDoc[] results, Query query) {
		try {
			int finalResultSize = Math.min(args.fetchCount(), results.length - args.startFrom());
			int[] postIds = new int[finalResultSize];
			//LOGGER.debug(String.format("collect: results=%d, args.fetchCount=%d, args.startFrom=%d",
			//				results.length, args.fetchCount(), args.startFrom()));

			IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(this.settings.directory()));
			StoredFields storedFields = searcher.storedFields();
			for (int docIndex = args.startFrom(), i = 0; 
					i < finalResultSize;
					docIndex++, i++) {
				ScoreDoc hit = results[docIndex];
				Document doc = storedFields.document(hit.doc);
				postIds[i] = Integer.parseInt(doc.get(SearchFields.Keyword.POST_ID));
			}
			return this.retrieveRealPosts(postIds, query);
		} catch (Exception e) {
			throw new ForumException(e.toString(), e);
		}		
	}

	private List<Post> retrieveRealPosts (int[] postIds, Query query) throws IOException, InvalidTokenOffsetsException
	{
		List<Post> posts = DataAccessDriver.getInstance().newLuceneDAO().getPostsData(postIds);

		for (Post post : posts) {
			QueryScorer scorer = new QueryScorer(query);

			// see also ContentSearchOperation.prepareForDisplay
			Formatter formatter = new SimpleHTMLFormatter("<span class='sr'>", "</span>");
			//Formatter formatter = new SimpleHTMLFormatter("<u><b><font color=\"red\">", "</font></b></u>");
			Highlighter highlighter = new Highlighter(formatter, scorer);

			// Highlight keyword in post text
			TokenStream tokenStream = this.settings.analyzer().tokenStream(
				SearchFields.Indexed.CONTENTS, new StringReader(post.getText()));

			String fragment = highlighter.getBestFragment(tokenStream, post.getText());
			post.setText(fragment != null ? fragment : post.getText());

			// Highlight keyword in post subject
			tokenStream = this.settings.analyzer().tokenStream(
					SearchFields.Indexed.SUBJECT, new StringReader(post.getSubject()));

			fragment = highlighter.getBestFragment(tokenStream, post.getSubject());
			post.setSubject(fragment != null ? fragment : post.getSubject());
		}
		//LOGGER.debug("retrieveRealPosts: postIds.length="+postIds.length+", posts.length="+posts.size());

		return posts;
	}
}
