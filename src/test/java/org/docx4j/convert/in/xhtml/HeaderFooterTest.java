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

import org.apache.commons.io.FileUtils;
import org.docx4j.TextUtils;
import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.Parts;
import org.docx4j.relationships.Relationship;
import org.docx4j.wml.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HeaderFooterTest {

	private WordprocessingMLPackage wordMLPackage;
	
	@Before
	public void setup() throws InvalidFormatException {
		wordMLPackage = WordprocessingMLPackage.createPackage();
	}

	@Test public void testMarginBoxes() throws Docx4JException, IOException, URISyntaxException {
		String html = readFile("header_marginboxes.html");
		List<Object> objects = fromXHTML(html);

		Parts parts = wordMLPackage.getParts();
	}

	@Ignore
	@Test public void testRunningElements() throws Docx4JException, IOException, URISyntaxException {
		String html = readFile("header_runningelements.html");
		List<Object> objects = fromXHTML(html);

		Parts parts = wordMLPackage.getParts();
	}

	private String readFile(String path)
			throws IOException, URISyntaxException {
		URL resource = getClass().getClassLoader().getResource(path);
		File htmlFile = new File(resource.toURI());
		return FileUtils.readFileToString(htmlFile);
	}
	private List<Object> convert(String xhtml) throws Docx4JException {
        XHTMLImporterImpl XHTMLImporter = new XHTMLImporterImpl(wordMLPackage);		
		return XHTMLImporter.convert(xhtml, "");
	}

	private List<Object> fromXHTML(String xhtml) throws Docx4JException {

		List<Object> converted = convert(xhtml);
		System.out.println(XmlUtils.marshaltoString(converted.get(0), true, true));

		return converted;
	}

}
