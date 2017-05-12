/*
 *  This file is part of the docx4j-ImportXHTML library.
 *
 *  Copyright 2011-2013, Plutext Pty Ltd, and contributors.
 *  Portions contributed before 15 July 2013 formed part of docx4j 
 *  and were contributed under ASL v2 (a copy of which is incorporated
 *  herein by reference and applies to those portions). 
 *   
 *  This library as a whole is licensed under the GNU Lesser General 
 *  Public License as published by the Free Software Foundation; 
    version 2.1.
    
    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library (see legals/LICENSE); if not, 
    see http://www.gnu.org/licenses/lgpl-2.1.html
    
 */
package org.docx4j.convert.in.xhtml;

import org.docx4j.TextUtils;
import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.relationships.Relationship;
import org.docx4j.wml.*;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBElement;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import static org.docx4j.wml.STPTabAlignment.RIGHT;
import static org.docx4j.wml.STPTabRelativeTo.MARGIN;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.*;

public class HyperlinkTest {

	private WordprocessingMLPackage wordMLPackage;
	
	@Before
	public void setup() throws InvalidFormatException {
		wordMLPackage = WordprocessingMLPackage.createPackage();
	}

	private List<Object> convert(String xhtml) throws Docx4JException {
        XHTMLImporterImpl XHTMLImporter = new XHTMLImporterImpl(wordMLPackage);		
		return XHTMLImporter.convert(xhtml, "");
	}

	private List<Object> fromXHTML(String content) throws Docx4JException {
		
		String followingSpanContent = "SPAN";
		String followingPContent = "NEXTP";
		List<Object> converted = convert("<div><p>" +content + "<span>" + followingSpanContent + "</span></p>" +
				"<p>" + followingPContent + "</p></div>");
		System.out.println(XmlUtils.marshaltoString(converted.get(0), true, true));
		
		// Address risk that hyperlink processing destroys following content
		testFollowingSpan(((P)converted.get(0)).getContent(),  followingSpanContent);
		testFollowingP((P)converted.get(1),  followingPContent);
		
		return converted;
	}

	private void testFollowingSpan(List<Object> contents, String followingSpanContent) {
		
		R lastRun = null;
		for (Object o : contents) {
			o = XmlUtils.unwrap(o);
			if (o instanceof R) {
				lastRun = (R)o;
			}
		}
		
		testContent(lastRun, R.class, followingSpanContent);
	}
	
	private void testFollowingP(P p, String followingPContent) {

		System.out.println(XmlUtils.marshaltoString(p, true, true));
		
		R r = (R) p.getContent().get(0);
		testContent(r, R.class, followingPContent);
	}
		
	@Test public void testHref() throws Docx4JException {
		String name = null;
		String href= "http://www.google.com";
		String content = "Google";
		List<Object> objects = fromXHTML(a( href,  name,  content));
		P p = (P)objects.get(0);
		P.Hyperlink h = (P.Hyperlink) p.getContent().get(0);
		
		testLink(h.getId(), href);
		
		// Test content
		testContent(h, P.Hyperlink.class, content);
	}

	@Test public void testHrefNoNameNoContent() throws Docx4JException {
		String name = null;
		String href= "http://www.google.com";
		String content = null;
		List<Object> objects = fromXHTML(a( href,  name,  content));
		P p = (P)objects.get(0);
		
		// No link
		assertEquals( p.getContent().get(0).getClass(), R.class);
	}

	@Test public void testHrefNameButNoContent() throws Docx4JException {
		String name = "anchor0";
		String href= "http://www.google.com";
		String content = null;
		List<Object> objects = fromXHTML(a( href,  name,  content));
		P p = (P)objects.get(0);
		
		// Test bookmark
		testBookmarkName(XmlUtils.unwrap(p.getContent().get(0)), name);
				
		// Test just bookmark start + end + span
		assertEquals(XmlUtils.unwrap(p.getContent().get(1)).getClass(), CTMarkupRange.class);
		assertTrue(p.getContent().size()==3);
	}
	
