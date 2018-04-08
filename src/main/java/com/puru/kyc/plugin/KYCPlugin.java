package com.puru.kyc.plugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.puru.kyc.api.KYCApi;
import com.puru.kyc.contract.KYCContract;
import com.puru.kyc.state.KYCState;
import com.puru.kyc.flow.AttachmentFlow;
import com.puru.kyc.flow.KYCFlow;
import com.puru.kyc.service.KYCService;

import net.corda.core.flows.IllegalFlowLogicException;
import net.corda.core.messaging.CordaRPCOps;

import net.corda.core.transactions.SignedTransaction;

import com.puru.kyc.model.KYC;
import com.esotericsoftware.kryo.Kryo;
import net.corda.webserver.services.WebServerPluginRegistry;

public class KYCPlugin implements WebServerPluginRegistry {
    /**
     * A list of classes that expose web APIs.
     */
    private final List<Function<CordaRPCOps, ?>> webApis = ImmutableList.of(KYCApi::new);

    /**
     * A list of directories in the resources directory that will be served by Jetty under /web.
     */
    private final Map<String, String> staticServeDirs = ImmutableMap.of(
            // This will serve the exampleWeb directory in resources to /web/kyc
            "kyc", getClass().getClassLoader().getResource("kycWeb").toExternalForm()
    );

    @Override public List<Function<CordaRPCOps, ?>> getWebApis() { return webApis; }
    @Override public Map<String, String> getStaticServeDirs() { return staticServeDirs; }
    @Override public void customizeJSONSerialization(ObjectMapper objectMapper) {

        objectMapper.canDeserialize(objectMapper.constructType(KYC.class));
        objectMapper.canDeserialize(objectMapper.constructType(Date.class));
    }


}
