package index

class ImportantTermsSpec extends spock.lang.Specification {

	def "importantTerms F1 oil"() {
		setup:
		IndexInfo.instance.setIndex(IndexEnum.R10)
		IndexInfo.instance.setCategoryNumber('2')
		IndexInfo.instance.setIndexFieldsAndTotals()
		ImportantTerms impTerms = new ImportantTerms()

		when:
		def oilList = impTerms.getF1TermQueryList ()

		then:
		oilList[0].toString((IndexInfo.FIELD_CONTENTS)) == 'oil'
		oilList[3].getTerm().text() == 'petroleum'
	}
	
	def "importantTerms F1 20NG graphics"() {
		setup:
		IndexInfo.instance.setIndex(IndexEnum.NG20)
		IndexInfo.instance.setCategoryNumber('2')
		IndexInfo.instance.setIndexFieldsAndTotals()
		ImportantTerms impTerms = new ImportantTerms()

		when:
		def graphicsList = impTerms.getF1TermQueryList ()

		then:
		graphicsList[0].toString((IndexInfo.FIELD_CONTENTS)) == 'windows'
		graphicsList[4].getTerm().text() == 'files'
	}

	def "ImportantTerms 20News3 tfidf"(){
		setup:
		IndexInfo.instance.setIndex(IndexEnum.NG3)
		IndexInfo.instance.setIndexFieldsAndTotals()
		ImportantTerms impTerms = new ImportantTerms()

		when:
		def tfidfList = impTerms.getTFIDFTermQueryList()

		then:
		tfidfList[0].getTerm().text() == 'god'
		tfidfList[1].getTerm().text() == 'space'
		tfidfList[3].getTerm().text() == 'game'
	}
}