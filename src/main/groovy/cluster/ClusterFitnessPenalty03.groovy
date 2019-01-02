package cluster

class ClusterFitnessPenalty03 extends ClusterFitness {

    double getFitness() {
        double f1WithPenalty = pseudo_f1 - (0.03 * k)
        baseFitness = f1WithPenalty > 0 ? f1WithPenalty : 0
        return baseFitness
    }
}
