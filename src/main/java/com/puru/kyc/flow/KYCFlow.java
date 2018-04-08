package com.puru.kyc.flow;

import static com.puru.kyc.contract.KYCContract.KYC_CONTRACT_ID;
import static kotlin.collections.CollectionsKt.single;
import static net.corda.core.contracts.ContractsDSL.requireThat;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.security.KeyPair;
import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.puru.kyc.contract.KYCContract;
import com.puru.kyc.model.KYC;
import com.puru.kyc.state.KYCState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;

import net.corda.core.contracts.TransactionState;

import net.corda.core.contracts.UniqueIdentifier;

import net.corda.core.crypto.DigitalSignature;

import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.transactions.WireTransaction;
import net.corda.core.utilities.ProgressTracker;

import co.paralleluniverse.fibers.Suspendable;

import com.google.common.collect.ImmutableSet;


public class KYCFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {
    	
    	

        private final KYCState kycState;
        private final Party otherParty;
        private net.corda.core.crypto.SecureHash.SHA256 attachmentHashValue;

        private final ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step("Generating transaction based on new IOU.");
        private final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying contract constraints.");
        private final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with our private key.");
        private final ProgressTracker.Step GATHERING_SIGS = new ProgressTracker.Step("Gathering the counterparty's signature.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.Companion.tracker();
            }
        };
        private final ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
            @Override
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.Companion.tracker();
            }
        };

        // The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
        // checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call()
        // function.
        private final ProgressTracker progressTracker = new ProgressTracker(
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                GATHERING_SIGS,

                FINALISING_TRANSACTION
        );
        public Initiator(KYCState kycState, Party otherParty, net.corda.core.crypto.SecureHash.SHA256 attachmentHashValue) {
            this.kycState = kycState;
            this.otherParty = otherParty;
            this.attachmentHashValue = attachmentHashValue;
        }

        public Initiator(KYCState kycState, Party otherParty) {
            this.kycState = kycState;
            this.otherParty = otherParty;

        }

        @Override public ProgressTracker getProgressTracker() { return progressTracker; }

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            // Obtain a reference to the notary we want to use.
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            System.out.println("Generating transaction........");
            // Stage 1.
            progressTracker.setCurrentStep(GENERATING_TRANSACTION);
            // Generate an unsigned transaction.
            Party me = getServiceHub().getMyInfo().getLegalIdentities().get(0);
            //KYCState kycState = new KYCState(kyc, me, otherParty);
            final Command<KYCContract.Commands.Create> txCommand = new Command<>(
                    new KYCContract.Commands.Create(),
                    ImmutableList.of(kycState.getBuyer().getOwningKey(), kycState.getSeller().getOwningKey()));
            final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addOutputState(kycState, KYC_CONTRACT_ID)
                    .addCommand(txCommand);

              System.out.println("Verifying transaction........");
              // Stage 2.
              progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
              // Verify that the transaction is valid.
              txBuilder.verify(getServiceHub());

            System.out.println("Signing transaction........");

            // Stage 3.
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            // Sign the transaction.
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            System.out.println("Gathering sign from other party........");
            // Stage 4.
            progressTracker.setCurrentStep(GATHERING_SIGS);
            // Send the state to the counterparty, and receive it back with their signature.

            System.out.println("Other party...." + otherParty);
            FlowSession otherPartySession = initiateFlow(otherParty);

            System.out.println("Other party session...." + otherPartySession);
            final SignedTransaction fullySignedTx = subFlow(
                    new CollectSignaturesFlow(partSignedTx, ImmutableSet.of(otherPartySession), CollectSignaturesFlow.Companion.tracker()));




            System.out.println("Finalizing transaction........");
            // Stage 5.
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(new FinalityFlow(fullySignedTx));
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Acceptor extends FlowLogic<SignedTransaction> {

        private final FlowSession otherPartyFlow;

        public Acceptor(FlowSession otherPartyFlow) {
            this.otherPartyFlow = otherPartyFlow;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
                    super(otherPartyFlow, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) {
                    System.out.println("Acceptor signing transaction......");
                    requireThat(require -> {
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        require.using("This must be an KYC transaction.", output instanceof KYCState);
                        KYCState iou = (KYCState) output;
                        //require.using("I won't accept IOUs with a value over 100.", iou.getValue() <= 100);
                        return null;
                    });
                }
            }

            return subFlow(new SignTxFlow(otherPartyFlow, SignTransactionFlow.Companion.tracker()));
        }
    }

    /*public static class KYCFlowResult {
        public static class Success extends KYCFlow.KYCFlowResult {
            private String message;

            private Success(String message) { this.message = message; }

            @Override
            public String toString() { return String.format("Success(%s)", message); }
        }

        public static class Failure extends KYCFlow.KYCFlowResult {
            private String message;

            private Failure(String message) { this.message = message; }

            @Override
            public String toString() { return String.format("Failure(%s)", message); }
        }
    }*/
}