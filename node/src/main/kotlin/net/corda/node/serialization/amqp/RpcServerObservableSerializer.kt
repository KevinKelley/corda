package net.corda.node.serialization.amqp

import net.corda.core.context.Trace
import net.corda.core.serialization.SerializationContext
import net.corda.core.serialization.SerializationDefaults
import net.corda.node.services.messaging.ObservableContextInterface
import net.corda.node.services.messaging.ObservableSubscription
import net.corda.nodeapi.RPCApi
import net.corda.nodeapi.internal.serialization.amqp.*
import org.apache.qpid.proton.codec.Data

import rx.Notification
import rx.Observable
import rx.Subscriber

import java.lang.reflect.Type

class RpcServerObservableSerializer(
        private val scheme: AbstractAMQPSerializationScheme,
        private val context: SerializationContext = SerializationDefaults.RPC_SERVER_CONTEXT
) : CustomSerializer.Implements<Observable<*>>(
        Observable::class.java
) {
    // Would be great to make this private, but then it's so much harder to unit test
    object RpcObservableContextKey

    companion object {
        fun createContext(
                observableContext: ObservableContextInterface,
                context: SerializationContext = SerializationDefaults.RPC_SERVER_CONTEXT
        ) : SerializationContext {
            return context.withProperty(
                    RpcServerObservableSerializer.RpcObservableContextKey, observableContext)
        }
    }

    override val schemaForDocumentation: Schema
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun readObject(
            obj: Any, schemas: SerializationSchemas,
            input: DeserializationInput
    ) : Observable<*> {
        throw UnsupportedOperationException()
    }

    override fun writeDescribedObject(
            obj: Observable<*>,
            data: Data,
            type: Type,
            output: SerializationOutput
    ) {
        val observableId = Trace.InvocationId.newInstance()
        val observableContext = context.properties[RpcServerObservableSerializer.RpcObservableContextKey]
                as ObservableContextInterface

        data.withList {
            data.putString(observableId.value)
            data.putLong(observableId.timestamp.toEpochMilli())
        }

        val observableWithSubscription = ObservableSubscription(
                // We capture [observableContext] in the subscriber. Note that all synchronisation/kryo borrowing
                // must be done again within the subscriber
                subscription = obj.materialize().subscribe(
                        object : Subscriber<Notification<*>>() {
                            override fun onNext(observation: Notification<*>) {
                                if (!isUnsubscribed) {
                                    val message = RPCApi.ServerToClient.Observation(
                                            id = observableId,
                                            content = observation,
                                            deduplicationIdentity = observableContext.deduplicationIdentity
                                    )
                                    observableContext.sendMessage(message)
                                }
                            }

                            override fun onError(exception: Throwable) {
                                //RpcServerObservableSerializer.log.error("onError called in materialize()d RPC Observable", exception)
                                println("SO MUcH NO!!!!")
                            }

                            override fun onCompleted() {
                                println ("Complete")
                            }
                        }
                )
        )
        
        observableContext.clientAddressToObservables.put(observableContext.clientAddress, observableId)
        observableContext.observableMap.put(observableId, observableWithSubscription)
    }
}