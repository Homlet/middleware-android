/*
 * Copyright (c) 2014, Francis Galiegue (fgaliegue@gmail.com)
 *
 * This software is dual-licensed under:
 *
 * - the Lesser General Public License (LGPL) version 3.0 or, at your option, any
 *   later version;
 * - the Apache Software License (ASL) version 2.0.
 *
 * The text of this file and of both licenses is available at the root of this
 * project or, if you have the jar distribution, in directory META-INF/, under
 * the names LGPL-3.0.txt and ASL-2.0.txt respectively.
 *
 * Direct link to the sources:
 *
 * - LGPL 3.0: https://www.gnu.org/licenses/lgpl-3.0.txt
 * - ASL 2.0: http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package com.github.fge.jsonschema.core.load;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonNodeReader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.load.configuration.LoadingConfiguration;
import com.github.fge.jsonschema.core.load.download.URIDownloader;
import com.github.fge.jsonschema.core.messages.JsonSchemaCoreMessageBundle;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.msgsimple.bundle.MessageBundle;
import com.github.fge.msgsimple.load.MessageBundles;
import com.google.common.io.Closer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

/**
 * Class to fetch JSON documents
 *
 * <p>This uses a map of {@link URIDownloader} instances to fetch the contents
 * of a URI as an {@link InputStream}, then tries and turns this content into
 * JSON using an {@link ObjectMapper}.</p>
 *
 * <p>Normally, you will never use this class directly.</p>
 *
 * @see SchemaLoader
 */
public final class URIManager
{
    private static final MessageBundle BUNDLE
        = MessageBundles.getBundle(JsonSchemaCoreMessageBundle.class);

    private final Map<String, URIDownloader> downloaders;

    private final JsonNodeReader reader;

    public URIManager()
    {
        this(LoadingConfiguration.byDefault());
    }

    public URIManager(final LoadingConfiguration cfg)
    {
        downloaders = cfg.getDownloaderMap();
        reader = cfg.getReader();
    }

    /**
     * Get the content at a given URI as a {@link JsonNode}
     *
     * @param uri the URI
     * @return the content
     * @throws NullPointerException provided URI is null
     * @throws ProcessingException scheme is not registered, failed to get
     * content, or content is not JSON
     */
    public JsonNode getContent(final URI uri)
        throws ProcessingException
    {
        BUNDLE.checkNotNull(uri, "jsonRef.nullURI");

        if (!uri.isAbsolute())
            throw new ProcessingException(new ProcessingMessage()
                .setMessage(BUNDLE.getMessage("refProcessing.uriNotAbsolute"))
                .put("uri", uri));

        final String scheme = uri.getScheme();

        final URIDownloader downloader = downloaders.get(scheme);

        if (downloader == null)
            throw new ProcessingException(new ProcessingMessage()
                .setMessage(BUNDLE.getMessage("refProcessing.unhandledScheme"))
                .putArgument("scheme", scheme).putArgument("uri", uri));

        final Closer closer = Closer.create();
        final InputStream in;

        try {
            in = closer.register(downloader.fetch(uri));
            return reader.fromInputStream(in);
        } catch (JsonMappingException e) {
            throw new ProcessingException(new ProcessingMessage()
                .setMessage(e.getOriginalMessage()).put("uri", uri));
        } catch (JsonParseException e) {
            throw new ProcessingException(new ProcessingMessage()
                .setMessage(BUNDLE.getMessage("uriManager.uriNotJson"))
                .putArgument("uri", uri)
                .put("parsingMessage", e.getOriginalMessage()));
        } catch (IOException e) {
            throw new ProcessingException(new ProcessingMessage()
                .setMessage(BUNDLE.getMessage("uriManager.uriIOError"))
                .putArgument("uri", uri)
                .put("exceptionMessage", e.getMessage()));
        } finally {
            try {
                closer.close();
            } catch (IOException ignored) {
                throw new IllegalStateException();
            }
        }
    }
}
