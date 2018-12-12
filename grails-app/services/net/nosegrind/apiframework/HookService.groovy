package net.nosegrind.apiframework

import grails.converters.JSON
import grails.converters.XML
import org.grails.validation.routines.UrlValidator
import org.grails.core.artefact.DomainClassArtefactHandler
import grails.core.GrailsDomainClass

class HookService {

	def grailsApplication

    static transactional = false
	
    int postData(String service, String data) {
		return send(data, service)
	}



    private boolean send(String data, String service) {

		def hooks = grailsApplication.getClassForName('net.nosegrind.apiframework.Hook').findAll("from Hook where is_enabled=true and service=?",[service])

		/*
		GrailsDomainClass dc = grailsApplication.getDomainClass('net.nosegrind.apiframework.Hook')
		def tempHook = dc.clazz.newInstance()

		def hooks = tempHook.find("from Hook where service=?",[service])
		*/

		hooks.each { hook ->
			String format = hook.format.toLowerCase()
			if(hook.attempts>=grailsApplication.config.apitoolkit.attempts){
				data = 	[message:'Number of attempts exceeded. Please reset hook via web interface']
			}

			HttpURLConnection myConn= null
			DataOutputStream os = null
			BufferedReader stdInput = null
			try{
				URL hostURL = new URL(hook.url.toString())
				myConn= (HttpURLConnection)hostURL.openConnection()
				myConn.setRequestMethod("POST")
				myConn.setRequestProperty("Content-Type", "application/json")
				myConn.setRequestProperty("Authorization", "Bearer 7ulupum69o6dsl3utm87uvira39a6jcd")
				myConn.setUseCaches(false)
				myConn.setDoInput(true)
				myConn.setDoOutput(true)
				myConn.setReadTimeout(15*1000)

				myConn.connect()

				OutputStreamWriter out = new OutputStreamWriter(myConn.getOutputStream())
				out.write(data)
				out.close()

				int code =  myConn.getResponseCode()
				myConn.diconnect()

				return code
			}catch (Exception e){
				try{
					Thread.sleep(15000)
				}catch (InterruptedException ie){
					println(e)
				}
			} finally{
				if (myConn!= null){
					myConn.disconnect()
				}
				if (stdInput != null){
					try{
						stdInput.close()
					}catch (IOException io){
						println(io)
					}
				}
			}
			return 400
		}
	}
	
	Map formatDomainObject(Object data){
	    def nonPersistent = ["log", "class", "constraints", "properties", "errors", "mapping", "metaClass","maps"]
	    def newMap = [:]
	    data.getProperties().each { key, val ->
	        if (!nonPersistent.contains(key)) {
				if(grailsApplication.isDomainClass(val.getClass())){
					newMap.put key, val.id
				}else{
					newMap.put key, val
				}
	        }
	    }
		return newMap
	}
	
	Map processMap(Map data,Map processor){
		processor.each() { key, val ->
			if(!val?.trim()){
				data.remove(key)
			}else{
				def matcher = "${data[key]}" =~ "${data[key]}"
				data[key] = matcher.replaceAll(val)
			}
		}
		return data
	}
	
	boolean validateUrl(String url){
		try {
			String[] schemes = ["http", "https"]
			UrlValidator urlValidator = new UrlValidator(schemes)
			if (urlValidator.isValid(url)) {
				return true
			} else {
				return false
			}
		}catch(Exception e){
			println(e)
		}
		return false
	}
}
