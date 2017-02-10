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

import javax.annotation.Resource
import grails.core.GrailsApplication
import grails.plugin.springsecurity.SpringSecurityService
import grails.util.Metadata
import groovy.json.JsonSlurper
import net.nosegrind.apiframework.RequestMethod

import org.grails.web.util.WebUtils

import javax.servlet.http.HttpServletResponse
import groovy.transform.CompileStatic


@CompileStatic
class BatchInterceptor extends ApiCommLayer{

	int order = HIGHEST_PRECEDENCE + 998

	@Resource
	GrailsApplication grailsApplication

	ApiCacheService apiCacheService
	SpringSecurityService springSecurityService


	String entryPoint = "b${Metadata.current.getProperty(Metadata.APPLICATION_VERSION, String.class)}"
	String format
	String mthdKey
	RequestMethod mthd

	BatchInterceptor(){
		match(uri:"/${entryPoint}/**")
	}

	boolean before(){
		//println('##### BATCHINTERCEPTOR (BEFORE)')

		format = (request?.format)?request.format:'JSON';
		mthdKey = request.method.toUpperCase()
		mthd = (RequestMethod) RequestMethod[mthdKey]

		//Map methods = ['GET':'show','PUT':'update','POST':'create','DELETE':'delete']
		boolean restAlt = RequestMethod.isRestAlt(mthd.getKey())

		// Init params
		//String format =request.format.toUpperCase()

		LinkedHashMap dataParams = [:]
		switch (format) {
			case 'JSON':
				String json = request.JSON.toString()
				def slurper = new JsonSlurper()
				slurper.parseText(json).each() { k, v ->
					dataParams[k] = v
				}
				request.setAttribute("JSON", dataParams)
				break
			default:
				render(status:HttpServletResponse.SC_BAD_REQUEST  , text: 'Expecting JSON Formatted batch data')
				return false
		}



		try{
			//if(request.class.toString().contains('SecurityContextHolderAwareRequestWrapper')){

			LinkedHashMap cache = (params.controller)? apiCacheService.getApiCache(params.controller.toString()):[:]

			if(cache) {
				params.apiObject = (params.apiObjectVersion)?params.apiObjectVersion:cache['currentStable']['value']
				params.action = (params.action==null)?cache['defaultAction']:params.action

				String expectedMethod = cache[params.apiObject][params.action.toString()]['method'] as String
				if(!checkRequestMethod(mthd,expectedMethod,restAlt)) {
					render(status: HttpServletResponse.SC_BAD_REQUEST, text: "Expected request method '${expectedMethod}' does not match sent method '${request.method}'")
					return false
				}

				if (request?.getAttribute('batchInc')==null) {
					request.setAttribute('batchInc',0)
				}else{
					Integer newBI = (Integer) request?.getAttribute('batchInc')
					request.setAttribute('batchInc',newBI+1)
				}

				setBatchParams(params)

				LinkedHashMap receives = cache[params.apiObject.toString()][params.action.toString()]['receives'] as LinkedHashMap
				boolean requestKeysMatch = checkURIDefinitions(params,receives)

				if(!requestKeysMatch){
					render(status:HttpServletResponse.SC_BAD_REQUEST, text: 'Expected request variables do not match sent variables')
					return false
				}



				// RETRIEVE CACHED RESULT
				/*
				if (cache[params.apiObject][params.action.toString()]['cachedResult']) {
					String authority = getUserRole() as String
					String domain = ((String) params.controller).capitalize()
					//Integer version = (Integer) JSON.parseText((String)cache[params.apiObject][params.action.toString()]['cachedResult'][authority]).version
					def slurper = new JsonSlurper()
					groovy.json.internal.LazyMap json = (groovy.json.internal.LazyMap) slurper.parseText((String)cache[params.apiObject][params.action.toString()]['cachedResult'][authority][request.format.toUpperCase()])

					if (isCachedResult((Integer)json.get('version'), domain)) {
						def result = cache[params.apiObject][params.action.toString()]['cachedResult'][authority][request.format.toUpperCase()]
						render(text: result, contentType: request.contentType)
						return false
					}
				}
				*/

				if(!params.action){
					String methodAction = mthd.toString()
					if(!cache[params.apiObject][methodAction]){
						params.action = cache[params.apiObject]['defaultAction']
					}else{
						params.action = mthd.toString()

						// FORWARD FOR REST DEFAULTS WITH NO ACTION
						String[] tempUri = request.getRequestURI().split("/")
						if(tempUri[2].contains('dispatch') && "${params.controller}.dispatch" == tempUri[2] && !cache[params.apiObject]['domainPackage']){
							forward(controller:params.controller,action:params.action,params:params)
							return false
						}
					}
				}

				// SET PARAMS AND TEST ENDPOINT ACCESS (PER APIOBJECT)
				ApiDescriptor cachedEndpoint = cache[(String)params.apiObject][(String)params.action] as ApiDescriptor
				boolean result = handleBatchRequest(cachedEndpoint['deprecated'] as List, cachedEndpoint['method']?.toString().trim(), mthd, response, params)
				//boolean result = handleBatchRequest(cache[params.apiObject.toString()][params.action.toString()], request, response, params)
				return result
			}

			//}
			return false

		}catch(Exception e) {
			log.error("[ApiToolkitFilters :: preHandler] : Exception - full stack trace follows:", e)
			return false
		}
	}

	boolean after(){
		//println('##### BATCHFILTER (AFTER)')
		try{
			LinkedHashMap newModel = [:]

			if (!model) {
				render(status:HttpServletResponse.SC_NOT_FOUND , text: 'No resource returned')
				return false
			} else {
				newModel = convertModel(model)
			}

			LinkedHashMap cache = apiCacheService.getApiCache(params.controller.toString())
			//LinkedHashMap content
			int batchLength = (int) request.getAttribute('batchLength')
			int batchInc = (int) request.getAttribute('batchInc')
			if(batchEnabled && (batchLength > batchInc+1)){
				WebUtils.exposeRequestAttributes(request, params);
				// this will work fine when we upgrade to newer version that has fix in iut
				params.uri = request.forwardURI.toString()
				forward(params)
				return false
			}

			ApiDescriptor cachedEndpoint = cache[params.apiObject][(String)params.action] as ApiDescriptor
			String content = handleBatchResponse(cachedEndpoint['returns'] as LinkedHashMap,cachedEndpoint['roles'] as List,mthd,format,response,newModel,params) as LinkedHashMap

			//content = handleBatchResponse(cache[params.apiObject][params.action.toString()],request,response,newModel,params) as LinkedHashMap

			if(content){
				render(text:content, contentType:request.contentType)
				return false
			}

			return false
		}catch(Exception e){
			log.error("[ApiToolkitFilters :: apitoolkit.after] : Exception - full stack trace follows:", e);
			return false
		}

	}

}