package cluster

import spock.lang.Specification

class ClusterFitnessTest extends Specification {

    def "check psuedo f1 precision recall "(){
        setup:
      //  def cf = new ClusterFitness()
        def totalHits = 10
        def totalDocs = 20
        def negativeHits = 4
        def positiveHits = 5
        def missedDocs = totalDocs - totalHits

        when:
        def precision = positiveHits/totalHits
        def recall = positiveHits / totalDocs
        def f1 = (2 * (precision * recall) ) / (precision + recall)
        def pseudoF1 = 2 * positiveHits / (2 * positiveHits + negativeHits + missedDocs)

        then:
        println "preciionst $precision  pseudo_recall $recall"
        println "f1 $f1 pseudoF1 $pseudoF1 missed docs $missedDocs"

        precision ==0.5
        recall == 0.25
    }
}
