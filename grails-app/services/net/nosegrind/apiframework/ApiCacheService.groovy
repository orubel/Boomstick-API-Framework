package net.nosegrind.apiframework

/* ****************************************************************************
 * Copyright 2014 Owen Rubel
 *****************************************************************************/


import grails.converters.JSON
import grails.converters.XML

import java.lang.reflect.Method
import java.util.HashSet;
import java.util.Map;

import grails.plugin.cache.CacheEvict
import grails.plugin.cache.Cacheable
import grails.plugin.cache.CachePut
//import grails.plugin.cache.GrailsValueWrapper
//import grails.plugin.cache.GrailsCacheManager
import org.springframework.cache.CacheManager
//import grails.plugin.springsecurity.SpringSecurityService

import org.springframework.cache.Cache
import org.grails.groovy.grails.commons.*
import org.grails.validation.routines.UrlValidator
import org.springframework.web.context.request.RequestContextHolder as RCH
import grails.core.GrailsApplication

import net.nosegrind.apiframework.*

class ApiCacheService{

	static transactional = false
	
	GrailsApplication grailsApplication
	//SpringSecurityService springSecurityService
	ApiLayerService apiLayerService
	//ApiToolkitService apiToolkitService
	//GrailsCacheManager grailsCacheManager
	CacheManager cacheManager
	
	
	/*
	 * Only flush on RESTART.
	 * DO NOT flush while LIVE!!!
	 */
	void flushAllApiCache(){
		grailsApplication?.controllerClasses?.each { controllerClass ->
			String controllername = controllerClass.logicalPropertyName
			if(controllername!='aclClass'){
				flushApiCache(controllername)
			}
		}
	}

	/*
	 * Only flush on RESTART.
	 * DO NOT flush while LIVE!!!
	 */
	@CacheEvict(value="ApiCache",key="#controllername")
	void flushApiCache(String controllername){} 

	/*
	@CacheEvict(value="ApiCache",key="#controllername")
	Map resetApiCache(String controllername,String method,ApiDescriptor apidoc){
		setApiCache(controllername,method,apidoc)
	}
	*/
	
	@CachePut(value="ApiCache",key="#controllername")
	Map setApiCache(String controllername,Map apidesc){
		return apidesc
	}
	
	@CachePut(value="ApiCache",key="#controllername")
	LinkedHashMap setApiCache(String controllername,String methodname, ApiDescriptor apidoc, String apiversion){
		try{
			def cache = getApiCache(controllername)
			if(!cache[apiversion][methodname]){
				cache[apiversion][methodname] = [:]
			}
			if(cache[apiversion][methodname]){
				cache[apiversion][methodname]['name'] = apidoc.name
				cache[apiversion][methodname]['description'] = apidoc.description
				cache[apiversion][methodname]['receives'] = apidoc.receives
				cache[apiversion][methodname]['returns'] = apidoc.returns
				cache[apiversion][methodname]['errorcodes'] = apidoc.errorcodes
				cache[apiversion][methodname]['doc'] = generateApiDoc(controllername, methodname,apiversion)
			}else{
				throw new Exception("[ApiCacheService :: setApiCache] : sts for controller/action pair of ${controllername}/${methodname}")
			}
			return cache
		}catch(Exception e){
			throw new Exception("[ApiCacheService :: setApiCache] : Exception - full stack trace follows:",e)
		}
	}

	/*
	@CachePut(value="ApiCache",key="#controllername")
	LinkedHashMap setApiDocCache(String controllername,String methodname, String apiversion, Map apidoc){
		try{
			def cache = getApiCache(controllername)
			if(cache[apiversion][methodname]){
				cache[apiversion][methodname]['doc'] = generateApiDoc(controllername, methodname, apiversion)
			}else{
				throw new Exception("[ApiCacheService :: setApiCache] : No Cache exists for controller/action pair of ${controllername}/${methodname}")
			}
			return cache
		}catch(Exception e){
			throw new Exception("[ApiCacheService :: setApiDocCache] : Exception - full stack trace follows:",e)
		}
	}
	*/
	
	Map generateApiDoc(String controllername, String actionname, String apiversion){
		try{
			Map doc = [:]
			def cache = getApiCache(controllername)
			String apiPrefix = (grailsApplication.config.apitoolkit.apiName)?"${grailsApplication.config.apitoolkit.apiName}_v${grailsApplication.metadata['app.version']}" as String:"v${grailsApplication.metadata['app.version']}" as String
			
			if(cache){
				String path = "/${apiPrefix}-${apiversion}/${controllername}/${actionname}"
				doc = ['path':path,'method':cache[apiversion][actionname]['method'],'description':cache[apiversion][actionname]['description']]
				if(cache[apiversion][actionname]['receives']){
	
					doc['receives'] = [:]
					for(receiveVal in cache[apiversion][actionname]['receives']){
						doc['receives']["$receiveVal.key"] = receiveVal.value
					}
				}
				
				if(cache[apiversion][actionname]['returns']){
					doc['returns'] = [:]
					for(returnVal in cache[apiversion][actionname]['returns']){
						doc['returns']["$returnVal.key"] = returnVal.value
					}
					doc['json'] = [:]
					doc['json'] = apiLayerService.processJson(doc["returns"])
				}
				
				//if(cont["${actionname}"]["${apiversion}"]["errorcodes"]){
				//	doc["errorcodes"] = processDocErrorCodes(cont[("${actionname}".toString())][("${apiversion}".toString())]["errorcodes"] as HashSet)
				//}
	
			}
			return doc
		}catch(Exception e){
			throw new Exception("[ApiCacheService :: generateApiDoc] : Exception - full stack trace follows:",e)
		}
	}
	
	LinkedHashMap getApiCache(String controllername){
		println("[ApiCacheService - getApiCache(${controllername})]")
		try{
			def cache = cacheManager.getCache('ApiCache').get(controllername)
			println(cache)
			if(cache){
				return cache.get() as LinkedHashMap
			}

		}catch(Exception e){
			//throw new Exception("[ApiCacheService :: getApiCache] : Exception - full stack trace follows:",e)
			println("[ApiCacheService :: getApiCache] : Exception - full stack trace follows: "+e)
		}
	}
	
	List getCacheNames(){
		List cacheNames = cacheManager.getCache('ApiCache').getAllKeys() as List
		// List cacheNames = temp.collect{ if(!['hook','iostate'].contains(it)){ it }}
		return cacheNames
	}
}
