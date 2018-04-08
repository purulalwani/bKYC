package com.puru.kyc.contract;

import com.puru.kyc.state.KYCState;
import net.corda.core.contracts.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;

import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;


public class KYCContract implements Contract {

    public static final String KYC_CONTRACT_ID = "com.puru.kyc.contract.KYCContract";

    /**
     * Currently this contract only implements one command.
     */

    public interface Commands extends CommandData {
        class Create implements Commands {
        }
    }


    @Override
    public void verify(LedgerTransaction tx) {
        {
            final CommandWithParties<Commands.Create> command = requireSingleCommand(tx.getCommands(), Commands.Create.class);

            requireThat(require -> {
                // Generic constraints around generation of the issue purchase order transaction.
                require.using("No inputs should be consumed when issuing a kyc.",
                        tx.getInputs().isEmpty());
                require.using("Only one output state should be created for each group.",
                        tx.getOutputs().size() == 1);
                final KYCState out = tx.outputsOfType(KYCState.class).get(0);
                require.using("The buyer and the seller cannot be the same entity.",
                        out.getBuyer() != out.getSeller());
               // System.out.println("Signers -> " + command.getSigners());
                //System.out.println("Participants -> " + out.getParticipants());
                require.using("All of the participants must be signers.",
                        command.getSigners().containsAll(out.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList())));

                // Purchase order specific constraints.
                    /*require.by("We only deliver to the UK.", out.getKYC().getKycId() == 111);
                    require.by("You must order at least one type of item.",
                            !out.getPurchaseOrder().getItems().isEmpty());
                    require.by("You cannot order zero or negative amounts of an item.",
                            out.getPurchaseOrder().getItems().stream().allMatch(item -> item.getAmount() > 0));
                    require.by("You can only order up to 100 items in total.",
                            out.getPurchaseOrder().getItems().stream().mapToInt(PurchaseOrder.Item::getAmount).sum() <= 100);
                    require.by("The delivery date must be in the future.",
                            out.getPurchaseOrder().getDeliveryDate().toInstant().isAfter(time));*/

                return null;
            });


        }

    }
}