package uk.ac.cam.seh208.middleware;

import junit.framework.Assert;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.Polarity;
import uk.ac.cam.seh208.middleware.common.Query;


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
                base.stream()
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
                base.stream()
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

    /**
     * Convenience function to create an array of blank initialised queries.
     *
     * @param size The size of the array.
     *
     * @return an array of blank initialised queries.
     */
    private static Query[] makeQueryArray(int size) {
        Query[] queries = new Query[size];
        for (int i = 0; i < size; i++) {
            queries[i] = new Query();
        }
        return queries;
    }


    @Test
    public void testNameRegex() throws Exception {
        // Create some queries.
        Query[] queries = makeQueryArray(3);
        queries[0].setNameRegex(".*");
        queries[1].setNameRegex("epTags[0-9]*");
        queries[2].setNameRegex("");
        queries[2].unsetNameRegex();

        // Test that the queries filter out the correct endpoints from the set.
        assertFilter(epsAll, queries[0], epsAll);
        assertFilter(epsAll, queries[1], epsTags);
        assertFilter(epsAll, queries[2], epsAll);
    }

    @Test
    public void testDescRegex() throws Exception {
        // Create some queries.
        Query[] queries = makeQueryArray(2);
        queries[0].setDescRegex(".*description.*");
        queries[1].setDescRegex("");
        queries[1].unsetDescRegex();

        // Test that the queries filter out the correct endpoints from the set.
        assertFilter(epsAll, queries[0], epsDesc);
        assertFilter(epsAll, queries[1], epsAll);
    }

    @Test
    public void testSchema() throws Exception {
        // Create some queries.
        Query[] queries = makeQueryArray(2);
        queries[0].setSchema("{\n" +
                "    \"description\": \"A geographical coordinate\",\n" +
                "    \"type\": \"object\",\n" +
                "    \"properties\": {\n" +
                "        \"latitude\": { \"type\": \"number\" },\n" +
                "        \"longitude\": { \"type\": \"number\" }\n" +
                "    }\n" +
                "}");
        queries[1].setSchema("");
        queries[1].unsetSchema();

        // Test that the queries filter out the correct endpoints from the set.
        assertFilter(epsAll, queries[0], epSet(epSchema));
        assertFilter(epsAll, queries[1], epsAll);
    }

    @Test
    public void testPolarity() throws Exception {
        // Create some polarity-specific queries.
        Query[] queries = makeQueryArray(3);
        queries[0].setPolarity(Polarity.SOURCE);
        queries[1].setPolarity(Polarity.SINK);
        queries[2].setPolarity(Polarity.SINK);
        queries[2].unsetPolarity();

        // Test that the queries filter out the correct endpoints from the set.
        assertFilter(epsPolarity, queries[0], epSet(epSource));
        assertFilter(epsPolarity, queries[1], epSet(epSink));
        assertFilter(epsAll, queries[2], epsAll);
    }

    @Test
    public void testIncludeTag() throws Exception {
        // Create some tag-including queries.
        Query[] queries = makeQueryArray(3);
        queries[0].includeTag(null);
        queries[1].includeTag("green");
        queries[1].includeTag("large");
        queries[2].includeTag("infrequent");

        // Test that the queries filter out the correct endpoints from the set.
        assertFilter(epsTags, queries[0], epsTags);
        assertFilter(epsTags, queries[1], epSet(epTags1));
        assertFilter(epsTags, queries[2], epSet(epTags3));
    }

    @Test
    public void testIncludeTags() throws Exception {
        // Create some tag-including queries.
        Query[] queries = makeQueryArray(5);
        queries[1].includeTags(Arrays.asList("green", "large"));
        queries[2].includeTags(Arrays.asList("green", "large", "frequent"));
        queries[3].includeTags(Arrays.asList("green", "infrequent"));
        queries[4].includeTags(Arrays.asList("infrequent", "green"));

        // Test that the queries filter out the correct endpoints from the set.
        assertFilter(epsTags, queries[0], epsTags);
        assertFilter(epsTags, queries[1], epSet(epTags1));
        assertFilter(epsTags, queries[2], epSet(epTags1));
        assertFilter(epsTags, queries[3], epSet());
        assertFilter(epsTags, queries[4], epSet());
    }

    @Test
    public void testExcludeTag() throws Exception {
        // Create some tag-excluding queries.
        Query[] queries = makeQueryArray(3);
        queries[0].excludeTag(null);
        queries[1].excludeTag("green");
        queries[1].excludeTag("large");
        queries[2].excludeTag("infrequent");

        // Test that the queries filter out the correct endpoints from the set.
        assertFilter(epsTags, queries[0], epsTags);
        assertFilter(epsTags, queries[1], epSet(epTags2, epTags3));
        assertFilter(epsTags, queries[2], epSet(epTags1, epTags2));
    }

    @Test
    public void testExcludeTags() throws Exception {
        // Create some tag-excluding queries.
        Query[] queries = makeQueryArray(5);
        queries[1].excludeTags(Arrays.asList("green", "large"));
        queries[2].excludeTags(Arrays.asList("green", "large", "frequent"));
        queries[3].excludeTags(Arrays.asList("green", "infrequent"));
        queries[4].excludeTags(Arrays.asList("infrequent", "green"));

        // Test that the queries filter out the correct endpoints from the set.
        assertFilter(epsTags, queries[0], epsTags);
        assertFilter(epsTags, queries[1], epSet(epTags2, epTags3));
        assertFilter(epsTags, queries[2], epSet(epTags2, epTags3));
        assertFilter(epsTags, queries[3], epSet(epTags2));
        assertFilter(epsTags, queries[4], epSet(epTags2));
    }

    @Test
    public void testIgnoreTag() throws Exception {
        // Create some tag-ignoring queries.
        Query[] queries = makeQueryArray(2);
        queries[0].excludeTags(Arrays.asList("green", "large"));
        queries[0].ignoreTag("green");
        queries[0].ignoreTag("large");
        queries[1].includeTags(Arrays.asList("green", "large"));
        queries[1].ignoreTag("green");
        queries[1].ignoreTag("large");

        // Test that the queries filter out the correct endpoints from the set.
        assertFilter(epsTags, queries[0], epsTags);
        assertFilter(epsTags, queries[1], epsTags);
    }

    @Test
    public void testIgnoreTags() throws Exception {
        // Create some tag-ignoring queries.
        Query[] queries = makeQueryArray(3);
        queries[0].ignoreTags(Collections.emptyList());
        queries[1].excludeTags(Arrays.asList("green", "large"));
        queries[1].ignoreTags(Arrays.asList("green", "large"));
        queries[2].includeTags(Arrays.asList("green", "large"));
        queries[2].ignoreTags(Arrays.asList("green", "large"));

        // Test that the queries filter out the correct endpoints from the set.
        assertFilter(epsTags, queries[0], epsTags);
        assertFilter(epsTags, queries[1], epsTags);
        assertFilter(epsTags, queries[2], epsTags);
    }

    @Test
    public void testMatches() throws Exception {
        // Create some tag-including queries.
        Query[] queries = makeQueryArray(3);
        queries[0].setMatches(Query.MATCH_INDEFINITELY);
        queries[1].setMatches(0);
        queries[2].setMatches(1);

        // Test that the queries filter out the correct endpoints from the set.
        assertCount(epsAll, queries[0], epsAll.size());
        assertCount(epsAll, queries[1], 0);
        assertCount(epsAll, queries[2], 1);
        assertCount(epsAll, queries[2], 1);  // Test the the count is reset.
    }
}
