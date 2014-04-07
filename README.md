rmivm
=====
_ruh-miv-em_


Java RMI / RPC Via Message 

**Transparent RMI/RPC layer for executing methods on remote objects decoupled via a message queue**

Currently uses: 

* RabbitMQ and its RPC example
  * One named queue _per_ remote object interface (using f.q.c.n of registered Interface)
  * One unamed queue _per_ local client proxy of each remote Interface
* Kryo for binary serialisation
* java.lang.reflect.Proxy for proxied invocation handler.

TODO

* Handle and package local/remote/framework Exceptions.
* configuration - request queues, reply queues, connection particulars.
* common queues with other metadata for discriminating requests
* Make things robust.
* Asynchronous mode using something like futures.
* Many more (proper) tests.
* pluggable serialisation libraries.
* pluggable queue implementation.
* pluggable proxy implementations.
* Optimisations
    * The server can reply with a token which can be used on subsequent requests from the client to prevent having to serialise all the parameter types etc. 

**How.**

Kro is used to serialise all of the following:

* Fully qualified class name of the Interface to target.
* method name.
* String array of parameter types.
* Object array of parameter objects.

Kryo packages this into a request which can be deserialised by the server-side endpoint.  The server side endpoint registers an interface and an implementation of such.  Reflection is used to invoke the method on the remote object from the information sent in the serialisation package.


**Why?**

Nice to have decoupled consumers and providers in a Service Oriented Architecture. Most RPC/Remoting requires a configured single host as the target. Where there are many consumers and providers, all the wiring must be either hard-coded (statically configured in a config file) or one must employ some sort of service locator (Zookeeper etc) to discover the service endpoints.  load-balancing and fault-tolderance is non-trivial as the client must discover and maintain the state of the each peer.

Using a message queue, means we can load-balance and distribute the services easier.

Some other systems like to use a particular request/response package. (Roger)

This approach creates a transparent RMI which is both good (ease of use) and bad (hiding network artefacts).
