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

import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.org.xhtmlrenderer.render.BlockBox;
import org.docx4j.org.xhtmlrenderer.render.InlineBox;
import org.docx4j.wml.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBElement;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class ContainerParseTest {
    private final String PNG_IMAGE_DATA = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAIAAAACAgMAAAAP2OW3AAAADFBMVEUDAP//AAAA/wb//AAD4Tw1AAAACXBIWXMAAAsTAAALEwEAmpwYAAAADElEQVQI12NwYNgAAAF0APHJnpmVAAAAAElFTkSuQmCC";

	private WordprocessingMLPackage wordMLPackage;

	@Before
	public void setup() throws Exception  {
		wordMLPackage = WordprocessingMLPackage.createPackage();
	}
	
	private List<Object> convert(String xhtml) throws Docx4JException {
        XHTMLImporterImpl XHTMLImporter = new XHTMLImporterImpl(wordMLPackage);		
		return XHTMLImporter.convert(xhtml, "");
	}	

	@Test
	public void testParagraphInParagraphLayout() throws Exception {
        String html = "<p><img src='" + PNG_IMAGE_DATA + "' height='16' width='19'/><img src='" + PNG_IMAGE_DATA + "' height='16' width='19'/>" +
                      "<p><img src='" + PNG_IMAGE_DATA + "' height='16' width='19'/><img src='" + PNG_IMAGE_DATA + "' height='16' width='19'/></p>" +
                         "<img src='" + PNG_IMAGE_DATA + "' height='16' width='19'/><img src='" + PNG_IMAGE_DATA + "' height='16' width='19'/></p>";
        List<Object> convert = convert(html);
        Assert.assertTrue(convert.size() == 3);
        for (Object o : convert) {
            Assert.assertTrue(o instanceof P);
            P paragraph = (P) o;
            List<Object> content = paragraph.getContent();
            Assert.assertTrue(content.size() == 2);
            for (Object child : content) {
                Assert.assertTrue(child instanceof R);
                R run = ((R)child);
                List<Object> rContent = run.getContent();
                Assert.assertTrue(rContent.size() == 1);
                Assert.assertTrue(rContent.get(0) instanceof Drawing);
            }
        }
	}

    @Test
	public void testParagraphInTableCellLayout() throws Exception {
        String html = "<table><tbody><tr>" +
                      "<td><img src='" + PNG_IMAGE_DATA + "' height='16' width='19'/><img src='" + PNG_IMAGE_DATA + "' height='16' width='19'/>" +
                      "<p><img src='" + PNG_IMAGE_DATA + "' height='16' width='19'/><img src='" + PNG_IMAGE_DATA + "' height='16' width='19'/></p>" +
                         "<img src='" + PNG_IMAGE_DATA + "' height='16' width='19'/><img src='" + PNG_IMAGE_DATA + "' height='16' width='19'/></td></tr></tbody></table>";
        List<Object> tConvert = convert(html);
        Assert.assertTrue(tConvert.size() == 1);
        for (Object t : tConvert) {
            Assert.assertTrue(t instanceof Tbl);
            Tbl table = (Tbl) t;
            List<Object> convert = ((Tc)((Tr)table.getContent().get(0)).getContent().get(0)).getContent();
            Assert.assertTrue(convert.size() == 3);
            for (Object o : convert) {
                Assert.assertTrue(o instanceof P);
                P paragraph = (P) o;
                List<Object> content = paragraph.getContent();
                Assert.assertTrue(content.size() == 2);
                for (Object child : content) {
                    Assert.assertTrue(child instanceof R);
                    R run = ((R)child);
                    List<Object> rContent = run.getContent();
                    Assert.assertTrue(rContent.size() == 1);
                    Assert.assertTrue(rContent.get(0) instanceof Drawing);
                }
            }
        }
	}

    @Test
    public void div() throws Exception {
        List<Object> convert = convert("<div>Content</div>");

        assertThat(convert.size() , is(1));
        P box = (P) convert.get(0);
        assertThat(box.getContent().size(), is(1));
        R r = (R) box.getContent().get(0);
        Text text = (Text) r.getContent().get(0);
        assertThat(text.getValue(), is("Content"));
    }

    @Test
    public void divPageInserted() throws Exception {
        String style = ".pageNo:after{" +
                "content: counter(page);" +
                "}";
        List<Object> converted = convert("<html><head><style>"+style+"</style></head><body><div class='pageNo'></div></body></html>");

        P p = (P)converted.get(0);

        CTSimpleField fldSimple = ((JAXBElement<CTSimpleField>) ((R)p.getContent().get(0)).getContent().get(0)).getValue();
        assertThat(fldSimple.getInstr(), is(" PAGE \\* MERGEFORMAT "));

        Text pageNumberText = (Text) ((R)fldSimple.getContent().get(0)).getContent().get(1);
        assertThat(pageNumberText.getValue(), is("999"));
    }

    @Test
    public void runningelementIgnored() throws Exception {
        List<Object> convert = convert("<div style='position:running(runningelement)'>Content</div>");

        assertThat(convert.size() , is(0));
    }

}
