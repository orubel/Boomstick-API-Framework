/*
 * The MIT License (MIT)
 * Copyright 2014 Owen Rubel
 *
 * IO State (tm) Owen Rubel 2014
 * API Chaining (tm) Owen Rubel 2013
 *
 *   https://opensource.org/licenses/MIT
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * The above copyright/trademark notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.nosegrind.apiframework

import org.grails.web.json.JSONObject
import grails.core.GrailsApplication

//import grails.util.Holders
import grails.converters.JSON
import grails.util.Environment
import net.nosegrind.apiframework.ApiDescriptor
import net.nosegrind.apiframework.ApiStatuses

//import grails.application.cache.GrailsCacheManager
//import org.springframework.cache.CacheManager
//import grails.application.springsecurity.SpringSecurityService
import org.grails.groovy.grails.commons.*

class ApiObjectService{

	GrailsApplication grailsApplication
	//SpringSecurityService springSecurityService
	//ApiToolkitService apiToolkitService
	//GrailsCacheManager grailsCacheManager
	//CacheManager cacheManager
	ApiCacheService apiCacheService
	def grailsResourceLocator
	
	
	static transactional = false
	
	public initialize(){
		//println("#### ApiObjectService > initialize")
		//Object version = 0.1

		try {
			if(grailsApplication.config.apitoolkit.serverType=='master'){
				String ioPath
				if(grailsApplication.isWarDeployed()){
					//ioPath = Holders.servletContext.getRealPath('/')
					ioPath = grailsApplication.mainContext.servletContext.getRealPath('/')
					if(Environment.current == Environment.DEVELOPMENT || Environment.current == Environment.TEST){
						ioPath += 'WEB-INF/classes/iostate'
					}else{
						// test in Environment.PRODUCTION
						ioPath += 'WEB-INF/classes/iostate'
					}
				}else{
					//ioPath = (String) BuildSettings.BASE_DIR?.path
					//ioPath = System.getProperty(BuildSettings.PROJECT_RESOURCES_DIR)
					//GrailsPluginManager pluginMngr = grailsApplication.getMainContext().getBean('pluginManager')
					//ioPath = pluginMngr.getGrailsPlugin("grailsApiFramework").getPluginPath()
					if(Environment.current == Environment.DEVELOPMENT || Environment.current == Environment.TEST){
						//ioPath = grailsApplication.mainContext.getResources("conf/iostate").path
						ioPath = 'src/iostate'
					}else{
						// test in Environment.PRODUCTION
						//ioPath = grailsApplication.mainContext.getResources("conf/iostate").path
						ioPath = 'src/iostate'
					}
					
				}
				parseFiles(ioPath)
			}
			
			String apiObjectSrc = grailsApplication.config.apitoolkit.iostate.preloadDir
			parseFiles(apiObjectSrc.toString())

		} catch (Exception e) {
			throw new Exception("[ApiObjectService :: initialize] : Exception - full stack trace follows:",e)
		}
	}
	
	private Map parseFile(JSONObject json){
		String apiObjectName = json.NAME.toString()
		parseJson(json.NAME.toString(),json)
	}
	
	private parseFiles(String path){
		//println("#### ApiObjectService > parseFiles : "+path)
		new File(path).eachFile() { file ->
			def tmp = file.name.toString().split('\\.')

			if(tmp[1]=='json'){
				try{
					JSONObject json = JSON.parse(file.text)
					parseJson(json.NAME.toString(),json)
					//def cache = apiCacheService.getApiCache(apiName)
				}catch(Exception e){
					throw new Exception("[ApiObjectService :: initialize] : Unacceptable file '${file.name}' - full stack trace follows:",e)
				}
			}
		}
	}
	
	String getKeyType(String reference, String type){
		String keyType = (reference.toLowerCase()=='self')?((type.toLowerCase()=='long')?'PKEY':'INDEX'):((type.toLowerCase()=='long')?'FKEY':'INDEX')
		return keyType
	}
	
	private LinkedHashMap getIOSet(JSONObject io,LinkedHashMap apiObject){
		LinkedHashMap<String,ParamsDescriptor> ioSet = [:]

		io.each{ k, v ->
			// init
			if(!ioSet[k]){ ioSet[k] = [] }
			
			def roleVars=v.toList()
			roleVars.each{ val ->
				if(v.contains(val)){
					if(!ioSet[k].contains(apiObject[val])){
						ioSet[k].add(apiObject[val])
					}
				}
			}
		}
		
		// add permitAll vars to other roles after processing
		ioSet.each(){ key, val ->
			if(key!='permitAll'){
				ioSet['permitAll'].each(){ it ->
						ioSet[key].add(it)
				}
			}
		}
		
		return ioSet
	}
	
	private ApiDescriptor createApiDescriptor(String apiname,String apiMethod, String apiDescription, List apiRoles, List batchRoles, String uri, JSONObject values, JSONObject json){
		LinkedHashMap<String,ParamsDescriptor> apiObject = [:]
		ApiParams param = new ApiParams()
		LinkedHashMap mocks = [
			"STRING":'Mock String',
			"DATE":'Mock Date',
			"LONG":999,
			"BOOLEAN":true,
			"FLOAT":0.01,
			"BIGDECIMAL":123456789,
			"EMAIL":'test@mockdata.com',
			"URL":'www.mockdata.com',
			"ARRAY":['this','is','mock','data']
		]
		
		List fkeys = []
		values.each{ k,v ->
			v.type = (v.references)?getKeyType(v.references, v.type):v.type
			if(v.type=='FKEY'){
				fkeys.add(k)	
			}
			
			String references = ''
			String hasDescription = ''
			String hasMockData = mocks[v.type]?mocks[v.type]:''

			param.setParam(v.type,k)
			
			def configType = grailsApplication.config.apitoolkit.apiobject.type."${v.type}"
			
			hasDescription = (configType?.description)?configType.description:hasDescription
			hasDescription = (v?.description)?v.description:hasDescription
			if(hasDescription){ param.hasDescription(hasDescription) }
			
			references = (configType?.references)?configType.references:""
			references = (v?.references)?v.references:references
			if(references){ param.referencedBy(references) }
			
			hasMockData = (v?.mockData)?v.mockData:hasMockData
			if(hasMockData){ param.hasMockData(hasMockData) }

			// collect api vars into list to use in apiDescriptor
			apiObject[param.param.name] = param.toObject()
		}
		
		LinkedHashMap receives = getIOSet(json.URI[uri]?.REQUEST,apiObject)
		LinkedHashMap returns = getIOSet(json.URI[uri]?.RESPONSE,apiObject)

		ApiDescriptor service = new ApiDescriptor(
				'empty':false,
			'method':"$apiMethod",
			'fkeys':fkeys,
			'description':"$apiDescription",
			'roles':[],
			'batchRoles':[],
			'doc':[:],
			'receives':receives,
			'returns':returns
		)
		
		service['roles'] = apiRoles
		service['batchRoles'] = batchRoles

		return service
	}
	
	Boolean parseJson(String apiName,JSONObject json){
		//println("#### ApiObjectService > parseJson")
		LinkedHashMap methods = [:]
		json.VERSION.each() { vers ->
			def versKey = vers.key
			String defaultAction = (vers.value['DEFAULTACTION'])?vers.value.DEFAULTACTION:'index'
			List deprecated = (vers.value.DEPRECATED)?vers.value.DEPRECATED:[]
			String domainPackage = (vers.value.DOMAINPACKAGE!=null || vers.value.DOMAINPACKAGE?.size()>0)?vers.value.DOMAINPACKAGE:null

			vers.value.URI.each() { it ->
				def cache = (apiCacheService?.getApiCache(apiName))?apiCacheService.getApiCache(apiName):[:]
				methods['cacheversion'] = (!cache?.cacheversion)? 1 : cache['cacheversion']+1
				
				JSONObject apiVersion = json.VERSION[vers.key]
				
				//List temp = it.key.split('/')
				//String actionname = temp[1]
				String actionname = it.key
				
				//ApiStatuses error = new ApiStatuses()
				
				ApiDescriptor apiDescriptor
				Map apiParams
				
				String apiMethod = it.value.METHOD
				String apiDescription = it.value.DESCRIPTION
				List apiRoles = it.value.ROLES
				List batchRoles = it.value.BATCH
				
				String uri = it.key
				apiDescriptor = createApiDescriptor(apiName, apiMethod, apiDescription, apiRoles, batchRoles, uri, json.get('VALUES'), apiVersion)
				if(!methods[vers.key]){
					methods[vers.key] = [:]
				}
				
				if(!methods['currentStable']){
					methods['currentStable'] = [:]
					methods['currentStable']['value'] = json.CURRENTSTABLE
				}
				if(!methods[vers.key]['deprecated']){
					methods[vers.key]['deprecated'] = []
					methods[vers.key]['deprecated'] = deprecated
				}
				
				if(!methods[vers.key]['defaultAction']){
					methods[vers.key]['defaultAction'] = defaultAction
				}


				methods[vers.key][actionname] = apiDescriptor

			}

			if(methods){
				def cache = apiCacheService.setApiCache(apiName,methods)

				cache[vers.key].each(){ key1,val1 ->
					if(!['deprecated','defaultAction','domainPackage'].contains(key1)){
						apiCacheService.setApiCache(apiName,key1, val1, vers.key)
						//val.doc = apiCacheService.generateApiDoc(apiName, key, vers.key)
						//apiCacheService.resetApiCache(apiName,key,val)
					}
				}

			}
			
		}

	}

}
