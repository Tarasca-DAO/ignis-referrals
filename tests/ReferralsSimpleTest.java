package org.tarasca.contracts;


import com.jelurida.ardor.contracts.ContractTestHelper;
import nxt.addons.JO;
import nxt.http.callers.GetPrunableMessageCall;
import nxt.http.callers.GetPrunableMessagesCall;
import nxt.http.callers.TransferAssetCall;
import nxt.util.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.jelurida.ardor.contracts.AbstractContractTest;

import static com.jelurida.ardor.contracts.TarascaTester.initReferralAsset;
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
        generateBlock();
        generateBlock();

        JO response = GetPrunableMessagesCall.create(2).account(CHUCK.getRsAccount()).otherAccount(ALICE.getRsAccount()).call();
        JO retrievedGoodTransaction = response.getArray("prunableMessages").get(0);
        JO retrievedGoodMessage = JO.parse(retrievedGoodTransaction.getString("message"));

        Assert.assertTrue(retrievedGoodTransaction.getString("senderRS").equals(ALICE.getRsAccount()));
        Assert.assertTrue(retrievedGoodTransaction.getString("recipientRS").equals(CHUCK.getRsAccount()));
        Assert.assertTrue(retrievedGoodMessage.getString("submittedBy").equals(contractName));
        Assert.assertTrue(retrievedGoodMessage.getString("invitedBy").equals(BOB.getRsAccount()));
        Assert.assertTrue(retrievedGoodMessage.getString("invitedFor").equals("season01"));
        Logger.logInfoMessage("TEST: messages(): First message sent correctly");

        // The test sends a message to invite a new account "newAccountMessage"
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
        generateBlock();

        response = GetPrunableMessagesCall.create(2).account("ARDOR-6645-FEKY-BC5T-EPW5D").otherAccount(ALICE.getRsAccount()).call();
        JO retrievedNewAccountTransaction = response.getArray("prunableMessages").get(0);
        JO retrievedNewAccountMessage = JO.parse(retrievedGoodTransaction.getString("message"));

        Assert.assertTrue(retrievedGoodTransaction.getString("senderRS").equals(ALICE.getRsAccount()));
        Assert.assertTrue(retrievedGoodTransaction.getString("recipientRS").equals(CHUCK.getRsAccount()));
        Assert.assertTrue(retrievedGoodMessage.getString("submittedBy").equals(contractName));
        Assert.assertTrue(retrievedGoodMessage.getString("invitedBy").equals(BOB.getRsAccount()));
        Assert.assertTrue(retrievedGoodMessage.getString("invitedFor").equals("season01"));

        Logger.logInfoMessage("TEST: messages(): Second message sent correctly");


        Logger.logInfoMessage("TEST: messages(): Done, stop test");
    }
}
