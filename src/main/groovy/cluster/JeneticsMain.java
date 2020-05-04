package cluster;

import index.*;
import io.jenetics.engine.EvolutionStatistics;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import io.jenetics.*;
import io.jenetics.engine.Engine;
import io.jenetics.util.Factory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import index.IndexEnum;

import static io.jenetics.engine.EvolutionResult.toBestPhenotype;

public class JeneticsMain {

    static List<TermQuery> termQueryList;
    static IndexEnum indexEnum;
    static IndexReader ir;
    static int k;

    static double sqf(final Genotype<IntegerGene> gt) {

        int[] a = ((IntegerChromosome) gt.get(0)).toArray();

        List<BooleanQuery.Builder> bqbList = QueryListFromChromosomeJ.getOneWordQueryPerCluster(ir, a, termQueryList, k);

        final double f = ClusterFitnessJ.getUniqueHits(bqbList).getV2();
        //(Collections.unmodifiableList(Arrays.asList(bqbList))).getV2();

        return f;
    }

    public static void main(String[] args) throws Exception {

        IndexEnum indexEnum = IndexEnum.CRISIS3;
        Indexes.setIndex(indexEnum);
        k = indexEnum.getNumberOfCategories();
        ir = Indexes.getIndexReader();
        termQueryList = (Collections.unmodifiableList(ImportantTermQueries.getTFIDFTermQueryList(ir)));

        final int popSize = 512;
        final long maxGen = 31;
        Analysis finalReport = new Analysis();
        int maxGene = 100;
        int genomeLength = 3;

        ClusterFitness.setFitnessMethod(FitnessMethod.UNIQUE_HITS_COUNT);

        final Factory<Genotype<IntegerGene>> gtf = Genotype.of(

                IntegerChromosome.of(0, 100, 18));//,
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

                    System.out.println("gen " + ind.generation());
                    //  System.out.println("fit " + ind.bestFitness());
                    JeneticsHelper.getBest(ir, ((IntegerChromosome) g.get(0)).toArray(), termQueryList, k);

                })
                .peek(statistics)
                .collect(toBestPhenotype());

        System.out.println("Final result  " + result);
        Genotype<IntegerGene> g = result.genotype();

        int[] a2 = ((IntegerChromosome) g.get(0)).toArray();
        List<BooleanQuery.Builder> bqbList = QueryListFromChromosomeJ.getOneWordQueryPerCluster(ir, a2, termQueryList, k);

        System.out.println("Best of run **********************************");
        // Map<Query, Integer> queryMap = JeneticsHelper.getQueries(ir,bqbList);

        List<Query> queryList = JeneticsHelper.getBest(ir, a2, termQueryList, k, true);
        JeneticsHelper.classify(indexEnum, queryList, k);
        System.out.println("statistics " + statistics);
    }
}