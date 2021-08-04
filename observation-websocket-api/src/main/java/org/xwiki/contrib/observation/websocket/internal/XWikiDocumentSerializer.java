/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.observation.websocket.internal;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
public class XWikiDocumentSerializer extends StdSerializer<XWikiDocument>
{
    /**
     * Default constructor.
     */
    public XWikiDocumentSerializer()
    {
        super(XWikiDocument.class);
    }

    @Override
    public void serialize(XWikiDocument value, JsonGenerator jgen, SerializerProvider provider) throws IOException
    {
        jgen.writeStartObject();
        jgen.writeObjectField("documentReference", value.getDocumentReference());
        jgen.writeObjectField("documentReferenceWithLocale", value.getDocumentReferenceWithLocale());
        jgen.writeObjectField("locale", value.getLocale());
        jgen.writeEndObject();
    }
}
