
![alt text](https://github.com/orubel/logos/blob/master/beapi_logo_large.png)
# Grails API Plugin | BeApi(tm) Api Framework ( https://www.beapi.io/ )
**If you enjoy our work, please click our sponsor button at the top of the page**
[ ![Download](https://api.bintray.com/packages/orubel/plugins/api-framework/images/download.svg?version=2.0.17) ](https://bintray.com/orubel/plugins/api-framework/2.0.17/link)

## Benchmarks : [view](https://github.com/orubel/logos/blob/master/bench.png)
## Latest Version: [2.0.17](https://bintray.com/beta/#/orubel/plugins/api-framework/2.0.17?tab=overview) 
## Stable/Latest Profile Version: [2.0.17](https://bintray.com/beta/#/orubel/profiles/beapi-profile/2.0.17?tab=overview)

***
## Documentation - http://orubel.github.io/Beapi-API-Framework/
## Forums - http://beapi.freeforums.net/
## New App Installation
Edit your 'USER_HOME/.grails/settings.groovy' file to look like the following:
```

grails {
  profiles {
    repositories {
      maven {
        url =  "https://dl.bintray.com/orubel/profiles/"
        snapshotsEnabled = false
      }
      grailsCentral {
        url = "https://repo.grails.org/grails/core"
        snapshotsEnabled = true
      }
    }
  }
}
                      
```
and then run...
```
grails create-app name_of_your_app --profile org.grails.profiles:beapi-profile:2.0.17
```

<!-- ### Backend Implementation - https://github.com/orubel/beapi_backend -->
### Frontend Implementation - https://github.com/orubel/beapi_frontend

***
### Description
The BeAPI Framework is a Grails API plugin providing a full featured API Automation framework and an AVERAGE response time per call of [0.25 milliseconds per request](https://www.flickr.com/photos/orubel/32194321787/in/dateposted-public/) (Google requires their calls to be UNDER 200 ms). 

Some features include:

- **Automated Batching:** all endpoints are batchable by default with AUTH ROLES assignable to restrict access. Batching can also be TOGGLED to turn this feature ON/OFF per endpoint.

- **Built-in CORS:** Cross Origin Request handling secures all endpoints from domains that you don't want. Added capability for securing different network groups

- **JWT Tokens:** JWT Token handling for Javascript frontends to allow or better abstraction of the VIEW layer

- **[Schema Management/Caching](https://www.youtube.com/watch?v=A_Hozpb9gLc):** Manage your the state for all your endpoints through the IO State files. Unlike Swagger/OpenApi, the data associated with functionality for REQUEST/RESPONSE can be shared from a centralized cache and thus can be synchronized making it more secure, stable, faster and flexible. You can make changes to your apis, security and definitions on the fly all without taking your servers down.

- **Automated Web Hooks:** Enables secured Web Hooks for any endpoint so your developers/users can get push notification on updates.

- **Throttling & Rate/Data Limits:** Data Limits/Rate limits and Throttling for all API's through easy to configure options

- **[Autogenerated APIDocs](https://www.youtube.com/watch?v=tzuNczkedhw):**  Unlike Swagger/OpenApi which shows the ApiDocs regardless of ROLE, APIDocs are autogenerated based on your ROLE thus showing only the endpoints that you have access to. 

- **API Chaining(tm):** rather than using HATEOAS to make a request, get a link, make a request, get a link, make a request, etc... api chaining allows for creation of an 'api monad' wherein the output from a related set of apis can be chained client side allowing the output from one api to be accepted as the input to the next and so on and be passed with ONE REQUEST AND ONE RESPONSE. It is handled as a type of BATCH call wherein the backend automates the hand-off and processing.

- **[Automated API Caching](https://www.youtube.com/watch?v=KFAkdeLcXIs):** returned resources are cached,stored and updated with requesting ROLE/AUTH. Domains extend a base class that auto update this cache upon create/update/delete. This speeds up your api REQUEST/RESPONSE x10

- **Built-In Stats Reporting:** Statistics on all api calls (and errors) are stored and can be called up at any time for reporting tooling.

***

### FAQ

**Q: How hard is this to implement?**  
**A:** BeApi is a Grails Plugin and you dont even have to install the plugin. Implementing for a new project is as simple as a one line command:
```
grails create-app name_of_your_app --profile org.grails.profiles:beapi-profile:2.0.17
```

**Q: How do I implement the listener for IO state webhook on my proxy/Message queue?**  
**A:** It merely requires an endpoint to send the data to. As a side project, I may actually supply a simple daemon in the future with ehCache to do this for people.

