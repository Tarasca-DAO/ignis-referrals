package org.tarasca.contracts;

import nxt.addons.*;
import nxt.blockchain.TransactionType;
import nxt.http.callers.SendMessageCall;
import nxt.http.callers.RsConvertCall;
import nxt.http.responses.TransactionResponse;
import static nxt.blockchain.ChildChain.IGNIS;

public class ReferralsSimple extends AbstractContract {
    @ValidateContractRunnerIsRecipient
    @ValidateChain(accept = 2)
    public JO processTransaction(TransactionContext context){
        ReferralsSimple.ContractParams contractParams = context.getParams(ReferralsSimple.ContractParams.class);
        String refAsset = contractParams.refAsset();
        int DEADLINE = 180;

        // check if asset was received, else stop,
        // check message content, parse account.
        // check account valid, else return REF?
        TransactionResponse triggerTransaction = context.getTransaction();
        TransactionType transactionType = triggerTransaction.getTransactionType();

        if (transactionType.getType() == 2 && transactionType.getSubtype() == 1) {
            // AssetTransfer Transaction

            JO attachment = triggerTransaction.getAttachmentJson();

            if (attachment.getString("asset").equals(refAsset)){
                // we've received the right asset, on the right account.

                int version = attachment.getInt("version.PrunablePlainMessage");
                if (version == 1){
                    JO messageObj = JO.parse(attachment.getString("message"));
                    String invitedAccount = messageObj.getString("invitedAccount");
                    JO response = RsConvertCall.create().account(invitedAccount).call();

                    if(response.getString("accountRS").equals(invitedAccount)){
                        // String is a valid account.
                        //send message to account
                        JO message = new JO();
                        message.put("invitedFor","season01");
                        message.put("invitedBy",triggerTransaction.getSenderRs());
                        SendMessageCall sendMessageCall = SendMessageCall.create(2).
                                    recipient(invitedAccount).
                                    message(message.toJSONString()).
                                    messageIsText(true).
                                    messageIsPrunable(true).
                                    deadline(DEADLINE);
                        return context.createTransaction(sendMessageCall);
                    }
                    else {
                        return context.generateInfoResponse("transaction attached message does not contain invited Account");
                    }
                }
                else {
                    return context.generateInfoResponse("transaction parameter mismatch: version.PrunablePlainMessage");
                }
            }
            else {
                return context.generateInfoResponse("transaction parameter mismatch: assetId");
            }
        }
        else {
            return context.generateInfoResponse("transaction parameter mismatch: type,subtype");
        }
    }

    @ContractParametersProvider
    public interface ContractParams {

        @ContractSetupParameter
        default String refAsset() {
            return "2384570119093955894l";
        }
    }
}
