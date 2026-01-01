Chat Client & Server

This project is a lightweight encrypted chat application that allows multiple users to connect to a server over a network and exchange messages securely. All communication is encrypted using AES with PBKDF2 key derivation.

Features:

Client–server architecture using sockets.
AES-encrypted messaging using the CryptoUtil class.
Multi-client support with broadcast messaging.
Works on localhost or LAN networks.
Cross-platform as long as Java is installed.

---------------

Encryption Overview:

This project uses the CryptoUtil.java  class to encrypt and decrypt all messages before they are transmitted.


Encryption details:

Algorithm: AES
Mode: CBC
Padding: PKCS5Padding
Key Derivation: PBKDF2WithHmacSHA256
Key Size: 256 bits
Iterations: 65536
IV: Random 16-byte IV prepended to ciphertext

This ensures confidentiality and prevents predictable ciphertext.

---------------

How to Run:

Start the Server

 	Compile and run:

 	javac ChatServer.java
 	java ChatServer <port>

 
2.  Start a Client

 	Compile and run:

 	javac ChatClient.java
 	java ChatClient <server-ip> <port>


3.  Adding Multiple Clients
 
 	To add multiple clients to the same server, simply run the ChatClient.java again.
 
 	(java ChatClient <server-ip> <port>)

---------------

How It Works:

The client connects to the server using a socket.

Before sending any message, the client encrypts it using CryptoUtil.encrypt().

The server receives the encrypted message and broadcasts it to all connected clients.

Each client decrypts incoming messages using CryptoUtil.decrypt().

---------------

Requirements:

Java 8 or later
Network or localhost environment
Shared password (hardcoded in CryptoUtil)

---------------

Security Notes:

The shared password and salt are currently hardcoded. For production use, consider:

Secure key exchange (such as Diffie-Hellman)
Environment variables
External configuration files not stored in version control

This project is intended for learning and local experimentation, not production-grade secure messaging.

---------------

License:

Java Chat Server © 2025 by The-R34per is licensed under Creative Commons Attribution-
NonCommercial-ShareAlike 4.0 International. To view a copy of this license, visit 
https://creativecommons.org/licenses/by-nc-sa/4.0/


AI Assistance Attribution:
Portions of this project were developed with help from Microsoft Copilot and ChatGPT.

---------------

Contributions:

Suggestions and improvements are welcome. Feel free to fork the project and build on it.

