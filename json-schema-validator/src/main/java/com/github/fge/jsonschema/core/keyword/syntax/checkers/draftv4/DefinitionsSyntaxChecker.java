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

package com.github.fge.jsonschema.core.keyword.syntax.checkers.draftv4;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.core.keyword.syntax.checkers.SyntaxChecker;
import com.github.fge.jsonschema.core.keyword.syntax.checkers.helpers.SchemaMapSyntaxChecker;
import com.github.fge.jsonschema.core.tree.SchemaTree;
import com.github.fge.msgsimple.bundle.MessageBundle;

/**
 * Syntax checker for draft v4's {@code definitions} keyword
 */
public final class DefinitionsSyntaxChecker
    extends SchemaMapSyntaxChecker
{
    private static final SyntaxChecker INSTANCE
        = new DefinitionsSyntaxChecker();

    public static SyntaxChecker getInstance()
    {
        return INSTANCE;
    }

    private DefinitionsSyntaxChecker()
    {
        super("definitions");
    }
    @Override
    protected void extraChecks(final ProcessingReport report,
        final MessageBundle bundle, final SchemaTree tree)
        throws ProcessingException
    {
    }
}
