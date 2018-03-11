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

/**
 * Message sources
 *
 * <p>Message sources are the most low level component of the API. They are,
 * in essence, maps with key/value pairs of strings.</p>
 *
 * <p>Implementations of {@link com.github.fge.msgsimple.source.MessageSource}
 * should return {@code null} if no message is found for a given key.</p>
 *
 * <p>Two implementations are provided: one using a simple {@link java.util.Map}
 * as a backend, and another one for reading property files, either from the
 * classpath or from files on the filesystem.</p>
 */
package com.github.fge.msgsimple.source;