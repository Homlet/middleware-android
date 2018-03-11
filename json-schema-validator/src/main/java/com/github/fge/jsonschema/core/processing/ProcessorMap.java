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

package com.github.fge.jsonschema.core.processing;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.messages.JsonSchemaCoreMessageBundle;
import com.github.fge.jsonschema.core.report.MessageProvider;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.msgsimple.bundle.MessageBundle;
import com.github.fge.msgsimple.load.MessageBundles;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * {@link Map}-based processor selector, with an optional default processor
 *
 * <p>The processor produced by this class works as follows:</p>
 *
 * <ul>
 *     <li>a key, of type {@code K}, is computed from the processor input, of
 *     type {@code IN}, using a {@link Function};</li>
 *     <li>the processor then looks up this key in a {@link Map}, whose values
 *     are {@link Processor}s;</li>
 *     <li>if the key exists, the appropriate procesor is executed; otherwise,
 *     the default action is performed.</li>
 * </ul>
 *
 * <p>The default action depends on whether a default processor has been
 * supplied: if none exists, a {@link ProcessingException} is thrown.</p>
 *
 * <p>The {@link Function} used to extract a key from an input is the only
 * argument of the constructor. It cannot be null.</p>
 *
 * <p>Note that <b>null keys are not allowed</b>.</p>
 *
 * @param <K> the type of keys in the map
 * @param <IN> the input type of processors
 * @param <OUT> the output type of processors
 */
public final class ProcessorMap<K, IN extends MessageProvider, OUT extends MessageProvider>
{
    private static final MessageBundle BUNDLE
        = MessageBundles.getBundle(JsonSchemaCoreMessageBundle.class);

    private final Function<IN, K> keyFunction;
    /**
     * The map of processors
     */
    private final Map<K, Processor<IN, OUT>> processors = Maps.newHashMap();

    /**
     * The default processor
     */
    private Processor<IN, OUT> defaultProcessor = null;

    /**
     * Constructor
     *
     * @param keyFunction function to extract a key from an input
     * @throws NullPointerException key function is null
     */
    public ProcessorMap(final Function<IN, K> keyFunction)
    {
        BUNDLE.checkNotNull(keyFunction, "processing.nullFunction");
        this.keyFunction = keyFunction;
    }

    /**
     * Add an entry to the processor map
     *
     * @param key the key to match against
     * @param processor the processor for that key
     * @return this
     * @throws NullPointerException either the key or the processor are null
     */
    public ProcessorMap<K, IN, OUT> addEntry(final K key,
        final Processor<IN, OUT> processor)
    {
        BUNDLE.checkNotNull(key, "processing.nullKey");
        BUNDLE.checkNotNull(processor, "processing.nullProcessor");
        processors.put(key, processor);
        return this;
    }

    /**
     * Set the default processor if no matching key is found
     *
     * @param defaultProcessor the default processor
     * @return this
     * @throws NullPointerException processor is null
     */
    public ProcessorMap<K, IN, OUT> setDefaultProcessor(
        final Processor<IN, OUT> defaultProcessor)
    {
        BUNDLE.checkNotNull(defaultProcessor, "processing.nullProcessor");
        this.defaultProcessor = defaultProcessor;
        return this;
    }

    /**
     * Build the resulting processor from this map selector
     *
     * <p>The resulting processor is immutable: reusing a map builder after
     * getting the processor by calling this method will not alter the
     * processor you grabbed.</p>
     *
     * @return the processor for this map selector
     */
    public Processor<IN, OUT> getProcessor()
    {
        return new Mapper<K, IN, OUT>(processors, keyFunction,
            defaultProcessor);
    }

    private static final class Mapper<K, IN extends MessageProvider, OUT extends MessageProvider>
        implements Processor<IN, OUT>
    {
        private final Map<K, Processor<IN, OUT>> processors;
        private final Function<IN, K> f;
        private final Processor<IN, OUT> defaultProcessor;

        private Mapper(final Map<K, Processor<IN, OUT>> processors,
            final Function<IN, K> f, final Processor<IN, OUT> defaultProcessor)
        {
            this.processors = ImmutableMap.copyOf(processors);
            this.f = f;
            this.defaultProcessor = defaultProcessor;
        }

        @Override
        public OUT process(final ProcessingReport report, final IN input)
            throws ProcessingException
        {
            final K key = f.apply(input);
            Processor<IN, OUT> processor = processors.get(key);

            if (processor == null)
                processor = defaultProcessor;

            if (processor == null) // Not even a default processor. Ouch.
                throw new ProcessingException(new ProcessingMessage()
                    .setMessage(BUNDLE.getMessage("processing.noProcessor"))
                    .put("key", key));

            return processor.process(report, input);
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder("map[")
                .append(processors.size()).append(" entries with ");
            if (defaultProcessor == null)
                sb.append("no ");
            return sb.append("default processor]").toString();
        }
    }
}

