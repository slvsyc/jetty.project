DO NOT EDIT - See: https://www.eclipse.org/jetty/documentation/current/startup-modules.html

[description]
Enable a server wide accept rate limit

[tags]
connector

[depend]
server

[xml]
etc/jetty-acceptratelimit.xml

[ini-template]
## The limit of accepted connections
#jetty.acceptrate.limit=1000

## The period (in milliseconds) over which the rate applies
#jetty.acceptrate.period=1000
