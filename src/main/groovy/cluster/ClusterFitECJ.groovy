package cluster

import ec.simple.SimpleFitness
import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode
import index.IndexInfo
import org.apache.lucene.document.Document
import org.apache.lucene.index.Term
import org.apache.lucene.search.*

/**
 * Store cluster fitness information
 * 
 * @author Laurie 
 */

@groovy.transform.CompileStatic
@groovy.transform.TypeChecked

public class ClusterFitECJ extends SimpleFitness {

	Map <Query, Integer> queryMap  = [:]
	double positiveScoreTotal = 0.0 
	double negativeScoreTotal = 0.0 
	double fraction = 0.0
	double baseFitness = 0.0
	double scorePlus1000 = 0.0
	double scoreOnly = 0.0
	
	int positiveHits = 0
	int negativeHits = 0
	int duplicateCount = 0
	int lowSubqHits = 0
	int coreClusterPenalty = 0
	int totalHits = 0 
	int missedDocs = 0
	int zeroHitsCount = 0
	boolean isDummy = false
	boolean emptyQueries = false
	
	int treePenalty=0;
	int graphPenalty=0
	
	Formatter bestResultsOut
	IndexSearcher searcher = IndexInfo.indexSearcher;
	final int hitsPerPage=IndexInfo.indexReader.maxDoc()

	String queryShort (){
		def s="queryMap.size ${queryMap.size()} \n"
		queryMap.keySet().eachWithIndex {Query q, int index ->
			if (index>0) s+='\n';
			s +=  "ClusterQuery: $index :  ${queryMap.get(q)}  ${q.toString(IndexInfo.FIELD_CONTENTS)}"
		}
		return s
	}
	
	public double getF (){
		return baseFitness;
	}

	@TypeChecked(TypeCheckingMode.SKIP)
	public void queryStats (int job, int gen, int popSize){
		String messageOut=""
		FileWriter resultsOut = new FileWriter("results/clusterResultsF1.txt", true)
		resultsOut <<"${new Date()}  ***** Job: $job Gen: $gen PopSize: $popSize Noclusters: ${IndexInfo.NUMBER_OF_CLUSTERS}  pathToIndex: ${IndexInfo.pathToIndex}  *********** ${new Date()} ***************************************************** \n"

		def f1list = [], precisionList =[], recallList =[]
		queryMap.keySet().eachWithIndex {q, index ->

			TopScoreDocCollector collector = TopScoreDocCollector.create(hitsPerPage);
			searcher.search(q, collector);
			ScoreDoc[] hits = collector.topDocs().scoreDocs;
			def qString = q.toString(IndexInfo.FIELD_CONTENTS)

			println "***********************************************************************************"
			messageOut = "ClusterQuery: $index hits: ${hits.length} Query:  $qString \n"
			println messageOut
			resultsOut << messageOut

			//map of categories (ground truth) and their frequencies
			def catsFreq=[:]
			hits.eachWithIndex{ScoreDoc h, int i ->
				int docId = h.doc;
				def scr = h.score
				Document d = searcher.doc(docId);
				String catName = d.get(IndexInfo.FIELD_CATEGORY_NAME)
				int n = catsFreq.get((catName)) ?: 0
				catsFreq.put((catName), n + 1)

//view top 5 results
//				if (i <5){
//					messageOut = "$i path ${d.get(IndexInfo.FIELD_PATH)} cat name: $catName "
//					println messageOut
//					resultsOut << messageOut + '\n'
//				}
			}
			println "Gen: $gen ClusterQuery: $index catsFreq: $catsFreq for query: $qString "

			//find the category with maximimum returned docs for this query
			def catMax = catsFreq?.max{it?.value} ?:0

			println "catsFreq: $catsFreq cats max: $catMax "

			//purity measure - check this is correct?
            //def purity = (hits.size()==0) ? 0 : (1 / hits.size())  * catMax.value
	        //println "purity:  $purity"

			if (catMax !=0){
				TotalHitCountCollector totalHitCollector  = new TotalHitCountCollector();
				TermQuery catQ = new TermQuery(new Term(IndexInfo.FIELD_CATEGORY_NAME,
						catMax.key));
				searcher.search(catQ, totalHitCollector);
				int categoryTotal = totalHitCollector.getTotalHits();
				messageOut = "categoryTotal: $categoryTotal for category: $catQ \n"
				println messageOut
				resultsOut << messageOut

				double recall = catMax.value / categoryTotal;
				double precision = catMax.value / hits.size()
				double f1 = (2 * precision * recall) / (precision + recall);
				
				f1list << f1
				precisionList << precision
				recallList << recall
				messageOut = "f1: $f1 recall: $recall precision: $precision"
				println messageOut
				resultsOut << messageOut + "\n"
				//resultsOut << "Purity: $purity Job: $job \n"
			}
		}
	    
		double averageF1 = (f1list) ? f1list.sum()/ IndexInfo.NUMBER_OF_CLUSTERS : 0
		double averageRecall = (recallList) ? recallList.sum()/ IndexInfo.NUMBER_OF_CLUSTERS : 0
		double averagePrecision =(precisionList) ? precisionList.sum()/ IndexInfo.NUMBER_OF_CLUSTERS :0
		messageOut ="***  TOTALS:   *****   f1list: $f1list averagef1: :$averageF1  ** average precision: $averagePrecision average recall: $averageRecall"
		println messageOut
		
		resultsOut << "TotalHits: $totalHits Total Docs:  ${IndexInfo.instance.indexReader.maxDoc()} \n"
		resultsOut << "PosHits: $positiveHits NegHits: $negativeHits PosScore: $positiveScoreTotal NegScore: $negativeScoreTotal Fitness: ${fitness()} \n"
		resultsOut << messageOut + "\n"
		resultsOut << "************************************************ \n \n"

		resultsOut.flush()
		resultsOut.close()

		boolean appnd =  job > 1
		FileWriter fcsv = new FileWriter("results/resultsCluster.csv", appnd)
		Formatter csvOut = new Formatter(fcsv);
		if (!appnd){
			final String fileHead = "gen, job, popSize, fitness, averageF1, averagePrecision, averageRecall, query" + '\n';
			csvOut.format("%s", fileHead)			
		}
		csvOut.format(
				"%s, %s, %s, %.3f, %.3f, %.3f, %.3f, %s, %s, %s \n",
				gen,
				job,
				popSize,
				fitness(),
				averageF1,
				averagePrecision,
				averageRecall,//)//,
				queryForCSV(job),
				new Date(),
				IndexInfo.pathToIndex );

		csvOut.flush();
		csvOut.close()
	}

	private String queryForCSV (int job){
		def s="Job: $job "
		queryMap.keySet().eachWithIndex {q, index ->
			s += "ClusterQuery " + index + ": " + queryMap.get(q) + " " + q.toString(IndexInfo.FIELD_CONTENTS) + " ## "
		}
		return s
	}

	public String fitnessToStringForHumans() {
		return  "ClusterQuery Fitness: ${this.fitness()} "
	}

	public String toString(int gen) {
		return "Gen: $gen ClusterQuery Fitness: ${this.fitness} qMap: $queryMap}"
	}
}