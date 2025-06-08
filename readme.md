# 🔌 socketchat - simple lan chat application

**socketchat** is a multi-client console-based chat app designed for users on the same local network it is written in java and uses tcp sockets to establish secure real time communication between users via a central server in the same lan


## ⚙️ how to run

### server

```zsh
./gradlew clean build
./gradlew shadowJar
cd app/build/libs
java -jar app-all.jar server
```

### client

```zsh
./gradlew clean build
./gradlew shadowJar
cd app/build/libs
java -jar app-all.jar client
```

## client features

- command line based interface
- connects to server using ip scanning
- supports greeting keyword check
- auto reconnect loop if no valid server found
- username validation and feedback
- real time message sending
- message receive thread for incoming messages
- can change username during session
- can view connection details with /whoami
- supports graceful disconnection with /exit
- handles server disconnects properly

for more information run it and send a /help message


## server features

- admin console for live commands
- supports graceful shutdown with shutdown hook
- greeting keyword customization with /greeting
- client list with /users
- server info with /whoami
- thread per client architecture (client handler is a runnable)
- secure socket communication
- real time message broadcasting
- supports username change

for more information run it and send a /help message


## technologies i have used

- java jdk 21
- tcp sockets
- multi-threaded server (per client handler)
- gradle build tool
- jackson (json parsing)
- shadowJar plugin (to package dependencies)


## 👨‍💼 developer

* **name:**  ege cagan kantar
* **major:** computer engineering


## 📁 planned features

- whisper feature currently it is not implemented
- huge refactoring
- kick user
- sanitize the username and message characters to ascii characters
