package org.tarasca.contracts;

import nxt.addons.*;
import nxt.blockchain.TransactionType;
import nxt.http.callers.GetAccountPropertiesCall;
import nxt.http.callers.SendMessageCall;
import nxt.http.callers.RsConvertCall;
import nxt.http.callers.SetAccountPropertyCall;
import nxt.http.responses.TransactionResponse;
import org.junit.Assert;

import java.util.List;
import java.util.stream.Collectors;

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
        // check account valid
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
                        response = GetAccountPropertiesCall.create().setter(context.getAccountRs()).recipient(invitedAccount).call();
                        JA propsInvited = response.getArray("properties");
                        List<JO> foundRelevantProps = propsInvited.objects().stream().filter(
                                (el) -> {
                                    String property = el.getString("property");
                                    return property.equals("tdao.invite");
                                }).collect(Collectors.toList());

                        if (foundRelevantProps.size() == 0) {

                            JO message = new JO();
                            message.put("invitedFor","season01");
                            message.put("invitedBy",triggerTransaction.getSenderRs());
                            message.put("reason","referral");

                            SetAccountPropertyCall setPropertyCall = SetAccountPropertyCall.create(2)
                                    .recipient(invitedAccount)
                                    .property("tdao.invite")
                                    .value(message.toJSONString())
                                    .deadline(DEADLINE);
                            context.createTransaction(setPropertyCall);


                            // check current referral property of sender, increase count by one.
                            response = GetAccountPropertiesCall.create().setter(context.getAccountRs()).recipient(triggerTransaction.getSenderRs()).call();
                            JA propsSender = response.getArray("properties");
                            List<JO> foundSenderProps = propsSender.objects().stream().filter(
                                    (el) -> {
                                        String property = el.getString("property");
                                        return property.equals("tdao.referral");
                                    }).collect(Collectors.toList());

                            int curNumReferrals = 0;
                            JO message_referral = new JO();
                            // should only exist once!
                            if (foundSenderProps.size()==0) {
                                // first set
                                message_referral.put("numReferrals",curNumReferrals+1);
                            }
                            else if (foundSenderProps.size()==1) {
                                JO prop = foundSenderProps.get(0);
                                Assert.assertTrue(prop.getString("property").equals("tdao.referral"));
                                JO value = JO.parse(prop.getString("value"));
                                curNumReferrals = value.getInt("numReferrals");
                                message_referral.put("numReferrals",curNumReferrals+1);
                            }
                            else if (foundSenderProps.size()>1) {
                                message_referral.put("numReferrals",0);
                                context.generateInfoResponse("Unexpected: found multiple tdao.referral properties for "+triggerTransaction.getSenderRs());
                            }

                            message_referral.put("lastInvited", invitedAccount);
                            message_referral.put("invitedFor","season01");

                            SetAccountPropertyCall setReferralCall = SetAccountPropertyCall.create(2)
                                    .recipient(triggerTransaction.getSenderRs())
                                    .property("tdao.referral")
                                    .value(message_referral.toJSONString())
                                    .deadline(DEADLINE);
                            context.createTransaction(setReferralCall);

                            return context.getResponse();
                        }
                        else {
                            return context.generateInfoResponse("invited Account seems to have an invite already");
                        }
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
