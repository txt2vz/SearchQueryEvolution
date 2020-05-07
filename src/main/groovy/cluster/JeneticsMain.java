package cluster;

import classify.ClassifyUnassigned;
import classify.LuceneClassifyMethod;
import classify.UpdateAssignedFieldInIndex;
import groovy.lang.Tuple3;
import groovy.lang.Tuple4;
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

import index.IndexEnum;

import static io.jenetics.engine.EvolutionResult.toBestPhenotype;

public class JeneticsMain {

    static List<TermQuery> termQueryList;
    static IndexEnum indexEnum;
    static IndexReader ir;
    static int k;
    static List<IndexEnum> ieList = Arrays.asList(IndexEnum.CRISIS3, IndexEnum.NG5, IndexEnum.CLASSIC4);

    static double sqf(final Genotype<IntegerGene> gt) {

        int[] a = ((IntegerChromosome) gt.get(0)).toArray();
        List<BooleanQuery.Builder> bqbList = QueryListFromChromosomeJ.getOneWordQueryPerCluster(ir, a, termQueryList, k);
        final double f = ClusterFitnessJ.getUniqueHits(bqbList).getV2();
        //(Collections.unmodifiableList(Arrays.asList(bqbList))).getV2();

        return f;
    }

    public static void main(String[] args) throws Exception {
        final int popSize = 512;
        final int maxGen = 81;
        final int maxGene = 100;
        final int genomeLength = 18;

        int jobNumber = 0;
        ieList.stream().forEach(indexEnum -> {
            // IndexEnum indexEnum = IndexEnum.CRISIS3;
            Indexes.setIndex(indexEnum);
            k = indexEnum.getNumberOfCategories();

            termQueryList = (Collections.unmodifiableList(ImportantTermQueries.getTFIDFTermQueryList(indexEnum.getIndexReader())));

            ClusterFitness.setFitnessMethod(FitnessMethod.UNIQUE_HITS_COUNT);

            final Factory<Genotype<IntegerGene>> gtf = Genotype.of(
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

                    .survivorsSelector(new TournamentSelector<>(3))
                    .offspringSelector(new TournamentSelector<>(3))
                    .alterers(new Mutator<>(0.2), new MultiPointCrossover<>())//SinglePointCrossover<>(0.7))
                    .build();

            final EvolutionStatistics<Double, ?>
                    statistics = EvolutionStatistics.ofNumber();

            final Phenotype<IntegerGene, Double> result = engine.stream()
                    .limit(maxGen)
                    .peek(ind -> {
                        Genotype<IntegerGene> g = ind.bestPhenotype().genotype();
                        int[] intArrayBestGen = ((IntegerChromosome) g.get(0)).toArray();
                        Tuple3<Set<Query>, Integer, Double> queryDataGen = JeneticsHelper.getDataForReporting(ir, intArrayBestGen, termQueryList, k, true);

                        System.out.println("gen " + ind.generation() + " uniqueHits " + queryDataGen.getV2() + " querySet F1 " + queryDataGen.getV3());

                    })
                    .peek(statistics)
                    .collect(toBestPhenotype());

            System.out.println("Final result  " + result);
            Genotype<IntegerGene> g = result.genotype();

            int[] intArrayBestOfRun = ((IntegerChromosome) g.get(0)).toArray();
            Tuple3<Set<Query>, Integer, Double> queryDataRun = JeneticsHelper.getDataForReporting(ir, intArrayBestOfRun, termQueryList, k, true);

            Classifier classifier = ClassifyUnassigned.getClassifierForUnassignedDocuments(indexEnum, LuceneClassifyMethod.KNN);

            UpdateAssignedFieldInIndex.updateAssignedField(indexEnum, queryDataRun.getV1());// queryList )

            Tuple3<Double, Double, Double> classifierEffectiveness = Effectiveness.classifierEffectiveness(classifier, indexEnum, k);

            final double classifierF1 = classifierEffectiveness.getV1();

            System.out.println("Best of run **********************************  classifierF1 " + classifierF1);

            LuceneClassifyMethod lcm = LuceneClassifyMethod.KNN;

            System.out.println("statistics " + statistics);
            ReportsJenetics.reportCSV(jobNumber, maxGen, popSize, genomeLength, maxGene, queryDataRun.getV2(), queryDataRun.getV1(), indexEnum, lcm, queryDataRun.getV3(), classifierF1);
        });
    }
}