package index

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import org.apache.lucene.document.Document
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexReader
import org.apache.lucene.index.Term
import org.apache.lucene.search.*
import org.apache.lucene.search.similarities.BM25Similarity
import org.apache.lucene.search.similarities.ClassicSimilarity
import org.apache.lucene.search.similarities.Similarity
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory

import java.nio.file.Path
import java.nio.file.Paths


@CompileStatic
enum IndexEnum {

    CRISIS3('indexes/crisis3FireBombFlood', 3),
    CLASSIC3('indexes/classic3_300', 3),

    CLASSIC4('indexes/classic4_500', 4),

    NG3('indexes/20NG3', 3),
    NG5('indexes/20NG5WindowsmiscForsaleHockeySpaceChristian', 5),
    NG6('indexes/20NG6GraphicsHockeyCryptSpaceChristianGuns', 6),
    NG20('indexes/20NG', 20),

    R4('indexes/R4', 4),
    R5('indexes/R5', 5),
    R5_200('indexes/R5-200', 5),
    R6('indexes/R6', 6),
    R10('indexes/R10', 10),
    WarCrimes('indexes/warCrimes',8),
    Secrecy(       'indexes/resistance', 11),
   NG3N( 'indexes/ng3N',3)


    //R8('indexes/R8', 8),
    //R6('indexes/R6', 6),
    //OHS3('indexes/Ohsc06MuscC08RespC11Eye', 3),

    // private final Similarity similarity = new BM25Similarity()
    // new ClassicSimilarity()
    String pathString;
    int numberOfCategories

    IndexEnum(String pathString, int numberOfCategories) {
        this.numberOfCategories = numberOfCategories
        this.pathString = pathString
    }

    String toString() {
        return "${this.name()} path: $pathString numberOfCategories: $numberOfCategories "
    }

    IndexReader getIndexReader() {
        Path path = Paths.get(pathString)
        Directory directory = FSDirectory.open(path)
        IndexReader ir = DirectoryReader.open(directory)
        println "IndexReader: $ir"
        return ir
    }

    IndexSearcher getIndexSearcher() {
        Path path = Paths.get(pathString)
        Directory directory = FSDirectory.open(path)
        IndexReader ir = DirectoryReader.open(directory)
        IndexSearcher is = new IndexSearcher(ir)

        //    is.setSimilarity(similarity)
        return is
    }
}

@CompileStatic
@Singleton
class Indexes {
    static IndexEnum indexEnum //= IndexEnum.R8

    // Lucene field names
    static final String FIELD_CATEGORY_NAME = 'category',
                        FIELD_CONTENTS = 'contents',
                        FIELD_PATH = 'path',
                        FIELD_TEST_TRAIN = 'test_train',
                        FIELD_CATEGORY_NUMBER = 'categoryNumber',
                        FIELD_ASSIGNED_CLASS= 'assignedClass'   ;

    public static int NUMBER_OF_CATEGORIES// = indexEnum.getNumberOfCategories()
    public static int NUMBER_OF_CLUSTERS// = indexEnum.getNumberOfCategories()

    public static IndexSearcher indexSearcher// = indexEnum.getIndexSearcher()
    public static IndexReader indexReader// = indexSearcher.getIndexReader()

    public static BooleanQuery trainDocsInCategoryFilter, otherTrainDocsFilter, testDocsInCategoryFilter, otherTestDocsFilter;
    public static int totalTrainDocsInCat, totalTestDocsInCat, totalOthersTrainDocs, totalTestDocs;

    final TermQuery trainQ = new TermQuery(new Term(FIELD_TEST_TRAIN, 'train'));
    final TermQuery testQ = new TermQuery(new Term(FIELD_TEST_TRAIN, 'test'));

    // the categoryNumber of the current category
    static String categoryNumber = '0'

    //Query to return documents in the current category based on categoryNumber
    static TermQuery catQ;

    void setIndex(IndexEnum ie) {
        indexEnum = ie
        NUMBER_OF_CATEGORIES = indexEnum.getNumberOfCategories()
        NUMBER_OF_CLUSTERS = indexEnum.getNumberOfCategories()
        indexSearcher = indexEnum.getIndexSearcher()
        indexReader = indexSearcher.getIndexReader()
        setIndexFieldsAndTotals()
        println "indexEnum $indexEnum"
    }

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
    static String getCategoryName() {
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

    //set the filters and totals for the index for classification
    void setIndexFieldsAndTotals() {
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

        println "Indexes:- CategoryNumber: $categoryNumber Total train in cat: $totalTrainDocsInCat  Total others tain: $totalOthersTrainDocs   Total test in cat : $totalTestDocsInCat  "
    }
}