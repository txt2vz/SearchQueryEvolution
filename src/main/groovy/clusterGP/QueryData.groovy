package clusterGP

import ec.gp.GPData
import index.ImportantTerms
import index.IndexInfo
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.TermQuery

public class QueryData extends GPData {
	public BooleanQuery.Builder [] bqbArray = new BooleanQuery.Builder [IndexInfo.NUMBER_OF_CLUSTERS]
	public BooleanQuery.Builder bqb
	public BooleanQuery bq
	public TermQuery tq;    // return value
	public Boolean dummy

	final TermQuery[] termQueryArray = new ImportantTerms().getTFIDFTermQueryList()

	public void copyTo(final GPData gpd)   // copy my stuff to another DoubleData
	{
		((QueryData)gpd).tq = tq;
		((QueryData)gpd).dummy = dummy;
		((QueryData)gpd).bqb = bqb;
		((QueryData)gpd).bq = bq;
		((QueryData)gpd).bqbArray = bqbArray;
	}
}