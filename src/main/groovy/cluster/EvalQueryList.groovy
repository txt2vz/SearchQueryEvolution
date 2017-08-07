package cluster

import index.IndexInfo
import org.apache.lucene.search.*

@groovy.transform.CompileStatic
@groovy.transform.TypeChecked
class EvalQueryList {

	public static void setClusterFitness (ClusterFitECJ fitness, List <BooleanQuery.Builder> bqbArray, boolean gp){

		final int hitsPerPage = IndexInfo.indexReader.maxDoc()
		final int coreClusterSize = 20
		assert bqbArray.size() == IndexInfo.NUMBER_OF_CLUSTERS

		fitness.positiveScoreTotal=0
		fitness.negativeScoreTotal=0
		fitness.positiveHits=0
		fitness.negativeHits=0
		fitness.lowSubqHits= 0
		fitness.coreClusterPenalty=0
		fitness.totalHits=0
		fitness.missedDocs =0
		fitness.zeroHitsCount =0
		fitness.duplicateCount = 0

		Map <Query, Integer> qMap = new HashMap<Query, Integer>() 
		Set allHits = [] as Set

		bqbArray.each {BooleanQuery.Builder bqb ->

			Query q = bqb.build()

			if (gp){
				if ( q.toString(IndexInfo.FIELD_CONTENTS).contains("DummyXX") || q==null || q.toString(IndexInfo.FIELD_CONTENTS) == '' ){
					fitness.isDummy = true;
				}
			}

			def otherdocIdSet= [] as Set
			def otherQueries = bqbArray - bqb

			BooleanQuery.Builder bqbOthers = new BooleanQuery.Builder();
			otherQueries.each {obqb ->
				bqbOthers.add(obqb.build(),  BooleanClause.Occur.SHOULD)
			}
			Query otherBQ = bqbOthers.build()

			TopDocs otherTopDocs = IndexInfo.indexSearcher.search(otherBQ, hitsPerPage)
			ScoreDoc[] hitsOthers = otherTopDocs.scoreDocs;
			hitsOthers.each {ScoreDoc otherHit -> otherdocIdSet << otherHit.doc }

			TopDocs docs = IndexInfo.indexSearcher.search(q, hitsPerPage)
			ScoreDoc[] hits = docs.scoreDocs;
			qMap.put(q,hits.size())

			if (hits.size()<1)   fitness.zeroHitsCount ++

			hits.eachWithIndex {ScoreDoc d, int position ->
				allHits << d.doc

				if (otherdocIdSet.contains(d.doc)){
					fitness.negativeHits++;
					fitness.negativeScoreTotal += d.score
					if (position < coreClusterSize ){
						//heavy penalty
						//def reverseRank = coreClusterSize - position
						//fitness.coreClusterPenalty +=reverseRank
						fitness.coreClusterPenalty++
					}
				}
				else {
					fitness.positiveHits++
					fitness.positiveScoreTotal +=d.score
				}
			}
		}

		fitness.queryMap = qMap.asImmutable()

		if (gp && fitness.queryMap.size() != IndexInfo.NUMBER_OF_CLUSTERS) {
			fitness.emptyQueries = true
		}
		fitness.scoreOnly = fitness.positiveScoreTotal - fitness.negativeScoreTotal
		fitness.totalHits = allHits.size()
		fitness.fraction = fitness.totalHits / IndexInfo.indexReader.maxDoc()   
		fitness.missedDocs = IndexInfo.indexReader.maxDoc()  - allHits.size() 
	}
}