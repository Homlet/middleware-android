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

import com.github.fge.jsonschema.core.exceptions.ExceptionProvider;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.messages.JsonSchemaCoreMessageBundle;
import com.github.fge.jsonschema.core.report.MessageProvider;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.msgsimple.bundle.MessageBundle;
import com.github.fge.msgsimple.load.MessageBundles;

import javax.annotation.concurrent.Immutable;

/**
 * A processor chain
 *
 * <p>This class allows to build a chain out of different {@link Processor}
 * instances. Chaining two processors {@code p1} and {@code p2} only requires
 * that the output of {@code p1} be compatible with the input of {@code p2}.</p>
 *
 * <p>The result behaves like a processor itself, so it can be used in other
 * chains as well.</p>
 *
 * <p>Sample usage:</p>
 *
 * <pre>
 *     final Processor&lt;X, Y&gt; chain = ProcessorChain.startWith(p1)
 *         .chainWith(p2).chainWith(...).getProcessor();
 *
 *     // input is of type X
 *     final Y ret = chain.process(report, X);
 * </pre>
 *
 * <p>Note that <b>all instances are immutable</b>: each alteration of the chain
 * returns a <b>new chain</b>. This, for example, will not work:</p>
 *
 * <pre>
 *     final ProcessorChain&lt;X, Y&gt; chain = ProcessorChain.startWith(p1);
 *     chain.failOnError(); // WRONG!
 *     chain.getProcessor(); // Will return p1, not p1 with a stop condition
 * </pre>
 *
 * @param <IN> the input type for that chain
 * @param <OUT> the output type for that chain
 */
@Immutable
public final class ProcessorChain<IN extends MessageProvider, OUT extends MessageProvider>
{
    private static final MessageBundle BUNDLE
        = MessageBundles.getBundle(JsonSchemaCoreMessageBundle.class);

    /**
     * The resulting processor
     */
    private final Processor<IN, OUT> processor;

    /**
     * Start a processing chain with a single processor
     *
     * @param p the processor
     * @param <X> the input type
     * @param <Y> the output type
     * @return a single element processing chain
     * @throws NullPointerException processor is null
     */
    public static <X extends MessageProvider, Y extends MessageProvider>
        ProcessorChain<X, Y> startWith(final Processor<X, Y> p)
    {
        BUNDLE.checkNotNull(p, "processing.nullProcessor");
        return new ProcessorChain<X, Y>(p);
    }

    /**
     * Private constructor
     *
     * @param processor the processor
     */
    private ProcessorChain(final Processor<IN, OUT> processor)
    {
        this.processor = processor;
    }

    /**
     * Stop the processing chain on failure
     *
     * <p>Inserting this into a chain will stop the processing chain if the
     * previous processor ended up with an error (ie, {@link
     * ProcessingReport#isSuccess()} returns {@code false}).</p>
     *
     * @return a new chain
     */
    public ProcessorChain<IN, OUT> failOnError()
    {
        return failOnError(new ProcessingMessage()
            .setMessage(BUNDLE.getMessage("processing.chainStopped")));
    }

    /**
     * Stop the processing chain on failure
     *
     * <p>Inserting this into a chain will stop the processing chain if the
     * previous processor ended up with an error (ie, {@link
     * ProcessingReport#isSuccess()} returns {@code false}).</p>
     *
     * @param message the processing message to use
     * @return a new chain
     * @see ProcessingMessage#asException()
     * @see ProcessingMessage#setExceptionProvider(ExceptionProvider)
     */
    public ProcessorChain<IN, OUT> failOnError(final ProcessingMessage message)
    {
        final Processor<OUT, OUT> fail = new Processor<OUT, OUT>()
        {
            @Override
            public OUT process(final ProcessingReport report, final OUT input)
                throws ProcessingException
            {
                if (!report.isSuccess())
                    throw message.asException();
                return input;
            }
        };

        final ProcessorMerger<IN, OUT, OUT> merger
            = new ProcessorMerger<IN, OUT, OUT>(processor, fail);
        return new ProcessorChain<IN, OUT>(merger);
    }

    /**
     * Add a processor to the chain
     *
     * @param p the processor to add
     * @param <NEWOUT> the return type for that new processor
     * @return a new chain consisting of the previous chain with the new
     * processor appended
     * @throws NullPointerException processor to append is null
     */
    public <NEWOUT extends MessageProvider> ProcessorChain<IN, NEWOUT>
        chainWith(final Processor<OUT, NEWOUT> p)
    {
        BUNDLE.checkNotNull(p, "processing.nullProcessor");
        final Processor<IN, NEWOUT> merger
            = new ProcessorMerger<IN, OUT, NEWOUT>(processor, p);
        return new ProcessorChain<IN, NEWOUT>(merger);
    }

    public Processor<IN, OUT> getProcessor()
    {
        return processor;
    }

    private static final class ProcessorMerger<X extends MessageProvider,
        Y extends MessageProvider, Z extends MessageProvider>
        implements Processor<X, Z>
    {
        private final Processor<X, Y> p1;
        private final Processor<Y, Z> p2;

        private ProcessorMerger(final Processor<X, Y> p1,
            final Processor<Y, Z> p2)
        {
            this.p1 = p1;
            this.p2 = p2;
        }

        @Override
        public Z process(final ProcessingReport report, final X input)
            throws ProcessingException
        {
            final Y intermediate = p1.process(report, input);
            return p2.process(report, intermediate);
        }

        @Override
        public String toString()
        {
            return p1 + " -> " + p2;
        }
    }
}

