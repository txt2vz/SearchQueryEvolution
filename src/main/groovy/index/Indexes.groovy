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

    NG3TEST('indexes/NG3Test', 3),
    NG3TRAIN('indexes/NG3Train', 3),
    NG3TRAINSKEWED('indexes/NG3TrainSkewed', 3),

    NG5Train('indexes/NG5Train', 5),
    NG5Test('indexes/NG5Test', 5),

    NG6Train('indexes/NG6Train', 6),
    NG6Test('indexes/NG6Test', 6),

    R4Train('indexes/R4Train', 4),
    R4Test('indexes/R4Test', 4),
    R5Train('indexes/R5Train', 5),
    R5Test('indexes/R5Test', 5),

    R6Train('indexes/R6Train', 6),
    R6Test('indexes/R6Test', 6),

    CLASSIC4TRAIN('indexes/classic4Train', 4),
    CLASSIC4TEST('indexes/classic4Test', 4),

    CRISIS3TRAIN('indexes/crisis3FireBombFloodTrain', 3),
    CRISIS3TEST('indexes/crisis3FireBombFloodTest', 3),


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

 //   static BooleanQuery trainDocsInCategoryFilter, otherTrainDocsFilter, testDocsInCategoryFilter, otherTestDocsFilter;
 //   static int totalTrainDocsInCat, totalTestDocsInCat, totalOthersTrainDocs, totalTestDocs;

 //   final static TermQuery trainQ = new TermQuery(new Term(FIELD_TEST_TRAIN, 'train'));
 //   final static TermQuery testQ = new TermQuery(new Term(FIELD_TEST_TRAIN, 'test'));

    //Query to return documents in the current category based on categoryNumber
  //  static TermQuery catQ;

    static void setIndex(IndexEnum ie) {
        index = ie
        indexSearcher = index.getIndexSearcher()
        indexReader = indexSearcher.getIndexReader()
      //  setIndexFieldsAndTotals()
        println "indexEnum $index maxDocs ${indexReader.maxDoc()}"
    }

//    //get the category_name for the current category
//    static String getCategoryName() {
//        TopScoreDocCollector collector = TopScoreDocCollector.create(1)
//        indexSearcher.search(catQ, collector);
//        ScoreDoc[] hits = collector.topDocs().scoreDocs
//
//        String categoryName
//        hits.each { ScoreDoc h ->
//            Document d = indexSearcher.doc(h.doc)
//            categoryName = d.get(FIELD_CATEGORY_NAME)
//        }
//
//        return categoryName
//    }
}