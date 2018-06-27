package cluster;

import index.ImportantTerms;
import index.Indexes;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.jenetics.*;
import org.jenetics.engine.Engine;
import org.jenetics.util.Factory;

import java.util.List;
import java.util.stream.IntStream;

import static org.jenetics.engine.EvolutionResult.toBestPhenotype;

public class ClusterMainJenetics {

    private final static TermQuery[] termQueryArray = new ImportantTerms().getTFIDFTermQueryList() ;

    private static double evaluate(final Genotype<IntegerGene> gt) {

        ClusterFitness cfit = cf(gt);
        return cfit.getFitness();
    }

    private static ClusterFitness cf(final Genotype<IntegerGene> gt) {
        List<BooleanQuery.Builder> bqbList = QueryListFromChromosome
                .getSimpleQueryList(((IntegerChromosome) gt.getChromosome(0)).toArray(), termQueryArray, Indexes.NUMBER_OF_CLUSTERS, BooleanClause.Occur.SHOULD, 1);

        ClusterFitness clusterFitness = new ClusterFitness();
        clusterFitness.setClusterFitness(bqbList);

        return clusterFitness;
    }

    public static void main(String[] args) throws Exception {
        final int numberOfJobs = 2;
        IntStream.range(0, numberOfJobs).forEach(job ->
                new ClusterMainJenetics(job)
        );
    }

    public ClusterMainJenetics(int job) {

        final int popSize = 512;
        final int subpops = 1;
        final long maxGen = 210;
        JobReport finalReport = new JobReport();
        int maxGene=100;
        int genomeLength=18;

        final Factory<Genotype<IntegerGene>> gtf = Genotype.of(

                new IntegerChromosome(-1, maxGene, genomeLength));

        final Engine<IntegerGene, Double> engine = Engine.builder(ClusterMainJenetics::evaluate, gtf).populationSize(popSize)
                // .survivorsSelector(new
                // StochasticUniversalSelector<>()).offspringSelector(new
                // TournamentSelector<>(5))
                .survivorsSelector(new TournamentSelector<>(3)).offspringSelector(new TournamentSelector<>(3))
                .alterers(new Mutator<>(0.2), new SinglePointCrossover<>(0.7)).build();

        final Phenotype<IntegerGene, Double> result = engine.stream().limit(maxGen).peek(ind -> {

            Genotype<IntegerGene> g = ind.getBestPhenotype().getGenotype();
            cf(g).generationStats(ind.getGeneration());
            System.out.println();
        }).collect(toBestPhenotype());

        System.out.println("Final result job " + job + " " + result);
        Genotype<IntegerGene> g = result.getGenotype();
        ClusterFitness cfResult = cf(g);
        System.out.println("cluster fit result " + cfResult.queryShort());
      //  finalReport.queriesReport(job, (int) result.getGeneration(), popSize, subpops, genomeLength, maxGene, cfResult);
        System.out.println();
    }
}