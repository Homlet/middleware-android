package uk.ac.cam.seh208.middleware.common;

import junit.framework.Assert;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;


/**
 * Local test for the correctness of query filters.
 */
public class QueryTest {
    /**
     * Dummy endpoint details.
     */
    private static EndpointDetails epSchema = new EndpointDetails(
            "epSchema",
            null,
            null,
            "{\n" +
                    "    \"description\": \"A geographical coordinate\",\n" +
                    "    \"type\": \"object\",\n" +
                    "    \"properties\": {\n" +
                    "        \"latitude\": { \"type\": \"number\" },\n" +
                    "        \"longitude\": { \"type\": \"number\" }\n" +
                    "    }\n" +
                    "}",
            null
    );
    private static EndpointDetails epDesc1 = new EndpointDetails(
            "epDesc1",
            "An endpoint with a description.",
            null,
            null,
            null
    );
    private static EndpointDetails epDesc2 = new EndpointDetails(
            "epDesc2",
            "Another endpoint, having a different description.",
            null,
            null,
            null
    );
    private static EndpointDetails epSource = new EndpointDetails(
            "epSource",
            null,
            Polarity.SOURCE,
            null,
            null
    );
    private static EndpointDetails epSink = new EndpointDetails(
            "epSink",
            null,
            Polarity.SINK,
            null,
            null
    );
    private static EndpointDetails epTags1 = new EndpointDetails(
            "epTags1",
            null,
            null,
            null,
            Arrays.asList("green", "large", "frequent")
    );
    private static EndpointDetails epTags2 = new EndpointDetails(
            "epTags2",
            null,
            null,
            null,
            Collections.emptyList()
    );
    private static EndpointDetails epTags3 = new EndpointDetails(
            "epTags3",
            null,
            null,
            null,
            Arrays.asList("small", "infrequent")
    );

    /**
     * Groupings of the endpoint details into useful sets.
     */
    private static Set<EndpointDetails> epsTags
            = new HashSet<>(Arrays.asList(epTags1, epTags2, epTags3));
    private static Set<EndpointDetails> epsPolarity
            = new HashSet<>(Arrays.asList(epSource, epSink));
    private static Set<EndpointDetails> epsDesc
            = new HashSet<>(Arrays.asList(epDesc1, epDesc2));
    private static Set<EndpointDetails> epsAll
            = new HashSet<>(Arrays.asList(epSchema, epDesc1, epDesc2, epSource, epSink,
                                          epTags1, epTags2, epTags3));


    /**
     * Assert that the result of filtering a set of endpoints is the same as a given
     * hypothesis set (tests equality component-wise using the equals method).
     *
     * @param base Initial set to be filtered.
     * @param query Query to use for filtering.
     * @param hypothesis Asserted result set after filtering.
     */
    private static void assertFilter(Set<EndpointDetails> base, Query query,
                                     Set<EndpointDetails> hypothesis) {
        Assert.assertEquals(
                hypothesis,
                StreamSupport.stream(base)
                        .filter(query.getFilter())
                        .collect(Collectors.toSet())
        );
    }

    /**
     * Assert that a filter on a particular set of endpoints accepts exactly a given
     * number of endpoints.
     *
     * @param base Initial set to be filtered.
     * @param query Query to use for filtering.
     * @param hypothesis Asserted number of endpoints accepted.
     */
    private static void assertCount(Set<EndpointDetails> base, Query query,
                                    int hypothesis) {
        Assert.assertEquals(
                hypothesis,
                StreamSupport.stream(base)
                        .filter(query.getFilter())
                        .collect(Collectors.toSet())
                        .size()
        );
    }

    /**
     * Convenience function to create a set from an argument array of endpoint details.
     */
    private static Set<EndpointDetails> epSet(EndpointDetails... endpoints) {
        return new HashSet<>(Arrays.asList(endpoints));
    }


    @Test
    public void testNameRegex() {
        // Create some queries.
        Query q1 = new Query.Builder().setNameRegex(".*").build();
        Query q2 = new Query.Builder().setNameRegex("epTags[0-9]*").build();

        // Test that the queries filter out the correct endpoints from the set.
        assertFilter(epsAll, q1, epsAll);
        assertFilter(epsAll, q2, epsTags);
    }

    @Test
    public void testDescRegex() {
        // Create some queries.
        Query q1 = new Query.Builder().setDescRegex(".*description.*").build();

        // Test that the queries filter out the correct endpoints from the set.
        assertFilter(epsAll, q1, epsDesc);
    }

