package org.tarasca.contracts;


import com.jelurida.ardor.contracts.ContractTestHelper;
import nxt.addons.JO;
import nxt.http.callers.GetAccountPropertiesCall;
import nxt.http.callers.GetPrunableMessageCall;
import nxt.http.callers.GetPrunableMessagesCall;
import nxt.http.callers.TransferAssetCall;
import nxt.util.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.jelurida.ardor.contracts.AbstractContractTest;

import static org.tarasca.contracts.TarascaTester.initReferralAsset;
import static nxt.blockchain.ChildChain.IGNIS;

public class ReferralsSimpleTest extends AbstractContractTest {

    @Test
    public void messages() {
        Logger.logInfoMessage("TEST: messages(): Start test");
        JO REF = initReferralAsset();
        String referralAssetId = REF.getString("asset");
        generateBlock();

        JO setupParams = new JO();
        setupParams.put("refAsset", referralAssetId);
        String contractName = ContractTestHelper.deployContract(ReferralsSimple.class,setupParams,true);
        generateBlock();
        TransferAssetCall.create(2).secretPhrase(DAVE.getSecretPhrase())
                .asset(referralAssetId).quantityQNT(4l)
                .recipient(BOB.getRsAccount())
                .feeNQT(IGNIS.ONE_COIN)
                .call();

        generateBlock();
        Logger.logInfoMessage("TEST: messages(): Setup complete");

        // The test sends a message to invite a known account "goodMessage"
        JO goodMessage = new JO();
        goodMessage.put("contract","ReferralsSimple");
        goodMessage.put("invitedAccount",CHUCK.getRsAccount());
        TransferAssetCall.create(2)
                .secretPhrase(BOB.getSecretPhrase())
                .asset(referralAssetId)
                .quantityQNT(1l)
                .recipient(ALICE.getRsAccount())
                .messageIsPrunable(true)
                .message(goodMessage.toJSONString())
                .feeNQT(IGNIS.ONE_COIN)
                .call();

        generateBlock();

        JO newAccountMessage = new JO();
        newAccountMessage.put("contract","ReferralsSimple");
        newAccountMessage.put("invitedAccount","ARDOR-6645-FEKY-BC5T-EPW5D");
        TransferAssetCall.create(2)
                .secretPhrase(BOB.getSecretPhrase())
                .asset(referralAssetId)
                .quantityQNT(1l)
                .recipient(ALICE.getRsAccount())
                .messageIsPrunable(true)
                .message(newAccountMessage.toJSONString())
                .feeNQT(IGNIS.ONE_COIN)
                .call();

        generateBlock();
        generateBlock();


        Logger.logInfoMessage("TEST: messages(): Evaluating results");
        JO response = GetAccountPropertiesCall.create().setter(ALICE.getRsAccount()).call();
        //JO response = GetPrunableMessagesCall.create(2).account(CHUCK.getRsAccount()).otherAccount(ALICE.getRsAccount()).call();

        JO retrievedPropertiesChuck = response.getArray("properties").get(1);
        Assert.assertTrue(retrievedPropertiesChuck.getString("recipientRS").equals(CHUCK.getRsAccount()));
        Assert.assertTrue(retrievedPropertiesChuck.getString("property").equals("tdao"));

        JO retrievedValueChuck = JO.parse(retrievedPropertiesChuck.getString("value"));
        Assert.assertTrue(retrievedValueChuck.getString("reason").equals("referral"));
        Assert.assertTrue(retrievedValueChuck.getString("invitedBy").equals(BOB.getRsAccount()));
        Assert.assertTrue(retrievedValueChuck.getString("invitedFor").equals("season01"));


        JO retrievedPropertiesNew = response.getArray("properties").get(0);
        Assert.assertTrue(retrievedPropertiesNew.getString("recipientRS").equals("ARDOR-6645-FEKY-BC5T-EPW5D"));
        Assert.assertTrue(retrievedPropertiesNew.getString("property").equals("tdao"));

        JO retrievedValueNew = JO.parse(retrievedPropertiesNew.getString("value"));
        Assert.assertTrue(retrievedValueNew.getString("reason").equals("referral"));
        Assert.assertTrue(retrievedValueNew.getString("invitedBy").equals(BOB.getRsAccount()));
        Assert.assertTrue(retrievedValueNew.getString("invitedFor").equals("season01"));


        Logger.logInfoMessage("TEST: messages(): Done, stop test");
    }
}
