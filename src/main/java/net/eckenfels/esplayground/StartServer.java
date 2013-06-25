package net.eckenfels.esplayground;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.datehistogram.DateHistogramFacet;
import org.elasticsearch.search.facet.terms.TermsFacet;
import org.elasticsearch.search.highlight.HighlightField;

import com.google.gwt.user.client.ui.RadioButton;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.VerticalLayout;

public class StartServer
{
    private static Client client;

    private static String getFieldorHighlite(SearchHit hit, String fieldName) {
        String value = null;
        Map<String, HighlightField> hf = hit.highlightFields();
        if (hf != null) {
            HighlightField field = hf.get(fieldName);
            if (field != null) {
                Text[] frags = field.getFragments();
                if (frags != null && frags.length > 0)
                    for(int f=0;f<frags.length;f++)
                    {
                        if (f > 0) value += " ... ";
                        value = frags[0].string();
                    }
            }
        }
        if (value == null)
            value = hit.getFields().get(fieldName).getValue();
        return value;
    }

    public static void startES()
    {
        System.out.println("starting ES...");
        // needs to run in background or better move to OSGi
        Node node = org.elasticsearch.node.NodeBuilder.nodeBuilder()
                .clusterName("websearch")
                .data(true).local(true).client(false)
                .settings(ImmutableSettings.builder()
                        .put("discovery.zen.ping.multicast.enabled", false)
                        .put("http.enabled", true)
                        //.put("name", "eckenfels01")
                        )
                        .node();
        client = node.client();
    }

    public static void searchFor(String value, VerticalLayout leftLayout, VerticalLayout resultLayout)
    {
        leftLayout.removeAllComponents();
        resultLayout.removeAllComponents();
        ListenableActionFuture<SearchResponse> req = client.prepareSearch("kb")
                .setSearchType(SearchType.DFS_QUERY_AND_FETCH)
                .addHighlightedField("title").addHighlightedField("summary")
                .setQuery(QueryBuilders.queryString(value).field("title",2f).field("summary",1.2f).field("body").lenient(true))
                .addFields("title", "summary", "postDate", "category")
                .addFacet(FacetBuilders.termsFacet("cat").field("category").size(5))
                .addFacet(FacetBuilders.dateHistogramFacet("date").field("postDate").interval("quarter"))
                .setExplain(false).setSize(20)
                .execute();

        System.out.println("submitted");

        SearchResponse result = null;
        try { 
            result = req.get(3, TimeUnit.SECONDS); 
            if (result == null) 
            {
                resultLayout.addComponent(new Label("<font color=\"red\">No Results.</font>", ContentMode.HTML));
                System.out.println("Null result");
                return;
            }
        } catch (ExecutionException ee) {
            Throwable cause = ee.getCause();
            resultLayout.addComponent(new Label("<font color=\"red\">Error...</font> Cause=<pre>" + cause + "</pre>", ContentMode.HTML));
            System.out.println("Exception " + ee.getMessage() + " -> " + ee);
            return;
        } catch (Exception ex) { 
            resultLayout.addComponent(new Label("<font color=\"red\">Error...</font> Cause=<pre>" + ex + "</pre>", ContentMode.HTML));
            System.out.println("Exception " + ex);
            return;
        }

        SearchHits hits = result.getHits();
        resultLayout.addComponent(new Label("Took " + result.getTookInMillis()+"ms on " + result.getSuccessfulShards() + " of " + result.getTotalShards() + " shards. Found " + hits.getTotalHits() + " hits.<br/><br/>", ContentMode.HTML));

        // TOOD: count 0 -> use default facets
        
        for(SearchHit hit : hits.getHits())
        {
            resultLayout.addComponent(new Label(" <small>(" + hit.getScore() + " " + hit.getType() + ")</small> " + getFieldorHighlite(hit, "title"), ContentMode.HTML));
            resultLayout.addComponent(new Label(getFieldorHighlite(hit, "summary") + "<br/><br/>", ContentMode.HTML));
        }

        TermsFacet facet = (TermsFacet)result.getFacets().getFacets().get("cat");
        if (facet != null)
        {
            System.out.println("Facet " + facet.getTotalCount() + " " + facet.getOtherCount() + " " + facet.getMissingCount());
            OptionGroup og = new OptionGroup("Category");
            og.setStyleName("search-facet-category");
            String all = "All (" + facet.getTotalCount() + ")";
            og.addItem(all);
            og.select(all);
            for(TermsFacet.Entry entry : facet)
            {
                og.addItem(entry.getTerm() + " (" + entry.getCount() + ")");
            }
            if (facet.getOtherCount() > 0)
                og.addItem("Other (" + facet.getOtherCount() + ")");
            leftLayout.addComponent(og);
        }

        DateHistogramFacet dfacet = (DateHistogramFacet)result.getFacets().getFacets().get("date");
        if (dfacet != null) {
            OptionGroup og = new OptionGroup("Posted");
            og.setStyleName("search-facet-date");
            String any = "Anytime";
            og.addItem(any);
            og.select(any);
            for(DateHistogramFacet.Entry entry : dfacet)
            {
                og.addItem(new Date(entry.getTime()) + " (" + entry.getCount() + ")");
            }
            leftLayout.addComponent(og);
        }

    }

    public static void populate()
    {
        new Thread() {
            public void run() {
                System.out.println("Waiting in background for green...");
                client.admin().cluster().prepareHealth().setWaitForGreenStatus().execute().actionGet(); 

                IndicesExistsResponse ex = null;
                try {
                    ex = client.admin().indices().exists(org.elasticsearch.client.Requests.indicesExistsRequest("kb")).get();
                } catch (Exception e) { e.printStackTrace(); }

                if (ex != null && !ex.isExists())
                {
                    System.out.println("Index does not exist - filling");
                    long now = System.currentTimeMillis() - (2 * 24 * 60 * 60 * 1000);
                    for(int i = 0;i<1000;i++)
                    {
                        // every 13h a new entry
                        Date date = new Date(now + (i * 13 * 60 * 60 * 1000));
                        XContentBuilder builder;
                        try {
                            builder = XContentFactory.jsonBuilder().startObject()
                                    .field("type", "info")
                                    .field("postDate", date)
                                    .field("title", "This is a Title" + i % 40)
                                    .field("category", "cat" + i % 10)
                                    .field("summary", "This is a summary at " + date)
                                    .field("body", "<hml><b>Fett</b> und Normal und Title"+i%19+"</html>").endObject();
                            ListenableActionFuture<IndexResponse> resp = client.prepareIndex("kb", "entry", "" + i).setSource(builder).execute();
                            // TODO: bulk
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                else {
                    System.out.println("Index is already filled.");
                }
                //button.enabled();
            };
        }.start();
    }

}
