
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
    var modal = document.getElementById("popUpModel");
    document.getElementsByClassName("closePopUp")[0].onclick = function() {

      clearBoard();
      document.getElementById("chooseOpponentModel").style.display = "block";
      modal.style.display = "none";
    }
    window.onclick = function(event) {
      if (event.target == modal) {
        modal.style.display = "none";
      }
    }
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

    axios.get('nodes').then(function (result) {
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
    axios.get('board').then(response => {
        if (response.status >= 200 && response.status <= 300) document.getElementById("chooseOpponentModel").style.display = "none";
        else document.getElementById("chooseOpponentModel").style.display = "block";
    })
    .catch (error => {
        document.getElementById("chooseOpponentModel").style.display = "block";
    });

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

        axios.get('board').then(function (result) {

            if (typeof result.data !== 'string') {
                document.getElementById("chooseOpponentModel").style.display = "none";

                var array = Array.from(result.data);
                if (myBoard == null) resetPage();
                for (var i=0; i<array.length; i++) {
                    if (array[i] != myBoard[i]) {
                        resetPage();
                        myBoard = array;
                    }
                }

            }
            else if (document.getElementById("popUpModel").style.display != "block" && document.getElementById("chooseOpponentModel").style.display != "block") {
                // Game Over

                // TODO: display last board state

                axios.get('get-winner-text').then(function (result) {
                    popUp(result.data);
                })

                function runEndGameFlow() {
                    setTimeout(function() {

                        axios.post('end-game', {headers: {'Content-Type': 'application/json'}})
                        .then(response => {
                            console.log(response);
                        })
                        .catch(error => {
                            console.log(error.response);
                        });

                    }, 2000);
                }

                runEndGameFlow();
            }

        })
        .catch(error => {
            // TODO
        });
        setTimeout(check, 2000); // check again in 2 seconds
    }
    check();
}

// Resets the page to display board updates
function resetPage() {
    setIdentityLabel();
    setIsMyTurnLabel();
    var buttons = document.getElementsByClassName("tic");
    axios.get('board').then(function (result) {
        var array = Array.from(result.data);
        for (var i=0; i<array.length; i++) {
            if (array[i] == 'E') buttons[i].innerHTML = ' ';
            else buttons[i].innerHTML = array[i];
        }
        myBoard = array;
    });
}

function setIsMyTurnLabel() {
    axios.get('my-turn').then(function (result) {
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
    axios.get('you-are').then(function (result) {
        document.getElementById("h1").innerHTML = result.data;
    })
}

// Attempt to run SubmitTurnFlow with index i
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