	@Test public void testNamedAnchorEmpty() throws Docx4JException {
		String name = "anchor1";
		String href= null;
		String content = null;
		List<Object> objects = fromXHTML(a( href,  name,  content));
		P p = (P)objects.get(0);
		
		// Test bookmark
		testBookmarkName(XmlUtils.unwrap(p.getContent().get(0)), name);
				
		// Test just bookmark start + end + span
		assertTrue(p.getContent().size()==3);
		assertEquals(XmlUtils.unwrap(p.getContent().get(1)).getClass(), CTMarkupRange.class);
	}

	@Test public void testNamedAnchorTargetCounter() throws Docx4JException {
		String anchor = "anchor";

		String pgNumContent = "content: leader(dotted) target-counter(attr(href), page, decimal)";
		String pgNumClass = "pageNum";
		String a = "<a href='#" + anchor + "' class='" + pgNumClass + "'></a>";

		String html = "<html><head><style>" +
				"."+pgNumClass+":after{"
				+pgNumContent+";}" +
				"</style></head><body>"+ a + "</body></html>";
		List<Object> converted = convert(html);
		System.out.println(XmlUtils.marshaltoString(converted.get(0), true, true));

		P p = (P)converted.get(0);

		R.Ptab tab = (R.Ptab) ((R)p.getContent().get(0)).getContent().get(0);
		assertThat(tab.getAlignment(), is(RIGHT));
		assertThat(tab.getRelativeTo(), is(MARGIN));

		CTSimpleField fldSimple = ((JAXBElement<CTSimpleField>) ((R)p.getContent().get(1)).getContent().get(0)).getValue();
		assertThat(fldSimple.getInstr(), is(" PAGEREF " + anchor + " \\* MERGEFORMAT "));

		Text pageNumberText = (Text) ((R)fldSimple.getContent().get(0)).getContent().get(1);
		assertThat(pageNumberText.getValue(), not(""));
	}

	@Test public void testNamedAnchorContent() throws Docx4JException {
		String name = "anchor2";
		String href= null;
		String content = "Google";
		List<Object> objects = fromXHTML(a( href,  name,  content));
		P p = (P)objects.get(0);
		
		// Test bookmark
		testBookmarkName(XmlUtils.unwrap(p.getContent().get(0)), name);
				
		// Test content - not hyperlinked
		R r = (R) p.getContent().get(1);
		testContent(r, R.class, content);
	}
	
	@Test public void testNamedAnchorInSpan() throws Docx4JException {

		String name = "anchor3";
		String href= null;
		String content = "Google";
		List<Object> objects = fromXHTML("<span>" + a( href,  name,  content) + "</span>");
		P p = (P)objects.get(0);
		
		// Test bookmark
		testBookmarkName(XmlUtils.unwrap(p.getContent().get(0)), name);
				
		// Test content - not hyperlinked
		R r = (R) p.getContent().get(1);
		testContent(r, R.class, content);
	}

	@Test public void testFull() throws Docx4JException {
		String name = "anchor4";
		String href= "http://www.google.com";
		String content = "Google";
		List<Object> objects = fromXHTML(a( href,  name,  content));
		P p = (P)objects.get(0);
		
		// Test bookmark
		testBookmarkName(XmlUtils.unwrap(p.getContent().get(0)), name);
		
		P.Hyperlink h = (P.Hyperlink) p.getContent().get(1);
		
		testLink(h.getId(), href);
		
		// Test content
		testContent(h, P.Hyperlink.class, content);
	}

