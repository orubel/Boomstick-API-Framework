/*
 * Copyright 2013-2019 Beapi.io
 * API Chaining(R) 2019 USPTO
 *
 * Licensed under the MPL-2.0 License;
 * you may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.nosegrind.apiframework

import grails.converters.JSON
//import grails.converters.XML
import grails.plugin.cache.CachePut
//import grails.plugin.cache.GrailsCacheManager
import org.grails.plugin.cache.GrailsCacheManager
import org.grails.groovy.grails.commons.*
import grails.core.GrailsApplication
import grails.plugin.springsecurity.SpringSecurityService
import grails.plugin.cache.CacheEvict
import grails.plugin.cache.CachePut

/**
 * A class for caching rate limiting data; used by handler interceptors.
 * @author Owen Rubel
 */
class ThrottleCacheService{

	static transactional = false

	/**
	 * Application Class
	 */
	GrailsApplication grailsApplication
	/**
	 * Cache Manager Class
	 */
	GrailsCacheManager grailsCacheManager
	/**
	 * Security service; used for accessing principal ID
	 */
	SpringSecurityService springSecurityService

	// called through generateJSON()


	/**
	 * Sets cached variables for user rate limiting / data limiting
	 * ex ['timestamp': currentTime, 'currentRate': 1, 'currentData':contentLength,'locked': false, 'expires': expires]
	 * @param String userId of user being rate limited
	 * @param LinkedHashMap cache of properties used in rate limiting
	 * @return A LinkedHashMap of Cached data associated with userId
	 */
	//@org.springframework.cache.annotation.CachePut(value="Throttle",key="#userId")
	@CachePut(value="Throttle",key={userId})
	LinkedHashMap setThrottleCache(String userId, LinkedHashMap cache){
		try{
			return cache
		}catch(Exception e){
			throw new Exception("[ThrottleCacheService :: setThrottleCache] : Exception - full stack trace follows:",e)
		}
	}

	/**
	 * increments the rate limit in the cache associated with user id
	 * @param String userId of user being rate limited
	 * @return A LinkedHashMap of Cached data associated with userId
	 */
	//@org.springframework.cache.annotation.CachePut(value="Throttle",key="#userId")
	@CachePut(value="Throttle",key={userId})
	LinkedHashMap incrementThrottleCache(String userId){
		try{
			def cache = getLimitCache(userId)
			cache['rateLimitCurrent']++

			// send via webhook to subscribing services
			List servers = grailsApplication.config.apitoolkit.apiServer

			return cache
		}catch(Exception e){
			throw new Exception("[ThrottleCacheService :: incrementThrottleCache] : Exception - full stack trace follows:",e)
		}
	}

	/*
	@org.springframework.cache.annotation.CachePut(value="Throttle",key="#userId")
	LinkedHashMap lockLimitCache(String uri){
		def cache = getLimitCache(userId)
		cache['locked']=true
		return cache
	}

	@org.springframework.cache.annotation.CachePut(value="Throttle",key="#userId")
	LinkedHashMap checkLimitCache(String userId,String role){
		// check role against config role limit
	}
	*/

	/**
	 * returns the rate limit cache associated with given user id
	 * @param String userId of user being rate limited
	 * @return A LinkedHashMap of Cached data associated with userId
	 */
	LinkedHashMap getLimitCache(String userId){
		try{
			def temp = grailsCacheManager?.getCache('Throttle')
			def cache = temp?.get(userId)
			if(cache?.get()){
				LinkedHashMap lcache = cache.get() as LinkedHashMap
				return lcache
			}else{ 
				return [:] 
			}

		}catch(Exception e){
			throw new Exception("[ThrottleCacheService :: getThrottleCache] : Exception - full stack trace follows:",e)
		}
	}

	/**
	 * Method to load the list of all object contained in the 'Throttle' cache
	 * @return A List of keys of all object names contained with the 'Throttle'
	 */
	List getCacheNames(){
		List cacheNames = []
		cacheNames = grailsCacheManager?.getCache('Throttle')?.getAllKeys() as List
		return cacheNames
	}
}
