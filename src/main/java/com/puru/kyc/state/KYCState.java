package com.puru.kyc.state;

import static java.util.stream.Collectors.toList;

import java.lang.reflect.Constructor;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.ImmutableList;
import com.puru.kyc.contract.KYCContract;
import com.puru.kyc.schema.KYCSchemaV1;
import net.corda.core.contracts.Command;

import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.CompositeKey;

import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;
import net.corda.core.transactions.TransactionBuilder;


import com.puru.kyc.model.KYC;
import org.jetbrains.annotations.NotNull;


public class KYCState implements LinearState, QueryableState {
    private final KYC kyc;
    private final Party buyer;
    private final Party seller;

    private final UniqueIdentifier linearId;

    public KYCState(KYC kyc,
                    Party buyer,
                    Party seller)
    {
        this.kyc = kyc;
        this.buyer = buyer;
        this.seller = seller;
        this.linearId = new UniqueIdentifier(
                Integer.toString(kyc.getKycId()),
                UUID.randomUUID());
    }

    public KYC getKYC() { return kyc; }
    public Party getBuyer() { return buyer; }
    public Party getSeller() { return seller; }

    @Override public UniqueIdentifier getLinearId() { return linearId; }

    @Override public List<AbstractParty> getParticipants() { return Arrays.asList(buyer, seller); }

    
//    @Override public boolean isRelevant(Set<? extends PublicKey> ourKeys) {
//        final List<PublicKey> partyKeys = getParties()
//                .stream()
//                .flatMap(party -> party.getOwningKey().getKeys().stream())
//                .collect(toList());
//        return ourKeys
//                .stream()
//                .anyMatch(partyKeys::contains);
//
//    }

    
   /* @Override public TransactionBuilder generateAgreement(Party notary) {
        *//*return new TransactionType.General().Builder(notary)
                .withItems(this, new Command(new Place(), getParticipants()));*//*
    	
    	Class memberClasses[] = TransactionType.General.class.getDeclaredClasses();    	
    	
    	Class classDefinition = memberClasses[0];
    	
    	TransactionBuilder builder = null;
    	try{
    		Constructor cons = classDefinition.getConstructor(Party.class);    		
    		Object obj = cons.newInstance(notary);
    		
    		TransactionBuilder tempBuilder = (TransactionBuilder) obj;
    		builder = tempBuilder.withItems(this, new Command(new Place(), getParticipants()));
    		
    		
    	}catch(Exception e){
    		e.printStackTrace();
    	} 
    	
    	return builder;    	
    	
    }
*/
    @NotNull
    @Override
    public Iterable<MappedSchema> supportedSchemas() {
        return ImmutableList.of(new KYCSchemaV1());
    }

    @NotNull
    @Override
    public PersistentState generateMappedObject(MappedSchema schema) {
        if (schema instanceof KYCSchemaV1) {
            return new KYCSchemaV1.PersistentKYC(
                    this.kyc,
                    this.buyer.getName().toString(),
                    this.seller.getName().toString(),
                    this.linearId.getId());
        } else {
            throw new IllegalArgumentException("Unrecognised schema $schema");
        }
    }
}