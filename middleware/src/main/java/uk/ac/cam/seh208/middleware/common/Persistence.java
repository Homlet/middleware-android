package uk.ac.cam.seh208.middleware.common;

/**
 * Enumeration of supported mapping persistence levels. These determine
 * the length the middleware will go in attempting to reestablish
 * endpoint mappings after failure (any process that destroys a mapping
 * other than a graceful unmap command).
 */
public enum Persistence {
    /**
     * Indicate that the middleware should make no attempt to
     * reestablish the mapping.
     */
    NONE,

    /**
     * Indicate that, in the case that all remaining mappings resulting
     * from an initial command fail, the mapping should be reestablished
     * by repeating the initial command, with the original query.
     */
    RESEND_QUERY,

    /**
     * Indicate that, in the case that any individual mapping resulting
     * from an initial command fails, the initial command should be
     * repeated to reestablish that mapping, with a minimally modified query.
     *
     * The modifications to the query prevent an exponential explosion of
     * mappings when the original query artificially limited this number.
     * Whenever the original query specified a hard limit on the number of
     * mappings, the modified query will specify that limit, minus the
     * number of remaining mappings. This ensures that the maximum number
     * of mappings stemming from the initial command remains constant.
     */
    RESEND_QUERY_INDIVIDUAL,

    /**
     * EXPERIMENTAL: Indicate that, in the case that any individual mapping
     * resulting from an initial command fails, that mapping should be
     * reestablished as soon as possible with the same endpoint on the
     * same middleware instance.
     */
    EXACT
}
