<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Corda Tic-Tac-Toe

Welcome to the Tic-Tac-Toe CorDapp.
Please visit the official R3 sample repo for a maintained version of this CorDapp: https://github.com/corda/samples-kotlin/tree/master/Accounts/tictacthor

# Deploying Locally
Navigate to the CorDapps root directory in a terminal window.

First, run the command listed below to deploy the Corda nodes.

    ./gradlew deployNodes

Then run the following command to run the Corda nodes.

    build/nodes/runnodes

Finally, we need to start the web servers for each node. Open three new terminal tabs and navigate to the root directory of the CorDapp in each. Now run one of the following commands in each terminal window.

    ./gradlew runPartyA
    ./gradlew runPartyB
    ./gradlew runPartyC

Now you should be able to connect to the web frontend of each node using the links below.

PartyA: http://localhost:10050
PartyB: http://localhost:10060
PartyC: http://localhost:10070

![](resources/screenshot.png)

