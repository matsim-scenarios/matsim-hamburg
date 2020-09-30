package org.matsim.run;

import org.junit.Test;

/**
 * @author zmeng
 */
public class RunHamburgScenarioTest {

    @Test
    public void runTest(){

        String args[] = new String[]{
          "test/input/test-hamburg.config.xml",
                "--output-directory" , "test/output/RunHamburgScenarioTest/"
        };

        MATSimApplication.run(RunHamburgScenario.class,args);
    }
}