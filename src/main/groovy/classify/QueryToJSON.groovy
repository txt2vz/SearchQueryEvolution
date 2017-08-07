package classify

import groovy.json.JsonBuilder
import org.apache.lucene.search.Query

import java.util.regex.Pattern

class QueryToJSON {

	public String getQueryForViz (Query query){

		def s1 = 
		//"atheists subject) (atheist free) (god atheists) (conner atheist) (vice.ico.tek.com subject) (solntze.wpd.sgi.com subject) (made atheists) (god reason) (god posting) (vice.ico.tek.com atheism"
		//"morality atheists) (subject atheists) (subject islamic) (atheists word) (islamic nntp) (moral atheists) (article morality) (morality didn't) (atheists discussion) (subject atheism"
	//	"subject motherboard) (bios subject) (subject scsi) (diamond subject) (subject floppy) (program motherboard) (motherboard anyone) (pin subject) (subject card) (dma card"
	//	"lines graphics) (graphics d) (points graphics) (image lines) (running graphics) (tiff lines) (advance graphics) (image vga) (card graphics) (card algorithm"
		"clinton hillari) (clinton trump) (i clinton) (hillari t.co) (clinton readi) (clinton http) (berni clinton) (hillaryclinton t.co) (clinton democrat) (state clinton"
		
		
		//"organization vga) (organization mode) (irq lines) (mode ok) (mode least) (one memory) (gateway ibm) (thanks ibm) (thanks graphics) (lines pc) (non one) (problems try) (data recently) (drive lines) (settings seagate) (2 graphics) (newsreader running) (graphics cards) (drive adaptec) (cards don't"
			
		//" atheist belief) (atheists host) (once bible) (thought ryan) (bible i3150101) (truth atheists) (mean particular) (truth institute) (god article) (host atheists) (may benedikt) (bu.edu jaeger) (saturn.wwc.edu ever) (jaeger proof) (i've i3150101) (life okcforum.osrhe.edu) (newsreader one) (little belief) (kmr4 article) (proof certainly)"
		//	final String queryWithoutPlus = classify.query.toString(
		//		IndexInfoStaticG.FIELD_CONTENTS).replaceAll(Pattern.quote("+"), "");
		//


		//def s3 = queryWithoutPlus.replaceAll(Pattern.quote(")"), "~#")

		def s2 = s1.split(Pattern.quote(") ("))
		println "s2: $s2  "

		//	def s3 = s2.replaceAll(Pattern.quote("("), " ")
		//	println "s3: $s3"

		def data = [

			links: s2.collect {

				def l=it.split()
				def a = l[0]
				def b = l[1]
				[source: a,
					target: b					
				]

			}
		]

		println " data " + data
		def json = new JsonBuilder(data)
		println "json $json"

		//return queryWithoutPlus;
	}

	static main (args){
		def x = new QueryToJSON()
		x.getQueryForViz()
	}

}