    @Test
    public void testSchema() {
        // Create some queries.
        Query q1 = new Query.Builder().setSchema("{\n" +
                "    \"description\": \"A geographical coordinate\",\n" +
                "    \"type\": \"object\",\n" +
                "    \"properties\": {\n" +
                "        \"latitude\": { \"type\": \"number\" },\n" +
                "        \"longitude\": { \"type\": \"number\" }\n" +
                "    }\n" +
                "}").build();

        // Test that the queries filter out the correct endpoints from the set.
        assertFilter(epsAll, q1, epSet(epSchema));
    }

    @Test
    public void testPolarity() {
        // Create some polarity-specific queries.
        Query q1 = new Query.Builder().setPolarity(Polarity.SOURCE).build();
        Query q2 = new Query.Builder().setPolarity(Polarity.SINK).build();

        // Test that the queries filter out the correct endpoints from the set.
        assertFilter(epsPolarity, q1, epSet(epSource));
        assertFilter(epsPolarity, q2, epSet(epSink));
    }

    @Test
    public void testIncludeTag() {
        // Create some tag-including queries.
        Query q1 = new Query.Builder().includeTag(null).build();
        Query q2 = new Query.Builder().includeTag("green").includeTag("large").build();
        Query q3 = new Query.Builder().includeTag("infrequent").build();

        // Test that the queries filter out the correct endpoints from the set.
        assertFilter(epsTags, q1, epsTags);
        assertFilter(epsTags, q2, epSet(epTags1));
        assertFilter(epsTags, q3, epSet(epTags3));
    }

    @Test
    public void testIncludeTags() {
        // Create some tag-including queries.
        Query q1 = new Query.Builder()
                .includeTags(Arrays.asList("green", "large")).build();
        Query q2 = new Query.Builder()
                .includeTags(Arrays.asList("green", "large", "frequent")).build();
        Query q3 = new Query.Builder()
                .includeTags(Arrays.asList("green", "infrequent")).build();
        Query q4 = new Query.Builder()
                .includeTags(Arrays.asList("infrequent", "green")).build();

        // Test that the queries filter out the correct endpoints from the set.
        assertFilter(epsTags, q1, epSet(epTags1));
        assertFilter(epsTags, q2, epSet(epTags1));
        assertFilter(epsTags, q3, epSet());
        assertFilter(epsTags, q4, epSet());
    }

    @Test
    public void testExcludeTag() {
        // Create some tag-excluding queries.
        Query q1 = new Query.Builder().excludeTag(null).build();
        Query q2 = new Query.Builder().excludeTag("green").excludeTag("large").build();
        Query q3 = new Query.Builder().excludeTag("infrequent").build();

        // Test that the queries filter out the correct endpoints from the set.
        assertFilter(epsTags, q1, epsTags);
        assertFilter(epsTags, q2, epSet(epTags2, epTags3));
        assertFilter(epsTags, q3, epSet(epTags1, epTags2));
    }

    @Test
    public void testExcludeTags() {
        // Create some tag-excluding queries.
        Query q1 = new Query.Builder()
                .excludeTags(Arrays.asList("green", "large")).build();
        Query q2 = new Query.Builder()
                .excludeTags(Arrays.asList("green", "large", "frequent")).build();
        Query q3 = new Query.Builder()
                .excludeTags(Arrays.asList("green", "infrequent")).build();
        Query q4 = new Query.Builder()
                .excludeTags(Arrays.asList("infrequent", "green")).build();

        // Test that the queries filter out the correct endpoints from the set.
        assertFilter(epsTags, q1, epSet(epTags2, epTags3));
        assertFilter(epsTags, q2, epSet(epTags2, epTags3));
        assertFilter(epsTags, q3, epSet(epTags2));
        assertFilter(epsTags, q4, epSet(epTags2));
    }

    @Test
    public void testMatches() {
        // Create some tag-including queries.
        Query q1 = new Query.Builder().setMatches(Query.MATCH_INDEFINITELY).build();
        Query q2 = new Query.Builder().setMatches(0).build();
        Query q3 = new Query.Builder().setMatches(1).build();

        // Test that the queries filter out the correct endpoints from the set.
        assertCount(epsAll, q1, epsAll.size());
        assertCount(epsAll, q2, 0);
        assertCount(epsAll, q3, 1);
        assertCount(epsAll, q3, 1);  // Test the the count is reset.
    }

    @Test
    public void testCopy() {
        // Create a query.
        Query q = new Query.Builder().setNameRegex("test").setMatches(1).build();

        // Create a copy of that query using the builder.
        Query qc = new Query.Builder().copy(q).build();

        // Test that the two queries are equal.
        Assert.assertEquals(q, qc);
    }
}
