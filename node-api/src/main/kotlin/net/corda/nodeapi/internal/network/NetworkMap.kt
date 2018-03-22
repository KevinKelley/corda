package net.corda.nodeapi.internal.network

import net.corda.core.crypto.SecureHash
import net.corda.core.internal.CertRole
import net.corda.core.internal.SignedDataWithCert
import net.corda.core.node.NetworkParameters
import net.corda.core.node.NodeInfo
import net.corda.core.serialization.CordaSerializable
import net.corda.nodeapi.internal.crypto.X509Utilities
import java.security.cert.X509Certificate
import java.time.Instant


const val NETWORK_PARAMS_FILE_NAME = "network-parameters"
const val NETWORK_PARAMS_UPDATE_FILE_NAME = "network-parameters-update"

typealias SignedNetworkMap = SignedDataWithCert<NetworkMap>
typealias SignedNetworkParameters = SignedDataWithCert<NetworkParameters>

/**
 * Data structure representing the network map available from the HTTP network map service as a serialised blob.
 * @property nodeInfoHashes list of network participant's [NodeInfo] hashes
 * @property networkParameterHash hash of the current active [NetworkParameters]
 * @property parametersUpdate if present means that network operator has scheduled an update of the network parameters
 */
@CordaSerializable
data class NetworkMap(
        val nodeInfoHashes: List<SecureHash>,
        val networkParameterHash: SecureHash,
        val parametersUpdate: ParametersUpdate?
) {
    override fun toString(): String {
        return """${NetworkMap::class.java.simpleName}(nodeInfoHashes=
${nodeInfoHashes.joinToString("\n")}
networkParameterHash=$networkParameterHash
parametersUpdate=$parametersUpdate)"""
    }
}

/**
 * Data class representing scheduled network parameters update.
 * @property newParametersHash Hash of the new [NetworkParameters] which can be requested from the network map
 * @property description Short description of the update
 * @property updateDeadline deadline by which new network parameters need to be accepted, after this date network operator
 *          can switch to new parameters which will result in getting nodes with old parameters out of the network
 */
@CordaSerializable
data class ParametersUpdate(
        val newParametersHash: SecureHash,
        val description: String,
        val updateDeadline: Instant
)

fun <T : Any> SignedDataWithCert<T>.verifiedNetworkMapCert(rootCert: X509Certificate): T {
    require(CertRole.extract(sig.by) == CertRole.NETWORK_MAP) { "Incorrect cert role: ${CertRole.extract(sig.by)}" }
    X509Utilities.validateCertificateChain(rootCert, sig.by, rootCert)
    return verified()
}
