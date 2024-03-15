import org.apache.sling.api.resource.Resource;
import javax.jcr.Session;
import javax.jcr.query.Row
def resourceResolverFactory = osgi.getService(org.apache.sling.api.resource.ResourceResolverFactory.class);
def resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver();
final def query = buildQuery(resourceResolver);
final def result = query.execute()
result.rows.each {
    Row hit - >
        def path = hit.node.path
    Resource res = resourceResolver.getResource(path + "/jcr:content/metadata")
    if (res != null) {
        getAllReferences(res);
    }
}
def buildQuery(resourceResolver) {
    def session = resourceResolver.adaptTo(Session.class);
    def queryManager = session.workspace.queryManager;
    def statement = 'select * from [dam:Asset] as node where isdescendantnode([/content/dam/pwc-madison/ditaroot/us/en/sec]) and node.[jcr:content/metadata/dam:MIMEtype] = \'image/jpeg\'';
    queryManager.createQuery(statement, 'JCR-SQL2');
}
def getAllReferences(res) {
    def node = res.adaptTo(javax.jcr.Node);
    def width = 0;
    def height = 0;
    if (node.hasProperty("tiff:ImageWidth")) {
        width = node.getProperty("tiff:ImageWidth").getLong();
    }
    if (node.hasProperty("tiff:ImageLength")) {
        height = node.getProperty("tiff:ImageLength").getLong();
    }
    if (height > 1.5 * width) {
        println "Asset: " + node.path;
    }
}