	/**
	 * This test illustrates how Flying Saucer handles rich hyperlink content.
	 */
	@Test public void testRichContent() throws Docx4JException {
		
		String name = "anchor5";
		String href= "http://www.google.com";
		
		String followingSpanContent = "SPAN";
		String followingPContent = "NEXTP";
		List<Object> converted = convert("<div><p>" 
				+ "<a name='" + name + "' href='" + href + "' >Some <span>rich</span> <b>content</b></a>"
				+ "<span>" + followingSpanContent + "</span></p>" +
				"<p>" + followingPContent + "</p></div>");
		System.out.println(XmlUtils.marshaltoString(converted.get(0), true, true));
		
		// Address risk that hyperlink processing destroys following content
		testFollowingSpan(((P)converted.get(0)).getContent(),  followingSpanContent);
		testFollowingP((P)converted.get(1),  followingPContent);
		
		List<Object> objects = converted;
		P p = (P)objects.get(0);
		
		// Test bookmark
		testBookmarkName(XmlUtils.unwrap(p.getContent().get(0)), name);
		
		P.Hyperlink h = (P.Hyperlink) p.getContent().get(1);
		
		testLink(h.getId(), href);
		
		// Test content
		assertTrue(h.getContent().size()==4);

	}
	
	@Test public void testXmlPredefinedEntities() throws Exception {
		
		List<Object> converted = convert("<div><p><a href=\"#requirement897\">[R_897] &lt; &apos; Requirement 3 &lt; 2 &quot; done</a></p></div>");
		System.out.println(XmlUtils.marshaltoString(converted.get(0), true, true));
				
		List<Object> objects = converted;
		P p = (P)objects.get(0);
				
		P.Hyperlink h = (P.Hyperlink) p.getContent().get(0);
		
		// Test content
		Writer out = new StringWriter();
		TextUtils.extractText(h, out);
		out.close();				
		assertTrue(out.toString().equals("[R_897] < ' Requirement 3 < 2 \" done"));
		
	}
	
	@Test public void testRichContentTail() throws Exception {
		
		List<Object> converted = convert("<div><p><a href=\"#requirement897\">[R_897] <b>Requirement</b> 12</a></p></div>");
		System.out.println(XmlUtils.marshaltoString(converted.get(0), true, true));
				
		List<Object> objects = converted;
		P p = (P)objects.get(0);
				
		P.Hyperlink h = (P.Hyperlink) p.getContent().get(0);
				
		// Test content
		assertTrue(h.getContent().size()==3);

		Writer out = new StringWriter();
		TextUtils.extractText(h, out);
		out.close();				
		assertTrue(out.toString().equals("[R_897] Requirement 12"));
		
	}
	
	private void testBookmarkName(Object o, String name) {

		assertEquals(o.getClass(), CTBookmark.class);
		CTBookmark bookmark = (CTBookmark)o;
		assertEquals(bookmark.getName(), name);		
	}
	
	private void testLink(String relId, String href) {
		Relationship r = wordMLPackage.getMainDocumentPart().getRelationshipsPart().getRelationshipByID(relId);
		assertEquals(r.getTarget(), href);
	}
	
	private void testContent(Object o, Class clazz, String content) {
		
		assertEquals(o.getClass(), clazz);
		
		Object o2;
		if (o instanceof P.Hyperlink) {
			
			P.Hyperlink h = (P.Hyperlink) o;
			o2 = XmlUtils.unwrap(((R)h.getContent().get(0)).getContent().get(0));
			
		} else {

			R r = (R) o;
			o2 = XmlUtils.unwrap(r.getContent().get(0));
			
		}
		String runText = ((Text)o2).getValue();
		assertEquals(content, runText);	
		
		
	}
	
	private String a(String href, String name, String content) {
		
		String result;
		if (href==null) {
			result = "<a name='" + name + "' ";
		} else if (name ==null) {
			result = "<a href='" + href + "' ";			
		} else {
			result = "<a name='" + name + "' href='" + href + "' ";			
		}
		
		if (content==null) {
			result = result + "/>";
		} else {
			result = result + ">" + content + "</a>";			
		}
		return result;
	}

}
