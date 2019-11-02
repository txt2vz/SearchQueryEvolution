package index

class ImportantTermsSpec extends spock.lang.Specification {

	def "importantTerms F1 oil"() {
		setup:
		Indexes.instance.setIndex(IndexEnum.R10)
		Indexes.instance.setCategoryNumber('2')
		Indexes.instance.setIndexFieldsAndTotals()

		when:
		def oilList = ImportantTermQueries.getF1TermQueryList (Indexes.indexSearcher)

		then:
		oilList[0].toString((Indexes.FIELD_CONTENTS)) == 'oil'
		oilList[3].getTerm().text() == 'petroleum'
	}
	
	def "importantTerms F1 20NG graphics"() {
		setup:
		Indexes.instance.setIndex(IndexEnum.NG20)
		Indexes.instance.setCategoryNumber('2')
		Indexes.instance.setIndexFieldsAndTotals()

		when:
		def graphicsList = ImportantTermQueries.getF1TermQueryList (Indexes.indexSearcher)

		then:
		graphicsList[0].toString((Indexes.FIELD_CONTENTS)) == 'windows'
		graphicsList[4].getTerm().text() == 'files'
	}

	def "ImportantTerms 20News3 tfidf"(){
		setup:
		Indexes.instance.setIndex(IndexEnum.NG3)
		Indexes.instance.setIndexFieldsAndTotals()

		when:
		def tfidfList = ImportantTermQueries.getTFIDFTermQueryList(Indexes.indexReader)

		then:
		tfidfList[0].getTerm().text() == 'god'
		tfidfList[1].getTerm().text() == 'space'
		tfidfList[2].getTerm().text() == 'nasa'
	}
}