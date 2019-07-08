onLoad();

function onLoad() {

    var buttons = document.getElementsByClassName("tic");
    for(var i=0; i < buttons.length; i++) {
       let obj = i;
       buttons[i].addEventListener("click", function() {eventSubmitTurn(obj);}, false);
    }
    resetPage();
}

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
        console.log(error.response);
    });
}

function setIdentityLabel() {
    axios.get('you-are').then(function (result) {
        document.getElementById("h1").innerHTML = result.data;
    })
}

function eventSubmitTurn(i) {

    axios.post('submit-turn', i, {headers: {'Content-Type': 'application/json'}})
    .then(response => {
          	console.log(response);
          })
          .catch(error => {
              console.log(error.response);
          });

    // TODO wait???
    // TODO make other party reset
    setTimeout(function(){
        resetPage();
    }, 400);
}