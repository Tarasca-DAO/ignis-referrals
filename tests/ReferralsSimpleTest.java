package org.tarasca.contracts;


import com.jelurida.ardor.contracts.ContractTestHelper;
import nxt.util.Logger;
import org.junit.Test;
import com.jelurida.ardor.contracts.AbstractContractTest;


public class ReferralsSimpleTest extends AbstractContractTest {

    @Test
    public void messages() {
        Logger.logInfoMessage("TEST: messages(): Start test");

        generateBlock();

        String contractName = ContractTestHelper.deployContract(ReferralsSimple.class);
        generateBlock();

        Logger.logInfoMessage("TEST: messages(): Stop test");
    }
}
