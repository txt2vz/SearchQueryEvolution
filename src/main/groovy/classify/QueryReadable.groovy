package classify

import groovy.json.JsonBuilder
import index.Indexes
import org.apache.lucene.search.Query

import java.util.regex.Pattern

class QueryReadable {
	
	public static String getQueryJSONForViz  (Query query){
		
		final String s0 = getQueryMinimal(query)
	//	def s1 = s0.replaceAll("\\(", "");
		def s1 = s0.replaceAll("10", "");
		//classify.query.toString(
			//IndexInfoStaticG.FIELD_CONTENTS).replaceAll(Pattern.quote("+"), "");

		if (s1 ==null || s1.length() <2) return;
		def s2 = s1[1..s1.length()-2 ].split(Pattern.quote(") ("))  //remove initial and last bracket
		
		def data = 	 s2.collect {			
							def l=it.split()
							def a = l[0]
							def b = l[1]
							[source: a,
							 target: b
							]			
						}
			
		def json = new JsonBuilder(data)		
		return json; 
	}

	public static String getQueryMinimal(Query query) {

		final String queryWithoutComma = query.toString(
				Indexes.FIELD_CONTENTS).replaceAll(", ", "#~");

		boolean spanF = queryWithoutComma.contains("spanFirst");
		boolean spanN = queryWithoutComma.contains("spanNear")
		// spanNear([atheism#~god]#~10#~true) spanNear([atheist#~said]#~10#~true) spanNear([atheists#~belief]#~10#~true) spanNear([belief#~atheists]#~10#~true) spanNear([benedikt#~rosenau]#~10#~true) spanNear([christian#~moral]#~10#~true) spanNear([claim#~solntze.wpd.sgi.com]#~10#~true) spanNear([islam#~islamic]#~10#~true) spanNear([jaeger#~buphy.bu.edu]#~10#~true) spanNear([keith#~schneider]#~10#~true) spanNear([livesey#~jon]#~10#~true) spanNear([mozumder#~atheism]#~10#~true) spanNear([po.cwru.edu#~keith]#~10#~true) spanNear([political#~atheists]#~10#~true) spanNear([religious#~say]#~10#~true) spanNear([rushdie#~islamic]#~10#~true) spanNear([ryan#~po.cwru.edu]#~10#~true) spanNear([say#~christian]#~10#~true) spanNear([therefore#~atheism]#~10#~true)
		if (spanN){
			def s = queryWithoutComma.replaceAll(
					"spanNear", "");
			s = s.replaceAll("#~true", "");
			s = s.replaceAll("#~false", "");
			s = s.replaceAll("\\[", "");
			s = s.replaceAll("\\]", "");
			s = s.replaceAll("#~", " ");
			println "s is $s"
			return s
		}

		if (spanF) {
 
			String s = queryWithoutComma.replaceAll(
					"spanFirst", "");
			s = s.replaceAll("\\(", "");
			s = s.replaceAll(" ", "")
			s = s.replaceAll("\\)", "#~");

			List sfList = s.tokenize("#~")

			def spanFirstMap = [:]

			for (int x = 0; x < sfList.size; x = x + 2) {

				if (sfList[x+1]==null) continue;

				def word = sfList[x]
				if (spanFirstMap.containsKey(word)) {
					spanFirstMap.put(word, Math.max(spanFirstMap.get(word),Integer.parseInt(sfList[x +1])))
				} else{
					spanFirstMap.put((word), Integer.parseInt(sfList[x +1]).intValue());
				}
			}

			spanFirstMap = spanFirstMap.sort { it.value}

			def sfshort = ''
			spanFirstMap.each { entry ->
				sfshort += "("+ entry.key + " "+ entry.value + ")"
			}

			return sfshort;
		} else
			return queryWithoutComma;
	}


	static main(args) {
		def q = 	"(clinton hillari) (clinton trump) (i clinton) (hillari t.co) (clinton readi) (clinton http) (berni clinton) (hillaryclinton t.co) (clinton democrat)"

		println " "
		println "query is $q"
		println   getQueryJSONForViz(q)//  getQueryMinimal(query)
	}
}
