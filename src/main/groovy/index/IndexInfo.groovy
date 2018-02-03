package index

import org.apache.lucene.document.Document
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexReader
import org.apache.lucene.index.Term
import org.apache.lucene.search.*
import org.apache.lucene.search.similarities.BM25Similarity
import org.apache.lucene.search.similarities.Similarity
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory

import java.nio.file.Path
import java.nio.file.Paths

/**
 * Singleton class to store index information.
 * Set the path to the lucene index here
 */

@groovy.transform.TypeChecked
@groovy.transform.CompileStatic

enum ImportantTermsMethod {
    F1, TFIDF, IG, CHI, OR, MERGED
}

enum IndexEnum {

    NG20('indexes/20NG', 20),
    NG3('indexes/20NG3SpaceHockeyChristian', 3),
    NG5('indexes/20NG5WindowsMotorcyclesSpaceMedMideast', 5),
    R10('indexes/R10', 10),
    CRISIS3('indexes/crisis3FireBombFlood', 3),
    CLASSIC4('indexes/classic4_500', 4)

    private final Similarity similarity = new BM25Similarity()
                                       // new ClassicSimilarity()
    private final String pathString;
    private final int numberOfCategories

    IndexEnum(String pathString, int numberOfCategories) {
        this.numberOfCategories = numberOfCategories
        this.pathString = pathString
    }

    int getNumberOfCategories() {
        return numberOfCategories
    }

    String getPathString(){
        return pathString
    }

    String toString(){
        return "Index: ${this.name()} path: $pathString numberOfCategories: $numberOfCategories "
    }

    IndexSearcher getIndexSearcher() {
        Path path = Paths.get(pathString)
        Directory directory = FSDirectory.open(path)
        IndexReader ir = DirectoryReader.open(directory)
        IndexSearcher is = new IndexSearcher(ir)
        is.setSimilarity(similarity)
        return is
    }
}

@Singleton
class IndexInfo {
    static final IndexEnum indexEnum = IndexEnum.CLASSIC4
    public static final ImportantTermsMethod itm = ImportantTermsMethod.F1

    // Lucene field names
    public static final String FIELD_CATEGORY_NAME = 'category',
                               FIELD_CONTENTS = 'contents',
                               FIELD_PATH = 'path',
                               FIELD_TEST_TRAIN = 'test_train',
                               FIELD_CATEGORY_NUMBER = 'categoryNumber';

    public static final int NUMBER_OF_CATEGORIES = indexEnum.getNumberOfCategories()
    public static final int NUMBER_OF_CLUSTERS = indexEnum.getNumberOfCategories()

    static IndexSearcher indexSearcher = indexEnum.getIndexSearcher()
    static IndexReader indexReader = indexSearcher.getIndexReader()

    static BooleanQuery trainDocsInCategoryFilter, otherTrainDocsFilter, testDocsInCategoryFilter, otherTestDocsFilter;
    static int totalTrainDocsInCat, totalTestDocsInCat, totalOthersTrainDocs, totalTestDocs;

    final TermQuery trainQ = new TermQuery(new Term(
            FIELD_TEST_TRAIN, 'train'));
    final TermQuery testQ = new TermQuery(new Term(
            FIELD_TEST_TRAIN, 'test'));

    // the categoryNumber of the current category
    static String categoryNumber = '0'

    //Query to return documents in the current category based on categoryNumber
    static TermQuery catQ;

    //get hits for a particular query using filter (e.g. a particular category)
    static int getQueryHitsWithFilter(IndexSearcher searcher, Query filter, Query q) {
        TotalHitCountCollector collector = new TotalHitCountCollector();
        BooleanQuery.Builder bqb = new BooleanQuery.Builder();
        bqb.add(q, BooleanClause.Occur.MUST)
        bqb.add(filter, BooleanClause.Occur.FILTER)
        searcher.search(bqb.build(), collector);
        return collector.getTotalHits();
    }

    //get the category_name for the current category
    public static String getCategoryName() {
        TopScoreDocCollector collector = TopScoreDocCollector.create(1)
        indexSearcher.search(catQ, collector);
        ScoreDoc[] hits = collector.topDocs().scoreDocs

        String categoryName
        hits.each { ScoreDoc h ->
            Document d = indexSearcher.doc(h.doc)
            categoryName = d.get(FIELD_CATEGORY_NAME)
        }
        return categoryName
    }

    public static void setIndex() {
        println "NUBMER_OF_CATEGORIES: $NUMBER_OF_CATEGORIES"
    }

    //set the filters and totals for the index
    public void setIndexFieldsAndTotals() {
        println "NUBMER_OF_CATEGORIES: $NUMBER_OF_CATEGORIES"
        catQ = new TermQuery(new Term(FIELD_CATEGORY_NUMBER,
                categoryNumber));
        println "Index info catQ: $catQ"

        BooleanQuery.Builder bqb = new BooleanQuery.Builder()
        bqb.add(catQ, BooleanClause.Occur.FILTER)
        bqb.add(trainQ, BooleanClause.Occur.FILTER)
        //catTrainBQ
        trainDocsInCategoryFilter = bqb.build();

        bqb = new BooleanQuery.Builder()
        bqb.add(catQ, BooleanClause.Occur.FILTER)
        bqb.add(testQ, BooleanClause.Occur.FILTER)
        //catTestBQ
        testDocsInCategoryFilter = bqb.build();

        bqb = new BooleanQuery.Builder()
        bqb.add(catQ, BooleanClause.Occur.MUST_NOT)
        bqb.add(trainQ, BooleanClause.Occur.FILTER)
        //othersTrainBQ
        otherTrainDocsFilter = bqb.build();

        bqb = new BooleanQuery.Builder()
        bqb.add(catQ, BooleanClause.Occur.MUST_NOT)
        bqb.add(testQ, BooleanClause.Occur.FILTER)
        //othersTestBQ
        otherTestDocsFilter = bqb.build();

        TotalHitCountCollector collector = new TotalHitCountCollector();
        indexSearcher.search(trainDocsInCategoryFilter, collector);
        totalTrainDocsInCat = collector.getTotalHits();

        collector = new TotalHitCountCollector();
        indexSearcher.search(testDocsInCategoryFilter, collector);
        totalTestDocsInCat = collector.getTotalHits();

        collector = new TotalHitCountCollector();
        indexSearcher.search(otherTrainDocsFilter, collector);
        totalOthersTrainDocs = collector.getTotalHits();

        collector = new TotalHitCountCollector();
        indexSearcher.search(trainQ, collector);
        int totalTrain = collector.getTotalHits();

        collector = new TotalHitCountCollector();
        indexSearcher.search(testQ, collector);
        totalTestDocs = collector.getTotalHits();

        println "IndexInfo:- CategoryNumber: $categoryNumber Total train in cat: $totalTrainDocsInCat  Total others tain: $totalOthersTrainDocs   Total test in cat : $totalTestDocsInCat  "
    }
}