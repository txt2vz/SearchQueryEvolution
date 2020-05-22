package cluster;

import classify.ClassifyUnassigned;
import classify.LuceneClassifyMethod;
import classify.UpdateAssignedFieldInIndex;
import groovy.lang.Tuple3;
import groovy.time.TimeCategory;
import groovy.time.TimeDuration;
import index.*;
import io.jenetics.engine.EvolutionStatistics;
import org.apache.lucene.classification.Classifier;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import io.jenetics.*;
import io.jenetics.engine.Engine;
import io.jenetics.util.Factory;

import java.util.*;
import java.util.stream.IntStream;

import index.IndexEnum;

import static io.jenetics.engine.EvolutionResult.toBestPhenotype;

enum QType {OR1, OR_INTERSECT}

public class JeneticsMain {

    static List<TermQuery> termQueryList;
    static QType qType = //QType.OR1;
        QType.OR_INTERSECT;
    static IndexEnum indexEnum;
    static IndexReader ir;
    final static boolean setk = true;

    //static int k;
    static List<IndexEnum> ieList = Arrays.asList(IndexEnum.CRISIS3
            , IndexEnum.CLASSIC4
            , IndexEnum.NG3, IndexEnum.NG5
            , IndexEnum.NG6
            , IndexEnum.R4, IndexEnum.R5, IndexEnum.R6
    );

    static double sqf(final Genotype<IntegerGene> gt) {
        final int k = getK(gt, indexEnum, setk);
        int[] intArray = ((IntegerChromosome) gt.get(0)).toArray();

        List<BooleanQuery.Builder> bqbList = QuerySet.getQueryList(intArray, termQueryList, k, qType);
        final int uniqueHits = UniqueHits.getUniqueHits(bqbList).getV2();

        double f = (setk) ? uniqueHits * (1.0 - (0.04 * k)) : uniqueHits;
        f = (f > 0) ? f : 0.0d;

        return f;
    }

    public static void main(String[] args) throws Exception {

        final Date startRun = new Date();
        final int popSize = 1024;
        final int maxGen = 200;
        final int maxGene = 100;
        final LuceneClassifyMethod classifyMethod = LuceneClassifyMethod.KNN;
        final int setkMaxNumberOfCategories = 9;
        final int numberOfJobs = 3;
        final boolean onlyDocsInOneQueryForClassification = true;
        final int maxGenomeLength = 19;

        ieList.stream().forEach(ie -> {
            ReportsJenetics reportsJenetics = new ReportsJenetics();
            List<Phenotype<IntegerGene, Double>> resultList = new ArrayList<Phenotype<IntegerGene, Double>>();
            indexEnum = ie;

            IntStream.range(0, numberOfJobs).forEach(jobNumber -> {

                Indexes.setIndex(ie, true);
                termQueryList = (Collections.unmodifiableList(ImportantTermQueries.getTFIDFTermQueryList(ie.getIndexReader())));

                final int maxCats = (setk) ? setkMaxNumberOfCategories : indexEnum.getNumberOfCategories();
                final int genomeLength = (qType == QType.OR1) ? maxCats : maxGenomeLength;

                final Factory<Genotype<IntegerGene>> gtf =
                        (setk) ?
                                Genotype.of(
                                        IntegerChromosome.of(0, maxGene, genomeLength),
                                        IntegerChromosome.of(2, setkMaxNumberOfCategories)) :  //possible values for k

                                Genotype.of(
                                        IntegerChromosome.of(0, maxGene, genomeLength));
                //  IntegerChromosome.of(0, 100, IntRange.of(2, 8)));//,

                final Engine<IntegerGene, Double> engine = Engine.
                        builder(
                                JeneticsMain::sqf,
                                gtf)
                        .populationSize(popSize)
                        // .survivorsSelector(new
                        // StochasticUniversalSelector<>()).offspringSelector(new
                        // TournamentSelector<>(5))

                        .survivorsSelector(new TournamentSelector<>(5))
                        .offspringSelector(new TournamentSelector<>(5))
                        .alterers(new Mutator<>(0.2), //new MultiPointCrossover<>())
                                new SinglePointCrossover<>(0.7))
                        .build();

                final EvolutionStatistics<Double, ?>
                        statistics = EvolutionStatistics.ofNumber();

                final Phenotype<IntegerGene, Double> result =

                        engine.stream()
                                .limit(maxGen)
                                .peek(ind -> {
                                    Genotype<IntegerGene> g = ind.bestPhenotype().genotype();
                                    int[] intArrayBestGen = ((IntegerChromosome) g.get(0)).toArray();
                                    final int k = getK(g, ie, setk);

                                    Tuple3<Set<Query>, Integer, Double> queryDataGen = QuerySet.querySetInfo(intArrayBestGen, termQueryList, k, qType, true);
                                    System.out.println("gen: " + ind.generation() + " bestPhenoFit " + ind.bestPhenotype().fitness() + " fitness: " + ind.bestFitness() + " uniqueHits: " + queryDataGen.getV2() + " querySet F1: " + queryDataGen.getV3());
                                    System.out.println();

                                })
                                .peek(statistics)
                                .collect(toBestPhenotype());

                System.out.println("Final result  " + result);
                resultList.add(result);
                Genotype<IntegerGene> g = result.genotype();

                int[] intArrayBestOfRun = ((IntegerChromosome) g.get(0)).toArray();
                final int k = getK(g, ie, setk);

                Tuple3<Set<Query>, Integer, Double> bestQueryData = QuerySet.querySetInfo(intArrayBestOfRun, termQueryList, k, qType, true, true);

                Classifier classifier = ClassifyUnassigned.getClassifierForUnassignedDocuments(ie, LuceneClassifyMethod.KNN);
                UpdateAssignedFieldInIndex.updateAssignedField(ie, bestQueryData.getV1());// queryList )
                Tuple3<Double, Double, Double> classifierEffectiveness = Effectiveness.classifierEffectiveness(classifier, ie, k);
                final double classifierF1 = classifierEffectiveness.getV1();

                System.out.println("Best of run **********************************  classifierF1 " + classifierF1 + " " + ie.name() + '\n');

                //System.out.println("statistics " + statistics);
                reportsJenetics.reportCSV(jobNumber, ie, bestQueryData.getV3(), classifierF1, bestQueryData.getV2(), qType, classifyMethod, popSize, g.chromosome().length(), maxGene, maxGen, setk, onlyDocsInOneQueryForClassification);

            });
            reportsJenetics.reportMaxFitness();
        });

        final Date endRun = new Date();
        TimeDuration duration = TimeCategory.minus(endRun, startRun);
        System.out.println("Duration: " + duration);
    }

    static int getK(Genotype g, IndexEnum indexEnum, final boolean setk) {

        final int k = (setk) ? ((IntegerChromosome) g.get(1)).gene().allele() :
                indexEnum.getNumberOfCategories();

        return k;
    }
}