rmivm
=====

Java RMI / RPC via Message Bus

Transparent RMI/RPC layer for executing methods on remote objects decoupled via a message bus.
currently uses: 

* RabbitMQ and it's RPC example
* Kryo for binary serialisation
* java.lang.reflect.Proxy for proxied invocation handler.

TOTO

* Handle and package local/remote/framework Exceptions. 
* Make things robust.
* Many more (proper) tests.
* pluggable serialisation libraries.
* pluggable queue implementation.
* pluggable proxy implementations.


