const delay = t => new Promise(resolve => setTimeout(resolve, t));

var canvas = document.getElementById("canvas");
var ctx = canvas.getContext("2d");
ctx.font = "8px Arial";
var socket = null;
var stompClient = null;
var connected = false;

function connect() {
  showMessage("Connecting...");
  socket = new SockJS('http://localhost:8080/connect');
  stompClient = webstomp.over(socket);
  stompClient.connect({}, function (frame) {
    showMessage("Connected!");
    connected = true;
    stompClient.subscribe("/topic/snakes", function (message) {
      drawSnake(message.body);
    });
    stompClient.subscribe("/topic/police", function (message) {
      drawSnake(message.body);
    });
    stompClient.subscribe("/topic/news", function (message) {
      showMessage(message.body);
    });
    stompClient.subscribe("/topic/score", function (message) {
      showScore(message.body);
    });
  });

  socket.onclose = function(e) {
    socket = null;
    stompClient = null;
    if (connected) showMessage("Connection closed.");
    connected = false;
    delay(2500).then(() => connect());
  };
 
}

document.onkeydown = function(e) {
  switch (e.keyCode) {
      case 37: //Left
        stompClient.send("/inbound/controls", "left");
        break;
      case 38: //Up
        stompClient.send("/inbound/controls", "up");
        break;
      case 39: //Right
        stompClient.send("/inbound/controls", "right");
        break;
      case 40: //Down
        stompClient.send("/inbound/controls", "down");
        break;
      case 82: // 'r'
        stompClient.send("/inbound/controls", "reset");
        break;
      case 32: // whitespace
        stompClient.send("/inbound/controls", "pause");
        break;
  }
  // https://keycode.info/
};

function drawSnake(message) {
  let animal = JSON.parse(message);

  let oldAnimal = jQuery.data(document.body, String(animal.animalType + animal.number));
  if (oldAnimal) {
    ctx.clearRect(oldAnimal.x, oldAnimal.y, oldAnimal.size, oldAnimal.size);
  }
  
  ctx.fillStyle = "#59c500";
  if (animal.animalType === "POLICE"){
    ctx.fillStyle = "blue";
  }
  if (animal.animalType === "GOLD"){
    ctx.fillStyle = "gold";
  }
  if (animal.animalType === "PLAYER"){
    ctx.fillStyle = "purple";
  }
  if (animal.alive) {
    ctx.fillRect(animal.x, animal.y, animal.size, animal.size);
    jQuery.data(document.body, String(animal.animalType + animal.number), animal);
  }
}

function showMessage(message) {
  if (message === 'reset') {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    message = '* Restart Game *';
  }

  $("#news").append("<pre>" + message + "</pre>");
  if ($("#news pre").length > 6) {
    $("#news pre").first().remove();
  }
}

function showScore(message) {
  $("#title").html("Catch the gold - score: " + message);
}
