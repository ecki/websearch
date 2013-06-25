World ugliest Google Clone
==========================

https://github.com/ecki/websearch

Actually this is a test project to learn ElasticSearch and Vaadin.

This will produce a generic search site with some support for faceted search.
The main focus is to allow search documents in a knowledge-base kind of organization.

This is currently a playground with no sane error handling, architecture or even
security.

Feel free to copy this code for any purpose.

This Maven project currently builds a WAR file containing Vaadin and ElasticSearch. 
I tried it successfully on JBoss WildFly AS8 Alpha. It does no proper life-cycle.
It will set up a single local ES node with no Index Optimizations set.

You can preview the app in the build in Jetty by using:

    > mvn jetty:run
    
Currently you need to search for "Title1" and use the Search Button (enter does not work).

Bernd

- http://elasticsearch.org
- http://vaadin.com 
