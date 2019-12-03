package index

import groovy.transform.CompileStatic
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexReader
import org.apache.lucene.index.Term
import org.apache.lucene.search.*
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory

import java.nio.file.Path
import java.nio.file.Paths


@CompileStatic
enum IndexEnum {

    NG5Train('indexes/NG5Train', 5),
    NG5Test('indexes/NG5Test', 5),
    R4Train('indexes/R4Train', 4),
    R4Test('indexes/R4Test', 4),
    CLASSIC4TRAIN('indexes/classic4Train', 4),
    CLASSIC4TEST('indexes/classic4Test', 4),


    CRISIS3('indexes/crisis3FireBombFlood', 3),
    CRISIS3TRAIN('indexes/crisis3FireBombFloodTrain', 3),
    CRISIS3TEST('indexes/crisis3FireBombFloodTest', 3),

    CLASSIC3('indexes/classic3_300', 3),
    CLASSIC4('indexes/classic4_500', 4),

    CLASSIC4B('indexes/classic4b', 4),

    NG3('indexes/NG3', 3),
    NG5('indexes/20NG5WindowsmiscForsaleHockeySpaceChristian', 5),
    NG6('indexes/20NG6GraphicsHockeyCryptSpaceChristianGuns', 6),
    NG20('indexes/20NG', 20),

    R4('indexes/R4', 4),
    R5('indexes/R5', 5),
    R5_200('indexes/R5-200', 5),
    R6('indexes/R6', 6),
    R10('indexes/R10', 10),

    WarCrimes('indexes/warCrimes', 8),
    Secrecy('indexes/resistance', 11),
    Science4('indexes/science4', 4),
    NG3N('indexes/ng3N', 3)


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
class Indexes {
    static IndexEnum index

    // Lucene field names
    static final String FIELD_CATEGORY_NAME = 'category',
                        FIELD_CONTENTS = 'contents',
                        FIELD_PATH = 'path',
                        FIELD_TEST_TRAIN = 'test_train',
                        FIELD_CATEGORY_NUMBER = 'categoryNumber',
                        FIELD_ASSIGNED_CLASS = 'assignedClass',
                        FIELD_DOCUMENT_ID = 'document_id';

    static final Analyzer analyzer = new StandardAnalyzer()  //new EnglishAnalyzer();  //with stemming  new WhitespaceAnalyzer()

    static IndexSearcher indexSearcher
    static IndexReader indexReader

    static BooleanQuery trainDocsInCategoryFilter, otherTrainDocsFilter, testDocsInCategoryFilter, otherTestDocsFilter;
    static int totalTrainDocsInCat, totalTestDocsInCat, totalOthersTrainDocs, totalTestDocs;

    final static TermQuery trainQ = new TermQuery(new Term(FIELD_TEST_TRAIN, 'train'));
    final static TermQuery testQ = new TermQuery(new Term(FIELD_TEST_TRAIN, 'test'));

    //Query to return documents in the current category based on categoryNumber
    static TermQuery catQ;

    static void setIndex(IndexEnum ie) {
        index = ie
        indexSearcher = index.getIndexSearcher()
        indexReader = indexSearcher.getIndexReader()
      //  setIndexFieldsAndTotals()
        println "indexEnum $index"
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
    static void setIndexFieldsAndTotals(String categoryNumber) {
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