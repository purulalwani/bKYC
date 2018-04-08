package com.puru.kyc.plugin;

import com.google.common.collect.ImmutableList;
import com.puru.kyc.model.KYC;
import net.corda.core.serialization.SerializationWhitelist;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;

public class KYCWhitelist implements SerializationWhitelist {
    @NotNull
    @Override
    public List<Class<?>> getWhitelist() {
        return ImmutableList.of(KYC.class,Date.class);
    }
}
