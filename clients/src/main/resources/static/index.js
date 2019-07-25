
onLoad();
// Runs on load to setup page
function onLoad() {
    var buttons = document.getElementsByClassName("tic");
    for(var i=0; i < buttons.length; i++) {
       let obj = i;
       buttons[i].addEventListener("click", function() {eventSubmitTurn(obj);}, false);
    }

    setUpPopUpWindow();
    setUpChooseOpponentWindow();
    startUpdateCheck();
}


function setUpPopUpWindow() {
    document.getElementsByClassName("closePopUp")[0].onclick = function() {
      clearBoard();
      document.getElementById("popUpModel").style.display = "none";
    }
    window.onclick = function(event) {
      if (event.target == modal) {
        document.getElementById("popUpModel").style.display = "none";
      }
    }
}

function setUpChooseOpponentWindow() {

    var modal = document.getElementById("chooseOpponentModel");
    document.getElementsByClassName("closeOpponentWindow")[0].onclick = function() {
      modal.style.display = "none";
    }
    window.onclick = function(event) {
      if (event.target == modal) {
        modal.style.display = "none";
      }
    }

    axios.get('get-nodes').then(function (result) {
        var nodesArray = Array.from(result.data);
        var select = document.getElementById("options");
        for (var i=0; i<nodesArray.length; i++) {
            var el = document.createElement("option");
            el.textContent = nodesArray[i];
            el.value = nodesArray[i];
            select.appendChild(el);
        }
    })

    let startGameButton = document.getElementById("startGameButtonId");
    startGameButton.addEventListener("click", function() {
        let opponentName = document.getElementById("options").selectedOptions[0].innerHTML;
        eventStartGame(opponentName);
        document.getElementById("chooseOpponentModel").style.display = "none";
    })

    // Show window if game not already in progress
    axios.get('get-board').then(response => {
        if (response.status >= 200 && response.status <= 300) document.getElementById("chooseOpponentModel").style.display = "none";
        else document.getElementById("chooseOpponentModel").style.display = "block";
    })
    .catch (error => {
        document.getElementById("chooseOpponentModel").style.display = "block";
    });
}


function clearBoard() {
    var buttons = document.getElementsByClassName("tic");
    for (var i=0; i<buttons.length; i++) {
        buttons[i].innerHTML = ' ';
    }
    myBoard = null;
    document.getElementById("h1").innerHTML = "";
    document.getElementById("h2").innerHTML = "";
}

function popUp(text) {
    document.getElementById("popUpText").textContent = text;
    document.getElementById("popUpModel").style.display = "block";
}


// Attempts to run StartGameFlow with party
function eventStartGame(party) {
    axios.post('start-game', party, {headers: {'Content-Type': 'application/json'}})
    .then(response => {
        console.log(response);
    })
    .catch(error => {
        console.log(error.response);
    });
}


// Pings "board" every couple seconds to check for updates
var myBoard;
function startUpdateCheck() {

    var check = function() {

        axios.get('get-board').then(function (result) {

            var array = Array.from(result.data);

            if (array.length == 0) { // no active game
                // If no pop up is already being displayed, display the choose opponent window
                if (document.getElementById("popUpModel").style.display == "none") document.getElementById("chooseOpponentModel").style.display = "block";
            }
            else { // active game

                document.getElementById("chooseOpponentModel").style.display = "none";
                if (myBoard == null) resetPage(); // if myBoard has never been set, call resetPage() to set it

                for (var i=0; i<array.length; i++) {
                    if (array[i] != myBoard[i]) { // check for board update
                        resetPage();
                        myBoard = array;
                    }
                }
            }

        })
        .catch(error => {
            // TODO
        });
        setTimeout(check, 1000); // check again in 2 seconds
    }
    check();
}

// Resets the page to display any type of board updates
function resetPage() {
    setIdentityLabel();
    setIsMyTurnLabel();
    var buttons = document.getElementsByClassName("tic");
    axios.get('get-board').then(function (result) {
        var array = Array.from(result.data);
        for (var i=0; i<array.length; i++) {
            if (array[i] == 'E') buttons[i].innerHTML = ' ';
            else buttons[i].innerHTML = array[i];
        }
        myBoard = array;
    });
    checkForGameOver();
}

// Checks if the game is over, if it is then display the winner and run EndGameFlow in two seconds
function checkForGameOver() {

    axios.get('get-is-game-over').then(function (result) {
        var isGameOver = Boolean(result.data);

        if (isGameOver) {

            // Get the winner and display it
            axios.get('get-winner-text').then(function (result) {
                popUp(result.data);
            })

            // Run EndGameFlow in 2 seconds to mark the BoardState as consumed
            // We wait 2 seconds so the other party is able to also detect that there is a winner and display that message before the BoardState is removed
            function runEndGameFlow() {
                setTimeout(function() {
                    axios.post('end-game', {headers: {'Content-Type': 'application/json'}})
                    .then(response => {
                        console.log(response);
                    })
                    .catch(error => {
                        console.log(error.response);
                    });

                }, 1000);
            }
            runEndGameFlow();

        }
    })

}

function setIsMyTurnLabel() {
    axios.get('get-my-turn').then(function (result) {
        var isMyTurn = Boolean(result.data);
        if (isMyTurn) document.getElementById("h2").innerHTML = "It's your turn!";
        else document.getElementById("h2").innerHTML = "Wait for your turn...";
    })
    .then(response => {
    	console.log(response);
    })
    .catch(error => {
        //console.log(error.response);
    });
}

function setIdentityLabel() {
    axios.get('get-you-are-text').then(function (result) {
        document.getElementById("h1").innerHTML = result.data;
    })
}

// Attempts to execute a SubmitTurnFlow on the space that corresponds with index i
function eventSubmitTurn(i) {
    axios.post('submit-turn', i, {headers: {'Content-Type': 'application/json'}})
    .then(response => {
          	console.log(response);
            resetPage();
    })
    .catch(error => {
        console.log(error.response);
    });
}