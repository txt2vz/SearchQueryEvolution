package cluster;

import index.*;
import io.jenetics.engine.EvolutionStatistics;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import io.jenetics.*;
import io.jenetics.engine.Engine;
import io.jenetics.util.Factory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import index.IndexEnum;

import static io.jenetics.engine.EvolutionResult.toBestPhenotype;

public class JeneticsMain {

    static List<TermQuery> termQueryList;
    static QueryListFromChromosome qlc;

    // private final static List<TermQuery> termQueryArray = iterms.getTFIDFTermQueryList();

    public static void main(String[] args) throws Exception {

        Indexes.setIndex(IndexEnum.NG3);
        termQueryList = ImportantTermQueries.getTFIDFTermQueryList(Indexes.getIndexReader());
        qlc = new QueryListFromChromosome(termQueryList);
        final int numberOfJobs = 2;
        int job = 1;

        //  IntStream.range(0, numberOfJobs).forEach(job ->
        new JeneticsMain(job);
        //  );
    }

    private static double sqf(final Genotype<IntegerGene> gt) {

        int[] a = ((IntegerChromosome) gt.get(0)).toArray();
        double f = JeneticsHelper.getF(qlc, a);


       // BooleanQuery.Builder[] bqbArray = qlc.getSimple(((IntegerChromosome) gt.get(0)).toArray());
        //QueryListFromChromosome
        // .getOR_List(((IntegerChromosome) gt.getChromosome(0)).toArray(), termQueryArray, Indexes.NUMBER_OF_CLUSTERS, BooleanClause.Occur.SHOULD, 1);

      //  ClusterFitness clusterFitness = new ClusterFitness();
      //  clusterFitness.setClusterFitness(Collections.unmodifiableList(Arrays.asList(bqbArray)));
     //   System.out.println (clusterFitness.queryShort());

        // clusterFitness.setClusterFitness( new HashSet<BooleanQuery.Builder>(Arrays.asList(bqbArray)));  //java 9 Set.of(bqbArray)

        return f;// clusterFitness.getFitness();
    }


    public JeneticsMain(int job) {

        IndexEnum indexEnum = IndexEnum.NG3;
        // static IndexSearcher indexSearcher = indexEnum.getIndexSearcher();
        IndexReader indexReader = indexEnum.getIndexReader();

        final int popSize = 248;
        final long maxGen = 41;
        Analysis finalReport = new Analysis();
        int maxGene = 100;
        int genomeLength = 18;
        ClusterFitness.setFitnessMethod(FitnessMethod.UNIQUE_HITS_COUNT);

        final Factory<Genotype<IntegerGene>> gtf = Genotype.of(

                //   new IntegerChromosome(-1, maxGene, genomeLength));
                IntegerChromosome.of(0, 100, 6));//,
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
                    // sqf(g).generationStats(ind.generation());
                  //  ind.generation();
                   // ind.bestFitness()
                    System.out.println("gen " + ind.generation());
                    System.out.println("fit " + ind.bestFitness());

                })
                .peek(statistics)
                .collect(toBestPhenotype());

      //  System.out.println("gen rse " + result.generation());
        System.out.println("Final result job " + job + " " + result);
        Genotype<IntegerGene> g = result.genotype();
        double cfResult = sqf(g);
        // System.out.println("cluster fit result " + cfResult.queryShort());
        //  finalReport.reportsOut(job, (int) result.getGeneration(), popSize, subpops, genomeLength, maxGene, cfResult);
        System.out.println("statistics " + statistics);

        double d = sqf(g);


    }
}