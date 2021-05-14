package org.tarasca.contracts;


import com.jelurida.ardor.contracts.ContractTestHelper;
import nxt.addons.JA;
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


        // The test sends a message to invite an unknown account "newAccountMessage", too
        JO newAccountMessage = new JO();
        newAccountMessage.put("contract","ReferralsSimple");
        newAccountMessage.put("invitedAccount","ARDOR-6645-FEKY-BC5T-EPW5D");

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

        TransferAssetCall.create(2)
                .secretPhrase(BOB.getSecretPhrase())
                .asset(referralAssetId)
                .quantityQNT(1l)
                .recipient(ALICE.getRsAccount())
                .messageIsPrunable(true)
                .message(goodMessage.toJSONString())
                .feeNQT(IGNIS.ONE_COIN)
                .call();

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
        Assert.assertTrue(retrievedPropertiesChuck.getString("property").equals("tdao.invite"));

        JO retrievedValueChuck = JO.parse(retrievedPropertiesChuck.getString("value"));
        Assert.assertTrue(retrievedValueChuck.getString("reason").equals("referral"));
        Assert.assertTrue(retrievedValueChuck.getString("invitedBy").equals(BOB.getRsAccount()));
        Assert.assertTrue(retrievedValueChuck.getString("invitedFor").equals("season01"));


        JO retrievedPropertiesNew = response.getArray("properties").get(0);
        Assert.assertTrue(retrievedPropertiesNew.getString("recipientRS").equals("ARDOR-6645-FEKY-BC5T-EPW5D"));
        Assert.assertTrue(retrievedPropertiesNew.getString("property").equals("tdao.invite"));

        JO retrievedValueNew = JO.parse(retrievedPropertiesNew.getString("value"));
        Assert.assertTrue(retrievedValueNew.getString("reason").equals("referral"));
        Assert.assertTrue(retrievedValueNew.getString("invitedBy").equals(BOB.getRsAccount()));
        Assert.assertTrue(retrievedValueNew.getString("invitedFor").equals("season01"));


        Logger.logInfoMessage("TEST: messages(): Done, stop test");
    }


    @Test
    public void referralsCounter() {
        Logger.logInfoMessage("TEST: referralsCounter(): Start test");
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
        Logger.logInfoMessage("TEST: referralsCounter(): Setup complete");

        // The test sends messages to invite different accounts

        JO goodMessage = new JO();
        goodMessage.put("contract","ReferralsSimple");

        String lastInvited = CHUCK.getRsAccount();
        goodMessage.put("invitedAccount",lastInvited);

        TransferAssetCall.create(2)
                .secretPhrase(BOB.getSecretPhrase())
                .asset(referralAssetId)
                .quantityQNT(1l)
                .recipient(ALICE.getRsAccount())
                .messageIsPrunable(true)
                .message(goodMessage.toJSONString())
                .feeNQT(IGNIS.ONE_COIN)
                .call();

        generateBlock(); //transfer happens
        generateBlock(); //contract has reacted

        // check property is correct
        JO response = GetAccountPropertiesCall.create().setter(ALICE.getRsAccount()).recipient(BOB.getRsAccount()).call();
        JA properties = response.getArray("properties");
        Assert.assertTrue(properties.size()==1);
        JO refProperty = properties.get(0);
        Assert.assertTrue(refProperty.getString("property").equals("tdao.referral"));
        JO refValue = JO.parse(refProperty.getString("value"));
        //Assert.assertTrue(refValue.getString("referral").equals("active")); // not in the property anymore.
        Assert.assertTrue(refValue.getString("lastInvited").equals(lastInvited));
        Assert.assertTrue(refValue.getInt("numReferrals")==1);


        lastInvited = "ARDOR-CPAZ-G66V-ATK8-6EPHJ";
        goodMessage.put("invitedAccount",lastInvited);

        TransferAssetCall.create(2)
                .secretPhrase(BOB.getSecretPhrase())
                .asset(referralAssetId)
                .quantityQNT(1l)
                .recipient(ALICE.getRsAccount())
                .messageIsPrunable(true)
                .message(goodMessage.toJSONString())
                .feeNQT(IGNIS.ONE_COIN)
                .call();

        generateBlock(); //transfer happens
        generateBlock(); //contract has reacted

        // check property is correct
        response = GetAccountPropertiesCall.create().setter(ALICE.getRsAccount()).recipient(BOB.getRsAccount()).call();
        properties = response.getArray("properties");
        Assert.assertTrue(properties.size()==1);
        refProperty = properties.get(0);
        Assert.assertTrue(refProperty.getString("property").equals("tdao.referral"));
        refValue = JO.parse(refProperty.getString("value"));
        //Assert.assertTrue(refValue.getString("referral").equals("active"));
        Assert.assertTrue(refValue.getString("lastInvited").equals(lastInvited));
        Assert.assertTrue(refValue.getInt("numReferrals")==2);


        lastInvited = "ARDOR-94UK-NE47-Z68Z-BAM67";
        goodMessage.put("invitedAccount",lastInvited);

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


        // check property is correct
        response = GetAccountPropertiesCall.create().setter(ALICE.getRsAccount()).recipient(BOB.getRsAccount()).call();
        properties = response.getArray("properties");
        Assert.assertTrue(properties.size()==1);
        refProperty = properties.get(0);
        refValue = JO.parse(refProperty.getString("value"));
        //Assert.assertTrue(refValue.getString("referral").equals("active"));
        Assert.assertTrue(refValue.getString("lastInvited").equals(lastInvited));
        Assert.assertTrue(refValue.getInt("numReferrals")==3);


        lastInvited = "ARDOR-YBJ2-4RRQ-BHK5-FF4XY";
        goodMessage.put("invitedAccount",lastInvited);

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


        // check property is correct
        response = GetAccountPropertiesCall.create().setter(ALICE.getRsAccount()).recipient(BOB.getRsAccount()).call();
        properties = response.getArray("properties");
        Assert.assertTrue(properties.size()==1);
        refProperty = properties.get(0);
        refValue = JO.parse(refProperty.getString("value"));
        //Assert.assertTrue(refValue.getString("referral").equals("active"));
        Assert.assertTrue(refValue.getString("lastInvited").equals(lastInvited));
        Assert.assertTrue(refValue.getInt("numReferrals")==4);



        Logger.logInfoMessage("TEST: referralsCounter(): Done, stop test");
    }
}
