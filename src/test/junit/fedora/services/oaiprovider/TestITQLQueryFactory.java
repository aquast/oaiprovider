package fedora.services.oaiprovider;

import java.util.Date;
import java.util.Properties;

import junit.framework.TestCase;

import proai.error.RepositoryException;

/**
 * @author Edwin Shin
 */
public class TestITQLQueryFactory extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(TestITQLQueryFactory.class);
    }
    
    public void testListRecordsQuery() throws Exception {
        Properties props = new Properties();
        props.put(FedoraOAIDriver.PROP_ITEMID, "http://www.openarchives.org/OAI/2.0/itemID");
        props.put(FedoraOAIDriver.PROP_SETSPEC, "http://www.openarchives.org/OAI/2.0/setSpec");
        props.put(FedoraOAIDriver.PROP_SETSPEC_NAME, "http://www.openarchives.org/OAI/2.0/setName");
        props.put(FedoraOAIDriver.PROP_ITEM_SETSPEC_PATH, "$item <fedora-rels-ext:isMemberOf> $set $set <http://www.openarchives.org/OAI/2.0/setSpec> $setSpec");
        
        ITQLQueryFactory iqf = new ITQLQueryFactory();
        QueryFactory qf = (QueryFactory)iqf;
        qf.init(null, null, props);
        
        Date df = new Date(0L); // the epoch (1970-01-01T00:00:00 GMT)
        Date du = new Date(1000000000000L); // the billenium (2001-09-09T:01:46:40 UTC)
        
        
        String query = iqf.getListRecordsQuery(df, du, "info:fedora/*/oai_dc", "info:fedora/*/about_oai_dc", true);
        
        String q = "select $itemID $recordDiss $date $deleted\n" +
                   "  subquery(\n" +
                   "    select $setSpec\n" +
                   "     from <#ri>\n" +
                   "    where\n" +
                   "      $item <http://www.openarchives.org/OAI/2.0/itemID> $itemID\n" +
                   "      and $item <info:fedora/fedora-system:def/view#disseminates> $recordDiss\n" +
                   "      and $recordDiss <info:fedora/fedora-system:def/view#disseminationType> <info:fedora/*/oai_dc>\n" +
                   "      and $item <fedora-rels-ext:isMemberOf> $set and $set <http://www.openarchives.org/OAI/2.0/setSpec> $setSpec\n" +
                   "  )\n" +
                   "  subquery(\n" +
                   "    select $aboutDiss\n" +
                   "    from <#ri>\n" +
                   "    where\n" +
                   "      $item <http://www.openarchives.org/OAI/2.0/itemID> $itemID\n" +
                   "      and $item <info:fedora/fedora-system:def/view#disseminates> $recordDiss\n" +
                   "      and $recordDiss <info:fedora/fedora-system:def/view#disseminationType> <info:fedora/*/oai_dc>\n" +
                   "      and $item <info:fedora/fedora-system:def/view#disseminates> $aboutDiss\n" +
                   "      and $aboutDiss <info:fedora/fedora-system:def/view#disseminationType> <info:fedora/*/about_oai_dc>)\n" +
                   "from <#ri>\n" +
                   "where\n" +
                   "  $item <http://www.openarchives.org/OAI/2.0/itemID> $itemID\n" +
                   "  and $item <info:fedora/fedora-system:def/view#disseminates> $recordDiss\n" +
                   "  and $recordDiss <info:fedora/fedora-system:def/view#disseminationType> <info:fedora/*/oai_dc>\n" +
                   "  and $recordDiss <info:fedora/fedora-system:def/view#lastModifiedDate> $date\n" +
                   "  and $recordDiss <info:fedora/fedora-system:def/model#state> $deleted\n" +
                   "  and $date <http://tucana.org/tucana#after> '1969-12-31T23:59:59.999Z' in <#xsd>\n" +
                   "  and $date <http://tucana.org/tucana#before> '2001-09-09T01:46:40.001Z' in <#xsd>\n" +
                   "  order by $itemID asc\n";
        assertEquals(q, query);
    }
    
    public void testParseItemSetSpecPath() {
        Properties props = new Properties();
        props.put(FedoraOAIDriver.PROP_ITEMID, "urn:baz");
        String ssp = "$item <urn:foo> $set $set <urn:bar> $setSpec";
        props.put(FedoraOAIDriver.PROP_ITEM_SETSPEC_PATH, ssp);
        ITQLQueryFactory iqf = new ITQLQueryFactory();
        QueryFactory qf = (QueryFactory) iqf;
        qf.init(null, null, props);

        assertEquals("$item <urn:foo> $set and $set <urn:bar> $setSpec", 
                     iqf.parseItemSetSpecPath(ssp));
        
        String[] badSetPaths = {"foo",
                                "$item <predicate:foo> $set",
                                "$foo <predicate:bar> $setSpec",
                                "$item $predicate $setSpec",
                                "$item <urn:foo> $set and $set <urn:bar> $setSpec",
                                "$item <urn:foo> $set $set <urn:bar>",
                                "$item urn:baz $setSpec"
        };
        
        for (int i = 0; i < badSetPaths.length; i++) {
            try {
                iqf.parseItemSetSpecPath(badSetPaths[i]);
                fail("Should have failed with a RepositoryException");
            } catch (RepositoryException e) {
                assertTrue(e.getMessage().indexOf("Required property, itemSetSpecPath, ") != -1);
            }
        }
        
    }
}
