package cluster


import spock.lang.Specification

class AnalysisTest extends Specification {
    def "OverallSummary"() {
        setup:

        def analysisAndReports = new Analysis()
//        Tuple5 indexAndParams0 = new Tuple5(IndexEnum.NG3.name(), FitnessMethod.PSEUDOF1, QueryType.OR3_INSTERSECT_SETK, true, 0)
//        Tuple5 indexAndParams1 = new Tuple5(IndexEnum.NG3.name(), FitnessMethod.PSEUDOF1_K_PENALTY0_3, QueryType.OR3_INSTERSECT_SETK, true, 1)
//        Tuple5 indexAndParams2 = new Tuple5(IndexEnum.NG3.name(), FitnessMethod.PSEUDOF1, QueryType.OR3_INSTERSECT_SETK, true, 2)
//
//        Tuple5 indexAndParams3 = new Tuple5(IndexEnum.CRISIS3.name(), FitnessMethod.PSEUDOF1, QueryType.OR3_INSTERSECT_SETK, true, 0)
//        Tuple5 indexAndParams4 = new Tuple5(IndexEnum.CRISIS3.name(), FitnessMethod.PSEUDOF1, QueryType.OR3_INSTERSECT_SETK, true, 1)
//        Tuple5 indexAndParams5 = new Tuple5(IndexEnum.CRISIS3.name(), FitnessMethod.PSEUDOF1, QueryType.OR3_INSTERSECT_SETK, true, 2)
//        Tuple5 indexAndParams6 = new Tuple5(IndexEnum.CRISIS3.name(), FitnessMethod.PSEUDOF1_K_PENALTY0_3, QueryType.OR3_INSTERSECT_SETK, true, 2)

//        when:   //pseudoF1, F1
//
//
//        analysisAndReports.resultsFitnessWithF1 << [(indexAndParams0): new Tuple2(0.5, 0.6)]
//        analysisAndReports.resultsFitnessWithF1 << [(indexAndParams1): new Tuple2(0.7, 0.82)]
//
//        then:
//        println "index " + IndexEnum.NG3.name()
//        println  " analysisAndReports.resultsFitnessWithF1 " + analysisAndReports.resultsFitnessWithF1
//        analysisAndReports.f1fromMaxPseudoF1(0) == 0.82
//
//        when:
//        analysisAndReports = new AnalysisAndReports()
//        analysisAndReports.resultsFitnessWithF1 << [(indexAndParams0): new Tuple2(0.7, 0.6)]
//        analysisAndReports.resultsFitnessWithF1 << [(indexAndParams1): new Tuple2(0.8, 0.5)]
//
//        println  "size " + analysisAndReports.resultsFitnessWithF1.size()
//
//        then:
//        analysisAndReports.f1fromMaxPseudoF1(0) == 0.5
//
//        when:
//        analysisAndReports = new AnalysisAndReports()
//        analysisAndReports.resultsFitnessWithF1 << [(indexAndParams0): new Tuple2(0.7, 0.6)]
//        analysisAndReports.resultsFitnessWithF1 << [(indexAndParams1): new Tuple2(0.2, 0.8)]
//        analysisAndReports.resultsFitnessWithF1 << [(indexAndParams2): new Tuple2(0.8, 0.4)]
//
//        then:
//        analysisAndReports.f1fromMaxPseudoF1(0) == 0.4
//
//        when:
//        analysisAndReports = new AnalysisAndReports()
//        analysisAndReports.resultsFitnessWithF1 << [(indexAndParams0): new Tuple2(0.7, 0.7)]
//        analysisAndReports.resultsFitnessWithF1 << [(indexAndParams1): new Tuple2(0.8, 0.4)]
//        analysisAndReports.resultsFitnessWithF1 << [(indexAndParams2): new Tuple2(0.7, 0.7)]
//        analysisAndReports.resultsFitnessWithF1 << [(indexAndParams3): new Tuple2(0.7, 0.7)]
//        analysisAndReports.resultsFitnessWithF1 << [(indexAndParams4): new Tuple2(0.9, 0.2)]
//        analysisAndReports.resultsFitnessWithF1 << [(indexAndParams5): new Tuple2(0.7, 0.7)]
//
//        then:
//        println analysisAndReports.f1fromMaxPseudoF1(0)
//        analysisAndReports.f1fromMaxPseudoF1(0) == 0.3


        when:
        analysisAndReports = new Analysis()
//        analysisAndReports.resultsFitnessWithF1 << [( new Tuple5(IndexEnum.CRISIS3.name(), FitnessMethod.PSEUDOF1, QueryType.OR3_INSTERSECT_SETK, true, 0)): new Tuple2(0.7, 0.3)]
//        analysisAndReports.resultsFitnessWithF1 << [(new Tuple5(IndexEnum.CRISIS3.name(), FitnessMethod.PSEUDOF1_K_PENALTY0_3, QueryType.OR3_INSTERSECT_SETK, true, 1)): new Tuple2(0.99, 0.4)]
//        analysisAndReports.resultsFitnessWithF1 << [(new Tuple5(IndexEnum.CRISIS3.name(), FitnessMethod.PSEUDOF1, QueryType.OR3_INSTERSECT_SETK, true, 2)): new Tuple2(0.9, 0.1)]
//        analysisAndReports.resultsFitnessWithF1 << [(new Tuple5(IndexEnum.NG3.name(), FitnessMethod.PSEUDOF1, QueryType.OR3_INSTERSECT_SETK, true, 2)): new Tuple2(0.8, 0.2)]
//        analysisAndReports.resultsFitnessWithF1 << [(new Tuple5(IndexEnum.NG3.name(), FitnessMethod.PSEUDOF1_K_PENALTY0_3, QueryType.OR3_INSTERSECT_SETK, true, 2)): new Tuple2(0.9, 0.6)]


then:
       // println  "xx " + analysisAndReports.f1fromMaxPseudoF1(0)
        println "   analysisAndReports.resultsFitnessWithF1 " +   analysisAndReports.resultsFitnessWithF1
//analysisAndReports.f1fromMaxPseudoF1(0) == 0.5




    }
}
